<script setup>
import {useRoute} from "vue-router";
import JobListEntry from "@/components/JobListEntry.vue";
import {ref} from "vue";

const serverId = useRoute().params.serverId;
const jobs = ref();

fetch(`/api/servers/${serverId}/jobs`).then(res => res.json()).then(json => {
  jobs.value = [];
  for (let job of json) {
    job.serverId = serverId;
    jobs.value.push(job);
  }
});
</script>

<template>
  <JobListEntry v-for="job in jobs" :job="job"></JobListEntry>
</template>

<style scoped>

</style>