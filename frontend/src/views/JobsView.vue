<script setup>
import {useRoute} from "vue-router";
import IconButton from "@/components/IconButton.vue";
import JobListEntry from "@/components/JobListEntry.vue";

const serverId = useRoute().params.serverId;
const job = {
  name: "Daily restart",
  tasks: []
};
</script>

<template>
  <JobListEntry :job="job"></JobListEntry>
</template>

<style scoped>

</style>