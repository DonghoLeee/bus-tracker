import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach Auth Token & Device ID
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const deviceId = localStorage.getItem('guest_device_id');
  if (deviceId) {
    config.headers['X-Device-Id'] = deviceId;
  }
  return config;
});

export const busApi = {
  // 인증 관련 API
  loginGuest(deviceId) {
    return api.post('/auth/guest', { deviceId });
  },

  loginGoogle(credential, deviceId) {
    return api.post('/auth/google', { credential, deviceId });
  },

  getMe() {
    return api.get('/auth/me');
  },

  mergeBookmarks(guestDeviceId) {
    return api.post('/auth/merge', { guestDeviceId });
  },

  // 정류장 검색 (페이지네이션)
  searchStations(keyword = '', page = 0, size = 10) {
    return api.get('/stations', { params: { keyword, page, size } });
  },

  // 특정 정류장 정보 단건 조회
  getStation(stationId) {
    return api.get(`/stations/${stationId}`);
  },

  // 특정 정류장 경유 버스 목록 및 실시간 도착정보 조회
  getStationBuses(stationId) {
    return api.get(`/stations/${stationId}/buses`);
  },

  // 저장된 버스(북마크) 목록 조회 (실시간 도착정보 포함)
  getBookmarks() {
    return api.get('/bookmarks');
  },

  // 관심 버스 등록
  addBookmark(data) {
    return api.post('/bookmarks', data);
  },

  // 관심 버스 삭제 (ID 기준)
  deleteBookmark(id) {
    return api.delete(`/bookmarks/${id}`);
  },

  // 관심 버스 삭제 (정류장+노선 기준)
  deleteBookmarkByRoute(stationId, busRouteId) {
    return api.delete(`/bookmarks/station/${stationId}/route/${busRouteId}`);
  },

  // 전체 등록 버스 실시간 도착정보 새로고침
  refreshArrivals() {
    return api.get('/bookmarks/arrivals');
  },
};
