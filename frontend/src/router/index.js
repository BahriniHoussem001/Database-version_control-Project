import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../services/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginPage.vue'),
    meta: {
      public: true,
      title: 'Login',
      subtitle: 'Secure access',
    },
  },
  {
    path: '/',
    name: 'dashboard',
    component: () => import('../views/DashboardPage.vue'),
    meta: {
      title: 'Dashboard',
      subtitle: 'Database migration control center',
    },
  },
  {
    path: '/migration-history',
    name: 'migration-history',
    component: () => import('../views/MigrationHistoryPage.vue'),
    meta: {
      title: 'Migration History',
      subtitle: 'Applied Liquibase changesets by environment',
    },
  },
  {
    path: '/pending-migrations',
    name: 'pending-migrations',
    component: () => import('../views/PendingMigrationsPage.vue'),
    meta: {
      title: 'Pending Migrations',
      subtitle: 'Changesets waiting to be applied',
    },
  },
  {
    path: '/execution-logs',
    name: 'execution-logs',
    component: () => import('../views/ExecutionLogsPage.vue'),
    meta: {
      title: 'Execution Logs',
      subtitle: 'Audit trail of migration executions',
    },
  },
  {
    path: '/environment-comparison',
    name: 'environment-comparison',
    component: () => import('../views/EnvironmentComparisonPage.vue'),
    meta: {
      title: 'Environment Comparison',
      subtitle: 'Compare DEV, TEST, and PROD synchronization',
    },
  },
  {
    path: '/schema-explorer',
    name: 'schema-explorer',
    component: () => import('../views/SchemaExplorerPage.vue'),
    meta: {
      title: 'Schema Explorer',
      subtitle: 'Inspect database tables and columns',
    },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SettingsPage.vue'),
    meta: {
      title: 'Settings',
      subtitle: 'Environment configuration overview',
    },
  },
  {
  path: '/migration-builder',
  name: 'migration-builder',
  component: () => import('../views/MigrationBuilderPage.vue'),
  meta: {
    title: 'Migration Builder',
    subtitle: 'Generate application migration files',
  },
},
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }

  if (!isAuthenticated()) {
    return '/login'
  }

  return true
})

export default router