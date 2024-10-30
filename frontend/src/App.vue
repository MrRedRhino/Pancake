<script setup>
import {initialLoad} from "@/main.js";
import Header from "@/components/Header.vue";
</script>

<template>
  <Header></Header>
  <router-view v-if="initialLoad">
  </router-view>
</template>

<style scoped>

</style>
