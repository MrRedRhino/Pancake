<script setup>
import {inject, ref, watch} from "vue";
import ModrinthLogo from "@/assets/ModrinthLogo.vue";
import CurseforgeLogo from "@/assets/CurseforgeLogo.vue";
import HangarLogo from "@/assets/HangarLogo.vue";
import LoadingButton from "@/components/LoadingButton.vue";

const dialogRef = inject("dialogRef");
const platforms = [
  {name: "Modrinth", code: "MODRINTH", logo: ModrinthLogo},
  {name: "Curseforge", code: "CURSEFORGE", logo: CurseforgeLogo},
  {name: "Hangar", code: "HANGAR", logo: HangarLogo}
];
const platform = ref(platforms[0]);
const query = ref("");
const results = ref([]);
let isSearching = false;

watch(query, () => {
  if (!isSearching) {
    search();
  }
}, {immediate: true});

watch(platform, () => {
  if (!isSearching) {
    search();
  }
});

function search() {
  const queryValue = query.value;
  const platformValue = platform.value.code;

  isSearching = true;
  const server = dialogRef.value.data.server;
  // fetch(`/api/mods/search?query=${queryValue}&platform=${platformValue}&loader=${server?.loader}&gameVersion=${server?.gameVersion}`).then(r => r.json()).then(r => {
  fetch(`/api/mods/search?query=${queryValue}&type=MODPACK&platform=${platformValue}&loader=${server?.loader || ''}&gameVersion=${server?.gameVersion || ''}`).then(r => r.json()).then(r => {
    isSearching = false;
    results.value = r.map(r => {
      r.platform = platformValue;
      return r;
    });
    if (query.value !== queryValue && !isSearching && platformValue !== platform.value.code) search();
  });
}

function isInstalled(result) {
  return dialogRef.value.data.installedAddons.filter(a => a.name === result.name).length > 0;
}

async function install(searchResult) {
  await dialogRef.value.data.onInstall(searchResult, dialogRef.value);
}
</script>

<template>
  <div class="max-w-screen-2xl w-[85dvw] h-[70dvh] max-h-[800px] flex flex-col">
    <div class="flex flex-row gap-2">
      <IconField class="w-full">
        <InputIcon class="pi pi-search"/>
        <InputText :fluid="true" v-model="query" placeholder="Search"/>
      </IconField>

      <Select :options="platforms" v-model="platform" option-label="name"
              :option-disabled="option => option.code === 'HANGAR'"
              pt:label:class="!pr-0">
        <template #option="slotProps">
          <div class="flex items-center">
            <component :is="slotProps.option.logo" class="w-4 mr-2"></component>
            <div>{{ slotProps.option.name }}</div>
          </div>
        </template>
      </Select>
    </div>

    <div v-if="results.length > 0" class="overflow-y-scroll h-full flex flex-col gap-2 mt-2">
      <a v-for="result in results" class="flex gap-2 items-start" :href="result.url" target="_blank">
        <img class="h-16 w-16" :src="result.iconUrl" alt="Icon">
        <div>
          <h1 class="overflow-hidden">{{ result.name }} by {{ result.author }}</h1>
          <h2 class="text-surface-400">{{ result.description }}</h2>
        </div>
        <LoadingButton @click.prevent="install(result)"
                       :icon="`pi ${isInstalled(result) ? 'pi-check' : 'pi-download'}`"
                       :disabled="isInstalled(result)"
                       class="aspect-square ml-auto w-[40px] self-center flex-shrink-0">
        </LoadingButton>
      </a>
    </div>
    <div v-else class="text-xl flex justify-center items-center h-full">
      <h1>No results :(</h1>
    </div>
  </div>
</template>

<style scoped>

</style>