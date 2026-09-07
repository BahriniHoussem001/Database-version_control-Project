import axios from 'axios'
import { getAuthToken, logout } from './auth'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

api.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      logout()
    }

    return Promise.reject(error)
  }
)

export function login(payload) {
  return api.post('/auth/login', payload)
}

export function getCurrentUser() {
  return api.get('/auth/me')
}

export function getEnvironmentSummary(environment) {
  return api.get(`/environments/${environment}/migrations/summary`)
}

export function getEnvironmentMigrationHistory(environment) {
  return api.get(`/environments/${environment}/migrations/history`)
}

export function getEnvironmentPendingMigrations(environment) {
  return api.get(`/environments/${environment}/migrations/pending`)
}

export function getEnvironmentSchemaTables(environment) {
  return api.get(`/environments/${environment}/schema/tables`)
}

export function getEnvironmentTableColumns(environment, tableName) {
  return api.get(`/environments/${environment}/schema/tables/${tableName}/columns`)
}

export function getMigrationExecutions() {
  return api.get('/migration-executions')
}

export function getMigrationExecutionById(id) {
  return api.get(`/migration-executions/${id}`)
}

export function createMigrationExecutionRequest(payload) {
  return api.post('/migration-executions', payload)
}

export function compareEnvironments(source, target) {
  return api.get('/environments/compare', {
    params: {
      source,
      target,
    },
  })
}
export function getEnvironmentPromotionStatus() {
  return api.get('/environments/promotion-status')
}
export function createGeneratedMigration(payload) {
  return api.post('/generated-migrations', payload)
}

export function getMigrationExecutionArtifactLog(id) {
  return api.get(`/migration-executions/${id}/artifact-log`, {
    responseType: 'text',
  })
}
export function getArtifactStorageHealth() {
  return api.get('/artifact-storage/health')
}