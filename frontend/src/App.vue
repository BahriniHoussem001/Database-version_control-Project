<script setup>
import { computed, provide, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  Database,
  GitBranch,
  Clock,
  Layers,
  Settings,
  Server,
  FileClock,
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

const pageTitle = computed(() => {
  return route.meta?.title || 'Dashboard'
})

const pageSubtitle = computed(() => {
  return route.meta?.subtitle || 'Database migration control center'
})
</script>

<template>
  <div class="app-shell">
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

        <RouterLink
          class="nav-item"
          exact-active-class="active"
          to="/"
        >
          <LayoutDashboard :size="18" />
          Dashboard
        </RouterLink>

        <p class="nav-label">Migrations</p>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/migration-history"
        >
          <GitBranch :size="18" />
          Migration History
        </RouterLink>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/pending-migrations"
        >
          <Clock :size="18" />
          Pending Migrations
        </RouterLink>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/execution-logs"
        >
          <FileClock :size="18" />
          Execution Logs
        </RouterLink>

        <p class="nav-label">Compare & Schema</p>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/environment-comparison"
        >
          <Layers :size="18" />
          Environment Comparison
        </RouterLink>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/schema-explorer"
        >
          <Server :size="18" />
          Schema Explorer
        </RouterLink>

        <p class="nav-label">Admin</p>

        <RouterLink
          class="nav-item"
          active-class="active"
          to="/settings"
        >
          <Settings :size="18" />
          Settings
        </RouterLink>
      </nav>

      <div class="sidebar-user">
        <div class="avatar">HB</div>
        <div>
          <strong>Houssem</strong>
          <span>Developer / Admin</span>
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

          <button class="icon-button">
            <UserCircle :size="24" />
          </button>
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>