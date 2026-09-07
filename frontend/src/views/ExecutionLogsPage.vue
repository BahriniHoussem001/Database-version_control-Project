<script setup>
import { computed, onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getMigrationExecutions } from '../services/api'

const loading = ref(false)
const errorMessage = ref(null)
const executions = ref([])

const totalExecutions = computed(() => executions.value.length)

const archivedExecutions = computed(() => {
  return executions.value.filter((execution) => hasLogArtifact(execution)).length
})

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '—'
}

function formatDuration(durationMs) {
  if (durationMs === null || durationMs === undefined) return '—'
  return durationMs < 1000 ? `${durationMs} ms` : `${Math.round(durationMs / 1000)}s`
}

function statusClass(status) {
  if (!status) return 'muted'

  const value = status.toUpperCase()

  if (value.includes('SUCCESS')) return 'success'
  if (value.includes('RUNNING') || value.includes('QUEUED')) return 'warning'
  if (value.includes('FAILED')) return 'danger'

  return 'muted'
}

function executionActionLabel(requestType, executionMode) {
  if (requestType === 'UPDATE') {
    return executionMode === 'ALL' ? 'Apply all migrations' : 'Apply next migration'
  }

  if (requestType === 'ROLLBACK') {
    return 'Rollback'
  }

  return requestType || 'Unknown action'
}

function executionModeLabel(executionMode) {
  if (executionMode === 'ALL') return 'ALL'
  if (executionMode === 'NEXT') return 'NEXT'
  return '—'
}

function hasLogArtifact(execution) {
  return Boolean(execution?.logArtifactBucket && execution?.logArtifactKey)
}

async function loadExecutions() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await getMigrationExecutions()
    executions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load execution logs'
  } finally {
    loading.value = false
  }
}

onMounted(loadExecutions)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Execution Logs</h2>
        <p>Audit trail of migration execution requests and archived MinIO log artifacts.</p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="loadExecutions">
          <RefreshCw :size="16" />
          Refresh
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading execution logs...</div>

    <section class="cards-grid">
      <article class="metric-card">
        <div>
          <p>Total Executions</p>
          <h3>{{ totalExecutions }}</h3>
          <span>Stored in platform database</span>
        </div>
      </article>

      <article class="metric-card">
        <div>
          <p>Archived Logs</p>
          <h3>{{ archivedExecutions }}</h3>
          <span>Available in MinIO artifact storage</span>
        </div>
      </article>
    </section>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>Execution History</h3>
          <p>
            Each new execution stores its result in Oracle and archives a readable log file in MinIO.
          </p>
        </div>
      </div>

      <div class="execution-log-table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Environment</th>
              <th>Action</th>
              <th>Mode</th>
              <th>Status</th>
              <th>Duration</th>
              <th>Requested By</th>
              <th>Requested At</th>
              <th>Artifact Archive</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="executions.length === 0">
              <td colspan="9">
                <div class="empty-state">No execution logs found.</div>
              </td>
            </tr>

            <tr v-for="execution in executions" :key="execution.id">
              <td>
                <strong>#{{ execution.id }}</strong>
              </td>

              <td>
                <strong>{{ execution.environment }}</strong>
              </td>

              <td>
                {{ executionActionLabel(execution.requestType, execution.executionMode) }}
              </td>

              <td>
                <span class="status-pill muted">
                  {{ executionModeLabel(execution.executionMode) }}
                </span>
              </td>

              <td>
                <span class="status-pill" :class="statusClass(execution.status)">
                  {{ execution.status }}
                </span>
              </td>

              <td>{{ formatDuration(execution.durationMs) }}</td>

              <td>{{ execution.requestedBy || '—' }}</td>

              <td>{{ formatDate(execution.requestedAt) }}</td>

              <td>
                <div v-if="hasLogArtifact(execution)" class="log-artifact-reference">
                  <span class="log-artifact-badge">Archived in MinIO</span>

                  <div class="log-artifact-line">
                    <span>Bucket</span>
                    <strong>{{ execution.logArtifactBucket }}</strong>
                  </div>

                  <div class="log-artifact-line">
                    <span>Object key</span>
                    <code>{{ execution.logArtifactKey }}</code>
                  </div>
                </div>

                <div v-else class="log-artifact-empty">
                  Not archived
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="form-help">
        The MinIO bucket is private. This page shows the archived log location only.
        A download or preview action can be added later through a backend endpoint.
      </p>
    </article>
  </section>
</template>