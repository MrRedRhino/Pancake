<script setup>
import {ref, watch} from "vue";

const props = defineProps({
  whenEmpty: {
    type: Number,
    default: 0
  },
  min: {
    type: Number,
    default: undefined
  }
});
const value = ref();
const model = defineModel();
value.value = model.value;

watch(value, newValue => {
  const trimmed = newValue.toString().trim();
  if (trimmed.length < 1) {
    model.value = props.whenEmpty;
  } else {
    const int = Number.parseInt(trimmed);
    if (!isNaN(int) && int >= props.min) {
      model.value = int;
    }
  }
});
</script>

<template>
  <input v-model="value" type="number" :min="props.min">
</template>

<style scoped>

</style>