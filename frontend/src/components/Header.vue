<script setup>
import {header, showBackButton, tabs} from "../header.js";
import IconButton from "@/components/IconButton.vue";
</script>

<template>
  <div class="header">
    <div class="wrapper">
      <IconButton @click="$router.push('/')" v-if="showBackButton" icon="back"></IconButton>
      <h1>{{ header }}</h1>

    </div>
    <div class="tabs">
      <router-link :to="tab.route" v-for="tab in tabs">
        {{ tab.title }}
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.header {
  margin-bottom: 8px;
}

.wrapper {
  display: flex;
  background: var(--color-background-mute);
  align-items: center;
}

.tabs {
  display: flex;
  gap: 10px;
  font-size: 20px;
  background: var(--color-background-mute);
}

.tabs a.router-link-exact-active {
  border-bottom: var(--vt-c-indigo) 3px solid;
}
</style>