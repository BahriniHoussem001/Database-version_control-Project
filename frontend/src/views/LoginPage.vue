<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../services/api'
import { saveAuthSession } from '../services/auth'

const router = useRouter()

const username = ref('admin')
const password = ref('Admin123!')
const loading = ref(false)
const errorMessage = ref(null)

async function submitLogin() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await login({
      username: username.value,
      password: password.value,
    })

    saveAuthSession(response.data)

    window.location.href = '/'
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      'Invalid username or password'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="login-brand">
        <div class="login-logo">DB</div>
        <div>
          <h1>DB Version Control</h1>
          <p>Secure migration dashboard</p>
        </div>
      </div>

      <form class="login-form" @submit.prevent="submitLogin">
        <div>
          <h2>Sign in</h2>
          <p>Use your platform account to access migration controls.</p>
        </div>

        <div v-if="errorMessage" class="error-banner">
          {{ errorMessage }}
        </div>

        <label>
          Username
          <input v-model="username" type="text" autocomplete="username" />
        </label>

        <label>
          Password
          <input v-model="password" type="password" autocomplete="current-password" />
        </label>

        <button class="primary-button login-button" type="submit" :disabled="loading">
          {{ loading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>
    </section>
  </main>
</template>