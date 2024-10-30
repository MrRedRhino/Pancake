<script setup>
import Select from "@/components/Select.vue";
import {computed} from "vue";

const options = [
  {name: 'Start', id: 'start'},
  {name: 'Stop', id: 'stop'}
];

const model = defineModel();
const value = computed({
  get: () => model.value === 'start' ? options[0] : options[1],
  set: newValue => model.value = newValue.id,
});
</script>

<template>
  <Select v-model="value" :options="options"></Select>
</template>

<style scoped>

</style>