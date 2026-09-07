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
  <header class="bg-white/95 backdrop-blur-xl sticky top-0 z-40 border-b border-slate-200/80 shadow-xs transition-colors">
    <div class="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 h-15 flex items-center justify-between gap-2 sm:gap-4">
      
      <!-- Brand & Title (Clean White/Indigo Theme) -->
      <div class="flex items-center gap-2.5 sm:gap-3 min-w-0 shrink-0">
        <!-- Rounded Squircle Bus App Icon -->
        <div class="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-indigo-50 border border-indigo-200/80 shadow-xs flex items-center justify-center shrink-0 text-indigo-600 hover:bg-indigo-100 hover:border-indigo-300 transition-all">
          <Bus class="w-4 h-4 sm:w-5 sm:h-5 text-indigo-600" />
        </div>

        <!-- Brand Title -->
        <div class="flex items-center gap-2 min-w-0">
          <span class="font-extrabold text-slate-900 text-sm sm:text-base tracking-tight whitespace-nowrap">
            서울 버스 도착 알림
          </span>
          
          <!-- Live Activity Badge -->
          <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-50 text-emerald-600 border border-emerald-200/80 shrink-0">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            LIVE
          </span>
        </div>
      </div>

      <!-- Toolbar & Actions -->
      <div class="flex items-center gap-1.5 sm:gap-2.5 shrink-0">
        
        <!-- Status Badges -->
        <div class="hidden md:flex items-center gap-2 text-xs">
          <!-- Imminent Arrival Alert -->
          <div
            v-if="store.imminentArrivalsCount > 0"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 font-bold transition-colors shadow-2xs"
          >
            <AlertTriangle class="w-3.5 h-3.5 text-rose-500 shrink-0 animate-bounce" />
            <span>도착 임박</span>
            <span class="ml-0.5 px-1.5 py-0.2 rounded-full bg-rose-200/80 text-rose-800 text-[11px]">
              {{ store.imminentArrivalsCount }}
            </span>
          </div>

          <!-- Total Buses Badge -->
          <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-100 border border-slate-200 text-slate-600 font-medium">
            <Bookmark class="w-3.5 h-3.5 text-indigo-500 shrink-0" />
            <span>등록</span>
            <span class="text-indigo-600 font-bold">{{ store.bookmarkCount }}</span>
          </div>
        </div>

        <!-- Segmented Refresh & Interval Toolbar -->
        <div class="inline-flex items-center rounded-xl border border-slate-200 bg-slate-50/90 divide-x divide-slate-200 text-xs shadow-2xs">
          <!-- Select Interval with Icon -->
          <label class="flex items-center gap-1.5 px-2.5 py-1.5 cursor-pointer text-slate-600 hover:text-slate-900 hover:bg-slate-100/80 transition-colors">
            <Clock class="w-3.5 h-3.5 text-indigo-500 shrink-0" />
            <select
              :value="store.refreshIntervalSec"
              @change="handleIntervalChange"
              class="bg-transparent text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer pr-1"
            >
              <option v-for="item in intervals" :key="item.value" :value="item.value" class="bg-white text-slate-800">
                {{ item.label }}
              </option>
            </select>
          </label>

          <!-- Monospace Countdown Timer -->
          <div
            v-if="store.refreshIntervalSec > 0"
            class="px-2.5 py-1.5 font-mono text-[11px] font-bold text-indigo-600 bg-indigo-50/60 flex items-center gap-1 select-none"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse"></span>
            <span>{{ store.secondsUntilNextRefresh }}s</span>
          </div>

          <!-- Instant Refresh Button -->
          <button
            @click="handleManualRefresh"
            :disabled="store.refreshing"
            title="즉시 새로고침"
            class="px-2.5 py-1.5 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 active:bg-slate-200 transition-colors disabled:opacity-30"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': store.refreshing }" />
          </button>
        </div>

        <!-- Account Widget -->
        <div>
          <!-- Google User Logged In -->
          <button
            v-if="authStore.isGoogleUser"
            @click="authStore.openModal()"
            class="flex items-center gap-2 px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 hover:border-slate-300 text-slate-800 shadow-2xs transition-all text-xs font-medium"
            title="계정 설정"
          >
            <img
              v-if="authStore.user?.picture"
              :src="authStore.user.picture"
              alt="Avatar"
              class="w-4 h-4 rounded-full object-cover border border-slate-200"
            />
            <div v-else class="w-4 h-4 rounded-full bg-indigo-600 text-white text-[10px] flex items-center justify-center font-bold">
              {{ authStore.user?.name?.charAt(0) || 'U' }}
            </div>
            <span class="max-w-[80px] sm:max-w-[110px] truncate text-slate-700 font-semibold text-xs">{{ authStore.user?.name }}</span>
          </button>

          <!-- Guest Mode -->
          <button
            v-else
            @click="authStore.openModal()"
            class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-indigo-50/80 hover:border-indigo-300 text-slate-700 hover:text-indigo-700 shadow-2xs transition-all text-xs font-semibold group"
            title="계정 연동"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>로그인</span>
            <ChevronRight class="w-3 h-3 text-slate-400 group-hover:text-indigo-600 transition-colors ml-0.5" />
          </button>
        </div>

      </div>

    </div>
  </header>
</template>



