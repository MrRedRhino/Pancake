<script setup>
import {computed, nextTick, onMounted, reactive, ref, watch} from "vue";
import ServerStatus from "@/components/ServerStatus.vue";
import {servers} from "@/main.js";

const props = defineProps({
  serverId: {
    required: true,
  }
});

const contentHolder = ref();
const content = servers[props.serverId].log;
const history = reactive([""]);
let positionInHistory = ref(0);
const command = computed({
  get: () => history[positionInHistory.value],
  set: value => history[positionInHistory.value] = value
});

async function sendCommand(event) {
  if (!event.shiftKey && command.value !== "") {
    await fetch(`/api/servers/${props.serverId}/command`, {
      method: "POST",
      body: JSON.stringify({
        command: command.value,
      })
    });

    if (history[history.length - 2] === command.value) {
      history[history.length - 1] = "";
    } else {
      history.push("");
      positionInHistory.value = history.length - 1;
    } // TODO improve with dropdown maybe
  }
}

function moveInHistory(delta) {
  const newValue = positionInHistory.value + delta;
  positionInHistory.value = Math.min(history.length - 1, Math.max(newValue, 0));
}

watch(content, value => {
  const scrollToBottom = (contentHolder.value.scrollHeight - contentHolder.value.clientHeight) - contentHolder.value.scrollTop < 3;
  if (scrollToBottom) {
    nextTick(() => {
      contentHolder.value.scrollTop = contentHolder.value.scrollHeight - contentHolder.value.clientHeight;
    });
  }
});

onMounted(() => {
  contentHolder.value.scrollTop = contentHolder.value.scrollHeight - contentHolder.value.clientHeight;
});
</script>

<template>
  <ServerStatus :server-id="serverId" :controls="true"></ServerStatus>
  <div class="console">
    <a ref="contentHolder" class="content">
      {{ servers[props.serverId].log.map(l => l.content).join("\n") }}
    </a>
    <div class="input-wrapper">
      <a>$ </a>
      <input v-model.trim="command" @keydown.enter="sendCommand($event)" @keydown.up.prevent="moveInHistory(-1)"
             @keydown.down="moveInHistory(1)">
    </div>
  </div>
</template>

<style scoped>
.console {
  background: var(--color-background-mute);
  font-family: "JetBrains Mono", serif;
  height: 400px;
  display: flex;
  width: 90%;
  flex-direction: column;
  padding: 10px;
  border-radius: 20px;
}

.console .content {
  padding-left: 10px;
  margin-bottom: 10px;
  flex-grow: 1;
  overflow: scroll;
  white-space: pre;
  background: var(--color-background-soft);
  border-radius: 15px;
}

.console .input-wrapper {
  width: 100%;
  align-self: end;
  background: var(--color-background-soft);
  display: flex;
  border-radius: 15px;
  padding: 10px;
  align-items: center;
}

.input-wrapper input {
  background: none;
  border: none;
  width: 100%;
  color: var(--color-text);
  font-family: "JetBrains Mono", serif;
  padding-left: 10px;
  font-size: 15px;
  outline: none;
}
</style>