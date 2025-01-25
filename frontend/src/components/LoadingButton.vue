<script setup>
import {ref} from "vue";

const props = defineProps(["onClick"])
const loading = ref(false);

async function click(...args) {
  loading.value = true;
  try {
    await props.onClick(...args);
  } catch (error) {
    loading.value = false;
    throw error;
  }
  loading.value = false;
}
</script>

<template>
  <Button @click="click" :loading="loading"></Button>
</template>

<style scoped>

</style>