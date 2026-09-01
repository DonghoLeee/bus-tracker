<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { Bus, RefreshCw, Clock, Bell, AlertTriangle } from 'lucide-vue-next';

const store = useBookmarkStore();

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
  <header class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white shadow-lg sticky top-0 z-40 border-b border-indigo-900/50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 flex flex-col md:flex-row items-center justify-between gap-4">
      
      <!-- Brand Logo & Title -->
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-indigo-500/20 border border-indigo-400/30 flex items-center justify-center text-indigo-400 shadow-inner">
          <Bus class="w-6 h-6" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-xl font-bold tracking-tight text-white flex items-center gap-1.5">
              실시간 버스 도착 알리미
            </h1>
            <span class="text-xs px-2 py-0.5 rounded-full bg-indigo-500/30 text-indigo-300 border border-indigo-400/20 font-medium">
              LIVE
            </span>
          </div>
          <p class="text-xs text-slate-400">정류장 검색 & 관심 버스 실시간 도착 모니터링</p>
        </div>
      </div>

      <!-- Live Dashboard Stats & Controls -->
      <div class="flex flex-wrap items-center gap-3 sm:gap-4 text-xs sm:text-sm">
        
        <!-- Imminent Alert Badge -->
        <div v-if="store.imminentArrivalsCount > 0" class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-500/20 text-amber-300 border border-amber-500/40 animate-pulse font-medium">
          <AlertTriangle class="w-4 h-4 text-amber-400" />
          <span>도착 임박: {{ store.imminentArrivalsCount }}대</span>
        </div>

        <!-- Total Bookmarks Count -->
        <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800/80 text-slate-300 border border-slate-700 font-medium">
          <Bell class="w-4 h-4 text-indigo-400" />
          <span>등록된 버스: <strong class="text-white">{{ store.bookmarkCount }}</strong>대</span>
        </div>

        <!-- Auto Refresh Settings & Manual Button -->
        <div class="flex items-center gap-2 bg-slate-800/90 rounded-lg p-1 border border-slate-700">
          <div class="flex items-center gap-1.5 px-2 py-1 text-slate-300">
            <Clock class="w-3.5 h-3.5 text-slate-400" />
            <select
              :value="store.refreshIntervalSec"
              @change="handleIntervalChange"
              class="bg-transparent text-white font-medium focus:outline-none cursor-pointer text-xs"
            >
              <option v-for="item in intervals" :key="item.value" :value="item.value" class="bg-slate-800 text-white">
                {{ item.label }}
              </option>
            </select>
          </div>

          <div v-if="store.refreshIntervalSec > 0" class="text-xs text-indigo-300 px-1 font-mono">
            {{ store.secondsUntilNextRefresh }}s
          </div>

          <button
            @click="handleManualRefresh"
            :disabled="store.refreshing"
            title="즉시 새로고침"
            class="p-1.5 hover:bg-indigo-600/30 text-indigo-300 hover:text-white rounded-md transition-colors disabled:opacity-50"
          >
            <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': store.refreshing }" />
          </button>
        </div>

      </div>

    </div>
  </header>
</template>
