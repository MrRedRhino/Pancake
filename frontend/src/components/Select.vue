<script setup>
import ExpandButton from "@/components/ExpandButton.vue";
import {ref} from "vue";

const props = defineProps({
  options: {
    type: Array,
    required: true,
  },
  placeholder: {
    type: String,
    required: false,
    default: "Select..."
  }
});
const expanded = ref();
const selectedOption = defineModel();

function selectOption(option) {
  selectedOption.value = option;
  expanded.value = false;
}
</script>

<template>
  <div>
    <div class="trigger" @click="expanded = !expanded">
      <h3 v-if="selectedOption">{{ selectedOption.selectedName || selectedOption.name }}</h3>
      <h3 v-else>{{ placeholder }}</h3>
      <ExpandButton v-model="expanded"></ExpandButton>
    </div>

    <div class="values" v-if="expanded">
      <h3 v-for="option in props.options" @click="selectOption(option)">{{ option.name }}</h3>
    </div>
  </div>
</template>

<style scoped>
.trigger {
  position: relative;
  display: flex;
  align-items: center;
  width: fit-content;
  cursor: pointer;
}

.values {
  position: absolute;
  background: var(--color-background-soft);
  z-index: 100;
  padding: 5px;
  cursor: pointer;
}
</style>