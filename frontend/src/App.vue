<script setup>
import Header from "@/components/Header.vue";
import PancakeLogo from "@/assets/PancakeLogo.vue";
import {authenticate, authorized, initialLoad} from "@/main.js";
import {computed, ref} from "vue";

const loading = ref(false);
const showLogin = computed(() => authorized.value === false);
let loginFailed = false;
const form = ref();
const loginResolver = ({values}) => {
  const errors = {};
  if (!values.username) errors.username = [{message: 'Username is required'}];
  if (!values.password) errors.password = [{message: 'Password is required'}];
  if (loginFailed) errors.password = [{message: 'Wrong username or password'}];

  return {
    errors
  };
};

async function onSubmit(event) {
  loading.value = true;
  loginFailed = false;
  await form.value.validate();

  if (form.value.valid) {
    const response = await fetch("/api/account/login", {
      method: "POST",
      body: JSON.stringify({
        username: event.states.username.value,
        password: event.states.password.value,
      })
    });

    if (response.ok) {
      const body = await response.json();
      document.cookie = `Authorization=${body.token};path=/`;
      await authenticate();
    } else {
      console.log(event.errors);
      loginFailed = true;
      form.value.validate();
    }
  }
  loading.value = false;
}
</script>

<template>
  <Dialog :closable="false" modal header="Login" v-model:visible="showLogin" class="w-80">
    <Form ref="form" v-slot="$form" :resolver="loginResolver" @submit="onSubmit" validate-on-value-update
          validate-on-submit>
      <div>
        <label for="username">Username</label>
        <InputText fluid id="username" name="username"/>
        <Message v-if="$form.username?.invalid" severity="error" size="small" variant="simple">
          {{ $form.username.error.message }}
        </Message>
      </div>

      <div>
        <label for="password">Password</label>
        <Password fluid id="password" :feedback="false" toggleMask name="password"/>
        <Message v-if="$form.password?.invalid" severity="error" size="small" variant="simple">
          {{ $form.password.error.message }}
        </Message>
      </div>

      <Button :loading="loading" type="submit" class="mt-4" fluid label="Login"></Button>
    </Form>
  </Dialog>

  <template v-if="initialLoad">
    <Header></Header>
    <router-view>
    </router-view>
  </template>
  <div v-else-if="authorized === false" class="w-full h-dvh flex justify-center items-center relative">

  </div>
  <div v-else class="w-full h-dvh flex justify-center items-center relative">
    <div
        class="w-44 h-44 absolute border-4 border-transparent border-t-emerald-600 rounded-full animate-spin animate-duration-2000"></div>
    <PancakeLogo class="w-36"></PancakeLogo>
  </div>
</template>

<style scoped>

</style>
