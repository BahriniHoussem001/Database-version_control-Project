<script setup>
import { inject, onMounted, ref, watch } from 'vue'
import { getEnvironmentPendingMigrations } from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')

const loading = ref(false)
const errorMessage = ref(null)
const pendingMigrations = ref([])

async function loadPending() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await getEnvironmentPendingMigrations(selectedEnvironment.value)
    pendingMigrations.value = response.data
  } catch (error) {
    errorMessage.value = error.message || 'Unable to load pending migrations'
  } finally {
    loading.value = false
  }
}

watch(selectedEnvironment, loadPending)
onMounted(loadPending)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Pending Migrations</h2>
        <p>Changesets waiting to be applied on {{ selectedEnvironment }}.</p>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading pending migrations...</div>

    <article class="panel">
      <div v-if="pendingMigrations.length === 0" class="empty-state">
        No pending migrations for {{ selectedEnvironment }}.
      </div>

      <div v-else class="pending-list">
        <div
          v-for="migration in pendingMigrations"
          :key="migration.id + migration.author"
          class="pending-item"
        >
          <div>
            <strong>{{ migration.id }}</strong>
            <span>{{ migration.filename }}</span>
          </div>
          <span class="status-pill warning">{{ migration.status }}</span>
        </div>
      </div>
    </article>
  </section>
</template>