<script setup>
import { ref, watch, onUnmounted } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { busApi } from '../api/busApi';
import StationMapModal from './StationMapModal.vue';
import BusLoading from './BusLoading.vue';
import { Search, MapPin, Map, Navigation, ChevronRight, X, Sparkles, AlertTriangle, Loader2 } from 'lucide-vue-next';

const store = useBookmarkStore();

const PAGE_SIZE = 10;

const query          = ref('');
const searchResults  = ref([]);
const isSearching    = ref(false);
const isLoadingMore  = ref(false);
const showResults    = ref(false);
const searchError    = ref('');
const currentPage    = ref(0);
const hasMore        = ref(false);
const totalCount     = ref(0);

// 카카오맵 모달 상태
const mapStation = ref(null);
const isMapModalOpen = ref(false);

const handleOpenMap = (station) => {
  mapStation.value = station;
  isMapModalOpen.value = true;
};

const handleCloseMap = () => {
  isMapModalOpen.value = false;
  mapStation.value = null;
};

// 현재 검색 키워드 (페이지 로드 시 일치 여부 확인용)
let activeKeyword = '';
let debounceTimer = null;

// Intersection Observer (무한스크롤 트리거)
let observer = null;
const sentinelRef = ref(null);

const popularHotspots = [
  '강남역', '잠실역', '사당역', '서울역',
];

const setupObserver = () => {
  if (observer) observer.disconnect();
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && hasMore.value && !isLoadingMore.value && !isSearching.value) {
        loadMore();
      }
    },
    { threshold: 0.1 }
  );
};

const observeSentinel = () => {
  if (sentinelRef.value && observer) {
    observer.observe(sentinelRef.value);
  }
};

// 첫 페이지 검색
const performSearch = async (keyword) => {
  if (!keyword || keyword.trim().length === 0) {
    searchResults.value = [];
    searchError.value = '';
    isSearching.value = false;
    showResults.value = false;
    return;
  }

  activeKeyword = keyword.trim();
  currentPage.value = 0;
  searchResults.value = [];
  searchError.value = '';
  isSearching.value = true;
  showResults.value = true;

  try {
    const res = await busApi.searchStations(activeKeyword, 0, PAGE_SIZE);
    // 키워드가 바뀌었으면 무시
    if (activeKeyword !== keyword.trim()) return;

    const body = res.data;
    searchResults.value = body.data ?? [];
    totalCount.value    = body.total ?? 0;
    hasMore.value       = body.hasMore ?? false;
    currentPage.value   = 0;

    // 다음 틱에 observer 연결
    setTimeout(observeSentinel, 50);
  } catch (err) {
    if (activeKeyword !== keyword.trim()) return;
    const msg = err.response?.data?.error || '정류소 검색 API에 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
    searchError.value   = msg;
    searchResults.value = [];
    hasMore.value       = false;
  } finally {
    isSearching.value = false;
  }
};

// 추가 페이지 로드 (무한스크롤)
const loadMore = async () => {
  if (!hasMore.value || isLoadingMore.value) return;
  const keyword = activeKeyword;
  const nextPage = currentPage.value + 1;
  isLoadingMore.value = true;

  try {
    const res = await busApi.searchStations(keyword, nextPage, PAGE_SIZE);
    if (activeKeyword !== keyword) return;

    const body = res.data;
    searchResults.value = [...searchResults.value, ...(body.data ?? [])];
    hasMore.value       = body.hasMore ?? false;
    currentPage.value   = nextPage;

    setTimeout(observeSentinel, 50);
  } catch (err) {
    // 추가 로드 실패는 조용히 처리 (기존 결과 유지)
    hasMore.value = false;
  } finally {
    isLoadingMore.value = false;
  }
};

const handleSearch = () => {
  if (!query.value || query.value.trim().length === 0) return;
  performSearch(query.value);
};

