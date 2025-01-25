<script setup>
import {header, showBackButton, tabs} from "@/header.js";
import {servers} from "@/main.js";
import ServerStatus from "@/components/ServerStatus.vue";
import {ref} from "vue";

header.value = "Servers";
showBackButton.value = false;
tabs.value = [];

const importDialogVisible = ref(false);
const contextMenu = ref();
const items = ref([
  {
    label: "Rename",
    icon: "pi pi-pencil"
  },
  {
    label: "Delete",
    icon: "pi pi-trash",
    class: "text-red-500"
  }
]);
</script>

<template>
  <router-link v-for="server in servers" :to="{name: 'server', params: {serverId: server.id}}"
               @contextmenu="contextMenu.show($event)">
    <h1>{{ server.name }}</h1>
    <ServerStatus :server-id="server.id" :controls="false"></ServerStatus>
  </router-link>
  <ContextMenu ref="contextMenu" :model="items"/>

  <Dialog v-model:visible="importDialogVisible" header="Add a Server">

  </Dialog>
</template>

<style scoped>

</style>