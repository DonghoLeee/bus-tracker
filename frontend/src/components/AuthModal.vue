<script setup>
import { ref, onMounted, watch } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { X, Smartphone, ShieldCheck, Sparkles, CheckCircle2, LogOut, ArrowRight, UserCheck } from 'lucide-vue-next';

const authStore = useAuthStore();
const googleButtonRef = ref(null);

// Google OAuth Client ID (Vite 환경변수 또는 데모 기본값)
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

const initGoogleSignIn = () => {
  if (typeof window === 'undefined' || !GOOGLE_CLIENT_ID) return;

  const tryRender = (retries = 10) => {
    if (window.google?.accounts?.id && googleButtonRef.value) {
      try {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleGoogleCredentialResponse,
        });
        window.google.accounts.id.renderButton(googleButtonRef.value, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          width: 320,
        });
      } catch (e) {
        console.warn('Google GSI 초기화 실패:', e);
      }
    } else if (retries > 0) {
      setTimeout(() => tryRender(retries - 1), 200);
    }
  };

  tryRender();
};

const handleGoogleCredentialResponse = (response) => {
  if (response.credential) {
    authStore.loginWithGoogle(response.credential);
  }
};

const base64UrlEncode = (obj) => {
  const json = JSON.stringify(obj);
  const bytes = new TextEncoder().encode(json);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
};

// Google Client ID가 아직 설정되지 않은 환경에서도 즉시 원클릭으로 작동하는 구글 로그인
const handleDemoGoogleLogin = () => {
  const header = base64UrlEncode({ alg: "HS256", typ: "JWT" });
  const payload = base64UrlEncode({
    sub: "google-user-account-12345",
    email: "user@gmail.com",
    name: "구글 사용자",
    picture: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80"
  });
  const dummyJwt = `${header}.${payload}.mock_signature`;
  authStore.loginWithGoogle(dummyJwt);
};

onMounted(() => {
  initGoogleSignIn();
});

watch(() => authStore.showAuthModal, (open) => {
  if (open) {
    setTimeout(initGoogleSignIn, 100);
  }
});
</script>

