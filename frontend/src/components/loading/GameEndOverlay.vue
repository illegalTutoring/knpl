<template>
  <div class="game_overlay" v-show="show">
    <p class="message">{{ contents }}</p>
    <button class="tryAgain" @click="restart">Try again</button>
  </div>
</template>

<script setup>
import { toRefs, computed } from "vue";

// props를 정의합니다.
const props = defineProps({
  board: {
    type: Object,
    required: true,
  },
  onrestart: {
    type: Function,
    required: true,
  },
});

const { board, onrestart } = toRefs(props);

// board의 상태에 따라 메시지를 보여줄지 결정하는 계산된 속성입니다.
const show = computed(() => {
  return board.value.hasWon() || board.value.hasLost();
});

// 게임의 결과에 따라 다른 내용을 보여주는 계산된 속성입니다.
const contents = computed(() => {
  if (board.value.hasWon()) {
    return "Good Job!";
  } else if (board.value.hasLost()) {
    return "Game Over";
  } else {
    return "";
  }
});

// 게임을 재시작하는 함수입니다.
const restart = () => {
  if (onrestart.value) {
    onrestart.value();
  }
};
</script>

<style scoped lang="scss">
// game
@import "./scss/main.scss";
@import "./scss/style.scss";
</style>
