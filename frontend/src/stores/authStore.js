import { defineStore } from 'pinia';
import { busApi } from '../api/busApi';
import { useBookmarkStore } from './bookmarkStore';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('auth_token') || null,
    guestDeviceId: null,
    loadingAuth: false,
    showAuthModal: false,
  }),

  getters: {
    isAuthenticated: (state) => !!state.user,
    isGoogleUser: (state) => state.user?.provider === 'GOOGLE',
    isGuestUser: (state) => state.user?.provider === 'GUEST',
  },

  actions: {
    getOrCreateDeviceId() {
      let deviceId = localStorage.getItem('guest_device_id');
      if (!deviceId) {
        deviceId = (typeof crypto !== 'undefined' && crypto.randomUUID)
          ? crypto.randomUUID()
          : 'device-' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
        localStorage.setItem('guest_device_id', deviceId);
      }
      this.guestDeviceId = deviceId;
      return deviceId;
    },

    async initAuth() {
      this.loadingAuth = true;
      const deviceId = this.getOrCreateDeviceId();

      try {
        if (this.token) {
          const res = await busApi.getMe();
          this.user = res.data;
        } else {
          // 기기 고유 게스트로 자동 로그인
          await this.loginGuestInternal(deviceId);
        }
      } catch (err) {
        console.warn('기존 토큰 인증 실패, 게스트로 재접속:', err);
        localStorage.removeItem('auth_token');
        this.token = null;
        await this.loginGuestInternal(deviceId);
      } finally {
        this.loadingAuth = false;

        // 첫 방문 유저(온보딩 미완료)인 경우 로그인 유도 모달 띄우기
        const onboardingSeen = localStorage.getItem('onboarding_seen');
        if (!onboardingSeen) {
          this.showAuthModal = true;
        }
      }
    },

    async loginGuestInternal(deviceId) {
      const res = await busApi.loginGuest(deviceId);
      this.token = res.data.token;
      this.user = res.data.user;
      localStorage.setItem('auth_token', this.token);
    },

    async continueAsGuest() {
      localStorage.setItem('onboarding_seen', 'true');
      this.showAuthModal = false;
      const bookmarkStore = useBookmarkStore();
      bookmarkStore.showToast('기기 저장 모드로 시작합니다.', 'info');
    },

    async loginWithGoogle(credential) {
      this.loadingAuth = true;
      try {
        const deviceId = this.guestDeviceId || localStorage.getItem('guest_device_id');
        const res = await busApi.loginGoogle(credential, deviceId);
        this.token = res.data.token;
        this.user = res.data.user;
        localStorage.setItem('auth_token', this.token);
        localStorage.setItem('onboarding_seen', 'true');
        this.showAuthModal = false;

        const bookmarkStore = useBookmarkStore();
        await bookmarkStore.fetchBookmarks();

        const migrated = res.data.migratedBookmarksCount;
        if (migrated > 0) {
          bookmarkStore.showToast(`환영합니다! 기기에 있던 ${migrated}개의 즐겨찾기가 구글 계정에 안전하게 동기화되었습니다.`, 'success');
        } else {
          bookmarkStore.showToast(`${this.user.name}님으로 구글 로그인되었습니다!`, 'success');
        }
        return true;
      } catch (err) {
        console.error('구글 로그인 실패:', err);
        const bookmarkStore = useBookmarkStore();
        bookmarkStore.showToast('구글 로그인에 실패했습니다. 다시 시도해 주세요.', 'error');
        return false;
      } finally {
        this.loadingAuth = false;
      }
    },

    async logout() {
      localStorage.removeItem('auth_token');
      this.token = null;
      this.user = null;
      const deviceId = this.getOrCreateDeviceId();
      await this.loginGuestInternal(deviceId);
      
      const bookmarkStore = useBookmarkStore();
      await bookmarkStore.fetchBookmarks();
      bookmarkStore.showToast('로그아웃되었습니다. (기기 저장 모드로 전환)', 'info');
    },

    openModal() {
      this.showAuthModal = true;
    },

    closeModal() {
      this.showAuthModal = false;
    }
  },
});
