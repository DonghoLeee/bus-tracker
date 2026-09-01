<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { 
  Bus, MapPin, X, BookmarkCheck, BookmarkPlus, 
  Clock, ShieldAlert, Users, Sparkles, Navigation 
} from 'lucide-vue-next';

const store = useBookmarkStore();

const station = computed(() => store.selectedStation);
const buses = computed(() => store.stationBuses);
const loading = computed(() => store.loadingStationBuses);

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
</script>

<template>
  <div v-if="station" class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6 animate-fadeIn">
    
    <div class="bg-white rounded-3xl shadow-2xl border border-slate-100 w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
      
      <!-- Modal Header -->
      <div class="bg-gradient-to-r from-slate-900 to-indigo-950 text-white px-6 py-5 flex items-start justify-between">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <span class="px-2.5 py-0.5 rounded-full bg-indigo-500/30 text-indigo-300 text-xs font-semibold tracking-wider">
              정류장 상세
            </span>
            <span v-if="station.arsId" class="text-xs text-slate-400 font-mono">
              ARS: {{ station.arsId }}
            </span>
          </div>
          <h2 class="text-2xl font-bold text-white flex items-center gap-2">
            <MapPin class="w-6 h-6 text-indigo-400" />
            {{ station.stationName }}
          </h2>
          <p class="text-xs sm:text-sm text-slate-300 mt-1 flex items-center gap-1.5">
            <Navigation class="w-3.5 h-3.5 text-indigo-400" />
            <span>다음 정류장: <strong>{{ station.nextStationName || '방면 미정' }}</strong></span>
            <span class="text-slate-500">|</span>
            <span>{{ station.cityName }}</span>
          </p>
        </div>

        <button
          @click="store.clearSelectedStation()"
          class="p-2 text-slate-400 hover:text-white rounded-full hover:bg-white/10 transition-colors"
        >
          <X class="w-6 h-6" />
        </button>
      </div>

      <!-- Modal Body (Bus List) -->
      <div class="p-6 overflow-y-auto flex-1 divide-y divide-slate-100">
        
        <div class="flex items-center justify-between pb-3">
          <h3 class="font-bold text-slate-800 text-sm flex items-center gap-1.5">
            <Bus class="w-4 h-4 text-indigo-600" />
            정차 버스 노선 목록 ({{ buses.length }}개 노선)
          </h3>
          <span class="text-xs text-slate-400">원하는 버스를 선택해 도착 알림에 등록하세요</span>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="py-16 text-center text-slate-400">
          <div class="inline-block w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3"></div>
          <p class="text-sm">실시간 버스 도착 정보를 불러오는 중입니다...</p>
        </div>

        <!-- Empty State -->
        <div v-else-if="buses.length === 0" class="py-16 text-center text-slate-400">
          <Bus class="w-10 h-10 mx-auto mb-2 text-slate-300" />
          <p class="text-sm font-medium">정차하는 버스 정보가 없습니다.</p>
        </div>

        <!-- Bus Items List -->
        <div v-else class="pt-2 space-y-3">
          <div
            v-for="bus in buses"
            :key="bus.busRouteId"
            class="p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            :class="bus.isBookmarked 
              ? 'bg-indigo-50/60 border-indigo-200 shadow-sm' 
              : 'bg-white border-slate-200/80 hover:border-slate-300 hover:bg-slate-50/50'"
          >
            
            <!-- Left: Bus Number, Type, Direction -->
            <div class="flex items-start sm:items-center gap-3">
              <div class="px-3 py-1.5 rounded-xl font-black text-lg sm:text-xl tracking-tight shadow-sm flex items-center justify-center min-w-[76px]"
                   :class="getBusBadgeStyle(bus.busType)">
                {{ bus.busRouteName }}
              </div>

              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs px-2 py-0.5 rounded-md font-semibold bg-slate-100 text-slate-600">
                    {{ bus.busTypeLabel }}
                  </span>
                  <span v-if="bus.isLowPlate1" class="text-[10px] px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-700 font-medium">
                    저상
                  </span>
                  <span v-if="bus.isLastBus" class="text-[10px] px-1.5 py-0.5 rounded bg-rose-100 text-rose-700 font-bold">
                    막차
                  </span>
                </div>
                <p class="text-xs text-slate-500 mt-1">
                  {{ bus.direction }}
                </p>
              </div>
            </div>

            <!-- Middle: Live Arrival Countdown Info -->
            <div class="flex flex-col sm:items-end gap-1">
              <!-- 1st Arrival -->
              <div class="flex items-center gap-2">
                <span class="text-sm font-bold" :class="bus.predictTimeSec1 <= 180 ? 'text-rose-600 animate-pulse' : 'text-slate-800'">
                  {{ bus.predictTimeSec1 < 50 ? '곧 도착' : formatSeconds(bus.predictTimeSec1) + ' 후' }}
                </span>
                <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
                  {{ bus.locationNo1 }}번째 전
                </span>
                <span v-if="bus.congestion1" class="text-xs px-2 py-0.5 rounded-full border font-medium"
                      :class="getCongestionBadge(bus.congestion1)">
                  {{ bus.congestion1 }}
                </span>
              </div>

              <!-- 2nd Arrival Info (Small) -->
              <div v-if="bus.predictTimeSec2" class="text-[11px] text-slate-400 flex items-center gap-1">
                <span>다음: {{ formatSeconds(bus.predictTimeSec2) }} 후</span>
                <span>({{ bus.locationNo2 }}번째 전)</span>
              </div>
            </div>

            <!-- Right: Bookmark Toggle Button -->
            <div class="flex items-center justify-end sm:pl-2">
              <button
                @click="handleToggle(bus)"
                class="px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-1.5 transition-all shadow-sm"
                :class="bus.isBookmarked
                  ? 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-indigo-200'
                  : 'bg-slate-100 text-slate-700 hover:bg-indigo-50 hover:text-indigo-600 border border-slate-200'"
              >
                <BookmarkCheck v-if="bus.isBookmarked" class="w-4 h-4" />
                <BookmarkPlus v-else class="w-4 h-4" />
                <span>{{ bus.isBookmarked ? '등록됨' : '도착 알림 등록' }}</span>
              </button>
            </div>

          </div>
        </div>

      </div>

      <!-- Modal Footer -->
      <div class="bg-slate-50 px-6 py-3.5 border-t border-slate-100 flex items-center justify-between">
        <span class="text-xs text-slate-400">등록된 버스는 메인 화면 대시보드에서 실시간으로 추적됩니다.</span>
        <button
          @click="store.clearSelectedStation()"
          class="px-4 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg transition-colors"
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
