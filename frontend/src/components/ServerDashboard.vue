<script setup>
import {nextTick, onMounted, ref, watch} from "vue";
import ServerStatus from "@/components/ServerStatus.vue";
import {servers, subscribeToLog} from "@/main.js";

const props = defineProps({
  serverId: {
    required: true,
  }
});

const scrollPanel = ref();
const content = servers[props.serverId].log;
const history = ref([]);
const historyDropdownItems = ref([]);
const command = ref("");

async function sendCommand(event) {
  if (!event.shiftKey && command.value.trim() !== "") {
    await fetch(`/api/servers/${props.serverId}/command`, {
      method: "POST",
      body: JSON.stringify({
        command: command.value,
      })
    });
    history.value = history.value.filter(v => v !== command.value);
    history.value.unshift(command.value);
    command.value = "";
  }
}

watch(content, value => {
  const scroller = scrollPanel.value.$refs.content;
  const scrollToBottom = (scroller.scrollHeight - scroller.clientHeight) - scroller.scrollTop < 3;
  if (scrollToBottom) {
    nextTick(() => {
      scroller.scrollTop = scroller.scrollHeight - scroller.clientHeight;
    });
  }
});

onMounted(() => {
  subscribeToLog(props.serverId);
  const scroller = scrollPanel.value.$refs.content;
  scroller.scrollTop = scroller.scrollHeight - scroller.clientHeight;
});

function updateDropdown() {
  historyDropdownItems.value = history.value.length > 0 ? [...history.value] : [command.value];
}
</script>

<template>
  <ServerStatus :server-id="serverId" :controls="true"></ServerStatus>
  <div class="console p-2 flex flex-col rounded-xl bg-surface-900">
    <div>
      <ScrollPanel ref="scrollPanel" class="h-80 p-1">
        <a class="whitespace-pre">
          {{ servers[props.serverId].log.map(l => l.content).join("\n") }}
        </a>
      </ScrollPanel>
    </div>
    <div class="mt-3 flex items-center gap-2">
      <a>$ </a>
      <AutoComplete class="w-full" dropdown v-model="command" :suggestions="historyDropdownItems"
                    @complete="updateDropdown" @keydown.enter="sendCommand($event)"/>
      <Button icon="pi pi-angle-right" :disabled="command.trim() === ''" @click="sendCommand($event)"/>
    </div>
  </div>
</template>

<style scoped>
.console {
  font-family: "JetBrainsMono", serif;
}
</style>