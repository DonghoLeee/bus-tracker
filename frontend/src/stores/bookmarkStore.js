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

    async selectStation(station) {
      this.selectedStation = station;
      this.loadingStationBuses = true;
      try {
        const response = await busApi.getStationBuses(station.stationId);
        this.stationBuses = response.data;
      } catch (error) {
        console.error('정류장 버스 조회 실패:', error);
        this.showToast('정류장 버스 정보를 불러올 수 없습니다.', 'error');
      } finally {
        this.loadingStationBuses = false;
      }
    },

    clearSelectedStation() {
      this.selectedStation = null;
      this.stationBuses = [];
    },

    setRefreshInterval(seconds) {
      this.refreshIntervalSec = seconds;
      this.secondsUntilNextRefresh = seconds;
      this.setupAutoRefresh();
    },

    setupAutoRefresh() {
      if (this.timerId) clearInterval(this.timerId);
      if (this.countdownTimerId) clearInterval(this.countdownTimerId);

      if (this.refreshIntervalSec <= 0) return;

      this.secondsUntilNextRefresh = this.refreshIntervalSec;

      // 1초마다 카운트다운
      this.countdownTimerId = setInterval(() => {
        if (this.secondsUntilNextRefresh > 1) {
          this.secondsUntilNextRefresh -= 1;
        } else {
          this.secondsUntilNextRefresh = this.refreshIntervalSec;
          this.refreshArrivalsSilently();
          if (this.selectedStation) {
            this.selectStation(this.selectedStation);
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