watch(query, (newVal) => {
  if (!newVal || newVal.trim() === '') {
    searchResults.value = [];
    searchError.value   = '';
    showResults.value   = false;
    hasMore.value       = false;
  }
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
  query.value         = '';
  searchResults.value = [];
  searchError.value   = '';
  showResults.value   = false;
  hasMore.value       = false;
};

const formatDirection = (name) => {
  if (!name) return '';
  const trimmed = name.trim();
  return trimmed.endsWith('방면') ? trimmed : `${trimmed} 방면`;
};

onUnmounted(() => {
  if (observer) observer.disconnect();
  if (debounceTimer) clearTimeout(debounceTimer);
});

// sentinel ref 변경 감지
watch(sentinelRef, (el) => {
  if (el && observer) observer.observe(el);
});

// 드롭다운 열릴 때 observer 준비
watch(showResults, (val) => {
  if (val) {
    setupObserver();
    setTimeout(observeSentinel, 50);
  } else if (observer) {
    observer.disconnect();
  }
});
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
        placeholder="서울 버스 정류장명 또는 5자리 ARS 번호 입력 후 Enter (예: 강남역, 22859)"
        class="w-full py-4 px-2 text-slate-800 placeholder-slate-400 bg-transparent focus:outline-none text-base sm:text-lg"
        @keydown.enter.prevent="handleSearch"
      />

      <button
        v-if="query"
        @click="clearInput"
        class="p-2 mr-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors"
        title="지우기"
      >
        <X class="w-5 h-5" />
      </button>

      <button
        @click="handleSearch"
        class="mr-2.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white text-sm font-semibold rounded-xl transition-all shadow-sm flex items-center gap-1.5 flex-shrink-0"
      >
        <Search class="w-4 h-4" />
        <span>검색</span>
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

    <!-- Search Results Dropdown -->
    <div
      v-if="showResults"
      class="absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl shadow-2xl border border-slate-100 z-50 overflow-hidden"
    >
      <!-- Header -->
      <div class="px-4 py-2 bg-slate-50 text-xs font-semibold text-slate-500 flex items-center justify-between sticky top-0 z-10">
        <span v-if="!searchError && searchResults.length > 0">
          검색 결과 {{ totalCount }}건 중 {{ searchResults.length }}건 표시
        </span>
        <span v-else-if="isSearching">검색 중...</span>
        <span v-else-if="searchError" class="text-red-500">검색 오류</span>
        <span v-else>검색 결과 없음</span>
        <button @click="showResults = false" class="text-slate-400 hover:text-slate-600">닫기</button>
      </div>

      <!-- API Error Banner -->
      <div
        v-if="searchError"
        class="flex items-start gap-3 px-5 py-4 bg-red-50 border-b border-red-100"
      >
        <AlertTriangle class="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
        <div>
          <p class="text-sm font-semibold text-red-700">정류소 검색 서비스 오류</p>
          <p class="text-xs text-red-500 mt-0.5">{{ searchError }}</p>
        </div>
      </div>

      <!-- Loading state with Bus Driving & Smoke Animation -->
      <BusLoading
        v-if="isSearching"
        size="sm"
        message="정류장을 검색하고 있습니다"
        subMessage="정류소 및 경유 노선을 조회 중입니다"
      />

      <!-- Results list (scrollable) -->
      <div v-else-if="searchResults.length > 0" class="max-h-[420px] overflow-y-auto divide-y divide-slate-100">
        <div
          v-for="station in searchResults"
          :key="station.stationId"
          @click="handleSelectStation(station)"
          class="w-full text-left px-5 py-3.5 hover:bg-indigo-50/70 transition-colors flex items-center justify-between group cursor-pointer"
        >
          <div class="flex items-start gap-3 flex-1 min-w-0 mr-3">
            <div class="mt-1 w-7 h-7 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
              <MapPin class="w-4 h-4" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <span class="font-bold text-slate-900 text-base group-hover:text-indigo-700 truncate">{{ station.stationName }}</span>
                <span v-if="station.arsId" class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 font-mono shrink-0">
                  {{ station.arsId }}
                </span>
              </div>
              <p class="text-xs text-slate-500 mt-0.5 truncate">
                {{ station.cityName }}
                <template v-if="station.nextStationName">
                  · <span class="text-slate-600">{{ formatDirection(station.nextStationName) }}</span>
                </template>
              </p>
            </div>
          </div>

          <!-- Right Action Area (지도 보기 + 버스 보기) -->
          <div class="flex items-center gap-2 shrink-0">
            <!-- 지도 모달 열기 버튼 (Trendy Modern Pill) -->
            <button
              type="button"
              @click.stop="handleOpenMap(station)"
              class="group/map inline-flex items-center gap-1.5 pl-1.5 pr-2.5 py-1 rounded-full text-xs font-bold text-indigo-600 bg-gradient-to-r from-indigo-50 via-sky-50/50 to-indigo-50/80 hover:from-indigo-600 hover:via-indigo-600 hover:to-violet-600 hover:text-white border border-indigo-100 hover:border-transparent shadow-xs hover:shadow-md hover:shadow-indigo-500/25 hover:-translate-y-0.5 active:translate-y-0 active:scale-95 transition-all duration-200 select-none"
              title="카카오 지도에서 정류장 위치 확인"
            >
              <span class="w-5 h-5 rounded-full bg-white text-indigo-600 group-hover/map:bg-white/20 group-hover/map:text-white flex items-center justify-center shadow-2xs transition-all duration-200">
                <MapPin class="w-3 h-3 text-indigo-600 group-hover/map:text-white transition-transform duration-200 group-hover/map:scale-110" />
              </span>
              <span class="tracking-tight">지도</span>
            </button>

            <!-- 버스 보기 -->
            <div class="flex items-center text-slate-400 group-hover:text-indigo-600 text-xs font-semibold gap-0.5 select-none transition-colors pl-1">
              <span>버스 보기</span>
              <ChevronRight class="w-4 h-4 transition-transform duration-200 group-hover:translate-x-0.5" />
            </div>
          </div>
        </div>

        <!-- Infinite scroll sentinel -->
        <div ref="sentinelRef" class="py-3 flex justify-center">
          <div v-if="isLoadingMore" class="flex items-center gap-2 text-slate-400 text-xs">
            <Loader2 class="w-4 h-4 animate-spin" />
            <span>더 불러오는 중...</span>
          </div>
          <div v-else-if="!hasMore && searchResults.length > 0" class="text-xs text-slate-400">
            총 {{ totalCount }}개의 정류소를 모두 표시했습니다.
          </div>
        </div>
      </div>

      <!-- Empty state (no error, no results, not loading) -->
      <div
        v-else-if="!isSearching && !searchError && query"
        class="p-6 text-center text-slate-500"
      >
        <p class="text-sm">"{{ query }}"에 대한 검색 결과가 없습니다.</p>
        <p class="text-xs text-slate-400 mt-1">정류장 이름이나 ARS 고유번호를 다시 확인해보세요.</p>
      </div>
    </div>

    <!-- Kakao Map Station Modal -->
    <StationMapModal
      :station="mapStation"
      :is-open="isMapModalOpen"
      @close="handleCloseMap"
      @select-station="handleSelectStation"
    />

  </div>
</template>