<template>
  <div
    v-if="authStore.showAuthModal"
    class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6 animate-fadeIn"
  >
    <div class="bg-white rounded-3xl shadow-2xl border border-slate-100 w-full max-w-md overflow-hidden animate-scaleUp">
      
      <!-- Top Accent Banner -->
      <div class="relative bg-gradient-to-br from-indigo-600 via-indigo-700 to-indigo-900 text-white p-7 text-center overflow-hidden">
        <div class="absolute -right-12 -top-12 w-36 h-36 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
        <div class="absolute -left-12 -bottom-12 w-36 h-36 bg-indigo-400/20 rounded-full blur-2xl pointer-events-none"></div>

        <button
          @click="authStore.closeModal()"
          class="absolute top-4 right-4 p-2 text-white/70 hover:text-white rounded-full hover:bg-white/10 transition-colors"
          title="닫기"
        >
          <X class="w-5 h-5" />
        </button>

        <div class="w-14 h-14 mx-auto rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center mb-3.5 shadow-inner border border-white/20">
          <Sparkles class="w-7 h-7 text-amber-300" />
        </div>

        <h2 class="text-xl sm:text-2xl font-bold tracking-tight">
          {{ authStore.isGoogleUser ? '내 계정 정보' : '실시간 버스 즐겨찾기' }}
        </h2>
        <p class="text-xs sm:text-sm text-indigo-100 mt-1.5 leading-relaxed">
          {{ authStore.isGoogleUser 
              ? '구글 계정과 즐겨찾기가 안전하게 동기화 중입니다'
              : '어떤 기기에서도 나만의 버스 알림을 유지해 보세요' }}
        </p>
      </div>

      <!-- Modal Body -->
      <div class="p-6">
        
        <!-- ALREADY GOOGLE LOGGED IN STATE -->
        <div v-if="authStore.isGoogleUser" class="space-y-5">
          <div class="p-4 rounded-2xl bg-indigo-50/70 border border-indigo-100 flex items-center gap-3.5">
            <img
              v-if="authStore.user?.picture"
              :src="authStore.user.picture"
              alt="Profile"
              class="w-12 h-12 rounded-full border-2 border-white shadow-sm object-cover"
            />
            <div v-else class="w-12 h-12 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold text-lg shadow-sm">
              {{ authStore.user?.name?.charAt(0) || 'U' }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="font-bold text-slate-800 text-base truncate">{{ authStore.user?.name || '구글 연동 사용자' }}</span>
                <span class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-700">Google 연동됨</span>
              </div>
              <p v-if="authStore.user?.email" class="text-xs text-slate-500 truncate mt-0.5">{{ authStore.user?.email }}</p>
              <p v-else class="text-xs text-emerald-600 font-medium truncate mt-0.5">개인정보 비공개 (익명 고유번호로 동기화 중)</p>
            </div>
          </div>

          <div class="space-y-2 text-xs text-slate-600 bg-slate-50 p-3.5 rounded-xl border border-slate-100">
            <div class="flex items-center gap-2 text-emerald-700 font-medium">
              <CheckCircle2 class="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>모든 기기에서 즐겨찾기가 자동 동기화됩니다.</span>
            </div>
            <div class="flex items-center gap-2 text-slate-500">
              <CheckCircle2 class="w-4 h-4 text-slate-400 flex-shrink-0" />
              <span>캐시를 비우거나 재접속해도 데이터가 안전합니다.</span>
            </div>
          </div>

          <div class="pt-2 flex items-center gap-3">
            <button
              @click="authStore.closeModal()"
              class="flex-1 py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm transition-colors text-center"
            >
              계속 이용하기
            </button>
            <button
              @click="authStore.logout()"
              class="py-3 px-4 rounded-xl border border-slate-200 hover:bg-rose-50 hover:border-rose-200 hover:text-rose-600 text-slate-600 font-medium text-sm transition-colors flex items-center justify-center gap-1.5"
            >
              <LogOut class="w-4 h-4" />
              <span>로그아웃</span>
            </button>
          </div>
        </div>

        <!-- NOT LOGGED IN / GUEST STATE -->
        <div v-else class="space-y-5">
          
          <!-- Benefit highlights -->
          <div class="space-y-2.5 text-xs text-slate-600">
            <div class="flex items-start gap-2.5">
              <div class="p-1 rounded-md bg-indigo-100 text-indigo-600 mt-0.5 flex-shrink-0">
                <CheckCircle2 class="w-3.5 h-3.5" />
              </div>
              <p><strong>기기 변경 시에도 복원</strong>: 스마트폰, 태블릿, PC 어디서나 내 버스 즐겨찾기가 유지됩니다.</p>
            </div>
            <div class="flex items-start gap-2.5">
              <div class="p-1 rounded-md bg-indigo-100 text-indigo-600 mt-0.5 flex-shrink-0">
                <ShieldCheck class="w-3.5 h-3.5" />
              </div>
              <p><strong>안전한 클라우드 저장</strong>: 브라우저 캐시 삭제 시에도 즐겨찾기를 잃어버리지 않습니다.</p>
            </div>
          </div>

          <!-- Google Login Action -->
          <div class="pt-2 flex flex-col items-center justify-center">
            <div v-if="GOOGLE_CLIENT_ID" ref="googleButtonRef" class="w-full flex justify-center my-1 min-h-[44px]"></div>

            <button
              v-else
              @click="handleDemoGoogleLogin"
              class="w-full py-3.5 px-4 rounded-2xl border border-slate-200 hover:border-slate-300 hover:shadow-md bg-white text-slate-700 font-bold text-sm transition-all flex items-center justify-center gap-3 group active:scale-98"
            >
              <!-- Google G Logo SVG -->
              <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
              </svg>
              <span>Google 계정으로 시작하기 (체험 모드)</span>
            </button>
          </div>

          <div class="relative flex items-center justify-center my-3">
            <div class="border-t border-slate-200 w-full"></div>
            <span class="bg-white px-3 text-slate-400 text-xs font-medium">또는</span>
            <div class="border-t border-slate-200 w-full"></div>
          </div>

          <!-- Guest Mode Action -->
          <div>
            <button
              @click="authStore.continueAsGuest()"
              class="w-full py-3 px-4 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm transition-all flex items-center justify-center gap-2 group active:scale-98"
            >
              <Smartphone class="w-4 h-4 text-slate-500 group-hover:text-slate-700" />
              <span>로그인 없이 바로 시작하기</span>
              <ArrowRight class="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 transition-transform" />
            </button>
            <p class="text-[11px] text-slate-400 text-center mt-2">
              (기기 고유 식별자로 현재 브라우저에 즐겨찾기가 보관됩니다)
            </p>
          </div>

        </div>

      </div>

    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
.animate-fadeIn {
  animation: fadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.animate-scaleUp {
  animation: scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>
