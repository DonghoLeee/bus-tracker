import { defineStore } from 'pinia';
import { busApi } from '../api/busApi';

export const useBookmarkStore = defineStore('bookmark', {
  state: () => ({
    bookmarks: [],
    loadingBookmarks: false,
    refreshing: false,
    lastUpdated: null,
    
    // 자동 새로고침 설정 (초 단위: 0이면 해제, 기본 15초)
    refreshIntervalSec: 15,
    secondsUntilNextRefresh: 15,
    timerId: null,
    countdownTimerId: null,

    // 선택된 정류장 및 버스 목록 모달/패널 상태
    selectedStation: null,
    stationBuses: [],
    loadingStationBuses: false,
    refreshingStationBuses: false,
    stationBusFetchError: null, // null이면 정상, 문자열이면 오류 메시지

    // 실시간 초 단위 시간 흐름 추적용 타임스탬프
    lastTickTimestamp: Date.now(),

    // 토스트 알림
    toastMessage: null,
    toastType: 'info', // success, info, warning, error
    toastTimeout: null,
  }),

  getters: {
    bookmarkCount: (state) => state.bookmarks.length,
    
    // 도착 임박 버스 수 (3분 이내 또는 1번째 전)
    imminentArrivalsCount: (state) => {
      return state.bookmarks.filter((b) => {
        const arrival = b.arrivalInfo;
        if (!arrival || !arrival.isOperating) return false;
        return (arrival.predictTimeSec1 != null && arrival.predictTimeSec1 <= 180) ||
               (arrival.locationNo1 != null && arrival.locationNo1 === 1);
      }).length;
    },

    isRouteBookmarked: (state) => (stationId, busRouteId) => {
      return state.bookmarks.some(
        (b) => b.stationId === stationId && b.busRouteId === busRouteId
      );
    },
  },

  actions: {
    showToast(message, type = 'info') {
      if (this.toastTimeout) clearTimeout(this.toastTimeout);
      this.toastMessage = message;
      this.toastType = type;
      this.toastTimeout = setTimeout(() => {
        this.toastMessage = null;
      }, 3500);
    },

    async fetchBookmarks() {
      this.loadingBookmarks = true;
      try {
        const response = await busApi.getBookmarks();
        this.bookmarks = response.data;
        this.lastUpdated = new Date();
      } catch (error) {
        console.error('북마크 목록 조회 실패:', error);
        this.showToast('등록된 버스 목록을 불러오지 못했습니다.', 'error');
      } finally {
        this.loadingBookmarks = false;
      }
    },

    async refreshArrivalsSilently() {
      if (this.refreshing) return;
      this.refreshing = true;
      try {
        const response = await busApi.refreshArrivals();
        this.bookmarks = response.data;
        this.lastUpdated = new Date();
        this.secondsUntilNextRefresh = this.refreshIntervalSec;
      } catch (error) {
        console.error('실시간 도착정보 갱신 실패:', error);
      } finally {
        this.refreshing = false;
      }
    },

    async toggleBookmark(station, bus) {
      const alreadyBookmarked = this.isRouteBookmarked(station.stationId, bus.busRouteId);

      if (alreadyBookmarked) {
        try {
          await busApi.deleteBookmarkByRoute(station.stationId, bus.busRouteId);
          this.bookmarks = this.bookmarks.filter(
            (b) => !(b.stationId === station.stationId && b.busRouteId === bus.busRouteId)
          );
          // 현재 정류장 버스 목록 상태도 갱신
          const target = this.stationBuses.find((b) => b.busRouteId === bus.busRouteId);
          if (target) target.isBookmarked = false;

          this.showToast(`[${bus.busRouteName}번] 등록이 해제되었습니다.`, 'info');
        } catch (error) {
          console.error('북마크 해제 실패:', error);
          this.showToast('북마크 해제에 실패했습니다.', 'error');
        }
      } else {
        try {
          const reqData = {
            stationId: station.stationId,
            stationName: station.stationName,
            arsId: station.arsId,
            busRouteId: bus.busRouteId,
            busRouteName: bus.busRouteName,
            busType: bus.busType,
            direction: bus.direction,
          };
          const response = await busApi.addBookmark(reqData);
          this.bookmarks.unshift(response.data);

          const target = this.stationBuses.find((b) => b.busRouteId === bus.busRouteId);
          if (target) target.isBookmarked = true;

          this.showToast(`[${bus.busRouteName}번] 도착 모니터링 목록에 등록되었습니다!`, 'success');
        } catch (error) {
          console.error('북마크 등록 실패:', error);
          this.showToast('북마크 등록에 실패했습니다.', 'error');
        }
      }
    },

    async removeBookmark(id, busRouteName) {
      try {
        await busApi.deleteBookmark(id);
        this.bookmarks = this.bookmarks.filter((b) => b.id !== id);
        this.showToast(`[${busRouteName}번] 삭제되었습니다.`, 'info');
      } catch (error) {
        console.error('북마크 삭제 실패:', error);
        this.showToast('삭제에 실패했습니다.', 'error');
      }
    },

    async selectStation(station, isSilent = false) {
      if (!station) return;
      const isSameStation = this.selectedStation?.stationId === station.stationId;

      this.selectedStation = station;
      this.stationBusFetchError = null;

      // 같은 정류장이면서 이미 데이터가 있으면 깜빡임 없이 silent/upsert 모드로 갱신
      const shouldBeSilent = isSilent || (isSameStation && this.stationBuses.length > 0);

      if (!shouldBeSilent) {
        this.stationBuses = [];
        this.loadingStationBuses = true;
      } else {
        this.refreshingStationBuses = true;
      }

      try {
        const response = await busApi.getStationBuses(station.stationId);
        const incoming = response.data || [];

        if (!shouldBeSilent || this.stationBuses.length === 0) {
          this.stationBuses = incoming;
        } else {
          this.upsertStationBuses(incoming);
        }
      } catch (error) {
        console.error('정류장 버스 조회 실패:', error);
        // 503 Service Unavailable: 서버에서 보낸 error 메시지 사용
        const serverMsg = error.response?.data?.error;
        const displayMsg = serverMsg || '도착정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.';
        if (this.stationBuses.length === 0) {
          this.stationBusFetchError = displayMsg;
        }
        if (!shouldBeSilent) {
          this.showToast(displayMsg, 'error');
        }
      } finally {
        this.loadingStationBuses = false;
        this.refreshingStationBuses = false;
      }
    },

    upsertStationBuses(incomingList) {
      if (!Array.isArray(incomingList)) return;

      const existingMap = new Map();
      this.stationBuses.forEach((item, index) => {
        const key = item.busRouteId || item.busRouteName;
        if (key) {
          existingMap.set(key, { item, index });
        }
      });

      const incomingKeys = new Set();

      incomingList.forEach((incoming) => {
        const key = incoming.busRouteId || incoming.busRouteName;
        if (!key) return;
        incomingKeys.add(key);

        if (existingMap.has(key)) {
          // UPDATE: 기존 인스턴스 속성만 in-place 갱신하여 DOM 재생성 및 깜빡임 방지
          const { item: target } = existingMap.get(key);
          Object.assign(target, {
            ...incoming,
            isBookmarked: this.isRouteBookmarked(this.selectedStation.stationId, incoming.busRouteId),
          });
        } else {
          // INSERT: 새로 진입한 노선 추가
          this.stationBuses.push({
            ...incoming,
            isBookmarked: this.isRouteBookmarked(this.selectedStation.stationId, incoming.busRouteId),
          });
        }
      });

      // 새 목록에서 제외된 노선 안전하게 제거
      for (let i = this.stationBuses.length - 1; i >= 0; i--) {
        const key = this.stationBuses[i].busRouteId || this.stationBuses[i].busRouteName;
        if (key && !incomingKeys.has(key)) {
          this.stationBuses.splice(i, 1);
        }
      }

      // 정렬 유지: 운행 중 우선, 도착 시간 빠른 순
      this.stationBuses.sort((a, b) => {
        if (a.isOperating !== b.isOperating) {
          return a.isOperating ? -1 : 1;
        }
        const t1 = a.predictTimeSec1 != null ? a.predictTimeSec1 : 99999;
        const t2 = b.predictTimeSec1 != null ? b.predictTimeSec1 : 99999;
        return t1 - t2;
      });
    },

    clearSelectedStation() {
      this.selectedStation = null;
      this.stationBuses = [];
      this.loadingStationBuses = false;
      this.refreshingStationBuses = false;
    },

    setRefreshInterval(seconds) {
      this.refreshIntervalSec = seconds;
      this.secondsUntilNextRefresh = seconds;
      this.setupAutoRefresh();
    },

    tickArrivalTimes(deltaSec = 1) {
      if (!deltaSec || deltaSec <= 0) return;

      // 1. 등록된 북마크 실시간 도착 초 차감 (기본적으로 시간이 흐르도록)
      if (Array.isArray(this.bookmarks)) {
        this.bookmarks.forEach((b) => {
          const info = b.arrivalInfo;
          if (info && info.isOperating) {
            if (typeof info.predictTimeSec1 === 'number' && info.predictTimeSec1 > 0) {
              info.predictTimeSec1 = Math.max(0, info.predictTimeSec1 - deltaSec);
            }
            if (typeof info.predictTimeSec2 === 'number' && info.predictTimeSec2 > 0) {
              info.predictTimeSec2 = Math.max(0, info.predictTimeSec2 - deltaSec);
            }
          }
        });
      }

      // 2. 정류장 상세 모달 내 실시간 도착 초 차감
      if (Array.isArray(this.stationBuses)) {
        this.stationBuses.forEach((bus) => {
          if (bus.isOperating) {
            if (typeof bus.predictTimeSec1 === 'number' && bus.predictTimeSec1 > 0) {
              bus.predictTimeSec1 = Math.max(0, bus.predictTimeSec1 - deltaSec);
            }
            if (typeof bus.predictTimeSec2 === 'number' && bus.predictTimeSec2 > 0) {
              bus.predictTimeSec2 = Math.max(0, bus.predictTimeSec2 - deltaSec);
            }
          }
        });
      }
    },

    setupAutoRefresh() {
      if (this.timerId) clearInterval(this.timerId);
      if (this.countdownTimerId) clearInterval(this.countdownTimerId);

      this.lastTickTimestamp = Date.now();
      this.secondsUntilNextRefresh = this.refreshIntervalSec > 0 ? this.refreshIntervalSec : 0;

      // 매 1초마다 실행: 도착 시간 초 단위 실시간 차감 + 주기적 자동 새로고침 진행
      this.countdownTimerId = setInterval(() => {
        const now = Date.now();
        const elapsedSec = Math.max(1, Math.round((now - this.lastTickTimestamp) / 1000));
        this.lastTickTimestamp = now;

        // 1. 기본적으로 모든 버스의 도착 시간이 실시간(초 단위)으로 흐르도록 차감
        this.tickArrivalTimes(elapsedSec);

        // 2. 자동 새로고침이 켜져 있는 경우 (refreshIntervalSec > 0)
        if (this.refreshIntervalSec > 0) {
          if (this.secondsUntilNextRefresh > elapsedSec) {
            this.secondsUntilNextRefresh -= elapsedSec;
          } else {
            this.secondsUntilNextRefresh = this.refreshIntervalSec;
            this.refreshArrivalsSilently();
            if (this.selectedStation) {
              // 팝업 열려 있을 시 깜빡임 없는 silent upsert로 서버 데이터와 재동기화
              this.selectStation(this.selectedStation, true);
            }
          }
        }
      }, 1000);
    },

    stopAutoRefresh() {
      if (this.timerId) clearInterval(this.timerId);
      if (this.countdownTimerId) clearInterval(this.countdownTimerId);
    },
  },
});
