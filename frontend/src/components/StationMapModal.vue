<script setup>
import { ref, watch, nextTick, computed } from 'vue';
import { MapPin, X, Navigation, ExternalLink, Bus, ShieldCheck, CheckCircle2 } from 'lucide-vue-next';

const props = defineProps({
  station: {
    type: Object,
    default: null,
  },
  isOpen: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['close', 'select-station']);

const mapContainer = ref(null);
const mapError = ref(null);
const isSnapped = ref(false);
const snappedStopName = ref('');
const activeCoords = ref({ lat: 0, lng: 0 });

let mapInstance = null;
let markerInstance = null;
let circleInstance = null;
let overlayInstance = null;

const hasValidCoordinates = computed(() => {
  if (!props.station) return false;
  const lat = Number(props.station.latitude);
  const lng = Number(props.station.longitude);
  return !isNaN(lat) && !isNaN(lng) && lat > 30 && lat < 45 && lng > 120 && lng < 135;
});

const kakaoMapLink = computed(() => {
  if (!props.station || !hasValidCoordinates.value) return 'https://map.kakao.com';
  const lat = activeCoords.value.lat || props.station.latitude;
  const lng = activeCoords.value.lng || props.station.longitude;
  return `https://map.kakao.com/link/map/${encodeURIComponent(props.station.stationName)},${lat},${lng}`;
});

const kakaoRouteLink = computed(() => {
  if (!props.station || !hasValidCoordinates.value) return 'https://map.kakao.com';
  const lat = activeCoords.value.lat || props.station.latitude;
  const lng = activeCoords.value.lng || props.station.longitude;
  return `https://map.kakao.com/link/to/${encodeURIComponent(props.station.stationName)},${lat},${lng}`;
});

// 두 좌표 사이의 거리(미터) 계산 (Haversine Formula)
const getDistanceMeters = (lat1, lng1, lat2, lng2) => {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
};

// 카카오맵 Places 서비스를 이용해 5자리 ARS 번호 100% 일치 정류소 검색
const findExactArsMatch = async (origLat, origLng, arsId, stationName) => {
  if (!arsId || !window.kakao?.maps?.services?.Places) {
    return null;
  }

  const cleanTargetArs = String(arsId).replace(/[^0-9]/g, '');
  if (cleanTargetArs.length < 4) {
    // 4~5자리 유효 ARS 번호가 아니면 스냅 시도 안 함
    return null;
  }

  // 13-014 형태의 하이픈 패턴도 지원
  const hyphenatedArs = cleanTargetArs.length === 5
    ? `${cleanTargetArs.slice(0, 2)}-${cleanTargetArs.slice(2)}`
    : null;

  // ARS 번호 100% 일치 검사 함수 (단어 경계 또는 특수문자/괄호로 감싸진 경우만 허용)
  const matchesArs = (text) => {
    if (!text) return false;
    const regexClean = new RegExp(`(^|[^0-9])${cleanTargetArs}([^0-9]|$)`);
    if (regexClean.test(text)) return true;
    if (hyphenatedArs) {
      const regexHyphen = new RegExp(`(^|[^0-9])${hyphenatedArs}([^0-9]|$)`);
      if (regexHyphen.test(text)) return true;
    }
    return false;
  };

  const ps = new window.kakao.maps.services.Places();
  const searchCenter = new window.kakao.maps.LatLng(origLat, origLng);

  const searchKeyword = (kw) => new Promise((resolve) => {
    ps.keywordSearch(kw, (data, status) => {
      if (status === window.kakao.maps.services.Status.OK && Array.isArray(data)) {
        resolve(data);
      } else {
        resolve([]);
      }
    }, {
      location: searchCenter,
      radius: 80, // 반경 80m 이내로 제한하여 반대편 차선이나 먼 정류장과의 혼동 차단
    });
  });

  // 1. ARS 고유 번호로 검색
  let candidates = await searchKeyword(cleanTargetArs);

  // 2. 검색 결과가 없으면 정류소명으로 검색
  if (!candidates || candidates.length === 0) {
    if (stationName) {
      candidates = await searchKeyword(stationName);
    }
  }

  // 3. 후보군 중에서 메타데이터(place_name 또는 address_name)에 ARS 번호가 100% 일치하는지 엄격히 검증
  for (const item of candidates) {
    const matched = matchesArs(item.place_name) || matchesArs(item.address_name);
    if (matched) {
      const itemLat = Number(item.y);
      const itemLng = Number(item.x);
      const dist = getDistanceMeters(origLat, origLng, itemLat, itemLng);

      // 반경 80m 이내이고 ARS 번호가 100% 일치할 때만 스냅 채택
      if (dist <= 80) {
        return {
          lat: itemLat,
          lng: itemLng,
          placeName: item.place_name,
          distance: Math.round(dist)
        };
      }
    }
  }

  // 조금이라도 다르거나 불확실하면 null 반환 (공공 API 좌표 안전 유지)
  return null;
};

const cleanupMap = () => {
  if (markerInstance) {
    markerInstance.setMap(null);
    markerInstance = null;
  }
  if (circleInstance) {
    circleInstance.setMap(null);
    circleInstance = null;
  }
  if (overlayInstance) {
    overlayInstance.setMap(null);
    overlayInstance = null;
  }
  mapInstance = null;
};

const initMap = () => {
  if (!window.kakao || !window.kakao.maps) {
    mapError.value = '카카오 지도 SDK를 불러오지 못했습니다. 도메인 설정 또는 네트워크를 확인해 주세요.';
    return;
  }

  if (!mapContainer.value) return;

  window.kakao.maps.load(async () => {
    try {
      cleanupMap();

      const origLat = hasValidCoordinates.value ? Number(props.station.latitude) : 37.5558;
      const origLng = hasValidCoordinates.value ? Number(props.station.longitude) : 126.9368;

      let finalLat = origLat;
      let finalLng = origLng;
      isSnapped.value = false;
      snappedStopName.value = '';

      // ARS 번호 100% 일치 시에만 스냅 검증
      if (hasValidCoordinates.value && props.station?.arsId) {
        const snapMatch = await findExactArsMatch(
          origLat,
          origLng,
          props.station.arsId,
          props.station.stationName
        );

        if (snapMatch) {
          finalLat = snapMatch.lat;
          finalLng = snapMatch.lng;
          isSnapped.value = true;
          snappedStopName.value = snapMatch.placeName;
        }
      }

      activeCoords.value = { lat: finalLat, lng: finalLng };
      const loc = new window.kakao.maps.LatLng(finalLat, finalLng);

      // level 2: 축척 약 20m로 정류소 쉘터와 차선 구분이 가장 선명한 정밀 레벨
      const options = {
        center: loc,
        level: 2,
      };

      mapInstance = new window.kakao.maps.Map(mapContainer.value, options);

      // 1. 승강 구역 반경 원 (Circle) - 정류장 대기 구역 시각화 (~12m)
      circleInstance = new window.kakao.maps.Circle({
        center: loc,
        radius: 12,
        strokeWeight: 2,
        strokeColor: isSnapped.value ? '#10b981' : '#6366f1',
        strokeOpacity: 0.65,
        strokeStyle: 'dashed',
        fillColor: isSnapped.value ? '#34d399' : '#818cf8',
        fillOpacity: 0.15,
      });
      circleInstance.setMap(mapInstance);

      // 2. 정밀 핀 마커 (끝점이 정확한 좌표점에 위치하도록 앵커 오프셋 정렬)
      const pinColor = isSnapped.value ? '%23059669' : '%234f46e5';
      const svgMarker = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="36" height="46" viewBox="0 0 36 46"><defs><filter id="shadow" x="-20%" y="-10%" width="140%" height="140%"><feDropShadow dx="0" dy="3" stdDeviation="3" flood-opacity="0.35"/></filter></defs><path d="M18 0C8.06 0 0 8.06 0 18c0 13.5 18 28 18 28s18-14.5 18-28C36 8.06 27.94 0 18 0z" fill="${pinColor}" filter="url(%23shadow)"/><circle cx="18" cy="18" r="7" fill="white"/><path d="M14.5 15h7a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-.5l-.5 1h-5l-.5-1H14a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1zm1 2v2h5v-2h-5z" fill="${pinColor}"/></svg>`;
      
      const markerImage = new window.kakao.maps.MarkerImage(
        svgMarker,
        new window.kakao.maps.Size(36, 46),
        { offset: new window.kakao.maps.Point(18, 46) }
      );

      markerInstance = new window.kakao.maps.Marker({
        position: loc,
        image: markerImage,
        map: mapInstance,
      });

      // 3. 커스텀 말풍선 오버레이 (마커 바로 위에 깔끔히 부착)
      const contentEl = document.createElement('div');
      contentEl.innerHTML = `
        <div style="background: rgba(15, 23, 42, 0.94); backdrop-filter: blur(8px); color: white; padding: 6px 12px; border-radius: 9999px; font-size: 12px; font-weight: 700; box-shadow: 0 8px 24px rgba(0,0,0,0.35); border: 1px solid rgba(255,255,255,0.18); display: flex; align-items: center; gap: 6px; transform: translateY(-50px); white-space: nowrap;">
          <span style="display:inline-block; width:8px; height:8px; border-radius:9999px; background:${isSnapped.value ? '#10b981' : '#6366f1'};"></span>
          <span>${props.station?.stationName || '정류소'}</span>
          ${props.station?.arsId ? `<span style="background: rgba(255,255,255,0.15); padding: 1px 6px; border-radius: 6px; font-size: 10px; font-family: monospace; color:#cbd5e1;">${props.station.arsId}</span>` : ''}
        </div>
      `;

      overlayInstance = new window.kakao.maps.CustomOverlay({
        position: loc,
        content: contentEl,
        yAnchor: 1,
      });
      overlayInstance.setMap(mapInstance);

      // 지도 확대/축소 컨트롤
      const zoomControl = new window.kakao.maps.ZoomControl();
      mapInstance.addControl(zoomControl, window.kakao.maps.ControlPosition.RIGHT);

      mapError.value = null;
    } catch (err) {
      console.error('카카오 지도 초기화 오류:', err);
      mapError.value = '지도를 표시하는 중 오류가 발생했습니다.';
    }
  });
};

watch(
  () => props.isOpen,
  async (open) => {
    if (open && props.station) {
      mapError.value = null;
      await nextTick();
      setTimeout(() => {
        initMap();
      }, 120);
    } else {
      cleanupMap();
    }
  },
  { immediate: true }
);

const handleViewBuses = () => {
  emit('select-station', props.station);
  emit('close');
};
</script>

<template>
  <div
    v-if="isOpen && station"
    class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6 animate-fadeIn"
    @click.self="emit('close')"
  >
    <div class="bg-white rounded-3xl shadow-2xl border border-slate-100 w-full max-w-2xl flex flex-col overflow-hidden animate-scaleUp">
      
      <!-- Modal Header -->
      <div class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white px-6 py-5 flex items-start justify-between">
        <div>
          <div class="flex flex-wrap items-center gap-2 mb-1.5">
            <span class="px-2.5 py-0.5 rounded-full bg-indigo-500/30 text-indigo-300 text-xs font-semibold tracking-wider flex items-center gap-1">
              <MapPin class="w-3 h-3" />
              정류장 위치
            </span>
            <span v-if="station.arsId" class="text-xs text-slate-400 font-mono">
              ARS: {{ station.arsId }}
            </span>
            <!-- ARS 100% 검증 스냅 상태 뱃지 -->
            <span
              v-if="isSnapped"
              class="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[11px] font-semibold border border-emerald-500/30 flex items-center gap-1"
              title="카카오맵 메타데이터의 ARS 번호와 100% 일치하여 정밀 스냅되었습니다."
            >
              <CheckCircle2 class="w-3 h-3 text-emerald-400" />
              ARS 일치 스냅 연동됨
            </span>
            <span
              v-else
              class="px-2 py-0.5 rounded-full bg-slate-800/80 text-slate-400 text-[11px] font-medium border border-slate-700/60 flex items-center gap-1"
              title="ARS 번호가 100% 일치하지 않거나 불확실한 경우 안전하게 공공데이터 원본 좌표를 유지합니다."
            >
              <ShieldCheck class="w-3 h-3 text-slate-400" />
              공공 API 좌표 유지
            </span>
          </div>

          <h2 class="text-2xl font-bold text-white flex items-center gap-2">
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
          @click="emit('close')"
          class="p-2 text-slate-400 hover:text-white rounded-full hover:bg-white/10 transition-colors"
          title="닫기"
        >
          <X class="w-6 h-6" />
        </button>
      </div>

      <!-- Map Body -->
      <div class="relative w-full h-80 sm:h-96 bg-slate-100 flex items-center justify-center overflow-hidden">
        
        <!-- Kakao Map Container -->
        <div ref="mapContainer" class="w-full h-full"></div>

        <!-- 안내 배지 (좌표 기준 안내) -->
        <div class="absolute bottom-3 left-3 z-10 flex flex-col gap-1.5 pointer-events-none">
          <div
            v-if="isSnapped"
            class="bg-slate-900/85 backdrop-blur-md text-emerald-300 text-[11px] px-3 py-1.5 rounded-xl border border-emerald-500/30 shadow-lg flex items-center gap-1.5"
          >
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>ARS {{ station.arsId }} 일치: 카카오맵 정류소 위치로 보정됨</span>
          </div>
          <div
            v-else-if="hasValidCoordinates"
            class="bg-slate-900/85 backdrop-blur-md text-slate-300 text-[11px] px-3 py-1.5 rounded-xl border border-slate-700/60 shadow-lg flex items-center gap-1.5"
          >
            <span class="w-2 h-2 rounded-full bg-indigo-400"></span>
            <span>공공 API 원본 좌표 유지 (승강장 구역 반경 12m 표시)</span>
          </div>
        </div>

        <!-- Error / Warning Overlay -->
        <div
          v-if="mapError"
          class="absolute inset-0 bg-slate-900/80 backdrop-blur-xs flex flex-col items-center justify-center p-6 text-center text-white gap-3 z-10"
        >
          <div class="w-12 h-12 rounded-full bg-rose-500/20 text-rose-400 flex items-center justify-center">
            <MapPin class="w-6 h-6" />
          </div>
          <p class="text-sm font-bold">{{ mapError }}</p>
          <p class="text-xs text-slate-300 max-w-sm">
            카카오 개발자 콘솔의 사이트 도메인에 <strong>http://localhost:5173</strong>이 등록되어 있는지 확인해 주세요.
          </p>
          <button
            @click="initMap"
            class="mt-2 px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold transition-colors"
          >
            다시 시도
          </button>
        </div>

        <!-- No Coordinates Warning -->
        <div
          v-else-if="!hasValidCoordinates"
          class="absolute bottom-3 left-3 bg-amber-900/85 backdrop-blur-md text-amber-200 text-xs px-3 py-1.5 rounded-xl border border-amber-500/30 z-10"
        >
          정확한 위경도 좌표가 제공되지 않아 기본 지역으로 표시되었습니다.
        </div>
      </div>

      <!-- Modal Footer -->
      <div class="bg-slate-50 px-6 py-4 border-t border-slate-100 flex items-center justify-between gap-3">
        <!-- External Links -->
        <div class="flex items-center gap-3 text-xs">
          <a
            :href="kakaoRouteLink"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-1 text-slate-600 hover:text-indigo-600 font-semibold transition-colors"
          >
            <span>카카오맵 길찾기</span>
            <ExternalLink class="w-3.5 h-3.5" />
          </a>
          <span class="text-slate-300">|</span>
          <a
            :href="kakaoMapLink"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-1 text-slate-500 hover:text-slate-800 transition-colors"
          >
            <span>큰 지도 보기</span>
            <ExternalLink class="w-3.5 h-3.5" />
          </a>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-2">
          <button
            @click="emit('close')"
            class="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs sm:text-sm font-semibold rounded-xl transition-colors"
          >
            닫기
          </button>
          <button
            @click="handleViewBuses"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-bold rounded-xl shadow-md shadow-indigo-200 flex items-center gap-1.5 transition-all"
          >
            <Bus class="w-4 h-4" />
            <span>이 정류장 버스 보기</span>
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
.animate-fadeIn {
  animation: fadeIn 0.15s ease-out;
}
.animate-scaleUp {
  animation: scaleUp 0.15s ease-out;
}
</style>

