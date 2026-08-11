<script setup>
import { inject, onMounted, ref, watch } from 'vue'
import {
  getEnvironmentSchemaTables,
  getEnvironmentTableColumns,
} from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')

const loading = ref(false)
const errorMessage = ref(null)
const schemaTables = ref([])
const selectedSchemaTable = ref(null)
const schemaColumns = ref([])

async function loadTables() {
  loading.value = true
  errorMessage.value = null
  selectedSchemaTable.value = null
  schemaColumns.value = []

  try {
    const response = await getEnvironmentSchemaTables(selectedEnvironment.value)
    schemaTables.value = response.data
  } catch (error) {
    errorMessage.value = error.message || 'Unable to load schema tables'
  } finally {
    loading.value = false
  }
}

async function openTable(tableName) {
  selectedSchemaTable.value = tableName
  schemaColumns.value = []

  try {
    const response = await getEnvironmentTableColumns(selectedEnvironment.value, tableName)
    schemaColumns.value = response.data
  } catch (error) {
    errorMessage.value = error.message || `Unable to load columns for ${tableName}`
  }
}

watch(selectedEnvironment, loadTables)
onMounted(loadTables)
</script>

<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Schema Explorer</h2>
        <p>Inspect tables and columns in {{ selectedEnvironment }}.</p>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading schema...</div>

    <article class="panel schema-panel">
      <div class="schema-explorer-layout">
        <div class="schema-table-list">
          <button
            v-for="table in schemaTables"
            :key="table.tableName"
            class="schema-table-button"
            :class="{ active: selectedSchemaTable === table.tableName }"
            @click="openTable(table.tableName)"
          >
            <strong>{{ table.tableName }}</strong>
            <span>{{ table.status }}</span>
          </button>
        </div>

        <div class="schema-columns-panel">
          <div v-if="!selectedSchemaTable" class="empty-state">
            Select a table to view its columns.
          </div>

          <div v-else>
            <div class="columns-title">
              <div>
                <h4>{{ selectedSchemaTable }}</h4>
                <p>{{ schemaColumns.length }} columns</p>
              </div>
            </div>

            <table>
              <thead>
                <tr>
                  <th>Column</th>
                  <th>Type</th>
                  <th>Nullable</th>
                  <th>Position</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="column in schemaColumns" :key="column.columnName">
                  <td><strong>{{ column.columnName }}</strong></td>
                  <td>{{ column.dataType }}</td>
                  <td>{{ column.nullable === 'Y' ? 'YES' : 'NO' }}</td>
                  <td>{{ column.columnId }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>