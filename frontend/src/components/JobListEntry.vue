<script setup>
import {computed, reactive, ref, watch} from "vue";
import Select from "@/components/Select.vue";
import ExpandButton from "@/components/ExpandButton.vue";
import NumberInput from "@/components/NumberInput.vue";
import IconButton from "@/components/IconButton.vue";
import Button from "@/components/Button.vue";
import ServerActionSelect from "@/components/ServerActionSelect.vue";

const props = defineProps({
  job: {
    required: true
  }
});

const intervals = [
  {name: 'Hour'},
  {name: 'n Hours', selectedName: 'Hours', showNumberInput: true},
  {name: 'Day', showTime: true},
  {name: 'n Days', selectedName: 'Days', showNumberInput: true, showTime: true},
  {name: 'Week', showDay: true, showTime: true},
  {name: 'n Weeks', selectedName: 'Weeks', showNumberInput: true, showDay: true, showTime: true},
];
const days = [
  {name: 'Monday', index: 0},
  {name: 'Tuesday', index: 1},
  {name: 'Wednesday', index: 2},
  {name: 'Thursday', index: 3},
  {name: 'Friday', index: 4},
  {name: 'Saturday', index: 5},
  {name: 'Sunday', index: 6},
];

const expanded = ref(false);
const selectedInterval = ref();
const selectedDay = ref();
const timeOfDay = ref();
const firstInterval = ref(2);
const newTask = ref();
const tasks = reactive(props.job.tasks.slice());
const originalTasks = ref(props.job.tasks.slice());
const hasChanged = computed(() => {
  const isSame = (tasks.length === originalTasks.value.length && tasks.every((value, index) => value === originalTasks.value[index]))
      && areIntervalsEqual(props.job.interval, interval.value);
  return !isSame;
});
const interval = computed({
  get: () => {
    console.log(selectedDay.value);
    return {
      interval: selectedInterval.value.showNumberInput ? firstInterval.value : 1,
      day: selectedInterval.value.showDay ? selectedDay.value?.index : -1,
      timeOfDay: selectedInterval.value.showTime ? hhmmToMinutes(timeOfDay.value) : -1,
    }
  },
  set: value => {
    const intervalIndex = (value.timeOfDay == -1 ? 0 : (value.day == -1 ? 2 : 4)) + (value.interval >= 2 ? 1 : 0);
    selectedInterval.value = intervals[intervalIndex];
    firstInterval.value = +value.interval;
    selectedDay.value = days[+value.day];
    timeOfDay.value = minutesToHHMM(+value.timeOfDay);
  }
});

interval.value = props.job.interval;

watch(newTask, value => {
  if (value === undefined) return;

  let config;
  switch (value.id) {
    case "sleep":
      config = {delay: 10};
      break;
    case "server_control":
      config = {action: "start"};
      break;
    case "chat_message":
      config = {message: "Hello"};
      break;
  }

  tasks.push({
    type: value.id,
    config: config
  });
  newTask.value = undefined;
});

function areIntervalsEqual(interval1, interval2) {
  return interval1.interval == interval2.interval
      && interval1.day == interval2.day
      && interval1.timeOfDay == interval2.timeOfDay;
}

function minutesToHHMM(minutes) {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  const formattedHours = String(hours).padStart(2, '0');
  const formattedMinutes = String(mins).padStart(2, '0');
  return `${formattedHours}:${formattedMinutes}`;
}

