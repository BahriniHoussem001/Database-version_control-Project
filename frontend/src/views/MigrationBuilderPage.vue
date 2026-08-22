<script setup>
import { computed, inject, ref } from 'vue'
import { FileText, RefreshCw } from 'lucide-vue-next'
import { createGeneratedMigration } from '../services/api'

const currentUser = inject('currentUser')

const loading = ref(false)
const errorMessage = ref(null)
const successMessage = ref(null)
const generatedMigration = ref(null)

const form = ref({
  name: '',
  author: currentUser.value?.username || 'houssem',
  sql: '',
  rollbackSql: '',
})

const canSubmit = computed(() => {
  return (
    form.value.name.trim() &&
    form.value.author.trim() &&
    form.value.sql.trim() &&
    form.value.rollbackSql.trim() &&
    !loading.value
  )
})

function resetForm() {
  form.value = {
    name: '',
    author: currentUser.value?.username || 'houssem',
    sql: '',
    rollbackSql: '',
  }

  errorMessage.value = null
  successMessage.value = null
  generatedMigration.value = null
}

async function submitGeneratedMigration() {
  if (!canSubmit.value) {
    errorMessage.value = 'Please fill all required fields.'
    return
  }

  loading.value = true
  errorMessage.value = null
  successMessage.value = null
  generatedMigration.value = null

  try {
    const response = await createGeneratedMigration({
      name: form.value.name,
      author: form.value.author,
      sql: form.value.sql,
      rollbackSql: form.value.rollbackSql,
    })

    generatedMigration.value = response.data
    successMessage.value = 'Migration file generated successfully.'
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to generate migration file'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Migration Builder</h2>
        <p>Create a new application database migration without manually editing the Liquibase changelog.</p>
      </div>

      <div class="page-actions">
        <button class="secondary-button" @click="resetForm">
          <RefreshCw :size="16" />
          Reset
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="successMessage" class="success-banner">{{ successMessage }}</div>

    <section class="dashboard-grid">
      <article class="panel large">
        <div class="panel-header">
          <div>
            <h3>New Migration</h3>
            <p>
              The platform will generate a separate Liquibase formatted SQL file inside
              <strong>liquibase/changelog/generated</strong>.
            </p>
          </div>

          <div class="metric-icon purple">
            <FileText :size="24" />
          </div>
        </div>

        <form class="execution-form" @submit.prevent="submitGeneratedMigration">
          <label>
            Migration Name
            <input
              v-model="form.name"
              type="text"
              placeholder="Add status column to product"
            />
          </label>

          <label>
            Author
            <input
              v-model="form.author"
              type="text"
              placeholder="houssem"
            />
          </label>

          <label class="full-width">
            SQL Change
            <textarea
              v-model="form.sql"
              rows="7"
              placeholder="ALTER TABLE PRODUCT ADD STATUS VARCHAR2(30) DEFAULT 'ACTIVE' NOT NULL"
            ></textarea>
          </label>

          <label class="full-width">
            Rollback SQL
            <textarea
              v-model="form.rollbackSql"
              rows="5"
              placeholder="ALTER TABLE PRODUCT DROP COLUMN STATUS"
            ></textarea>
          </label>

          <p class="form-help full-width">
            The developer writes the database change and rollback SQL only. The platform generates
            the Liquibase changeset id, file name, formatted SQL header, and rollback directives.
          </p>

          <div class="modal-actions">
            <button type="button" class="secondary-button" @click="resetForm">
              Reset
            </button>

            <button type="submit" class="primary-button" :disabled="!canSubmit">
              {{ loading ? 'Generating...' : 'Generate Migration File' }}
            </button>
          </div>
        </form>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h3>Generated Result</h3>
            <p>Details returned by the backend after file creation.</p>
          </div>
        </div>

        <div v-if="!generatedMigration" class="empty-state">
          No migration generated yet.
        </div>

        <div v-else class="execution-list">
          <div class="execution-item">
            <div>
              <strong>Changeset ID</strong>
              <span>{{ generatedMigration.changesetId }}</span>
            </div>
          </div>

          <div class="execution-item">
            <div>
              <strong>Filename</strong>
              <span>{{ generatedMigration.filename }}</span>
            </div>
          </div>

          <div class="execution-item">
            <div>
              <strong>Relative Path</strong>
              <span>{{ generatedMigration.relativePath }}</span>
            </div>
          </div>

          <p class="form-help">
            After generation, go to Pending Migrations or Dashboard. The new migration should appear
            as pending in DEV, TEST, and PROD. Apply it manually in order: DEV → TEST → PROD.
          </p>
        </div>
      </article>
    </section>
  </section>
</template>