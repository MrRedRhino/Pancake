<script setup>
import ServerDashboard from "@/components/ServerDashboard.vue";
import {useRoute} from "vue-router";

const serverId = useRoute().params.serverId;
</script>

<template>
<ServerDashboard :server-id="serverId"></ServerDashboard>
</template>

<style scoped>

</style>