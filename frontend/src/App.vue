<script setup>
import { computed, provide, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAuthUser, logout as clearAuthSession } from './services/auth'
import {
  Database,
  GitBranch,
  Clock,
  Layers,
  Settings,
  Server,
  FileClock,
  FileText,
  LayoutDashboard,
  Search,
  Bell,
  UserCircle,
  Menu,
} from 'lucide-vue-next'

const selectedEnvironment = ref('DEV')
const environmentNames = ['DEV', 'TEST', 'PROD']

provide('selectedEnvironment', selectedEnvironment)
provide('environmentNames', environmentNames)

const route = useRoute()
const currentUser = ref(getAuthUser())

provide('currentUser', currentUser)

const isLoginPage = computed(() => route.path === '/login')

const pageTitle = computed(() => route.meta?.title || 'Dashboard')
const pageSubtitle = computed(() => route.meta?.subtitle || 'Database migration control center')

function handleLogout() {
  clearAuthSession()
  currentUser.value = null
  window.location.href = '/login'
}
</script>

<template>
  <RouterView v-if="isLoginPage" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <Database :size="22" />
        </div>

        <div>
          <h1>DB Version Control</h1>
          <p>Oracle + Liquibase</p>
        </div>
      </div>

      <nav class="nav">
        <p class="nav-label">Overview</p>

        <RouterLink class="nav-item" exact-active-class="active" to="/">
          <LayoutDashboard :size="18" />
          <span>Dashboard</span>
        </RouterLink>

        <p class="nav-label">Migrations</p>

        <RouterLink class="nav-item" active-class="active" to="/migration-history">
          <GitBranch :size="18" />
          <span>Migration History</span>
        </RouterLink>

        <RouterLink class="nav-item" active-class="active" to="/pending-migrations">
          <Clock :size="18" />
          <span>Pending Migrations</span>
        </RouterLink>

        <RouterLink class="nav-item" active-class="active" to="/migration-builder">
          <FileText :size="18" />
          <span>Migration Builder</span>
        </RouterLink>

        <RouterLink class="nav-item" active-class="active" to="/execution-logs">
          <FileClock :size="18" />
          <span>Execution Logs</span>
        </RouterLink>

        <p class="nav-label">Compare & Schema</p>

        <RouterLink class="nav-item" active-class="active" to="/environment-comparison">
          <Layers :size="18" />
          <span>Environment Comparison</span>
        </RouterLink>

        <RouterLink class="nav-item" active-class="active" to="/schema-explorer">
          <Server :size="18" />
          <span>Schema Explorer</span>
        </RouterLink>

        <p class="nav-label">Admin</p>

        <RouterLink class="nav-item" active-class="active" to="/settings">
          <Settings :size="18" />
          <span>Settings</span>
        </RouterLink>
      </nav>

      <div class="sidebar-user">
        <div class="avatar">
          {{ currentUser?.username?.slice(0, 2).toUpperCase() || 'DB' }}
        </div>

        <div>
          <strong>{{ currentUser?.username || 'Guest' }}</strong>
          <span>{{ currentUser?.role || 'Unauthenticated' }}</span>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-button">
            <Menu :size="20" />
          </button>

          <div>
            <strong>{{ pageTitle }}</strong>
            <span>{{ pageSubtitle }}</span>
          </div>
        </div>

        <div class="topbar-right">
          <label class="environment-select">
            Environment
            <select v-model="selectedEnvironment">
              <option
                v-for="environment in environmentNames"
                :key="environment"
                :value="environment"
              >
                {{ environment }}
              </option>
            </select>
          </label>

          <button class="icon-button">
            <Search :size="19" />
          </button>

          <button class="icon-button">
            <Bell :size="19" />
          </button>

          <button class="logout-button" @click="handleLogout">
            <UserCircle :size="18" />
            Logout
          </button>
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>