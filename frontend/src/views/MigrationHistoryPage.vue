<script setup>
import { inject, onMounted, ref, watch } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getEnvironmentMigrationHistory } from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')

const loading = ref(false)
const errorMessage = ref(null)
const history = ref([])

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '—'
}

function getExecutionType(item) {
  return item.execType || item.executionType || 'EXECUTED'
}

function getExecutionOrder(item) {
  return item.orderExecuted ?? item.executionOrder ?? '—'
}

function getExecutedAt(item) {
  return item.dateExecuted || item.executedAt || item.executionDate
}

async function loadHistory() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await getEnvironmentMigrationHistory(selectedEnvironment.value)
    history.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load migration history'
  } finally {
    loading.value = false
  }
}

watch(selectedEnvironment, async () => {
  await loadHistory()
})

onMounted(async () => {
  await loadHistory()
})
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Migration History</h2>
        <p>Applied Liquibase changesets on {{ selectedEnvironment }}.</p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="loadHistory">
          <RefreshCw :size="16" />
          Refresh
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">
      {{ errorMessage }}
    </div>

    <div v-if="loading" class="loading-banner">
      Loading migration history...
    </div>

    <article class="panel">
      <div v-if="!loading && history.length === 0" class="empty-state">
        No migration history found for {{ selectedEnvironment }}.
      </div>

      <table v-else>
        <thead>
          <tr>
            <th>Changeset</th>
            <th>Author</th>
            <th>Filename</th>
            <th>Type</th>
            <th>Order</th>
            <th>Executed At</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="item in history"
            :key="`${item.id}-${item.author}-${getExecutionOrder(item)}`"
          >
            <td>
              <strong>{{ item.id }}</strong>
            </td>
            <td>{{ item.author }}</td>
            <td>{{ item.filename || '—' }}</td>
            <td>
              <span class="status-pill success">
                {{ getExecutionType(item) }}
              </span>
            </td>
            <td>{{ getExecutionOrder(item) }}</td>
            <td>{{ formatDate(getExecutedAt(item)) }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>