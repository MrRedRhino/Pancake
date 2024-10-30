<script setup>
import {header, showBackButton, tabs} from "@/header.js";
import {servers} from "@/main.js";
import {useRoute} from "vue-router";
import ServerDashboard from "@/components/ServerDashboard.vue";

const serverId = useRoute().params.serverId;
header.value = servers[serverId]?.name;
showBackButton.value = true;
tabs.value = [
  {
    title: "Dashboard",
    route: {name: 'server', params: {serverId: serverId}}
  },
  {
    title: "Jobs",
    route: {name: 'jobs', params: {serverId: serverId}}
  }
];
</script>

<template>
  <router-view>

  </router-view>
</template>

<style scoped>

</style>