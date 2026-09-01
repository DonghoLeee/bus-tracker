<script setup>
import { computed } from 'vue';
import { useBookmarkStore } from '../stores/bookmarkStore';
import { CheckCircle, AlertCircle, Info, XCircle } from 'lucide-vue-next';

const store = useBookmarkStore();
const message = computed(() => store.toastMessage);
const type = computed(() => store.toastType);

const getToastStyle = (t) => {
  switch (t) {
    case 'success':
      return 'bg-emerald-900/90 text-white border-emerald-500/50 shadow-emerald-950/20';
    case 'error':
      return 'bg-rose-900/90 text-white border-rose-500/50 shadow-rose-950/20';
    case 'warning':
      return 'bg-amber-900/90 text-white border-amber-500/50 shadow-amber-950/20';
    default:
      return 'bg-slate-900/90 text-white border-slate-700 shadow-slate-950/20';
  }
};
</script>

<template>
  <transition name="toast">
    <div
      v-if="message"
      class="fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-2xl border backdrop-blur-md shadow-xl text-sm font-medium"
      :class="getToastStyle(type)"
    >
      <CheckCircle v-if="type === 'success'" class="w-5 h-5 text-emerald-400 flex-shrink-0" />
      <XCircle v-else-if="type === 'error'" class="w-5 h-5 text-rose-400 flex-shrink-0" />
      <AlertCircle v-else-if="type === 'warning'" class="w-5 h-5 text-amber-400 flex-shrink-0" />
      <Info v-else class="w-5 h-5 text-indigo-400 flex-shrink-0" />

      <span>{{ message }}</span>
    </div>
  </transition>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}
</style>
