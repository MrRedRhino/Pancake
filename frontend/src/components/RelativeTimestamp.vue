<script setup>
import {onMounted, onUnmounted, ref} from "vue";

const props = defineProps({
  date: {
    type: Date,
    required: true
  }
});
let intervalId;
const message = ref(updateMessage());

function updateMessage() {
  const interval = Math.ceil((Date.now() - props.date) / 60_000);
  if (interval < 60) {
    return `${interval} Minutes`;
  } else if (interval < 1440) {
    return `${Math.ceil(interval / 60)} Hours`;
  } else {
    return `${Math.floor(interval / 1440)} Days`;
  } // TODO plural/singular
}

onMounted(() => {
  intervalId = setInterval(() => {
    message.value = updateMessage();
  }, 10_000);
});

onUnmounted(() => clearInterval(intervalId));
</script>

<template>
  {{ message }}
</template>

<style scoped>

</style>