import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

export function getEnvironmentSummary(environment) {
  return api.get(`/environments/${environment}/migrations/summary`)
}

export function getEnvironmentPendingMigrations(environment) {
  return api.get(`/environments/${environment}/migrations/pending`)
}

export function getEnvironmentSchemaTables(environment) {
  return api.get(`/environments/${environment}/schema/tables`)
}

export function getMigrationExecutions() {
  return api.get('/migration-executions')
}

export function compareEnvironments(source, target) {
  return api.get('/environments/compare', {
    params: {
      source,
      target,
    },
  })
}
export function createMigrationExecutionRequest(payload) {
  return api.post('/migration-executions', payload)
}
export function getMigrationExecutionById(id) {
  return api.get(`/migration-executions/${id}`)
}
export function getEnvironmentTableColumns(environment, tableName) {
  return api.get(`/environments/${environment}/schema/tables/${tableName}/columns`)
}
export function getEnvironmentMigrationHistory(environment) {
  return api.get(`/environments/${environment}/migrations/history`)
}