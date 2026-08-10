<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  Database,
  GitBranch,
  Clock,
  Layers,
  CheckCircle2,
  AlertTriangle,
  Settings,
  Server,
  FileClock,
  LayoutDashboard,
  Search,
  Bell,
  UserCircle,
  Menu,
  RefreshCw
} from 'lucide-vue-next'

import {
  compareEnvironments,
  createMigrationExecutionRequest,
  getEnvironmentPendingMigrations,
  getEnvironmentSchemaTables,
  getEnvironmentSummary,
  getMigrationExecutions
} from './services/api'

const environmentNames = ['DEV', 'TEST', 'PROD']

const selectedEnvironment = ref('DEV')
const loading = ref(false)
const errorMessage = ref(null)
const successMessage = ref(null)

const environmentRows = ref([])
const recentExecutions = ref([])
const schemaTables = ref([])
const comparisons = ref([])

const showApplyModal = ref(false)
const applyingMigrations = ref(false)

const applyForm = ref({
  environment: 'DEV',
  reason: '',
  requestedBy: 'houssem',
})

const currentEnvironmentRow = computed(() => {
  return environmentRows.value.find((item) => item.name === selectedEnvironment.value)
})

const currentTotalMigrations = computed(() => {
  return currentEnvironmentRow.value?.migrations ?? 0
})

const currentPendingMigrations = computed(() => {
  return currentEnvironmentRow.value?.pending ?? 0
})

const modalEnvironmentRow = computed(() => {
  return environmentRows.value.find((item) => item.name === applyForm.value.environment)
})

const modalPendingMigrations = computed(() => {
  return modalEnvironmentRow.value?.pending ?? 0
})

const canApplyMigrations = computed(() => {
  return modalPendingMigrations.value > 0 && !applyingMigrations.value
})

const globalEnvironmentStatus = computed(() => {
  if (!environmentRows.value.length) {
    return 'Loading'
  }

  const hasPending = environmentRows.value.some((item) => item.pending > 0)
  const hasOutOfSync = comparisons.value.some((item) => !item.inSync)

  if (hasPending || hasOutOfSync) {
    return 'Needs Review'
  }

  return 'In Sync'
})

function formatDate(value) {
  if (!value) {
    return '—'
  }

  return new Date(value).toLocaleString()
}

function formatDuration(durationMs) {
  if (durationMs === null || durationMs === undefined) {
    return '—'
  }

  if (durationMs < 1000) {
    return `${durationMs} ms`
  }

  return `${Math.round(durationMs / 1000)}s`
}

function environmentStatusFromPending(pendingCount) {
  if (pendingCount > 0) {
    return `Pending ${pendingCount}`
  }

  return 'Up to date'
}

function statusClass(status) {
  if (!status) {
    return 'muted'
  }

  const value = status.toUpperCase()

  if (value.includes('SUCCESS') || value.includes('SYNC') || value.includes('UP TO DATE')) {
    return 'success'
  }

  if (value.includes('RUNNING') || value.includes('QUEUED') || value.includes('PENDING')) {
    return 'warning'
  }

  if (value.includes('FAILED') || value.includes('ERROR')) {
    return 'danger'
  }

  return 'muted'
}

function executionActionLabel(requestType) {
  if (requestType === 'UPDATE') {
    return 'Apply migrations'
  }

  if (requestType === 'ROLLBACK') {
    return 'Rollback'
  }

  return requestType || 'Unknown action'
}

async function loadEnvironmentRows() {
  const rows = await Promise.all(
    environmentNames.map(async (environment) => {
      const [summaryResponse, pendingResponse] = await Promise.all([
        getEnvironmentSummary(environment),
        getEnvironmentPendingMigrations(environment),
      ])

      const summary = summaryResponse.data
      const pending = pendingResponse.data

      return {
        name: environment,
        migrations: summary.totalExecutedMigrations ?? 0,
        pending: pending.length,
        status: environmentStatusFromPending(pending.length),
        latestMigration: summary.latestMigrationId ?? '—',
        lastUpdate: formatDate(summary.latestExecutedAt),
      }
    })
  )

  environmentRows.value = rows
}

async function loadSchemaTables() {
  const response = await getEnvironmentSchemaTables(selectedEnvironment.value)
  schemaTables.value = response.data
}

async function loadRecentExecutions() {
  const response = await getMigrationExecutions()
  recentExecutions.value = response.data.slice(0, 6)
}

