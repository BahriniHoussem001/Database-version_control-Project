<script setup>
import { onMounted, ref } from 'vue'
import { getMigrationExecutions } from '../services/api'

const loading = ref(false)
const errorMessage = ref(null)
const executions = ref([])

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

async function loadExecutions() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await getMigrationExecutions()
    executions.value = response.data
  } catch (error) {
    errorMessage.value = error.message || 'Unable to load execution logs'
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
        <p>Audit trail of migration execution requests.</p>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading execution logs...</div>

    <article class="panel">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Environment</th>
            <th>Action</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Requested By</th>
            <th>Requested At</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="execution in executions" :key="execution.id">
            <td>{{ execution.id }}</td>
            <td><strong>{{ execution.environment }}</strong></td>
            <td>{{ execution.requestType }}</td>
            <td>
              <span class="status-pill" :class="statusClass(execution.status)">
                {{ execution.status }}
              </span>
            </td>
            <td>{{ formatDuration(execution.durationMs) }}</td>
            <td>{{ execution.requestedBy }}</td>
            <td>{{ formatDate(execution.requestedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>