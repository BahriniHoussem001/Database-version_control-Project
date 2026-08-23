<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import {
  getEnvironmentSchemaTables,
  getEnvironmentTableColumns,
} from '../services/api'

const selectedEnvironment = inject('selectedEnvironment')

const loading = ref(false)
const loadingColumns = ref(false)
const errorMessage = ref(null)

const schemaTables = ref([])
const selectedSchemaTable = ref(null)
const schemaColumns = ref([])

const tablesFolderOpen = ref(true)

const tableCountLabel = computed(() => {
  const count = schemaTables.value.length
  return `${count} table${count === 1 ? '' : 's'}`
})

const selectedTableLabel = computed(() => {
  return selectedSchemaTable.value || 'No table selected'
})

async function loadTables() {
  loading.value = true
  errorMessage.value = null
  selectedSchemaTable.value = null
  schemaColumns.value = []
  tablesFolderOpen.value = true

  try {
    const response = await getEnvironmentSchemaTables(selectedEnvironment.value)
    schemaTables.value = response.data
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Unable to load schema tables'
  } finally {
    loading.value = false
  }
}

async function openTable(tableName) {
  selectedSchemaTable.value = tableName
  schemaColumns.value = []
  loadingColumns.value = true
  errorMessage.value = null

  try {
    const response = await getEnvironmentTableColumns(selectedEnvironment.value, tableName)
    schemaColumns.value = response.data
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      `Unable to load columns for ${tableName}`
  } finally {
    loadingColumns.value = false
  }
}

function toggleTablesFolder() {
  tablesFolderOpen.value = !tablesFolderOpen.value

  if (!tablesFolderOpen.value) {
    selectedSchemaTable.value = null
    schemaColumns.value = []
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
        <p>
          Explore database objects in {{ selectedEnvironment }} using a SQL-style object tree.
        </p>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="loading" class="loading-banner">Loading schema objects...</div>

    <article class="panel schema-panel">
      <div class="schema-explorer-layout">
        <div class="schema-table-list">
          <div class="panel-header">
            <div>
              <h3>Object Explorer</h3>
              <p>{{ selectedEnvironment }} schema</p>
            </div>
          </div>

          <button
            class="schema-table-button"
            :class="{ active: tablesFolderOpen }"
            @click="toggleTablesFolder"
          >
            <strong>{{ tablesFolderOpen ? '▾' : '▸' }} Tables</strong>
            <span>{{ tableCountLabel }}</span>
          </button>

          <div v-if="tablesFolderOpen">
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

          <button class="schema-table-button" disabled>
            <strong>▸ Views</strong>
            <span>Later</span>
          </button>

          <button class="schema-table-button" disabled>
            <strong>▸ Indexes</strong>
            <span>Later</span>
          </button>

          <button class="schema-table-button" disabled>
            <strong>▸ Sequences</strong>
            <span>Later</span>
          </button>

          <button class="schema-table-button" disabled>
            <strong>▸ Constraints</strong>
            <span>Later</span>
          </button>
        </div>

        <div class="schema-columns-panel">
          <div class="panel-header">
            <div>
              <h3>{{ selectedTableLabel }}</h3>
              <p>
                {{
                  selectedSchemaTable
                    ? `${schemaColumns.length} column${schemaColumns.length === 1 ? '' : 's'}`
                    : 'Select a table from the object explorer'
                }}
              </p>
            </div>
          </div>

          <div v-if="!selectedSchemaTable" class="empty-state">
            Select Tables, then choose a table name to view its columns.
          </div>

          <div v-else-if="loadingColumns" class="loading-banner">
            Loading columns for {{ selectedSchemaTable }}...
          </div>

          <div v-else>
            <div class="columns-title">
             <div>
              <h4>Columns</h4>
              <p>{{ selectedSchemaTable }} table structure</p>
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