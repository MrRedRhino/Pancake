<script setup>
import Icon from "@/components/Icon.vue";

const props = defineProps({
  icon: String
});
</script>

<template>
<button>
<Icon :name="props.icon"></Icon>
</button>
</template>

<style scoped>
button {
  position: relative;
  border: none;
  border-radius: 100%;
  width: 32px;
  height: 32px;
  background: none;
  cursor: pointer;
  padding: 0;
}

svg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

button:hover {
  background: var(--color-background-mute);
}
</style>