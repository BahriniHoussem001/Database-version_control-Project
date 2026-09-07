<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import {
  getEnvironmentPendingMigrations,
  getGeneratedMigrationArtifact,
} from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')

const loading = ref(false)
const errorMessage = ref(null)
const pendingMigrations = ref([])

const showSqlPreviewModal = ref(false)
const previewLoading = ref(false)
const previewError = ref(null)
const previewContent = ref('')
const selectedMigration = ref(null)

const pendingCount = computed(() => pendingMigrations.value.length)

const selectedMigrationFilename = computed(() => {
  return selectedMigration.value ? getMigrationFilename(selectedMigration.value) : null
})

function statusClass(status) {
  if (!status) return 'muted'

  const value = status.toUpperCase()

  if (value.includes('VALID')) return 'success'
  if (value.includes('WARNING') || value.includes('PENDING')) return 'warning'
  if (value.includes('INVALID') || value.includes('FAILED') || value.includes('ERROR')) return 'danger'

  return 'warning'
}

function pendingMigrationLabel(migration) {
  if (!migration) return 'Pending changeset'

  return (
    migration.changesetId ||
    migration.changeSetId ||
    migration.id ||
    migration.description ||
    'Pending changeset'
  )
}

function getMigrationRawPath(migration) {
  return (
    migration?.filename ||
    migration?.fileName ||
    migration?.path ||
    migration?.filePath ||
    migration?.relativePath ||
    migration?.changelogFile ||
    migration?.changeLogFile ||
    migration?.changeLogPath ||
    ''
  )
}

function getMigrationFilename(migration) {
  const rawPath = getMigrationRawPath(migration)

  if (!rawPath) {
    return null
  }

  const filename = String(rawPath).split(/[\\/]/).pop()

  if (!filename || !filename.toLowerCase().endsWith('.sql')) {
    return null
  }

  return filename
}

function migrationPathLabel(migration) {
  const rawPath = getMigrationRawPath(migration)
  return rawPath || 'No changelog file path returned'
}

function isGeneratedMigration(migration) {
  const filename = getMigrationFilename(migration)

  if (!filename) {
    return false
  }

  const rawPath = getMigrationRawPath(migration).toLowerCase()

  return (
    rawPath.includes('generated') ||
    /^V\d{3}__.+\.sql$/i.test(filename)
  )
}

function migrationKey(migration, index) {
  return `${pendingMigrationLabel(migration)}-${migration.author || 'unknown'}-${index}`
}

async function loadPending() {
  loading.value = true
  errorMessage.value = null

  try {
    const response = await getEnvironmentPendingMigrations(selectedEnvironment.value)
    pendingMigrations.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load pending migrations'
  } finally {
    loading.value = false
  }
}

async function openSqlPreview(migration) {
  const filename = getMigrationFilename(migration)

  if (!filename) {
    errorMessage.value = 'Unable to preview SQL artifact because the migration filename is missing.'
    return
  }

  selectedMigration.value = migration
  showSqlPreviewModal.value = true
  previewLoading.value = true
  previewError.value = null
  previewContent.value = ''

  try {
    const response = await getGeneratedMigrationArtifact(filename)

    previewContent.value =
      typeof response.data === 'string'
        ? response.data
        : JSON.stringify(response.data, null, 2)
  } catch (error) {
    previewError.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to preview generated SQL artifact'
  } finally {
    previewLoading.value = false
  }
}

function closeSqlPreview() {
  showSqlPreviewModal.value = false
  selectedMigration.value = null
  previewContent.value = ''
  previewError.value = null
  previewLoading.value = false
}

watch(selectedEnvironment, async () => {
  closeSqlPreview()
  await loadPending()
})

onMounted(loadPending)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Pending Migrations</h2>
        <p>
          {{ pendingCount }} changeset{{ pendingCount === 1 ? '' : 's' }}
          waiting to be applied on {{ selectedEnvironment }}.
        </p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="loadPending">
          <RefreshCw :size="16" />
          Refresh
        </button>
      </div>
    </div>

    <div class="migration-policy-note">
      <strong>Ordered migration policy</strong>
      <p>
        The platform applies pending changesets in changelog order. Skipping individual pending
        migrations is not allowed in the MVP because later migrations may depend on earlier ones.
      </p>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading pending migrations...</div>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>{{ selectedEnvironment }} Pending Queue</h3>
          <p>Review the pending changesets before applying them through the dashboard.</p>
        </div>
      </div>

      <div v-if="pendingMigrations.length === 0" class="empty-state">
        No pending migrations for {{ selectedEnvironment }}.
      </div>

      <div v-else class="pending-list">
        <div
          v-for="(migration, index) in pendingMigrations"
          :key="migrationKey(migration, index)"
          class="pending-item"
        >
          <div class="pending-content">
            <strong>{{ pendingMigrationLabel(migration) }}</strong>
            <span>{{ migrationPathLabel(migration) }}</span>
          </div>

          <div class="pending-actions">
            <span class="status-pill" :class="statusClass(migration.status)">
              {{ migration.status || 'PENDING' }}
            </span>

            <button
              v-if="isGeneratedMigration(migration)"
              type="button"
              class="secondary-button small-button"
              @click="openSqlPreview(migration)"
            >
              Preview SQL
            </button>
          </div>
        </div>
      </div>
    </article>

    <div v-if="showSqlPreviewModal" class="modal-backdrop">
      <div class="modal-card sql-preview-modal">
        <div class="modal-header">
          <div>
            <h3>Generated SQL Preview</h3>
            <p v-if="selectedMigration">
              {{ selectedEnvironment }} · {{ pendingMigrationLabel(selectedMigration) }}
            </p>
          </div>

          <button class="icon-button" @click="closeSqlPreview">×</button>
        </div>

        <div v-if="previewLoading" class="loading-banner">
          Loading generated SQL artifact from MinIO...
        </div>

        <div v-if="previewError" class="error-banner">
          {{ previewError }}
        </div>

        <div v-if="selectedMigrationFilename" class="sql-preview-meta">
          <div>
            <span>Filename</span>
            <strong>{{ selectedMigrationFilename }}</strong>
          </div>

          <div>
            <span>Artifact object key</span>
            <code>generated/{{ selectedMigrationFilename }}</code>
          </div>
        </div>

        <pre v-if="previewContent" class="sql-preview-content">{{ previewContent }}</pre>

        <div class="modal-actions">
          <button type="button" class="secondary-button" @click="closeSqlPreview">
            Close
          </button>
        </div>
      </div>
    </div>
  </section>
</template>