function hhmmToMinutes(time) {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

function moveTask(index, newIndex) {
  if (newIndex >= tasks.length || newIndex < 0) return;
  const element = tasks[index];
  tasks[index] = tasks[newIndex];
  tasks[newIndex] = element;
}

function deleteTask(index) {
  tasks.splice(index, 1);
}

async function save() {
  const response = await fetch(`/api/servers/${props.job.serverId}/jobs/${props.job.id}`, {
    method: "PATCH",
    body: JSON.stringify({
      name: props.job.name,
      interval: interval.value,
      tasks: tasks
    })
  });
  if (response.ok) {
    originalTasks.value = tasks.slice();
    props.job.interval = interval.value;
  }
}

function cancel() {
  tasks.splice(0, tasks.length, ...originalTasks.value);
  interval.value = props.job.interval;
}
</script>

<template>
  <div class="wrapper">
    <div class="header" @click="expanded = !expanded">
      <ExpandButton v-model="expanded"></ExpandButton>
      <h1>{{ props.job.name }}</h1>
    </div>

    <div class="expanded" v-if="expanded">
      <div style="display: flex; gap: 10px; flex-wrap: wrap;">
        <h3>Run every </h3>
        <div class="select-wrapper">
          <NumberInput v-if="selectedInterval?.showNumberInput" :min="2"
                       v-model="firstInterval"></NumberInput>
          <Select v-model="selectedInterval" placeholder="Select an interval" :options="intervals"></Select>
        </div>

        <div class="thing" v-if="selectedInterval?.showDay">
          <h3>On </h3>
          <div class="select-wrapper">
            <Select v-model="selectedDay" placeholder="Select a day" :options="days"></Select>
          </div>
        </div>

        <div class="thing" v-if="selectedInterval?.showTime">
          <h3>At </h3>
          <input v-model="timeOfDay" type="time">
        </div>
      </div>

      <div>
        <h2>Tasks</h2>
        <div v-for="(task, i) in tasks" class="task">
          <div>
            <template v-if="task.type === 'sleep'">
              <h3>Wait {{ task.config.delay }}s</h3>
              <div class="config-option">
                <h4>Delay: </h4>
                <NumberInput v-model="task.config.delay" :min="0"></NumberInput>
              </div>
            </template>

            <template v-if="task.type === 'server_control'">
              <h3>{{ task.config.action === "start" ? "Start " : "Stop" }} server</h3>

              <div class="config-option">
                <h4>Action: </h4>
                <ServerActionSelect v-model="task.config.action"></ServerActionSelect>
              </div>
            </template>

            <template v-if="task.type === 'chat_message'">
              <h3>Send "{{ task.config.message }}"</h3>

              <div class="config-option">
                <h4>Message: </h4>
                <input v-model="task.config.message">
              </div>
            </template>
          </div>

          <div style="margin-left: auto">
            <IconButton icon="arrow-up" @click="moveTask(i, i - 1)"></IconButton>
            <IconButton icon="arrow-down" @click="moveTask(i, i + 1)"></IconButton>
            <IconButton icon="delete" @click="deleteTask(i)"></IconButton>
          </div>
        </div>
        <h3 v-if="tasks.length === 0">
          No Tasks
        </h3>

        <Select v-model="newTask" placeholder="Add Task" :options="[
            {name: 'Delay', id: 'sleep'},
            {name: 'Send Chat Message', id: 'chat_message'},
            {name: 'Start/Stop Server', id: 'server_control'},
        ]"></Select>
      </div>

      <template
          v-if="hasChanged">
        <Button type="destructive" @click="cancel">Cancel</Button>
        <Button type="primary" @click="save">Save</Button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  border-radius: 20px;
  background: var(--color-background-mute);
  padding-left: 10px;
  padding-right: 10px;
}

.expanded {
  padding-bottom: 10px;
}

.header {
  display: flex;
  align-items: center;
}

.select-wrapper {
  display: flex;
  align-items: center;
  background: var(--color-background-soft);
  border-radius: 10px;
  padding-left: 5px;
}

.select-wrapper input {
  width: 40px;
  background: var(--color-background-soft);
  border: none;
  color: inherit;
}

.task {
  display: flex;
  border-radius: 20px;
  padding: 10px;
  background: var(--color-background-soft);
  margin-bottom: 7px;
}

.thing {
  display: flex;
  gap: 10px;
}

.config-option {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>