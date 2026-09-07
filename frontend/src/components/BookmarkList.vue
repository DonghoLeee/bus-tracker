<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import BusLoading from './BusLoading.vue';
import { 
  Bus, Trash2, MapPin, Clock, Users, AlertCircle, 
  ChevronRight, Navigation, Sparkles, PlusCircle 
} from 'lucide-vue-next';

const store = useBookmarkStore();

const bookmarks = computed(() => store.bookmarks);
const loading = computed(() => store.loadingBookmarks);

const getBusBadgeStyle = (type) => {
  switch (type) {
    case 'MAIN':
      return 'bg-blue-600 text-white';
    case 'BRANCH':
      return 'bg-emerald-600 text-white';
    case 'RAPID':
      return 'bg-rose-600 text-white';
    case 'CIRCULAR':
      return 'bg-amber-500 text-white';
    case 'TOWN':
      return 'bg-teal-600 text-white';
    case 'AIRPORT':
      return 'bg-purple-600 text-white';
    default:
      return 'bg-slate-700 text-white';
  }
};

const getCongestionBadge = (congestion) => {
  switch (congestion) {
    case '여유':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case '보통':
      return 'bg-blue-50 text-blue-700 border-blue-200';
    case '혼잡':
      return 'bg-rose-50 text-rose-700 border-rose-200';
    default:
      return 'bg-slate-50 text-slate-600 border-slate-200';
  }
};

const isImminent = (arrival) => {
  if (!arrival || !arrival.isOperating) return false;
  return (arrival.predictTimeSec1 != null && arrival.predictTimeSec1 <= 180) ||
         (arrival.locationNo1 != null && arrival.locationNo1 === 1);
};

const formatSeconds = (sec) => {
  if (sec == null) return '';
  if (sec < 60) return `${sec}초`;
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return s > 0 ? `${m}분 ${s}초` : `${m}분`;
};

const handleDelete = (bookmark) => {
  if (confirm(`'${bookmark.stationName}'의 '${bookmark.busRouteName}번' 버스를 목록에서 삭제하시겠습니까?`)) {
    store.removeBookmark(bookmark.id, bookmark.busRouteName);
  }
};
</script>

