<script setup>
import {onMounted, reactive, ref} from "vue";
import {useRoute} from "vue-router";

const serverId = useRoute().params.serverId;
const logs = ref([]);
const logContentCache = reactive({});

async function load() {
  const response = await fetch(`/api/servers/${serverId}/logs`);
  logs.value = await response.json();

  // const log = await fetch(`/api/servers/${serverId}/logs/${logs.value[0]}`);
  // logContentCache[logs.value[0]] = await log.text();
}

load().then();

const root = ref();
const textAreas = ref([]);

let observer;
onMounted(() => {
  observer = new IntersectionObserver((entries) => console.log(entries), {
    // root: document,
  });

  textAreas.value.forEach(area => observer.observe(area));
});
</script>

<template>
  <div ref="root" class="flex flex-col gap-2 m-2 overflow-scroll h-screen">
    <div v-for="log in logs" ref="textAreas" class="bg-surface-900 rounded-xl p-2">
      <h1 class="text-xl border-b border-surface-600">{{ log }}</h1>

      <textarea class="log-content w-full"
                :style="{height: 24 * logContentCache[log]?.split('\n').length + 15 + 'px'}"
                wrap="off"
                autocorrect="off"
                autocapitalize="off"
                readonly="true"
                spellcheck="false">{{ logContentCache[log] }}</textarea>

      <!--      <VirtualScroller class="h-96" :item-size="24" :items="logContentCache[log]?.split('\n')">-->
      <!--        <template v-slot:item="{item, options}">-->
      <!--          <h1 class="log-content text-nowrap">{{ item }}</h1>-->
      <!--        </template>-->
      <!--      </VirtualScroller>-->
    </div>
  </div>
</template>

<style scoped>
.log-content {
  font-family: "JetBrainsMono", serif;
}

textarea {
  border: none;
  overflow: auto;
  outline: none;
  box-shadow: none;
  resize: none;
}
</style>