<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { useAuthStore } from '../stores/authStore';
import { Bus, RefreshCw, Clock, AlertTriangle, Bookmark, ChevronRight } from 'lucide-vue-next';

const store = useBookmarkStore();
const authStore = useAuthStore();

const intervals = [
  { label: '15초', value: 15 },
  { label: '30초', value: 30 },
  { label: '60초', value: 60 },
  { label: '정지', value: 0 },
];

const formattedLastUpdate = computed(() => {
  if (!store.lastUpdated) return '없음';
  const d = new Date(store.lastUpdated);
  return d.toLocaleTimeString('ko-KR', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
});

const handleIntervalChange = (e) => {
  store.setRefreshInterval(Number(e.target.value));
};

const handleManualRefresh = () => {
  store.refreshArrivalsSilently();
};
</script>

<template>
  <header class="bg-gradient-to-r from-slate-900 via-[#111827] to-indigo-950/90 text-white backdrop-blur-xl sticky top-0 z-40 border-b border-indigo-500/20 shadow-md transition-colors w-full max-w-full">
    <div class="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 h-14 sm:h-15 flex items-center justify-between gap-2 sm:gap-4">
      
      <!-- Brand & Title (Dark Navy / Indigo Theme) -->
      <div class="flex items-center gap-2 sm:gap-3 min-w-0 shrink-0">
        <!-- Rounded Squircle Bus App Icon -->
        <div class="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-indigo-500/20 border border-indigo-400/30 shadow-xs flex items-center justify-center shrink-0 text-indigo-300 hover:bg-indigo-500/30 hover:border-indigo-300 transition-all">
          <Bus class="w-4 h-4 sm:w-5 sm:h-5 text-indigo-300" />
        </div>

        <!-- Brand Title -->
        <div class="flex items-center gap-1.5 sm:gap-2 min-w-0">
          <span class="font-extrabold text-white text-sm sm:text-base tracking-tight whitespace-nowrap">
            서울 버스<span class="hidden sm:inline"> 도착 알림</span>
          </span>
          
          <!-- Live Activity Badge -->
          <span class="inline-flex items-center gap-1 sm:gap-1.5 px-1.5 sm:px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-950/70 text-emerald-400 border border-emerald-500/30 shrink-0">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            LIVE
          </span>
        </div>
      </div>

      <!-- Toolbar & Actions -->
      <div class="flex items-center gap-1.5 sm:gap-2.5 shrink-0">
        
        <!-- Status Badges (Desktop) -->
        <div class="hidden md:flex items-center gap-2 text-xs">
          <!-- Imminent Arrival Alert -->
          <div
            v-if="store.imminentArrivalsCount > 0"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-rose-950/60 border border-rose-500/30 text-rose-300 font-bold transition-colors shadow-2xs"
          >
            <AlertTriangle class="w-3.5 h-3.5 text-rose-400 shrink-0 animate-bounce" />
            <span>도착 임박</span>
            <span class="ml-0.5 px-1.5 py-0.2 rounded-full bg-rose-900/80 text-rose-200 text-[11px]">
              {{ store.imminentArrivalsCount }}
            </span>
          </div>

          <!-- Total Buses Badge -->
          <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-800/80 border border-slate-700/80 text-slate-300 font-medium">
            <Bookmark class="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <span>등록</span>
            <span class="text-indigo-300 font-bold">{{ store.bookmarkCount }}</span>
          </div>
        </div>

        <!-- Segmented Refresh & Interval Toolbar -->
        <div class="inline-flex items-center rounded-xl border border-slate-700/80 bg-slate-800/90 divide-x divide-slate-700/80 text-xs shadow-2xs shrink-0">
          <!-- Select Interval with Icon (Hidden on mobile to ensure clean fit) -->
          <label class="hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 cursor-pointer text-slate-300 hover:text-white hover:bg-slate-700/50 transition-colors">
            <Clock class="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <select
              :value="store.refreshIntervalSec"
              @change="handleIntervalChange"
              class="bg-transparent text-xs font-semibold text-slate-200 focus:outline-none cursor-pointer pr-1"
            >
              <option v-for="item in intervals" :key="item.value" :value="item.value" class="bg-slate-900 text-slate-200">
                {{ item.label }}
              </option>
            </select>
          </label>

          <!-- Monospace Countdown Timer -->
          <div
            v-if="store.refreshIntervalSec > 0"
            class="px-2 sm:px-2.5 py-1.5 font-mono text-[11px] font-bold text-indigo-300 bg-indigo-950/60 flex items-center gap-1 select-none"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-pulse"></span>
            <span>{{ store.secondsUntilNextRefresh }}s</span>
          </div>

          <!-- Instant Refresh Button -->
          <button
            @click="handleManualRefresh"
            :disabled="store.refreshing"
            title="즉시 새로고침"
            class="px-2 sm:px-2.5 py-1.5 text-slate-300 hover:text-indigo-300 hover:bg-slate-700/50 active:bg-slate-700 transition-colors disabled:opacity-30"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': store.refreshing }" />
          </button>
        </div>

        <!-- Account Widget -->
        <div class="shrink-0">
          <!-- Google User Logged In -->
          <button
            v-if="authStore.isGoogleUser"
            @click="authStore.openModal()"
            class="flex items-center gap-1.5 sm:gap-2 px-2.5 py-1.5 rounded-xl border border-slate-700 bg-slate-800/90 hover:bg-slate-700 hover:border-slate-600 text-slate-200 shadow-2xs transition-all text-xs font-medium shrink-0"
            title="계정 설정"
          >
            <img
              v-if="authStore.user?.picture"
              :src="authStore.user.picture"
              alt="Avatar"
              class="w-4 h-4 rounded-full object-cover border border-slate-600"
            />
            <div v-else class="w-4 h-4 rounded-full bg-indigo-600 text-white text-[10px] flex items-center justify-center font-bold">
              {{ authStore.user?.name?.charAt(0) || 'U' }}
            </div>
            <span class="max-w-[70px] sm:max-w-[110px] truncate text-slate-200 font-semibold text-xs">{{ authStore.user?.name }}</span>
          </button>

          <!-- Guest Mode -->
          <button
            v-else
            @click="authStore.openModal()"
            class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border border-indigo-500/40 bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-200 hover:text-white shadow-2xs transition-all text-xs font-semibold group shrink-0"
            title="계정 연동"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 shrink-0"></span>
            <span class="whitespace-nowrap">로그인</span>
            <ChevronRight class="w-3 h-3 text-indigo-400 group-hover:text-white transition-colors shrink-0" />
          </button>
        </div>

      </div>

    </div>
  </header>
</template>