<template>
  <div class="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    
    <!-- Section Title (Simple & Trendy 1-Line) -->
    <div class="flex items-center justify-between gap-3 mb-5">
      <div class="flex items-center gap-2.5">
        <h2 class="text-lg sm:text-xl font-black text-slate-900 flex items-center gap-2">
          <span>내 등록 버스</span>
          <span class="text-xs px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 font-extrabold font-mono">
            {{ bookmarks.length }}
          </span>
        </h2>
        <span class="hidden sm:inline-block text-xs text-slate-400 font-medium border-l border-slate-200 pl-2.5">
          서울 실시간 도착 현황
        </span>
      </div>

      <div v-if="store.lastUpdated" class="text-xs text-slate-400 flex items-center gap-1 font-medium shrink-0">
        <Clock class="w-3.5 h-3.5 text-slate-400" />
        <span>마지막 갱신: {{ new Date(store.lastUpdated).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) }}</span>
      </div>
    </div>

    <!-- Loading State with Bus Driving & Smoke Animation -->
    <BusLoading
      v-if="loading && bookmarks.length === 0"
      message="등록된 버스 도착 정보를 불러오는 중입니다"
      subMessage="실시간 운행 현황과 버스 위치를 조회하고 있습니다"
    />

    <!-- Empty State -->
    <div
      v-else-if="bookmarks.length === 0"
      class="bg-white rounded-3xl border border-dashed border-slate-300 p-12 text-center shadow-sm"
    >
      <div class="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-500 flex items-center justify-center mx-auto mb-4">
        <Bus class="w-8 h-8" />
      </div>
      <h3 class="text-lg font-bold text-slate-800 mb-1">등록된 버스가 없습니다</h3>
      <p class="text-sm text-slate-500 max-w-md mx-auto mb-6">
        상단 검색창에서 자주 이용하는 버스 정류장을 검색하고, 경유하는 버스를 도착 알림 목록에 등록해보세요!
      </p>
    </div>

    <!-- Bookmarks Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="item in bookmarks"
        :key="item.id"
        class="bg-white rounded-3xl p-5 border transition-all duration-300 relative overflow-hidden flex flex-col justify-between"
        :class="isImminent(item.arrivalInfo) 
          ? 'border-rose-400/80 shadow-lg shadow-rose-100/50 ring-2 ring-rose-200' 
          : 'border-slate-200/90 hover:border-indigo-300 hover:shadow-md'"
      >
        
        <!-- Imminent Highlight Ribbon -->
        <div
          v-if="isImminent(item.arrivalInfo)"
          class="absolute top-0 right-0 bg-rose-500 text-white text-[10px] font-black px-3 py-0.5 rounded-bl-xl tracking-wider uppercase animate-pulse flex items-center gap-1 shadow-sm"
        >
          <Sparkles class="w-3 h-3" />
          도착 임박
        </div>

        <div>
          <!-- Station Header -->
          <div class="flex items-start justify-between gap-2 mb-3">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center flex-shrink-0">
                <MapPin class="w-4 h-4" />
              </div>
              <div>
                <h4 class="font-bold text-slate-900 text-base leading-tight">{{ item.stationName }}</h4>
                <p v-if="item.arsId" class="text-[11px] text-slate-400 font-mono">ARS {{ item.arsId }}</p>
              </div>
            </div>

            <!-- Delete Action -->
            <button
              @click="handleDelete(item)"
              title="삭제"
              class="p-1.5 text-slate-300 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>

          <!-- Bus Route Badge & Info -->
          <div class="flex items-center justify-between bg-slate-50 rounded-2xl p-3 mb-4">
            <div class="flex items-center gap-3">
              <div
                class="px-3.5 py-1.5 rounded-xl font-black text-xl tracking-tight shadow-sm flex items-center justify-center min-w-[70px]"
                :class="getBusBadgeStyle(item.busType)"
              >
                {{ item.busRouteName }}
              </div>
              <div>
                <span class="text-xs px-2 py-0.5 rounded bg-white font-medium text-slate-600 border border-slate-200">
                  {{ item.busTypeLabel }}
                </span>
                <p class="text-xs text-slate-500 mt-1 line-clamp-1">
                  {{ item.direction || '운행 방면' }}
                </p>
              </div>
            </div>
          </div>

          <!-- Arrival Information Display -->
          <div v-if="item.arrivalInfo && item.arrivalInfo.isOperating && item.arrivalInfo.predictTimeSec1 != null && item.arrivalInfo.predictTimeSec1 > 0" class="space-y-2.5">
            
            <!-- 1st Bus Arrival (Main) -->
            <div
              class="rounded-2xl p-3.5 border transition-all"
              :class="isImminent(item.arrivalInfo) 
                ? 'bg-rose-50/70 border-rose-200' 
                : 'bg-indigo-50/40 border-indigo-100'"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs font-semibold text-slate-500">첫 번째 도착</span>
                <span
                  v-if="item.arrivalInfo.congestion1"
                  class="text-[11px] px-2 py-0.5 rounded-full border font-medium"
                  :class="getCongestionBadge(item.arrivalInfo.congestion1)"
                >
                  {{ item.arrivalInfo.congestion1 }}
                </span>
              </div>

              <div class="flex items-baseline justify-between">
                <div class="text-xl sm:text-2xl font-black tracking-tight"
                     :class="isImminent(item.arrivalInfo) ? 'text-rose-600' : 'text-indigo-950'">
                  {{ item.arrivalInfo.predictTimeSec1 < 50 ? '곧 도착' : formatSeconds(item.arrivalInfo.predictTimeSec1) }}
                </div>
                <div
                  v-if="item.arrivalInfo.locationNo1 != null && item.arrivalInfo.locationNo1 > 0"
                  class="text-xs font-bold text-slate-600 bg-white/80 px-2 py-1 rounded-lg border border-slate-200/60 shadow-2xs"
                >
                  {{ item.arrivalInfo.locationNo1 }}번째 전
                </div>
              </div>
            </div>

            <!-- 2nd Bus Arrival (Sub) -->
            <div
              v-if="item.arrivalInfo.predictTimeSec2 != null && item.arrivalInfo.predictTimeSec2 > 0"
              class="flex items-center justify-between px-3 py-2 rounded-xl bg-slate-50 text-xs text-slate-600 border border-slate-100"
            >
              <span class="text-slate-400">두 번째 버스</span>
              <div class="flex items-center gap-2 font-medium">
                <span class="font-bold text-slate-700">{{ formatSeconds(item.arrivalInfo.predictTimeSec2) }}</span>
                <span v-if="item.arrivalInfo.locationNo2 != null && item.arrivalInfo.locationNo2 > 0" class="text-slate-400">({{ item.arrivalInfo.locationNo2 }}번째 전)</span>
                <span
                  v-if="item.arrivalInfo.congestion2"
                  class="text-[10px] px-1.5 py-0.2 rounded border"
                  :class="getCongestionBadge(item.arrivalInfo.congestion2)"
                >
                  {{ item.arrivalInfo.congestion2 }}
                </span>
              </div>
            </div>

          </div>

          <!-- Non-operating or No Data -->
          <div v-else class="py-6 text-center bg-slate-50 rounded-2xl border border-slate-100 text-slate-400">
            <AlertCircle class="w-6 h-6 mx-auto mb-1 text-slate-300" />
            <p class="text-xs font-medium">{{ item.arrivalInfo?.statusMessage || '도착 정보 없음' }}</p>
          </div>
        </div>

        <!-- Card Footer (Memo if any) -->
        <div v-if="item.memo" class="mt-4 pt-2.5 border-t border-slate-100 text-xs text-slate-400 flex items-center gap-1.5">
          <span class="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
          <span>{{ item.memo }}</span>
        </div>

      </div>
    </div>

  </div>
</template>
