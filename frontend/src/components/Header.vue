<script setup>
import {header, showBackButton, tabs} from "../header.js";
import IconButton from "@/components/IconButton.vue";
import {TabList} from "primevue";
import {ref} from "vue";
import {account, logout} from "@/main.js";

const accountMenu = ref();
const accountMenuItems = ref([
  {
    label: account.value.name,
    items: [
      {
        separator: true,
      },
      {
        label: "Settings",
        icon: "pi pi-cog"
      },
      {
        label: "Logout",
        class: "text-red-500",
        icon: "pi pi-sign-out",
        command: logout
      }
    ]
  }
]);
</script>

<template>
  <Menu ref="accountMenu" :model="accountMenuItems" :popup="true">
  </Menu>

  <div>
    <div class="flex items-center p-2 bg-surface-900">
      <IconButton @click="$router.push('/')" v-if="showBackButton" icon="back"></IconButton>
      <h1 class="text-3xl">{{ header }}</h1>

      <Avatar class="ml-auto cursor-pointer" shape="circle" :label="account.name.charAt(0).toUpperCase()"
              @click="accountMenu.show($event)"></Avatar>
    </div>

    <Tabs :value="$route.name" v-if="tabs.length > 0" scrollable>
      <TabList>
        <router-link v-for="tab in tabs" v-slot="{ href, navigate }" :to="tab.route">
          <Tab :value="tab.route.name">
            <a :href="href" @click="navigate">
              <span>{{ tab.title }}</span>
            </a>
          </Tab>
        </router-link>
      </TabList>
    </Tabs>
  </div>
</template>

<style scoped>
</style>