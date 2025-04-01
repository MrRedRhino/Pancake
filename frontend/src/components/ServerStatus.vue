<script setup>
import {servers} from "@/main.js";
import {computed, onUnmounted, ref} from "vue";
import {useConfirm} from "primevue";
import RelativeTimestamp from "@/components/RelativeTimestamp.vue";

const confirm = useConfirm();
const props = defineProps({
  serverId: {
    required: true
  },
  textStatus: {
    type: Boolean,
    required: false,
    default: false
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
const runningOrStopping = computed(() => {
  const s = status.value;
  return s === 'running' || s === 'stopping';
});
const transitioning = computed(() => {
  const s = status.value;
  return s === 'starting' || s === 'stopping';
})

const empty = ref(false);

const interval = setInterval(() => {
  empty.value = !empty.value;
}, 800);

async function terminate() {
  const response = await fetch(`/api/servers/${props.serverId}/terminate`, {
    method: "POST"
  });
  if (response.ok) {
    status.value = "stopped";
  }
}

async function startStop() {
  if (status.value === "stopped") {
    const response = await fetch(`/api/servers/${props.serverId}/start`, {
      method: "POST"
    });
    if (response.ok) {
      status.value = "starting";
    }
  } else if (status.value === "running") {
    const response = await fetch(`/api/servers/${props.serverId}/stop`, {
      method: "POST"
    });
    if (response.ok) {
      status.value = "stopping";
    }
  }
}

function confirmTerminate() {
  confirm.require({
    message: "Are you sure you want to kill this server?",
    header: "Kill this server?",
    acceptProps: {
      label: "Kill",
      severity: "danger"
    },
    rejectProps: {
      label: "Cancel",
      severity: "secondary"
    },
    accept: terminate
  });
}

onUnmounted(() => clearInterval(interval));
</script>

<template>
  <ConfirmDialog></ConfirmDialog>
  <div class="wrapper">
    <div class="status-display"
         :class="[status === 'running' || status === 'stopped' ? status : empty ? 'empty' : status]">
    </div>
    <a v-if="props.textStatus">{{ status }}</a>
    <template v-if="props.controls">
      <ButtonGroup>
        <Button :disabled="transitioning"
                :icon="`pi pi-${runningOrStopping ? 'stop' : 'play'}`"
                :label="runningOrStopping ? 'Stop' : 'Start'"
                :severity="runningOrStopping ? 'danger' : 'success'"
                size="small"
                @click="startStop"
        />

        <Button v-if="status !== 'stopped'" @click="confirmTerminate" size="small" severity="secondary">Kill</Button>
      </ButtonGroup>
    </template>
    <h1 v-if="status === 'running' && props.textStatus"><i class="pi pi-clock"></i>
      <RelativeTimestamp :date="new Date(+servers[props.serverId].startedAt)"/>
    </h1>
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
  background: var(--p-green-500);
}

.stopped, .stopping {
  background: var(--p-red-500);
}

.empty {
  background: var(--p-surface-800);
}
</style>