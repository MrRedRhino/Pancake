<script setup>
import {ref, watch} from "vue";
import {zodResolver} from '@primevue/forms/resolvers/zod';
import {z} from 'zod';
import {servers} from "@/main.js";
import {useDialog} from "primevue";
import AddonSearchDialog from "@/components/AddonSearchDialog.vue";

const model = defineModel("visible");
const emit = defineEmits(["close"]);
const dialog = useDialog();

const loaders = [
  {name: "Fabric"},
  {name: "Forge"},
  {name: "Paper"},
  {name: "Purpur"},
  {name: "Velocity"},
  {name: "Vanilla"},
];
const loader = ref(loaders[0]);
const showUnstableVersions = ref(false);
const gameVersions = ref([]);

const addServerModes = [
  {id: 'install', label: 'Install'},
  {id: 'import', label: 'Import'},
];
const addServerMode = ref(addServerModes[0]);
const templateModes = [
  {id: "custom", label: "Custom"},
  {id: "modpack", label: "Modpack"},
  {id: "file", label: "File"}
];
const templateMode = ref(templateModes[0]);

const selectedModpack = ref();

const resolver = ref(zodResolver(
    z.object({
      name: z.string({required: true}).min(1, {message: "Required"}),
      path: z.union([z.record(z.boolean()), z.literal(null)]).refine(obj => obj !== null && Object.keys(obj).length > 0, {message: "Required"}),
      loader: z.union([
        z.object({name: z.string().min(1, 'Required')}),
        z.any().refine(val => false, {message: 'Required'})
      ]),
      gameVersion: z.union([
        z.object({gameVersion: z.string().min(1, 'Required')}),
        z.any().refine(val => false, {message: 'Required'})
      ]),
    })
));
const nameInput = ref();
const form = ref();
const serverName = ref("");
const importDirectories = ref([]);

async function fetchRoot(path) {
  const response = await fetch(`/api/files/server-directories?path=${encodeURIComponent(path)}`);

  importDirectories.value = (await response.json()).map(e => {
    return {key: e.path, label: e.name, leaf: !e.hasChildren, icon: "pi pi-folder"}
  });
}

async function fetchNodeChildren(node) {
  const response = await fetch(`/api/files/server-directories?path=${encodeURIComponent(node.key)}`);
  node.children = (await response.json()).map(e => {
    return {key: e.path, label: e.name, leaf: !e.hasChildren, icon: e.isServer ? "pi pi-database" : "pi pi-folder"}
  });
}

fetchRoot("/");

watch(loader, async value => {
  const response = await fetch(`/api/loaders/${value.name}/game-versions`);
  gameVersions.value = await response.json();
}, {immediate: true});

function updateName(path) {
  const keys = Object.keys(path);
  if (keys.length === 1) {
    const dirName = keys[0].split("/");
    serverName.value = dirName[dirName.length - 1];
  }
}

watch(serverName, v => {
  nameInput.value.formField.onChange({value: serverName.value});
});

async function importServer(event) {
  if (event.values) {
    const response = await fetch(`/api/servers/import`, {
      method: "POST",
      body: JSON.stringify({
        name: event.values.name,
        path: Object.keys(event.values.path)[0],
        loader: event.values.loader.name.toUpperCase(),
        gameVersion: event.values.gameVersion.gameVersion,
      })
    });

    if (response.ok) {
      const body = await response.json();
      servers[body.id] = {
        ...body,
        log: [],
        state: "stopped"
      }
      emit("close");
    }
  }
}

function onModpackSelected(modpack, dialog) {
  selectedModpack.value = modpack;
  serverName.value = modpack.name;
  dialog.close();
}
</script>

<template>

  <Dialog class="w-80" v-model:visible="model" header="Add a Server">
    <DynamicDialog></DynamicDialog>

    <div class="w-full">
      <SelectButton v-model="addServerMode" class="w-full mb-2" pt:pcToggleButton:root:class="w-[50%]" size="small"
                    :options="addServerModes" option-label="label"/>

      <div>
        <Form ref="form" :initial-values="{loader: {name: 'Fabric'}}" :resolver="resolver" @submit="importServer"
              :validate-on-value-update="false">
          <FormField name="path" v-slot="$field" class="mb-2">
            <label>{{ addServerMode.id === "import" ? "Directory" : "Parent Directory" }}</label>
            <TreeSelect :options="importDirectories"
                        filter
                        fluid
                        loading-mode="icon"
                        @node-expand="fetchNodeChildren"
                        @change="updateName"/>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </FormField>

          <FormField name="name" v-slot="$field" class="mb-2">
            <label>Server Name</label>
            <InputText ref="nameInput" v-model="serverName" fluid></InputText>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </FormField>

          <div v-if="addServerMode.id === 'install'">
            <label>Template</label>
            <SelectButton v-model="templateMode" class="w-full mb-2" pt:pcToggleButton:root:class="w-[50%]" size="small"
                          :options="templateModes" option-label="label"/>

            <div :hidden="templateMode.id !== 'custom'">
              <FormField name="loader" v-slot="$field" class="mb-2">
                <label>Loader</label>
                <Select fluid :options="loaders" option-label="name" v-model="loader"></Select>
                <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
                  {{ $field.error?.message }}
                </Message>
              </FormField>

              <FormField name="gameVersion" v-slot="$field">
                <label>Game Version</label>
                <Select fluid :options="gameVersions.filter(v => showUnstableVersions || v.stable)"
                        option-label="gameVersion"></Select>

                <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
                  {{ $field.error?.message }}
                </Message>
              </FormField>

              <div class="flex items-center gap-2">
                <Checkbox v-model="showUnstableVersions" input-id="unstable-versions" binary/>
                <label for="unstable-versions">Show unstable versions</label>
              </div>
            </div>

            <div :hidden="templateMode.id !== 'modpack'">
              <InputGroup>
                <InputGroupAddon class="w-full !justify-normal gap-2">
                  <template v-if="selectedModpack">
                    <img class="max-h-6 max-w-6" :src="selectedModpack.iconUrl" alt="">
                    <h1 class="overflow-hidden text-nowrap overflow-ellipsis">{{ selectedModpack.name }}</h1>
                  </template>
                  <h1 v-else>Select a modpack</h1>
                </InputGroupAddon>
                <Button variant="outlined" severity="contrast" icon="pi pi-search"
                        @click="dialog.open(AddonSearchDialog, {props: {header: 'Search Mods'}, data: {onInstall: onModpackSelected, installedAddons: [], server: null}})"/>
              </InputGroup>
            </div>

            <div :hidden="templateMode.id !== 'file'">

            </div>
          </div>

          <Button type="submit" class="mt-4" fluid :label="addServerMode?.label"/>
        </Form>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>

</style>