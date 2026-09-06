<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useBookmarkStore } from './stores/bookmarkStore';
import { useAuthStore } from './stores/authStore';
import Header from './components/Header.vue';
import StationSearch from './components/StationSearch.vue';
import BookmarkList from './components/BookmarkList.vue';
import StationBusList from './components/StationBusList.vue';
import AuthModal from './components/AuthModal.vue';
import Toast from './components/Toast.vue';

const store = useBookmarkStore();
const authStore = useAuthStore();

onMounted(async () => {
  await authStore.initAuth();
  await store.fetchBookmarks();
  store.setupAutoRefresh();
});

onUnmounted(() => {
  store.stopAutoRefresh();
});
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex flex-col font-sans text-slate-800 antialiased selection:bg-indigo-500 selection:text-white">
    
    <!-- Top Navigation Header with Unified Title & Tagline -->
    <Header />

    <!-- Main Content -->
    <main class="flex-1 py-5 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        <!-- Station Search Bar & Hotspots -->
        <StationSearch />

        <!-- Registered Buses Dashboard Section -->
        <div class="mt-10">
          <BookmarkList />
        </div>

      </div>
    </main>

    <!-- Station Bus Details Modal -->
    <StationBusList />

    <!-- Google Login / Guest Onboarding Modal -->
    <AuthModal />

    <!-- Global Floating Toast Notification -->
    <Toast />

    <!-- Footer -->
    <footer class="bg-white border-t border-slate-200 py-8 text-center text-xs text-slate-400">
      <div class="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>© 2026 Realtime Bus Arrival Tracker · Spring Boot & Vue.js</p>
        <div class="flex items-center gap-4 text-slate-500">
          <span>MySQL & Cloud Sync Ready</span>
          <span>·</span>
          <span>Dynamic Simulator Engine</span>
          <span>·</span>
          <span>Seoul TOPIS & Open API</span>
        </div>
      </div>
    </footer>

  </div>
</template>

<style>
/* Smooth scrolling */
html {
  scroll-behavior: smooth;
}
</style>
