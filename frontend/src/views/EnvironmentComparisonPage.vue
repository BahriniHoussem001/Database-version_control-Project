<script setup>
import { onMounted, ref } from 'vue'
import { compareEnvironments } from '../services/api'

const loading = ref(false)
const errorMessage = ref(null)
const comparisons = ref([])

async function loadComparisons() {
  loading.value = true
  errorMessage.value = null

  try {
    const [devTestResponse, testProdResponse] = await Promise.all([
      compareEnvironments('DEV', 'TEST'),
      compareEnvironments('TEST', 'PROD'),
    ])

    comparisons.value = [
      { label: 'DEV → TEST', ...devTestResponse.data },
      { label: 'TEST → PROD', ...testProdResponse.data },
    ]
  } catch (error) {
    errorMessage.value = error.message || 'Unable to compare environments'
  } finally {
    loading.value = false
  }
}

onMounted(loadComparisons)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Environment Comparison</h2>
        <p>Check if target environments are synchronized.</p>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading comparison...</div>

    <section class="dashboard-grid">
      <article
        v-for="comparison in comparisons"
        :key="comparison.label"
        class="panel"
      >
        <div class="panel-header">
          <div>
            <h3>{{ comparison.label }}</h3>
            <p>
              Source: {{ comparison.sourceTotalMigrations }} migrations ·
              Target: {{ comparison.targetTotalMigrations }} migrations
            </p>
          </div>
          <span class="status-pill" :class="comparison.inSync ? 'success' : 'warning'">
            {{ comparison.inSync ? 'In sync' : 'Needs review' }}
          </span>
        </div>

        <div class="details-grid">
          <div class="detail-item">
            <span>Missing in target</span>
            <strong>{{ comparison.missingInTargetCount }}</strong>
          </div>
          <div class="detail-item">
            <span>Extra in target</span>
            <strong>{{ comparison.extraInTargetCount }}</strong>
          </div>
        </div>
      </article>
    </section>
  </section>
</template>