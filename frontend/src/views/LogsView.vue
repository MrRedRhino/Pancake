<script setup>
import {ref} from "vue";
import {useRoute} from "vue-router";

const serverId = useRoute().params.serverId;
const logs = ref([]);
const logContent = ref("");

fetch(`/api/servers/${serverId}/logs`).then(r => r.json()).then(json => logs.value = json);
fetch(`/api/servers/${serverId}/logs/latest`).then(r => r.text()).then(text => logContent.value = text);
</script>

<template>
  <h1 v-for="log in logs">
    {{ log }}
  </h1>

  <h1>{{ logContent }}</h1>
</template>

<style scoped>

</style>