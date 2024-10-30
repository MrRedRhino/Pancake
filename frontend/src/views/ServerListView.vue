<script setup>
import {header, showBackButton, tabs} from "@/header.js";
import {servers} from "@/main.js";
import ServerStatus from "@/components/ServerStatus.vue";

header.value = "Servers";
showBackButton.value = false;
tabs.value = [];
</script>

<template>
<router-link v-for="server in servers" :to="{name: 'server', params: {serverId: server.id}}">
  <h1>{{ server.name }}</h1>
  <ServerStatus :server-id="server.id" :controls="false"></ServerStatus>
</router-link>
</template>

<style scoped>

</style>