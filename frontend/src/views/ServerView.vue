<script setup>
import {header, showBackButton, tabs} from "@/header.js";
import {servers} from "@/main.js";
import {useRoute} from "vue-router";

const serverId = useRoute().params.serverId;
header.value = servers[serverId]?.name;
showBackButton.value = true;
tabs.value = [
  {
    title: "Dashboard",
    route: {name: 'server', params: {serverId: serverId}}
  }
];

if (servers[serverId].showPluginFolder) {
  tabs.value.push({
    title: "Plugins",
    route: {name: 'plugins', params: {serverId: serverId}}
  });
}

if (servers[serverId].showModsFolder) {
  tabs.value.push({
    title: "Mods",
    route: {name: 'mods', params: {serverId: serverId}}
  });
}

if (servers[serverId].showDatapacksFolder) {
  tabs.value.push({
    title: "Datapacks",
    route: {name: 'datapacks', params: {serverId: serverId}}
  });
}

tabs.value.push({
  title: "Files",
  route: {name: 'jobs', params: {serverId: serverId}}
});


tabs.value.push({
  title: "Backups",
  route: {name: 'jobs', params: {serverId: serverId}}
});

tabs.value.push({
  title: "Jobs",
  route: {name: 'jobs', params: {serverId: serverId}}
});

tabs.value.push({
  title: "Settings",
  route: {name: 'jobs', params: {serverId: serverId}}
});
</script>

<template>
  <router-view>

  </router-view>
</template>

<style scoped>

</style>