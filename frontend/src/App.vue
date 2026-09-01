<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useBookmarkStore } from './stores/bookmarkStore';
import Header from './components/Header.vue';
import StationSearch from './components/StationSearch.vue';
import BookmarkList from './components/BookmarkList.vue';
import StationBusList from './components/StationBusList.vue';
import Toast from './components/Toast.vue';
import { Sparkles, MapPin, Bus, Heart, Compass } from 'lucide-vue-next';

const store = useBookmarkStore();

onMounted(() => {
  store.fetchBookmarks();
  store.setupAutoRefresh();
});

onUnmounted(() => {
  store.stopAutoRefresh();
});
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex flex-col font-sans text-slate-800 antialiased selection:bg-indigo-500 selection:text-white">
    
    <!-- Top Navigation Header -->
    <Header />

    <!-- Main Content -->
    <main class="flex-1 py-8 sm:py-12">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <!-- Hero Intro Section -->
        <div class="text-center max-w-3xl mx-auto mb-8">
          <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-50 border border-indigo-200/60 text-indigo-700 text-xs font-bold mb-3 shadow-2xs">
            <Sparkles class="w-3.5 h-3.5 text-indigo-600" />
            <span>실시간 대중교통 모니터링 시스템</span>
          </div>
          <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-900 tracking-tight leading-tight">
            어디서든 내 버스를 <span class="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-violet-600">한눈에 추적</span>하세요
          </h1>
          <p class="mt-3 text-sm sm:text-base text-slate-600">
            정류장을 검색하고 자주 타는 버스를 등록하면, 실시간 도착 시간과 남은 정류장을 실시간으로 자동 갱신해 드립니다.
          </p>
        </div>

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

    <!-- Global Floating Toast Notification -->
    <Toast />

    <!-- Footer -->
    <footer class="bg-white border-t border-slate-200 py-8 text-center text-xs text-slate-400">
      <div class="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>© 2026 Realtime Bus Arrival Tracker · Spring Boot & Vue.js</p>
        <div class="flex items-center gap-4 text-slate-500">
          <span>H2 In-Memory DB</span>
          <span>·</span>
          <span>Dynamic Simulator Engine</span>
          <span>·</span>
          <span>TAGO Open API Ready</span>
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
