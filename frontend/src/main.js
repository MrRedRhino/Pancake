import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import {createRouter, createWebHistory} from "vue-router";

const routes = [
    {path: '/', component: App},
    {path: '/albums/:albumId', component: App},
];

export const router = createRouter({
    history: createWebHistory(),
    routes: routes
});

createApp(App)
    .use(router)
    .mount('#app');
