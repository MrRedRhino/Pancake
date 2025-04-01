<script setup>
import {header, showBackButton, tabs} from "@/header.js";
import {servers} from "@/main.js";
import ServerStatus from "@/components/ServerStatus.vue";
import {ref} from "vue";
import AddServerDialog from "@/components/AddServerDialog.vue";

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
  <div class="flex items-center flex-col w-full p-2 gap-2">
    <router-link v-for="server in servers" :to="{name: 'server', params: {serverId: server.id}}"
                 class="bg-surface-900 rounded-lg p-2 flex gap-2 max-w-4xl w-full"
                 @contextmenu="contextMenu.show($event)">
      <ServerStatus :server-id="server.id" :controls="false"></ServerStatus>
      <h1 class="text-lg">{{ server.name }}</h1>
    </router-link>
    <div class="absolute top-0 h-full flex justify-center w-full pointer-events-none">
      <Button class="self-end mb-10 pointer-events-auto" label="Add a Server" icon="pi pi-plus"
              @click="importDialogVisible = true"/>
    </div>
    <ContextMenu ref="contextMenu" :model="items"/>
    <AddServerDialog v-model:visible="importDialogVisible" @close="importDialogVisible = false"/>
  </div>
</template>

<style scoped>

</style>