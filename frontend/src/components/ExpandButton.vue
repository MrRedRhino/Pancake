<script setup>
import IconButton from "@/components/IconButton.vue";

const expanded = defineModel({
  default: false
});
</script>

<template>
<IconButton :class="[expanded ? 'opened' : 'closed', 'toggle-button']" icon="back"></IconButton>
</template>

<style scoped>
.toggle-button {
  transition: rotate 0.3s;
}

.closed {
  rotate: -180deg;
}

.opened {
  rotate: -90deg;
}
</style>