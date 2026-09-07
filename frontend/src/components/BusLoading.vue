<script setup>
import { Bus } from 'lucide-vue-next';

defineProps({
  message: {
    type: String,
    default: '실시간 버스 정보를 불러오는 중...'
  },
  subMessage: {
    type: String,
    default: '잠시만 기다려 주세요'
  },
  size: {
    type: String,
    default: 'default' // 'sm' | 'default'
  }
});
</script>

<template>
  <div class="flex flex-col items-center justify-center py-10 sm:py-14 select-none">
    
    <!-- Bus Animation Stage -->
    <div class="relative flex items-center justify-center mb-5">
      
      <!-- Exhaust Smoke Puffs (연기를 뿜고 가는 효과) -->
      <div class="absolute -left-7 sm:-left-9 bottom-3 flex items-center pointer-events-none">
        <span class="smoke-puff smoke-1"></span>
        <span class="smoke-puff smoke-2"></span>
        <span class="smoke-puff smoke-3"></span>
        <span class="smoke-puff smoke-4"></span>
      </div>

      <!-- Bus Icon Container (사용자가 지정한 라운드 박스 아이콘) -->
      <div
        class="relative z-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center shadow-md shadow-indigo-100/80 border border-indigo-100/90 bus-bounce"
        :class="size === 'sm' ? 'w-12 h-12' : 'w-16 h-16'"
      >
        <!-- Wind speed lines on front (우측 바람선) -->
        <div class="absolute -right-3 top-3 flex flex-col gap-1 pointer-events-none opacity-60">
          <span class="wind-line wind-1"></span>
          <span class="wind-line wind-2"></span>
        </div>

        <Bus :class="size === 'sm' ? 'w-6 h-6' : 'w-8 h-8 text-indigo-600'" />
      </div>

      <!-- Moving Ground Road Track (달리는 도로 트랙) -->
      <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 w-36 sm:w-44 h-1 overflow-hidden pointer-events-none">
        <div class="road-track"></div>
      </div>
    </div>

    <!-- Text Feedback -->
    <div class="text-center px-4">
      <h4 class="text-sm sm:text-base font-bold text-slate-800 tracking-tight flex items-center justify-center gap-1">
        <span>{{ message }}</span>
        <span class="inline-flex text-indigo-600 animate-pulse font-mono font-bold">...</span>
      </h4>
      <p v-if="subMessage" class="text-xs text-slate-400 mt-1 font-medium">
        {{ subMessage }}
      </p>
    </div>

  </div>
</template>

<style scoped>
/* 버스 상하 진동 / 달리는 모션 (Suspension Bobbing) */
.bus-bounce {
  animation: busDrive 0.8s ease-in-out infinite alternate;
}

@keyframes busDrive {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-4px) rotate(-1deg);
  }
  100% {
    transform: translateY(-1px) rotate(1deg);
  }
}

/* 연기 뿜기 (Exhaust Smoke Puffs) */
.smoke-puff {
  position: absolute;
  border-radius: 9999px;
  background: radial-gradient(circle, rgba(165, 180, 252, 0.8) 0%, rgba(224, 231, 255, 0.4) 60%, rgba(241, 245, 249, 0) 100%);
  animation: exhaustPuff 1.2s cubic-bezier(0.2, 0.8, 0.4, 1) infinite;
}

.smoke-1 {
  width: 14px;
  height: 14px;
  bottom: 0px;
  right: 0px;
  animation-delay: 0s;
}

.smoke-2 {
  width: 18px;
  height: 18px;
  bottom: 4px;
  right: 6px;
  animation-delay: 0.3s;
}

.smoke-3 {
  width: 22px;
  height: 22px;
  bottom: 8px;
  right: 12px;
  animation-delay: 0.6s;
}

.smoke-4 {
  width: 26px;
  height: 26px;
  bottom: 12px;
  right: 18px;
  animation-delay: 0.9s;
}

@keyframes exhaustPuff {
  0% {
    transform: scale(0.2) translate(0, 0);
    opacity: 0.9;
  }
  50% {
    opacity: 0.6;
  }
  100% {
    transform: scale(1.6) translate(-36px, -12px);
    opacity: 0;
  }
}

/* 전면 바람선 효과 (Speed Wind Lines) */
.wind-line {
  height: 2px;
  background: linear-gradient(to right, rgba(99, 102, 241, 0.6), transparent);
  border-radius: 1px;
  animation: windSpeed 0.6s linear infinite;
}

.wind-1 {
  width: 14px;
  animation-delay: 0.1s;
}

.wind-2 {
  width: 20px;
  animation-delay: 0.35s;
}

@keyframes windSpeed {
  0% {
    transform: translateX(0);
    opacity: 0.8;
  }
  100% {
    transform: translateX(12px);
    opacity: 0;
  }
}

/* 바닥 도로 트랙 애니메이션 */
.road-track {
  width: 200%;
  height: 100%;
  background-image: repeating-linear-gradient(
    90deg,
    #cbd5e1 0px,
    #cbd5e1 10px,
    transparent 10px,
    transparent 20px
  );
  animation: roadMove 0.5s linear infinite;
}

@keyframes roadMove {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-20px);
  }
}
</style>
