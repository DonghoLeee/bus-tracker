<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import BusLoading from './BusLoading.vue';
import { 
  Bus, MapPin, X, BookmarkCheck, BookmarkPlus, 
  Clock, ShieldAlert, Users, Sparkles, Navigation 
} from 'lucide-vue-next';

const store = useBookmarkStore();

const station = computed(() => store.selectedStation);
const buses = computed(() => {
  const list = store.stationBuses || [];
  const seen = new Set();
  return list.filter((bus) => {
    const key = bus.busRouteId || bus.busRouteName;
    if (!key) return true;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
});
const loading  = computed(() => store.loadingStationBuses);
const refreshing = computed(() => store.refreshingStationBuses);
const fetchError = computed(() => store.stationBusFetchError);

const getBusBadgeStyle = (type) => {
  switch (type) {
    case 'MAIN': // 간선(파랑)
      return 'bg-blue-600 text-white';
    case 'BRANCH': // 지선(초록)
      return 'bg-emerald-600 text-white';
    case 'RAPID': // 광역(빨강)
      return 'bg-rose-600 text-white';
    case 'CIRCULAR': // 순환(노랑)
      return 'bg-amber-500 text-white';
    case 'TOWN': // 마을(연초록)
      return 'bg-teal-600 text-white';
    case 'AIRPORT': // 공항(보라)
      return 'bg-purple-600 text-white';
    default:
      return 'bg-slate-700 text-white';
  }
};

const getCongestionBadge = (congestion) => {
  switch (congestion) {
    case '여유':
      return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    case '보통':
      return 'bg-blue-100 text-blue-700 border-blue-200';
    case '혼잡':
      return 'bg-rose-100 text-rose-700 border-rose-200';
    default:
      return 'bg-slate-100 text-slate-600 border-slate-200';
  }
};

const handleToggle = (bus) => {
  if (!station.value) return;
  store.toggleBookmark(station.value, bus);
};

const formatSeconds = (sec) => {
  if (sec == null) return '';
  if (sec < 60) return `${sec}초`;
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return s > 0 ? `${m}분 ${s}초` : `${m}분`;
};

const getDirectionTextClass = (direction) => {
  if (!direction) return 'text-xs';
  const len = direction.length;
  if (len >= 17) return 'text-[10px] sm:text-[10.5px] tracking-tight leading-tight';
  if (len >= 12) return 'text-[11px] sm:text-[11.5px] tracking-tight leading-tight';
  return 'text-xs';
};
</script>

<template>
  <div v-if="station" class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-2.5 sm:p-6 animate-fadeIn">
    
    <div class="bg-white rounded-3xl shadow-2xl border border-slate-100 w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
      
      <!-- Modal Header -->
      <div class="bg-gradient-to-r from-slate-900 to-indigo-950 text-white px-4 sm:px-6 py-4 sm:py-5 flex items-start justify-between">
        <div class="min-w-0 flex-1 mr-2">
          <div class="flex items-center gap-2 mb-1">
            <span class="px-2.5 py-0.5 rounded-full bg-indigo-500/30 text-indigo-300 text-xs font-semibold tracking-wider shrink-0">
              정류장 상세
            </span>
            <span v-if="station.arsId" class="text-xs text-slate-400 font-mono shrink-0">
              ARS: {{ station.arsId }}
            </span>
            <span v-if="refreshing" class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-indigo-500/25 text-indigo-200 text-[11px] font-medium animate-pulse shrink-0">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              실시간 갱신 중
            </span>
          </div>
          <h2 class="text-xl sm:text-2xl font-bold text-white flex items-center gap-2 truncate">
            <MapPin class="w-5 h-5 sm:w-6 sm:h-6 text-indigo-400 shrink-0" />
            <span class="truncate">{{ station.stationName }}</span>
          </h2>
          <p class="text-xs sm:text-sm text-slate-300 mt-1 flex items-center gap-1.5 truncate">
            <Navigation class="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <span class="truncate">다음 정류장: <strong>{{ station.nextStationName || '방면 미정' }}</strong></span>
            <span class="text-slate-500">|</span>
            <span class="shrink-0">{{ station.cityName }}</span>
          </p>
        </div>

        <button
          @click="store.clearSelectedStation()"
          class="p-2 text-slate-400 hover:text-white rounded-full hover:bg-white/10 transition-colors shrink-0"
        >
          <X class="w-6 h-6" />
        </button>
      </div>

      <!-- Modal Body (Bus List) -->
      <div class="p-3 sm:p-6 overflow-y-auto flex-1 divide-y divide-slate-100">
        
        <div class="flex items-center justify-between pb-3">
          <h3 class="font-bold text-slate-800 text-xs sm:text-sm flex items-center gap-1.5">
            <Bus class="w-4 h-4 text-indigo-600 shrink-0" />
            <span>정차 버스 목록 ({{ buses.length }}개)</span>
          </h3>
          <span class="text-[11px] sm:text-xs text-slate-400">도착 알림을 등록하세요</span>
        </div>

        <!-- Loading State with Bus Driving & Smoke Animation -->
        <BusLoading
          v-if="loading && buses.length === 0"
          message="정차 버스 도착 정보를 불러오는 중입니다"
          subMessage="서울시 실시간 버스 도착 API와 통신하고 있습니다"
        />

        <!-- Error State -->
        <div v-else-if="fetchError && buses.length === 0" class="py-14 flex flex-col items-center text-center gap-3">
          <div class="w-14 h-14 rounded-full bg-rose-100 flex items-center justify-center mb-1">
            <ShieldAlert class="w-7 h-7 text-rose-500" />
          </div>
          <p class="text-sm font-bold text-slate-700">도착정보를 가져오지 못했습니다</p>
          <p class="text-xs text-slate-400 max-w-xs">{{ fetchError }}</p>
          <button
            @click="store.selectStation(station)"
            class="mt-2 px-4 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors"
          >
            다시 시도
          </button>
        </div>

        <!-- Empty State -->
        <div v-else-if="!loading && buses.length === 0" class="py-16 text-center text-slate-400">
          <Bus class="w-10 h-10 mx-auto mb-2 text-slate-300" />
          <p class="text-sm font-medium">정차하는 버스 정보가 없습니다.</p>
        </div>

        <!-- Bus Items List -->
        <div v-else class="pt-2 space-y-2.5 sm:space-y-3">
          <div
            v-for="bus in buses"
            :key="bus.busRouteId"
            class="p-3 sm:p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 sm:gap-4 overflow-hidden"
            :class="bus.isBookmarked 
              ? 'bg-indigo-50/60 border-indigo-200 shadow-sm' 
              : 'bg-white border-slate-200/80 hover:border-slate-300 hover:bg-slate-50/50'"
          >
            
            <!-- Top on mobile / Left on desktop: Bus Number, Type, Direction -->
            <div class="flex items-center gap-2.5 sm:gap-3 flex-1 min-w-0">
              <div class="px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-xl font-black text-base sm:text-xl tracking-tight shadow-sm flex items-center justify-center min-w-[68px] sm:min-w-[76px] shrink-0"
                   :class="getBusBadgeStyle(bus.busType)">
                {{ bus.busRouteName }}
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-1.5 sm:gap-2">
                  <span class="text-[11px] sm:text-xs px-2 py-0.5 rounded-md font-semibold bg-slate-100 text-slate-600 shrink-0">
                    {{ bus.busTypeLabel }}
                  </span>
                  <span v-if="bus.isLowPlate1" class="text-[10px] px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-700 font-medium shrink-0">
                    저상
                  </span>
                  <span v-if="bus.isLastBus" class="text-[10px] px-1.5 py-0.5 rounded bg-rose-100 text-rose-700 font-bold shrink-0">
                    막차
                  </span>
                </div>
                <p 
                  class="text-slate-500 mt-1 truncate transition-all text-xs"
                  :class="getDirectionTextClass(bus.direction)"
                  :title="bus.direction"
                >
                  {{ bus.direction }}
                </p>
              </div>
            </div>

            <!-- Bottom on mobile / Right on desktop: Status Info & Bookmark Button -->
            <div class="flex items-center justify-between sm:justify-end gap-2 sm:gap-3.5 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100 min-w-0">
              
              <!-- Arrival Countdown or Status Badge -->
              <div class="flex flex-col items-start sm:items-end justify-center min-w-0 flex-1 sm:flex-initial text-left sm:text-right pr-1">
                <!-- 1st Arrival (When active arrival prediction exists) -->
                <template v-if="bus.isOperating && bus.predictTimeSec1 != null && bus.predictTimeSec1 >= 0">
                  <div class="flex flex-wrap items-center gap-1 sm:gap-1.5">
                    <span class="text-xs sm:text-sm font-bold whitespace-nowrap" :class="bus.predictTimeSec1 <= 180 ? 'text-rose-600 animate-pulse' : 'text-slate-800'">
                      {{ bus.predictTimeSec1 < 50 ? '곧 도착' : formatSeconds(bus.predictTimeSec1) + ' 후' }}
                    </span>
                    <span v-if="bus.locationNo1 != null && bus.locationNo1 > 0" class="text-[10px] sm:text-[11px] px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-600 font-medium whitespace-nowrap">
                      {{ bus.locationNo1 }}번째 전
                    </span>
                    <span v-if="bus.congestion1" class="text-[10px] px-1.5 py-0.5 rounded-md border font-medium whitespace-nowrap"
                          :class="getCongestionBadge(bus.congestion1)">
                      {{ bus.congestion1 }}
                    </span>
                  </div>

                  <!-- 2nd Arrival Info (Small) -->
                  <div v-if="bus.predictTimeSec2 != null && bus.predictTimeSec2 > 0" class="text-[10px] sm:text-[11px] text-slate-400 flex items-center gap-1 mt-0.5 whitespace-nowrap">
                    <span>다음: {{ formatSeconds(bus.predictTimeSec2) }} 후</span>
                    <span v-if="bus.locationNo2 != null && bus.locationNo2 > 0">({{ bus.locationNo2 }}번째 전)</span>
                  </div>
                </template>

                <!-- When no arrival prediction / not operating -->
                <div v-else class="flex items-center">
                  <span class="text-[11px] sm:text-xs px-2 sm:px-2.5 py-1 rounded-lg font-medium bg-slate-100 text-slate-500 border border-slate-200/60 whitespace-nowrap">
                    {{ bus.statusMessage && bus.statusMessage !== '0' && bus.statusMessage !== 'null' ? bus.statusMessage : '도착 정보 없음' }}
                  </span>
                </div>
              </div>

              <!-- Bookmark Toggle Button (Responsive width: w-auto on mobile, fixed on desktop) -->
              <div class="shrink-0">
                <button
                  @click="handleToggle(bus)"
                  class="w-auto sm:w-[130px] px-2.5 sm:px-3 py-1.5 sm:py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center justify-center gap-1 sm:gap-1.5 transition-all shadow-sm select-none shrink-0"
                  :class="bus.isBookmarked
                    ? 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-indigo-200'
                    : 'bg-slate-100 text-slate-700 hover:bg-indigo-50 hover:text-indigo-600 border border-slate-200'"
                >
                  <BookmarkCheck v-if="bus.isBookmarked" class="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
                  <BookmarkPlus v-else class="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
                  <span class="whitespace-nowrap">{{ bus.isBookmarked ? '등록됨' : '도착 알림 등록' }}</span>
                </button>
              </div>

            </div>

          </div>
        </div>

      </div>

      <!-- Modal Footer -->
      <div class="bg-slate-50 px-4 sm:px-6 py-3.5 border-t border-slate-100 flex items-center justify-between gap-3">
        <span class="text-xs text-slate-400 min-w-0 flex-1 break-keep">
          등록된 버스는 메인 화면 대시보드에서 실시간으로 추적됩니다.
        </span>
        <button
          @click="store.clearSelectedStation()"
          class="shrink-0 whitespace-nowrap px-4 py-2 bg-slate-200 hover:bg-slate-300 active:bg-slate-400 text-slate-700 text-xs font-bold rounded-xl transition-colors shadow-2xs min-w-[60px] text-center"
        >
          닫기
        </button>
      </div>

    </div>

  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1); }
}
.animate-fadeIn {
  animation: fadeIn 0.15s ease-out;
}
</style>
