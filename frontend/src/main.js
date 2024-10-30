import './assets/main.css'

import {createApp, reactive, ref} from 'vue'
import App from './App.vue'
import {createRouter, createWebHistory} from "vue-router";
import ReconnectingWebSocket from "reconnecting-websocket";
import ServerView from "@/views/ServerView.vue";
import ServerListView from "@/views/ServerListView.vue";
import JobsView from "@/views/JobsView.vue";
import ServerDashboardView from "@/views/ServerDashboardView.vue";

export const servers = reactive({});
export const initialLoad = ref(false);

const webSocket = new ReconnectingWebSocket(`ws://localhost:8080/api/websocket`);
webSocket.onopen = async () => {
    const response = await fetch("/api/servers").then(r => r.json());

    for (let id in servers) {
        delete servers[id];
    }

    for (let server of response) {
        servers[server.id] = server;
        servers[server.id].log = [];
        servers[server.id].state = servers[server.id].state.toLowerCase();
    }
    initialLoad.value = true;
};

webSocket.onmessage = message => {
    const messageData = JSON.parse(message.data);
    const eventType = messageData.type;
    const data = messageData.data;

    switch (eventType) {
        case "APPEND_LOG":
            servers[data.serverId].log.push({content: data.line, lineNumber: data.lineNumber});
            break;
        case "SERVER_STATE_CHANGED":
            servers[data.serverId].state = data.state.toLowerCase();
            break;
    }
    console.log(message);
};

const routes = [
    {path: '/', component: ServerListView},
    {
        path: '/server/:serverId', component: ServerView, children: [
            {path: '', component: ServerDashboardView, name: 'server'},
            {path: 'jobs', component: JobsView, name: 'jobs'},
        ]
    },
];

export const router = createRouter({
    history: createWebHistory(),
    routes: routes
});

createApp(App)
    .use(router)
    .mount('#app');