async function loadComparisons() {
  const [devTestResponse, testProdResponse] = await Promise.all([
    compareEnvironments('DEV', 'TEST'),
    compareEnvironments('TEST', 'PROD'),
  ])

  comparisons.value = [
    {
      label: 'DEV → TEST',
      ...devTestResponse.data,
    },
    {
      label: 'TEST → PROD',
      ...testProdResponse.data,
    },
  ]
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = null

  try {
    await Promise.all([
      loadEnvironmentRows(),
      loadSchemaTables(),
      loadRecentExecutions(),
      loadComparisons(),
    ])
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load dashboard data'
  } finally {
    loading.value = false
  }
}

function openApplyModal() {
  successMessage.value = null
  errorMessage.value = null

  applyForm.value = {
    environment: selectedEnvironment.value,
    reason: `Apply pending migrations to ${selectedEnvironment.value}`,
    requestedBy: 'houssem',
  }

  showApplyModal.value = true
}

function closeApplyModal() {
  showApplyModal.value = false
}

async function submitApplyMigrations() {
  if (modalPendingMigrations.value === 0) {
    errorMessage.value = `No pending migrations to apply on ${applyForm.value.environment}.`
    return
  }

  applyingMigrations.value = true
  errorMessage.value = null
  successMessage.value = null

  try {
    await createMigrationExecutionRequest({
      environment: applyForm.value.environment,
      requestType: 'UPDATE',
      priority: 'NORMAL',
      reason: applyForm.value.reason,
      requestedBy: applyForm.value.requestedBy,
    })

    successMessage.value = `Migration application queued for ${applyForm.value.environment}.`
    showApplyModal.value = false

    await loadDashboard()

    setTimeout(() => {
      loadDashboard()
    }, 6000)
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to apply pending migrations'
  } finally {
    applyingMigrations.value = false
  }
}

watch(selectedEnvironment, async () => {
  await loadDashboard()
})

