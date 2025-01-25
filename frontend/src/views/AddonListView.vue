<script setup>
import {useRoute} from "vue-router";
import CurseforgeLogo from "@/assets/CurseforgeLogo.vue";
import ModrinthLogo from "@/assets/ModrinthLogo.vue";
import {servers} from "@/main.js";
import {reactive, ref} from "vue";
import axios from "axios";
import {useConfirm, useDialog, useToast} from "primevue";
import AddonSearchDialog from "@/views/AddonSearchDialog.vue";

const dialog = useDialog();
const confirm = useConfirm();
const toast = useToast();
const serverId = useRoute().params.serverId;
const props = defineProps({
  type: {
    required: true,
    type: String
  }
});

const loaded = ref(false);
const fileInput = ref();
const mods = reactive([]);

let addonDropdownSelected = null;
const addonDropdown = ref();
const addonDropdownItems = ref([
  {
    label: "Rename",
    icon: "pi pi-pencil"
  },
  {
    label: "Change version",
    icon: "pi pi-sort-alt"
  },
  {
    label: "Delete",
    icon: "pi pi-trash",
    class: "text-red-500",
    command: () => confirmDelete(addonDropdownSelected)
  }
]);

fetch(`/api/servers/${serverId}/${props.type}`).then(r => r.json()).then(data => {
  mods.push(...data);
  loaded.value = true;
});

function sortPageUrls(pageUrls) {
  const priorities = servers[serverId].modPlatformPriorities;

  return pageUrls.map(url => {
    const colon = url.indexOf(":");
    return {
      platform: url.substring(0, colon),
      url: url.substring(colon + 1)
    }
  }).sort((x, y) => {
    const indexX = priorities.indexOf(x.platform);
    const indexY = priorities.indexOf(y.platform);
    if (indexX === -1 && indexY === -1) return 0;
    if (indexX === -1) return 1;
    if (indexY === -1) return -1;
    return indexX - indexY;
  });
}

function getPlatformLogo(platform) {
  switch (platform) {
    case "modrinth":
      return ModrinthLogo;
    case "curseforge":
      return CurseforgeLogo;
  }
}

async function setEnabled(file, enabled) {
  await fetch(`/api/servers/${serverId}/${props.type}/${file}/${enabled ? 'enable' : 'disable'}`, {method: "POST"});
}

async function uploadFile() {
  const filename = fileInput.value.files[0].name;
  const mod = reactive({
        "filename": filename,
        "name": null,
        "author": null,
        "pageUrls": null,
        "iconUrl": null,
        "version": null,
        "enabled": true,
        "progress": 1
      }
  )
  mods.unshift(mod);

  try {
    await axios.post(`/api/servers/${serverId}/files/upload?path=${props.type}/${filename}`, fileInput.value.files[0], {
      onUploadProgress: event => mod.progress = event.progress * 95,
    });
  } catch (error) {
    mods.splice(mods.indexOf(mod), 1);
  }
  mod.progress = 100;

  setTimeout(() => {
    mod.progress = null;
    toast.add({
      severity: "success",
      summary: "Upload Complete",
      detail: `${filename} has been uploaded`,
      life: 3000,
      group: "br"
    });
  }, 500);
}

function openFileDialog() {
  fileInput.value.click();
}

async function deleteEntry(entry) {
  const response = await fetch(`/api/servers/${serverId}/files?path=${props.type}/${encodeURIComponent(entry.filename)}`, {method: "DELETE"});

  if (response.ok) {
    mods.splice(mods.indexOf(entry), 1);
  }
}

function confirmDelete(mod) {
  confirm.require({
    message: `Are you sure you want to delete ${mod.name || mod.filename}?`,
    header: "Delete addon?",
    acceptProps: {
      label: "Delete",
      severity: "danger"
    },
    rejectProps: {
      label: "Cancel",
      severity: "secondary"
    },
    accept: () => deleteEntry(mod)
  });
}

async function installAddon(searchResult) {
  const response = await fetch(`/api/servers/${serverId}/${props.type}/install?versionUri=${searchResult.versionInfo}`, {
    method: "POST",
  });
  if (response.ok) {
    mods.unshift({
      "id": searchResult.id,
      "filename": "",
      "name": searchResult.title,
      "author": searchResult.author,
      "pageUrls": [`${searchResult.platform}://${searchResult.url}`],
      "iconUrl": searchResult.iconUrl,
      "version": "",
      "enabled": true,
    });
  }
}
</script>

<template>
  <div class="m-2">
    <DynamicDialog></DynamicDialog>
    <ConfirmDialog></ConfirmDialog>
    <ContextMenu ref="addonDropdown" :model="addonDropdownItems"/>
    <Toast position="bottom-right" group="br"/>
    <input type="file" ref="fileInput" class="hidden" @change="uploadFile()">

    <div class="flex gap-2 mb-2">
      <SplitButton icon="pi pi-plus" :label="`Install ${props.type}`"
                   :model="[{label: 'Upload file', command: openFileDialog}]"
                   @click="dialog.open(AddonSearchDialog, {props: {header: 'Search Mods'}, data: {onInstall: installAddon, installedAddons: mods}})"></SplitButton>
      <Button icon="pi pi-sync" label="Fetch updates"/>
    </div>

    <div class="flex flex-col gap-3">
      <a v-if="loaded" v-for="mod in mods" class="rounded-xl bg-surface-900 h-20 relative"
         @contextmenu="addonDropdownSelected = mod; addonDropdown.show($event)">
        <div v-if="mod.progress" class="bg-emerald-900 rounded-xl h-full absolute transition-[width] duration-500"
             :style="{width: `${mod.progress}%`}"></div>

        <div class="p-2 flex items-center flex-row gap-3 z-10 relative h-full">
          <div class="w-16">
            <img v-if="mod.iconUrl" width="64px" height="64px" :src="mod.iconUrl">
            <div v-else class="pi pi-box" style="font-size: 3rem"></div>
          </div>

          <div>
            <h1>{{ mod.name || mod.filename }} <a class="text-surface-400">{{ mod.version }}</a></h1>
            <h1 v-if="mod.author" class="text-surface-400">by {{ mod.author }}</h1>
          </div>

          <div v-if="mod.pageUrls" class="flex flex-row gap-3" v-for="url in sortPageUrls(mod.pageUrls)">
            <a target="_blank" :href="url.url">
              <component :is="getPlatformLogo(url.platform)" class="p-2 w-12 rounded-full bg-surface-800"></component>
            </a>
          </div>

          <ToggleSwitch class="ml-auto" v-model="mod.enabled"
                        @value-change="value => setEnabled(mod.filename, value)"></ToggleSwitch>
        </div>
      </a>

      <Skeleton v-else v-for="i in 3" width="100%" height="5rem"></Skeleton>
    </div>
  </div>
</template>

<style scoped>

</style>