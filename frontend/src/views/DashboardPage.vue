<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  GitBranch,
  Server,
  RefreshCw,
} from 'lucide-vue-next'

import {
  compareEnvironments,
  createMigrationExecutionRequest,
  getEnvironmentPendingMigrations,
  getEnvironmentPromotionStatus,
  getEnvironmentSummary,
  getMigrationExecutions,
} from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')
const environmentNames = inject('environmentNames')
const currentUser = inject('currentUser')

const loading = ref(false)
const errorMessage = ref(null)
const successMessage = ref(null)

const environmentRows = ref([])
const recentExecutions = ref([])
const comparisons = ref([])
const promotionStatuses = ref([])

const showApplyModal = ref(false)
const applyingMigrations = ref(false)

const executionModeOptions = [
  {
    value: 'NEXT',
    title: 'Apply next pending migration only',
    description: 'Safest option. Applies only the first pending changeset in Liquibase order.',
  },
  {
    value: 'ALL',
    title: 'Apply all pending migrations',
    description: 'Advanced option. Applies every currently pending changeset in Liquibase order.',
  },
]

const applyForm = ref({
  environment: 'DEV',
  executionMode: 'NEXT',
  reason: '',
  requestedBy: 'houssem',
})

const isAdmin = computed(() => {
  return currentUser.value?.role === 'ADMIN'
})

const currentEnvironmentRow = computed(() => {
  return environmentRows.value.find((item) => item.name === selectedEnvironment.value)
})

const currentTotalMigrations = computed(() => currentEnvironmentRow.value?.migrations ?? 0)
const currentPendingMigrations = computed(() => currentEnvironmentRow.value?.pending ?? 0)

const modalEnvironmentRow = computed(() => {
  return environmentRows.value.find((item) => item.name === applyForm.value.environment)
})

const modalPendingMigrations = computed(() => modalEnvironmentRow.value?.pending ?? 0)

const modalNextPendingMigration = computed(() => {
  const nextMigration = modalEnvironmentRow.value?.pendingMigrations?.[0]
  return pendingMigrationLabel(nextMigration)
})

const isApplyAllMode = computed(() => applyForm.value.executionMode === 'ALL')

const applyModalTitle = computed(() => {
  return isApplyAllMode.value ? 'Apply All Pending Migrations' : 'Apply Next Pending Migration'
})

const applyModalDescription = computed(() => {
  return isApplyAllMode.value
    ? 'Apply all pending Liquibase changesets to the selected environment.'
    : 'Apply only the next pending Liquibase changeset to the selected environment.'
})

const applyButtonLabel = computed(() => {
  if (applyingMigrations.value) return 'Applying...'
  return isApplyAllMode.value ? 'Apply All Migrations' : 'Apply Next Migration'
})

const canApplyMigrations = computed(() => {
  return (
    modalPendingMigrations.value > 0 &&
    canApplyEnvironment(applyForm.value.environment) &&
    !applyingMigrations.value
  )
})

const globalEnvironmentStatus = computed(() => {
  if (!environmentRows.value.length) return 'Loading'

  const hasPending = environmentRows.value.some((item) => item.pending > 0)
  const hasOutOfSync = comparisons.value.some((item) => !item.inSync)

  return hasPending || hasOutOfSync ? 'Needs Review' : 'In Sync'
})

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '—'
}

function formatDuration(durationMs) {
  if (durationMs === null || durationMs === undefined) return '—'
  return durationMs < 1000 ? `${durationMs} ms` : `${Math.round(durationMs / 1000)}s`
}

function environmentStatusFromPending(pendingCount) {
  return pendingCount > 0 ? `Pending ${pendingCount}` : 'Up to date'
}

function pendingMigrationLabel(migration) {
  if (!migration) return null

  return (
    migration.changesetId ||
    migration.changeSetId ||
    migration.id ||
    migration.description ||
    migration.filename ||
    migration.fileName ||
    migration.path ||
    'Next pending changeset'
  )
}

