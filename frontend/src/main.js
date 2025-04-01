import './assets/main.css'

import {createApp, reactive, ref} from 'vue'
import App from './App.vue'
import {createRouter, createWebHistory} from "vue-router";
import ReconnectingWebSocket from "reconnecting-websocket";
import ServerView from "@/views/ServerView.vue";
import ServerListView from "@/views/ServerListView.vue";
import JobsView from "@/views/JobsView.vue";
import ServerDashboardView from "@/views/ServerDashboardView.vue";
import PrimeVue from 'primevue/config';
import Aura from '@primevue/themes/aura';
import {
    AutoComplete,
    Avatar,
    Button,
    ButtonGroup,
    Checkbox,
    ConfirmationService,
    ConfirmDialog,
    ContextMenu,
    Dialog,
    DialogService,
    DynamicDialog,
    FileUpload,
    FloatLabel,
    Fluid,
    IconField,
    InputGroup,
    InputGroupAddon,
    InputIcon,
    InputText,
    Menu,
    Message,
    Password,
    ScrollPanel,
    Select,
    SelectButton,
    Skeleton,
    SplitButton,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    Toast,
    ToastService,
    ToggleSwitch,
    TreeSelect
} from "primevue";
import {definePreset} from "@primevue/themes";
import ModListView from "@/views/ModListView.vue";
import PluginListView from "@/views/PluginListView.vue";
import DatapackListView from "@/views/DatapackListView.vue";
import {Form, FormField} from "@primevue/forms";

export const account = ref();
export const authorized = ref();
export const servers = reactive({});
export const initialLoad = ref(false);

let webSocket = null;
const subscribedLogs = [];

export async function authenticate() {
    const accountResponse = await fetch("/api/account");
    if (accountResponse.status !== 200) {
        authorized.value = false;
    } else {
        account.value = await accountResponse.json();
        authorized.value = true;

        webSocket = new ReconnectingWebSocket(`ws://${location.hostname}:8080/api/websocket`);
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

            subscribedLogs.forEach(subscribeToLog);
        };

        webSocket.onmessage = message => {
            const messageData = JSON.parse(message.data);
            const eventType = messageData.type;
            const data = messageData.data;

            switch (eventType) {
                case "UPSERT_CONSOLE_LINE":
                    const log = servers[data.serverId].log;
                    const lineNum = +data.lineNumber;

                    let low = 0;
                    let high = log.length;

                    while (low < high) {
                        const mid = (low + high) >>> 1;
                        if (log[mid].lineNumber < lineNum) low = mid + 1;
                        else high = mid;
                    }
                    const i = low;

                    if (log.length > i && log[i].lineNumber === lineNum) {
                        log[i].content = data.line;
                    } else {
                        log.splice(i, 0, {content: data.line, lineNumber: lineNum});
                    }

                    break;
                case "SERVER_STATE_CHANGED":
                    servers[data.serverId].state = data.state.toLowerCase();
                    break;
                case "SERVER_LAUNCHED":
                    servers[data.serverId].log.length = 0;
                    servers[data.serverId].startedAt = Date.now();
                    break;
            }
            console.log(message);
        };
    }
}

export function subscribeToLog(serverId) {
    webSocket.send(JSON.stringify({
        type: "SUBSCRIBE",
        data: {
            serverId: serverId
        }
    }));
    if (!subscribedLogs.includes(serverId)) {
        subscribedLogs.push(serverId);
    }
}

export async function logout() {
    await fetch("/api/account/logout", {
        method: "POST"
    });
    location.reload();
}

const routes = [
    {path: '/', component: ServerListView},
    {
        path: '/server/:serverId', component: ServerView, children: [
            {path: '', component: ServerDashboardView, name: 'server'},
            {path: 'mods', component: ModListView, name: 'mods'},
            {path: 'plugins', component: PluginListView, name: 'plugins'},
            {path: 'datapacks', component: DatapackListView, name: 'datapacks'},
            {path: 'jobs', component: JobsView, name: 'jobs'},
        ]
    },
];

export const router = createRouter({
    history: createWebHistory(),
    routes: routes
});

const preset = definePreset(Aura, {
    components: {
        tabs: {
            tab: {
                padding: "0 10px 10px 10px",
                font: {
                    weight: "normal"
                }
            }
        },
        contextmenu: {
            item: {
                color: "inherit",
                focus: {
                    color: "inherit"
                },
                icon: {
                    color: "inherit",
                    focus: {
                        color: "inherit"
                    }
                }
            }
        },
        menu: {
            item: {
                color: "inherit",
                focus: {
                    color: "inherit"
                },
                icon: {
                    color: "inherit",
                    focus: {
                        color: "inherit"
                    }
                }
            }
        }
    }
});

createApp(App)
    .use(router)
    .use(PrimeVue, {
        theme: {
            preset: preset,
        }
    })
    .use(ConfirmationService)
    .use(DialogService)
    .use(ToastService)
    .component("Toast", Toast)
    .component("Menu", Menu)
    .component("ContextMenu", ContextMenu)
    .component("Form", Form)
    .component("Checkbox", Checkbox)
    .component("ToggleSwitch", ToggleSwitch)
    .component("Button", Button)
    .component("SelectButton", SelectButton)
    .component("SplitButton", SplitButton)
    .component("ButtonGroup", ButtonGroup)
    .component("FloatLabel", FloatLabel)
    .component("InputText", InputText)
    .component("Password", Password)
    .component("IconField", IconField)
    .component("InputIcon", InputIcon)
    .component("InputGroup", InputGroup)
    .component("InputGroupAddon", InputGroupAddon)
    .component("Message", Message)
    .component("TabPanels", TabPanels)
    .component("TabPanel", TabPanel)
    .component("TabList", TabList)
    .component("Tabs", Tabs)
    .component("Tab", Tab)
    .component("ScrollPanel", ScrollPanel)
    .component("AutoComplete", AutoComplete)
    .component("ConfirmDialog", ConfirmDialog)
    .component("DynamicDialog", DynamicDialog)
    .component("Select", Select)
    .component("TreeSelect", TreeSelect)
    .component("FileUpload", FileUpload)
    .component("Dialog", Dialog)
    .component("Skeleton", Skeleton)
    .component("Avatar", Avatar)
    .component("FormField", FormField)
    .component("Fluid", Fluid)
    .mount('#app');

authenticate().then();

export class loginDialogVisible {
}