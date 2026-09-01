<script setup>
import { ref, watch } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { busApi } from '../api/busApi';
import { Search, MapPin, ChevronRight, X, Sparkles } from 'lucide-vue-next';

const store = useBookmarkStore();

const query = ref('');
const searchResults = ref([]);
const isSearching = ref(false);
const showResults = ref(false);
let debounceTimer = null;

const popularHotspots = [
  '강남역',
  '광화문',
  '홍대입구역',
  '여의도환승센터',
  '판교역',
  '수원역',
  '잠실역',
  '신촌오거리',
  '사당역',
  '인천공항'
];

const performSearch = async (keyword) => {
  if (!keyword || keyword.trim().length === 0) {
    searchResults.value = [];
    isSearching.value = false;
    return;
  }

  isSearching.value = true;
  try {
    const res = await busApi.searchStations(keyword.trim());
    searchResults.value = res.data;
    showResults.value = true;
  } catch (error) {
    console.error('검색 실패:', error);
    searchResults.value = [];
  } finally {
    isSearching.value = false;
  }
};

watch(query, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer);
  if (!newVal || newVal.trim() === '') {
    searchResults.value = [];
    showResults.value = false;
    return;
  }
  debounceTimer = setTimeout(() => {
    performSearch(newVal);
  }, 250);
});

const handleSelectStation = (station) => {
  store.selectStation(station);
  showResults.value = false;
};

const handleHotspotClick = (name) => {
  query.value = name;
  performSearch(name);
};

const clearInput = () => {
  query.value = '';
  searchResults.value = [];
  showResults.value = false;
};
</script>

<template>
  <div class="relative w-full max-w-4xl mx-auto mb-8">
    
    <!-- Search Input Bar -->
    <div class="relative flex items-center bg-white rounded-2xl shadow-md border border-slate-200 hover:border-indigo-400 focus-within:border-indigo-500 focus-within:ring-4 focus-within:ring-indigo-100 transition-all">
      <div class="pl-4.5 pr-2 text-slate-400">
        <Search class="w-5 h-5" :class="{ 'animate-pulse text-indigo-500': isSearching }" />
      </div>

      <input
        v-model="query"
        type="text"
        placeholder="버스 정류장 이름 또는 번호(ARS-ID)를 검색하세요 (예: 강남역, 22011, 광화문)"
        class="w-full py-4 px-2 text-slate-800 placeholder-slate-400 bg-transparent focus:outline-none text-base sm:text-lg"
        @focus="if (searchResults.length > 0) showResults = true;"
      />

      <button
        v-if="query"
        @click="clearInput"
        class="p-2 mr-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors"
      >
        <X class="w-5 h-5" />
      </button>
    </div>

    <!-- Quick Hotspot Chips -->
    <div class="mt-3 flex items-center gap-1.5 flex-wrap px-1">
      <span class="text-xs font-semibold text-slate-500 flex items-center gap-1 mr-1">
        <Sparkles class="w-3.5 h-3.5 text-indigo-500" />
        추천 정류장:
      </span>
      <button
        v-for="spot in popularHotspots"
        :key="spot"
        @click="handleHotspotClick(spot)"
        class="text-xs px-2.5 py-1 rounded-full bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-600 border border-slate-200/80 transition-all"
      >
        {{ spot }}
      </button>
    </div>

    <!-- Search Results Dropdown Overlay -->
    <div
      v-if="showResults && searchResults.length > 0"
      class="absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl shadow-2xl border border-slate-100 z-50 max-h-96 overflow-y-auto divide-y divide-slate-100"
    >
      <div class="px-4 py-2 bg-slate-50 text-xs font-semibold text-slate-500 flex items-center justify-between">
        <span>검색 결과 ({{ searchResults.length }}건)</span>
        <button @click="showResults = false" class="text-slate-400 hover:text-slate-600">닫기</button>
      </div>

      <button
        v-for="station in searchResults"
        :key="station.stationId"
        @click="handleSelectStation(station)"
        class="w-full text-left px-5 py-3.5 hover:bg-indigo-50/70 transition-colors flex items-center justify-between group"
      >
        <div class="flex items-start gap-3">
          <div class="mt-1 w-7 h-7 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center flex-shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
            <MapPin class="w-4 h-4" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-bold text-slate-900 text-base group-hover:text-indigo-700">{{ station.stationName }}</span>
              <span v-if="station.arsId" class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 font-mono">
                {{ station.arsId }}
              </span>
            </div>
            <p class="text-xs text-slate-500 mt-0.5">
              {{ station.cityName }} · <span class="text-slate-600">{{ station.nextStationName || '방면 정보' }}</span>
            </p>
          </div>
        </div>

        <div class="flex items-center text-slate-400 group-hover:text-indigo-600 text-xs font-medium gap-1">
          <span>버스 보기</span>
          <ChevronRight class="w-4 h-4" />
        </div>
      </button>
    </div>

    <!-- Empty Results -->
    <div
      v-else-if="showResults && !isSearching && query && searchResults.length === 0"
      class="absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl shadow-xl border border-slate-100 z-50 p-6 text-center text-slate-500"
    >
      <p class="text-sm">"{{ query }}"에 대한 검색 결과가 없습니다.</p>
      <p class="text-xs text-slate-400 mt-1">정류장 이름이나 ARS 고유번호를 다시 확인해보세요.</p>
    </div>

  </div>
</template>
