<script setup>
import {servers} from "@/main.js";
import {computed, onUnmounted, ref} from "vue";

const props = defineProps({
  serverId: {
    required: true
  },
  controls: {
    type: Boolean,
    required: false,
    default: false
  }
});
const status = computed({
  get: () => servers[props.serverId].state,
  set: value => servers[props.serverId].state = value
});

const empty = ref(false);

const interval = setInterval(() => {
  empty.value = !empty.value;
}, 800);

async function terminate() {
  const response = await fetch("/api/servers/1/terminate", {
    method: "POST"
  });
  if (response.ok) {
    status.value = "stopped";
  }
}

async function startStop() {
  if (status.value === "stopped") {
    const response = await fetch("/api/servers/1/start", {
      method: "POST"
    });
    if (response.ok) {
      status.value = "starting";
    }
  } else if (status.value === "running") {
    const response = await fetch("/api/servers/1/stop", {
      method: "POST"
    });
    if (response.ok) {
      status.value = "stopping";
    }
  }
}

onUnmounted(() => clearInterval(interval));
</script>

<template>
  <div class="wrapper">
    <div class="status-display"
         :class="[status === 'running' || status === 'stopped' ? status : empty ? 'empty' : status]">
    </div>
    <a>{{ status }}</a>
    <template v-if="props.controls">
      <button :disabled="status === 'starting' || status === 'stopping'" @click="startStop">
        {{ status === 'running' || status === 'stopping' ? 'Stop' : 'Start' }}
      </button>
      <button v-if="status !== 'stopped'" @click="terminate">Kill</button>
    </template>
  </div>
</template>

<style scoped>
.wrapper {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 4px;
}

.status-display {
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 100%;
  transition: background 0.4s;
}

.running, .starting {
  background: var(--color-green);
}

.stopped, .stopping {
  background: var(--color-red);
}

.empty {
  background: var(--color-background-soft);
}
</style>