function statusClass(status) {
  if (!status) return 'muted'

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

function executionActionLabel(requestType, executionMode) {
  if (requestType === 'UPDATE') {
    return executionMode === 'ALL' ? 'Apply all migrations' : 'Apply next migration'
  }

  if (requestType === 'ROLLBACK') return 'Rollback'

  return requestType || 'Unknown action'
}

function getPromotionStatus(environment) {
  return promotionStatuses.value.find((item) => item.environment === environment)
}

function canApplyEnvironment(environment) {
  return getPromotionStatus(environment)?.canApply === true
}

function getApplyBlockedReason(environment) {
  return getPromotionStatus(environment)?.blockedReason || null
}

function buildApplyReason(environment, executionMode) {
  if (executionMode === 'ALL') {
    return `Apply all pending migrations to ${environment}`
  }

  return `Apply next pending migration to ${environment}`
}

async function loadEnvironmentRows() {
  const rows = await Promise.all(
    environmentNames.map(async (environment) => {
      const [summaryResponse, pendingResponse] = await Promise.all([
        getEnvironmentSummary(environment),
        getEnvironmentPendingMigrations(environment),
      ])

      const summary = summaryResponse.data
      const pendingMigrations = Array.isArray(pendingResponse.data) ? pendingResponse.data : []

      return {
        name: environment,
        migrations: summary.totalExecutedMigrations ?? 0,
        pending: pendingMigrations.length,
        pendingMigrations,
        status: environmentStatusFromPending(pendingMigrations.length),
        latestMigration: summary.latestMigrationId ?? '—',
        lastUpdate: formatDate(summary.latestExecutedAt),
      }
    })
  )

  environmentRows.value = rows
}

async function loadRecentExecutions() {
  const response = await getMigrationExecutions()
  recentExecutions.value = response.data.slice(0, 3)
}

async function loadComparisons() {
  const [devTestResponse, testProdResponse] = await Promise.all([
    compareEnvironments('DEV', 'TEST'),
    compareEnvironments('TEST', 'PROD'),
  ])

  comparisons.value = [
    { label: 'DEV → TEST', ...devTestResponse.data },
    { label: 'TEST → PROD', ...testProdResponse.data },
  ]
}

async function loadPromotionStatuses() {
  const response = await getEnvironmentPromotionStatus()
  promotionStatuses.value = response.data
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = null

  try {
    await Promise.all([
      loadEnvironmentRows(),
      loadRecentExecutions(),
      loadComparisons(),
      loadPromotionStatuses(),
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
    executionMode: 'NEXT',
    reason: buildApplyReason(selectedEnvironment.value, 'NEXT'),
    requestedBy: currentUser.value?.username || 'SYSTEM',
  }

  showApplyModal.value = true
}

function closeApplyModal() {
  showApplyModal.value = false
}

function handleApplyEnvironmentChange() {
  applyForm.value.reason = buildApplyReason(
    applyForm.value.environment,
    applyForm.value.executionMode
  )
}

function handleExecutionModeChange() {
  applyForm.value.reason = buildApplyReason(
    applyForm.value.environment,
    applyForm.value.executionMode
  )
}

async function submitApplyMigrations() {
  if (modalPendingMigrations.value === 0) {
    errorMessage.value = `No pending migration to apply on ${applyForm.value.environment}.`
    return
  }

  if (!canApplyEnvironment(applyForm.value.environment)) {
    errorMessage.value =
      getApplyBlockedReason(applyForm.value.environment) ||
      `Pending migrations cannot be applied to ${applyForm.value.environment}.`
    return
  }

  applyingMigrations.value = true
  errorMessage.value = null
  successMessage.value = null

  try {
    await createMigrationExecutionRequest({
      environment: applyForm.value.environment,
      requestType: 'UPDATE',
      executionMode: applyForm.value.executionMode,
      priority: 'NORMAL',
      reason: applyForm.value.reason,
      requestedBy: applyForm.value.requestedBy,
    })

    successMessage.value = isApplyAllMode.value
      ? `All pending migrations execution queued for ${applyForm.value.environment}.`
      : `Next pending migration execution queued for ${applyForm.value.environment}.`

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
      'Unable to queue migration execution'
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
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Database Version Control Dashboard</h2>
        <p>Global overview of migration health, synchronization, and recent activity.</p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="loadDashboard">
          <RefreshCw :size="16" />
          Refresh
        </button>

        <button
          v-if="isAdmin"
          class="primary-button"
          @click="openApplyModal"
        >
          Apply Migrations
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="successMessage" class="success-banner">{{ successMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading dashboard data from backend...</div>

    <section class="cards-grid">
      <article class="metric-card">
        <div>
          <p>Selected Environment</p>
          <h3>{{ selectedEnvironment }}</h3>
          <span>Current workspace</span>
        </div>
        <div class="metric-icon blue">
          <Server :size="24" />
        </div>
      </article>

      <article class="metric-card">
        <div>
          <p>Applied Migrations</p>
          <h3>{{ currentTotalMigrations }}</h3>
          <span>On {{ selectedEnvironment }}</span>
        </div>
        <div class="metric-icon purple">
          <GitBranch :size="24" />
        </div>
      </article>

      <article class="metric-card">
        <div>
          <p>Pending Migrations</p>
          <h3>{{ currentPendingMigrations }}</h3>
          <span>Waiting on {{ selectedEnvironment }}</span>
        </div>
        <div class="metric-icon orange">
          <AlertTriangle :size="24" />
        </div>
      </article>

      <article class="metric-card">
        <div>
          <p>Global Status</p>
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
            <h3>Environment Overview</h3>
            <p>High-level state of each managed Oracle schema.</p>
          </div>

          <RouterLink class="secondary-button" to="/environment-comparison">
            View comparison
          </RouterLink>
        </div>

        <table>
          <thead>
            <tr>
              <th>Environment</th>
              <th>Applied</th>
              <th>Pending</th>
              <th>Status</th>
              <th>Latest Migration</th>
              <th>Last Update</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="environment in environmentRows" :key="environment.name">
              <td><strong>{{ environment.name }}</strong></td>
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
            <h3>Latest Executions</h3>
            <p>Last 3 migration execution requests.</p>
          </div>

          <RouterLink class="secondary-button" to="/execution-logs">
            View logs
          </RouterLink>
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
              <strong>
                {{ execution.environment }} ·
                {{ executionActionLabel(execution.requestType, execution.executionMode) }}
              </strong>
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
            <h3>Synchronization Summary</h3>
            <p>Quick comparison between deployment stages.</p>
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
          <span class="status-pill" :class="comparison.inSync ? 'success' : 'warning'">
            {{ comparison.inSync ? 'In sync' : 'Needs review' }}
          </span>
        </div>

        <RouterLink class="secondary-button" to="/environment-comparison">
          Open detailed comparison
        </RouterLink>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h3>Focused Modules</h3>
            <p>Use sidebar pages for detailed investigation.</p>
          </div>
        </div>

        <div class="module-links">
          <RouterLink to="/migration-history">Migration History</RouterLink>
          <RouterLink to="/pending-migrations">Pending Migrations</RouterLink>
          <RouterLink to="/execution-logs">Execution Logs</RouterLink>
          <RouterLink to="/schema-explorer">Schema Explorer</RouterLink>
        </div>
      </article>
    </section>

    <div v-if="showApplyModal" class="modal-backdrop">
      <div class="modal-card">
        <div class="modal-header">
          <div>
            <h3>{{ applyModalTitle }}</h3>
            <p>{{ applyModalDescription }}</p>
          </div>
          <button class="icon-button" @click="closeApplyModal">×</button>
        </div>

        <div class="apply-summary">
          <strong>{{ applyForm.environment }}</strong>
          <span>
            {{ modalPendingMigrations }} pending migration{{ modalPendingMigrations === 1 ? '' : 's' }}
          </span>
        </div>

        <p
          v-if="!isApplyAllMode && modalNextPendingMigration"
          class="form-help full-width"
        >
          Next to apply:
          <strong>{{ modalNextPendingMigration }}</strong>
        </p>

        <div v-if="modalPendingMigrations === 0" class="modal-warning">
          This environment is already up to date. There is no pending migration to apply.
        </div>

        <div
          v-if="getApplyBlockedReason(applyForm.environment)"
          class="modal-warning"
        >
          {{ getApplyBlockedReason(applyForm.environment) }}
        </div>

        <form class="execution-form" @submit.prevent="submitApplyMigrations">
          <label>
            Environment
            <select v-model="applyForm.environment" @change="handleApplyEnvironmentChange">
              <option
                v-for="environment in environmentNames"
                :key="environment"
                :value="environment"
                :disabled="!canApplyEnvironment(environment)"
              >
                {{ environment }}
              </option>
            </select>
          </label>

          <label>
            Requested By
            <input v-model="applyForm.requestedBy" type="text" disabled />
          </label>

          <div class="full-width execution-mode-group">
            <span class="field-title">Execution Mode</span>

            <label
              v-for="option in executionModeOptions"
              :key="option.value"
              class="execution-mode-option"
              :class="{ active: applyForm.executionMode === option.value }"
            >
              <input
                v-model="applyForm.executionMode"
                type="radio"
                name="executionMode"
                :value="option.value"
                @change="handleExecutionModeChange"
              />

              <div>
                <strong>{{ option.title }}</strong>
                <span>{{ option.description }}</span>
              </div>
            </label>
          </div>

          <div
            v-if="isApplyAllMode && modalPendingMigrations > 1"
            class="modal-warning full-width"
          >
            Advanced mode selected. This will apply all
            {{ modalPendingMigrations }} pending migrations in Liquibase order.
          </div>

          <label class="full-width">
            Reason
            <textarea v-model="applyForm.reason" rows="4"></textarea>
          </label>

          <p class="form-help full-width">
            This action queues a controlled Liquibase update. The backend validates the selected
            execution mode, applies changesets in Liquibase order, and stores execution logs.
          </p>

          <div class="modal-actions">
            <button type="button" class="secondary-button" @click="closeApplyModal">
              Cancel
            </button>
            <button type="submit" class="primary-button" :disabled="!canApplyMigrations">
              {{ applyButtonLabel }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>