<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { useAuthStore } from '../stores/authStore';
import { Bus, RefreshCw, Clock, Bell, AlertTriangle } from 'lucide-vue-next';

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
  <header class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white shadow-lg sticky top-0 z-40 border-b border-indigo-900/50 backdrop-blur-md">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 sm:py-3.5 flex flex-col md:flex-row items-center justify-between gap-3 sm:gap-4">
      
      <!-- Brand Logo (Custom Trendy Seoul Bus Emblem) & Unified Tagline -->
      <div class="flex items-center gap-3.5">
        <!-- Iconized Trendy Seoul Bus App Icon -->
        <div class="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-sky-400 p-1.5 shadow-md shadow-blue-500/30 flex items-center justify-center shrink-0 border border-white/25 group/logo hover:scale-105 active:scale-95 transition-all duration-200">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 drop-shadow-xs">
            <!-- Tires -->
            <rect x="6" y="25" width="4" height="2.5" rx="1.25" fill="#0f172a"/>
            <rect x="22" y="25" width="4" height="2.5" rx="1.25" fill="#0f172a"/>

            <!-- Main Bus Body (Crisp Modern White) -->
            <rect x="4" y="3.5" width="24" height="23" rx="5.5" fill="white"/>

            <!-- Top Destination Bar (Seoul Blue Pill) -->
            <rect x="8.5" y="6" width="15" height="2.5" rx="1.25" fill="#2563eb"/>

            <!-- Panoramic Windshield Glass -->
            <rect x="6.5" y="10.5" width="19" height="7.5" rx="2" fill="#0f172a"/>
            <!-- Subtle Glass Reflection -->
            <path d="M7 11H13L10 17.5H7V11Z" fill="white" fill-opacity="0.25"/>

            <!-- Dual Headlights (Warm Glowing Lights) -->
            <circle cx="8.5" cy="21.5" r="1.75" fill="#fbbf24"/>
            <circle cx="23.5" cy="21.5" r="1.75" fill="#fbbf24"/>

            <!-- Seoul 4 Transit Color Dots (파·초·빨·노: 간선·지선·광역·순환) -->
            <circle cx="13" cy="21.5" r="1.15" fill="#2563eb"/>
            <circle cx="15.7" cy="21.5" r="1.15" fill="#16a34a"/>
            <circle cx="18.4" cy="21.5" r="1.15" fill="#dc2626"/>
            <circle cx="21.1" cy="21.5" r="1.15" fill="#eab308"/>
          </svg>
        </div>
        <div>
          <div class="flex items-center gap-2">
            <span class="text-lg font-black tracking-tight text-white flex items-center gap-1.5">
              <span class="text-indigo-400">서울</span> 버스 실시간 알리미
            </span>
            <span class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow-2xs">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
              LIVE
            </span>
          </div>
          <p class="text-xs text-slate-300 font-medium tracking-tight mt-0.5">
            서울 시내·마을버스 도착 정보, <span class="text-indigo-300 font-semibold">실시간으로 한눈에</span>
          </p>
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

        <!-- User Profile / Google Login Action -->
        <div class="flex items-center">
          <!-- Google Logged In -->
          <button
            v-if="authStore.isGoogleUser"
            @click="authStore.openModal()"
            class="flex items-center gap-2 py-1 px-2.5 rounded-lg bg-indigo-600/30 hover:bg-indigo-600/50 border border-indigo-400/40 text-white transition-all text-xs font-semibold shadow-xs"
            title="내 계정 관리"
          >
            <img
              v-if="authStore.user?.picture"
              :src="authStore.user.picture"
              alt="Avatar"
              class="w-5 h-5 rounded-full object-cover border border-white/40"
            />
            <div v-else class="w-5 h-5 rounded-full bg-indigo-500 text-white text-[10px] flex items-center justify-center font-bold">
              {{ authStore.user?.name?.charAt(0) || 'G' }}
            </div>
            <span class="max-w-[100px] truncate">{{ authStore.user?.name }}</span>
          </button>

          <!-- Guest Mode -->
          <button
            v-else
            @click="authStore.openModal()"
            class="flex items-center gap-1.5 py-1.5 px-3 rounded-lg bg-slate-800 hover:bg-indigo-600/30 hover:border-indigo-400/50 border border-slate-700 text-slate-300 hover:text-white transition-all text-xs font-medium"
            title="구글 로그인으로 즐겨찾기 영구 보관"
          >
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>기기 저장됨</span>
            <span class="text-indigo-400 font-semibold ml-0.5 hover:underline">· 로그인</span>
          </button>
        </div>

      </div>

    </div>
  </header>
</template>
