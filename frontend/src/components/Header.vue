<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { useAuthStore } from '../stores/authStore';
import { Bus, RefreshCw, Clock, AlertTriangle, Bookmark, ChevronRight } from 'lucide-vue-next';

const store = useBookmarkStore();
const authStore = useAuthStore();

const intervals = [
  { label: '15s', value: 15 },
  { label: '30s', value: 30 },
  { label: '60s', value: 60 },
  { label: 'Off', value: 0 },
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
  <header class="bg-zinc-950/80 backdrop-blur-md sticky top-0 z-40 border-b border-zinc-800/80 transition-colors">
    <div class="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 h-14 flex items-center justify-between gap-2 sm:gap-4">
      
      <!-- Linear-style Monochromatic Brand & Title -->
      <div class="flex items-center gap-2 sm:gap-2.5 min-w-0 shrink-0">
        <!-- Sharp Geometric Monogram Icon -->
        <div class="w-7 h-7 rounded-md bg-zinc-900 border border-zinc-700/80 shadow-xs flex items-center justify-center shrink-0 text-zinc-100 hover:border-zinc-500 transition-colors">
          <Bus class="w-4 h-4 text-zinc-200" />
        </div>

        <!-- Clean Brand Title (Never awkwardly line-breaks) -->
        <div class="flex items-center gap-1.5 sm:gap-2 min-w-0">
          <span class="font-bold text-zinc-100 text-xs sm:text-sm tracking-tight whitespace-nowrap">
            서울 버스 도착 알림
          </span>
          
          <!-- Micro Status Badge -->
          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shrink-0">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            LIVE
          </span>
        </div>
      </div>

      <!-- Linear / Vercel Minimalist Toolbar & Widgets -->
      <div class="flex items-center gap-1.5 sm:gap-2.5 shrink-0">
        
        <!-- Status Badges -->
        <div class="hidden md:flex items-center gap-2 font-mono text-xs">
          <!-- Imminent Arrival Alert -->
          <div
            v-if="store.imminentArrivalsCount > 0"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-amber-500/10 border border-amber-500/25 text-amber-300 font-medium transition-colors"
          >
            <AlertTriangle class="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <span>도착 임박</span>
            <span class="ml-0.5 px-1 py-0.2 rounded bg-amber-500/20 text-amber-200 font-semibold text-[11px]">
              {{ store.imminentArrivalsCount }}
            </span>
          </div>

          <!-- Total Buses Badge -->
          <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-zinc-900/60 border border-zinc-800 text-zinc-400">
            <Bookmark class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
            <span>등록</span>
            <span class="text-zinc-200 font-semibold">{{ store.bookmarkCount }}</span>
          </div>
        </div>

        <!-- Linear-style Segmented Refresh & Interval Toolbar -->
        <div class="inline-flex items-center rounded-md border border-zinc-800 bg-zinc-900/70 divide-x divide-zinc-800/90 text-xs shadow-2xs">
          <!-- Select Interval with Icon -->
          <label class="flex items-center gap-1.5 px-2 py-1 cursor-pointer text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/40 transition-colors">
            <Clock class="w-3.5 h-3.5 text-zinc-500" />
            <select
              :value="store.refreshIntervalSec"
              @change="handleIntervalChange"
              class="bg-transparent text-xs font-mono text-zinc-300 focus:outline-none cursor-pointer pr-1"
            >
              <option v-for="item in intervals" :key="item.value" :value="item.value" class="bg-zinc-900 text-zinc-100">
                {{ item.label }}
              </option>
            </select>
          </label>

          <!-- Monospace Countdown Timer -->
          <div
            v-if="store.refreshIntervalSec > 0"
            class="px-2 py-1 font-mono text-[11px] text-zinc-400 bg-zinc-950/40 flex items-center gap-1 select-none"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-indigo-400/80"></span>
            <span>{{ store.secondsUntilNextRefresh }}s</span>
          </div>

          <!-- Instant Refresh Button -->
          <button
            @click="handleManualRefresh"
            :disabled="store.refreshing"
            title="즉시 새로고침"
            class="px-2 py-1.5 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/60 active:bg-zinc-800 transition-colors disabled:opacity-30"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': store.refreshing }" />
          </button>
        </div>

        <!-- Linear-style Account Widget -->
        <div>
          <!-- Google User Logged In -->
          <button
            v-if="authStore.isGoogleUser"
            @click="authStore.openModal()"
            class="flex items-center gap-2 px-2.5 py-1 rounded-md border border-zinc-800 bg-zinc-900/60 hover:bg-zinc-800/60 hover:border-zinc-700 text-zinc-200 transition-all text-xs font-medium"
            title="계정 설정"
          >
            <img
              v-if="authStore.user?.picture"
              :src="authStore.user.picture"
              alt="Avatar"
              class="w-4 h-4 rounded object-cover border border-zinc-700"
            />
            <div v-else class="w-4 h-4 rounded bg-zinc-700 text-zinc-200 text-[10px] flex items-center justify-center font-bold">
              {{ authStore.user?.name?.charAt(0) || 'U' }}
            </div>
            <span class="max-w-[80px] sm:max-w-[110px] truncate text-zinc-300 font-mono text-xs">{{ authStore.user?.name }}</span>
          </button>

          <!-- Guest Mode -->
          <button
            v-else
            @click="authStore.openModal()"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-md border border-zinc-800 bg-zinc-900/60 hover:bg-zinc-800/60 hover:border-zinc-700 text-zinc-300 hover:text-zinc-100 transition-all text-xs font-mono group"
            title="계정 연동"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
            <span>로그인</span>
            <ChevronRight class="w-3 h-3 text-zinc-500 group-hover:text-zinc-300 transition-colors ml-0.5" />
          </button>
        </div>

      </div>

    </div>
  </header>
</template>


