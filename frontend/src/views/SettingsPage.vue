<script setup>
import { onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getArtifactStorageHealth } from '../services/api'

const storageLoading = ref(false)
const storageErrorMessage = ref(null)
const artifactStorageHealth = ref(null)

function storageStatusClass() {
  if (!artifactStorageHealth.value) {
    return 'muted'
  }

  return artifactStorageHealth.value.available ? 'success' : 'danger'
}

function storageStatusLabel() {
  if (storageLoading.value) {
    return 'Checking...'
  }

  if (!artifactStorageHealth.value) {
    return 'Unknown'
  }

  return artifactStorageHealth.value.available ? 'Available' : 'Unavailable'
}

async function loadArtifactStorageHealth() {
  storageLoading.value = true
  storageErrorMessage.value = null

  try {
    const response = await getArtifactStorageHealth()
    artifactStorageHealth.value = response.data
  } catch (error) {
    storageErrorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load artifact storage health'

    artifactStorageHealth.value = null
  } finally {
    storageLoading.value = false
  }
}

onMounted(loadArtifactStorageHealth)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Settings</h2>
        <p>Environment and artifact storage configuration overview for the MVP.</p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="loadArtifactStorageHealth">
          <RefreshCw :size="16" />
          Refresh Storage
        </button>
      </div>
    </div>

    <div v-if="storageErrorMessage" class="error-banner">
      {{ storageErrorMessage }}
    </div>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>Configured Environments</h3>
          <p>The dashboard currently manages three Oracle schemas.</p>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>Environment</th>
            <th>Oracle Schema</th>
            <th>Liquibase Config</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong>DEV</strong></td>
            <td>DBVC_DEV</td>
            <td>liquibase-dev.properties</td>
          </tr>
          <tr>
            <td><strong>TEST</strong></td>
            <td>DBVC_TEST</td>
            <td>liquibase-test.properties</td>
          </tr>
          <tr>
            <td><strong>PROD</strong></td>
            <td>DBVC_PROD</td>
            <td>liquibase-prod.properties</td>
          </tr>
        </tbody>
      </table>
    </article>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>Artifact Storage</h3>
          <p>Health status of the MinIO/S3-compatible storage used for generated files and execution logs.</p>
        </div>

        <span class="status-pill" :class="storageStatusClass()">
          {{ storageStatusLabel() }}
        </span>
      </div>

      <div v-if="storageLoading" class="loading-banner">
        Checking artifact storage health...
      </div>

      <div v-if="artifactStorageHealth" class="settings-grid">
        <div class="settings-info-card">
          <span>Provider</span>
          <strong>{{ artifactStorageHealth.provider }}</strong>
        </div>

        <div class="settings-info-card">
          <span>Bucket</span>
          <strong>{{ artifactStorageHealth.bucket }}</strong>
        </div>

        <div class="settings-info-card">
          <span>Endpoint</span>
          <strong>{{ artifactStorageHealth.endpoint }}</strong>
        </div>

        <div class="settings-info-card">
          <span>Status Message</span>
          <strong>{{ artifactStorageHealth.message }}</strong>
        </div>
      </div>

      <div v-else-if="!storageLoading" class="empty-state">
        Artifact storage health is not available yet.
      </div>

      <p class="form-help">
        MinIO currently stores generated migration SQL files under
        <strong>generated/</strong>
        and execution logs under
        <strong>executions/DEV</strong>,
        <strong>executions/TEST</strong>,
        and
        <strong>executions/PROD</strong>.
      </p>
    </article>
  </section>
</template>