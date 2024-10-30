<script setup>
const props = defineProps({
  type: {
    type: String,
    required: false,
    default: 'primary'
  }
});
</script>

<template>
<button :class="[props.type]">
  <slot></slot>
</button>
</template>

<style scoped>
button {
  position: relative;
  border: none;
  border-radius: 8px;
  height: 32px;
  background: none;
  cursor: pointer;
  padding: 5px;
  color: var(--color-text);
  font-size: 18px;
}

button.primary {
  background: var(--color-green);
}

button.destructive {
  background: var(--color-red);
}

button:hover {
filter: brightness(80%);
}
</style>