onMounted(async () => {
  await loadDashboard()
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
        <a class="nav-item active" href="#">
          <LayoutDashboard :size="18" />
          Dashboard
        </a>

        <p class="nav-label">Migrations</p>
        <a class="nav-item" href="#">
          <GitBranch :size="18" />
          Migration History
        </a>
        <a class="nav-item" href="#">
          <Clock :size="18" />
          Pending Migrations
        </a>
        <a class="nav-item" href="#">
          <FileClock :size="18" />
          Execution Logs
        </a>

        <p class="nav-label">Compare & Schema</p>
        <a class="nav-item" href="#">
          <Layers :size="18" />
          Environment Comparison
        </a>
        <a class="nav-item" href="#">
          <Server :size="18" />
          Schema Explorer
        </a>

        <p class="nav-label">Admin</p>
        <a class="nav-item" href="#">
          <Settings :size="18" />
          Settings
        </a>
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
            <strong>Dashboard</strong>
            <span>Database migration control center</span>
          </div>
        </div>

        <div class="topbar-right">
          <label class="environment-select">
            Environment
            <select v-model="selectedEnvironment">
              <option v-for="environment in environmentNames" :key="environment">
                {{ environment }}
              </option>
            </select>
          </label>

          <button class="icon-button" @click="loadDashboard" title="Refresh dashboard">
            <RefreshCw :size="18" />
          </button>
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

      <section class="content">
        <div class="page-title">
          <div>
            <h2>Database Version Control Dashboard</h2>
            <p>Monitor migrations, schema state, and environment synchronization.</p>
          </div>
          <button class="primary-button" @click="openApplyModal">
            Apply Pending Migrations
          </button>
        </div>

        

        <div v-if="errorMessage" class="error-banner">
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-banner">
          {{ successMessage }}
        </div>

        <div v-if="loading" class="loading-banner">
          Loading dashboard data from backend...
        </div>

        <section class="cards-grid">
          <article class="metric-card">
            <div>
              <p>Current Environment</p>
              <h3>{{ selectedEnvironment }}</h3>
              <span>Selected workspace</span>
            </div>
            <div class="metric-icon blue">
              <Server :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Total Migrations</p>
              <h3>{{ currentTotalMigrations }}</h3>
              <span>Application changesets</span>
            </div>
            <div class="metric-icon purple">
              <GitBranch :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Pending Migrations</p>
              <h3>{{ currentPendingMigrations }}</h3>
              <span>Ready to apply</span>
            </div>
            <div class="metric-icon orange">
              <AlertTriangle :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Environment Status</p>
              <h3>{{ globalEnvironmentStatus }}</h3>
              <span>DEV / TEST / PROD</span>
            </div>
            <div class="metric-icon green">
              <CheckCircle2 :size="24" />
            </div>
          </article>
        </section>

        <section class="dashboard-grid">
          <article class="panel large">
            <div class="panel-header">
              <div>
                <h3>Migration Status by Environment</h3>
                <p>Current state of DEV, TEST, and PROD.</p>
              </div>
            </div>

            <table>
              <thead>
                <tr>
                  <th>Environment</th>
                  <th>Total Migrations</th>
                  <th>Pending</th>
                  <th>Status</th>
                  <th>Latest Migration</th>
                  <th>Last Update</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="environment in environmentRows" :key="environment.name">
                  <td>
                    <strong>{{ environment.name }}</strong>
                  </td>
                  <td>{{ environment.migrations }}</td>
                  <td>{{ environment.pending }}</td>
                  <td>
                    <span class="status-pill" :class="statusClass(environment.status)">
                      {{ environment.status }}
                    </span>
                  </td>
                  <td>{{ environment.latestMigration }}</td>
                  <td>{{ environment.lastUpdate }}</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Recent Executions</h3>
                <p>Latest migration execution logs.</p>
              </div>
            </div>

            <div v-if="recentExecutions.length === 0" class="empty-state">
              No execution logs found.
            </div>

            <div v-else class="execution-list">
              <div
                v-for="execution in recentExecutions"
                :key="execution.id"
                class="execution-item"
              >
                <div>
                  <strong>{{ execution.environment }} · {{ executionActionLabel(execution.requestType) }}</strong>
                  <span>Requested by {{ execution.requestedBy }}</span>
                </div>
                <div class="execution-meta">
                  <span class="status-pill" :class="statusClass(execution.status)">
                    {{ execution.status }}
                  </span>
                  <small>{{ formatDuration(execution.durationMs) }}</small>
                </div>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Environment Comparison</h3>
                <p>Synchronization status between environments.</p>
              </div>
            </div>

            <div
              v-for="comparison in comparisons"
              :key="comparison.label"
              class="compare-card"
            >
              <div>
                <strong>{{ comparison.label }}</strong>
                <span>
                  {{ comparison.missingInTargetCount }} missing,
                  {{ comparison.extraInTargetCount }} extra
                </span>
              </div>
              <span
                class="status-pill"
                :class="comparison.inSync ? 'success' : 'warning'"
              >
                {{ comparison.inSync ? 'In sync' : 'Needs review' }}
              </span>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Schema Explorer</h3>
                <p>Tables in {{ selectedEnvironment }} environment.</p>
              </div>
            </div>

            <div v-if="schemaTables.length === 0" class="empty-state">
              No tables found.
            </div>

            <div v-else class="schema-list">
              <span v-for="table in schemaTables" :key="table.tableName">
                {{ table.tableName }}
              </span>
            </div>
          </article>
        </section>
      </section>
    </main>

    <div v-if="showApplyModal" class="modal-backdrop">
      <div class="modal-card">
        <div class="modal-header">
          <div>
            <h3>Apply Pending Migrations</h3>
            <p>Apply existing Liquibase changesets to a selected environment.</p>
          </div>
          <button class="icon-button" @click="closeApplyModal">×</button>
        </div>

        <div class="apply-summary">
          <strong>{{ applyForm.environment }}</strong>
          <span>
            {{ modalPendingMigrations }} pending migration{{ modalPendingMigrations === 1 ? '' : 's' }}
          </span>
        </div>

        <div v-if="modalPendingMigrations === 0" class="modal-warning">
          This environment is already up to date. There are no pending migrations to apply.
        </div>

        <form class="execution-form" @submit.prevent="submitApplyMigrations">
          <label>
            Environment
            <select v-model="applyForm.environment">
              <option>DEV</option>
              <option>TEST</option>
              <option>PROD</option>
            </select>
          </label>

          <label>
            Requested By
            <input v-model="applyForm.requestedBy" type="text" />
          </label>

          <label class="full-width">
            Reason
            <textarea v-model="applyForm.reason" rows="4"></textarea>
          </label>

          <p class="form-help full-width">
            This action queues a controlled Liquibase update. The backend validates pending migrations,
            runs the update, and stores execution logs.
          </p>

          <div class="modal-actions">
            <button type="button" class="secondary-button" @click="closeApplyModal">
              Cancel
            </button>
            <button type="submit" class="primary-button" :disabled="!canApplyMigrations">
              {{ applyingMigrations ? 'Applying...' : 'Apply Migrations' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template> 