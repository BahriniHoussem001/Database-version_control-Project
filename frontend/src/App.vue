<script setup>
import {
  Database,
  GitBranch,
  Clock,
  Layers,
  CheckCircle2,
  AlertTriangle,
  Settings,
  Server,
  FileClock,
  LayoutDashboard,
  Search,
  Bell,
  UserCircle,
  Menu
} from 'lucide-vue-next'

const environments = [
  {
    name: 'DEV',
    migrations: 4,
    pending: 0,
    status: 'Up to date',
    lastUpdate: 'Ready',
    tone: 'success'
  },
  {
    name: 'TEST',
    migrations: 4,
    pending: 0,
    status: 'Up to date',
    lastUpdate: 'Ready',
    tone: 'success'
  },
  {
    name: 'PROD',
    migrations: 4,
    pending: 0,
    status: 'Up to date',
    lastUpdate: 'Ready',
    tone: 'success'
  }
]

const recentExecutions = [
  {
    id: 1,
    environment: 'DEV',
    requestType: 'UPDATE',
    status: 'SUCCESS',
    duration: '15s',
    requestedBy: 'houssem'
  },
  {
    id: 2,
    environment: 'TEST',
    requestType: 'UPDATE',
    status: 'SUCCESS',
    duration: '12s',
    requestedBy: 'houssem'
  },
  {
    id: 3,
    environment: 'PROD',
    requestType: 'UPDATE',
    status: 'SUCCESS',
    duration: '14s',
    requestedBy: 'houssem'
  }
]
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <Database :size="22" />
        </div>
        <div>
          <h1>DB Version Control</h1>
          <p>Oracle + Liquibase</p>
        </div>
      </div>

      <nav class="nav">
        <p class="nav-label">Overview</p>
        <a class="nav-item active" href="#">
          <LayoutDashboard :size="18" />
          Dashboard
        </a>

        <p class="nav-label">Migrations</p>
        <a class="nav-item" href="#">
          <GitBranch :size="18" />
          Migration History
        </a>
        <a class="nav-item" href="#">
          <Clock :size="18" />
          Pending Migrations
        </a>
        <a class="nav-item" href="#">
          <FileClock :size="18" />
          Execution Logs
        </a>

        <p class="nav-label">Compare & Schema</p>
        <a class="nav-item" href="#">
          <Layers :size="18" />
          Environment Comparison
        </a>
        <a class="nav-item" href="#">
          <Server :size="18" />
          Schema Explorer
        </a>

        <p class="nav-label">Admin</p>
        <a class="nav-item" href="#">
          <Settings :size="18" />
          Settings
        </a>
      </nav>

      <div class="sidebar-user">
        <div class="avatar">HB</div>
        <div>
          <strong>Houssem</strong>
          <span>Developer / Admin</span>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-button">
            <Menu :size="20" />
          </button>
          <div>
            <strong>Dashboard</strong>
            <span>Database migration control center</span>
          </div>
        </div>

        <div class="topbar-right">
          <label class="environment-select">
            Environment
            <select>
              <option>DEV</option>
              <option>TEST</option>
              <option>PROD</option>
            </select>
          </label>

          <button class="icon-button">
            <Search :size="19" />
          </button>
          <button class="icon-button">
            <Bell :size="19" />
          </button>
          <button class="icon-button">
            <UserCircle :size="24" />
          </button>
        </div>
      </header>

      <section class="content">
        <div class="page-title">
          <div>
            <h2>Database Version Control Dashboard</h2>
            <p>Monitor migrations, schema state, and environment synchronization.</p>
          </div>
          <button class="primary-button">Create Execution Request</button>
        </div>

        <section class="cards-grid">
          <article class="metric-card">
            <div>
              <p>Current Environment</p>
              <h3>DEV</h3>
              <span>Selected workspace</span>
            </div>
            <div class="metric-icon blue">
              <Server :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Total Migrations</p>
              <h3>4</h3>
              <span>Application changesets</span>
            </div>
            <div class="metric-icon purple">
              <GitBranch :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Pending Migrations</p>
              <h3>0</h3>
              <span>Ready to apply</span>
            </div>
            <div class="metric-icon orange">
              <AlertTriangle :size="24" />
            </div>
          </article>

          <article class="metric-card">
            <div>
              <p>Environment Status</p>
              <h3>In Sync</h3>
              <span>DEV / TEST / PROD</span>
            </div>
            <div class="metric-icon green">
              <CheckCircle2 :size="24" />
            </div>
          </article>
        </section>

        <section class="dashboard-grid">
          <article class="panel large">
            <div class="panel-header">
              <div>
                <h3>Migration Status by Environment</h3>
                <p>Current state of DEV, TEST, and PROD.</p>
              </div>
            </div>

            <table>
              <thead>
                <tr>
                  <th>Environment</th>
                  <th>Total Migrations</th>
                  <th>Pending</th>
                  <th>Status</th>
                  <th>Last Update</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="environment in environments" :key="environment.name">
                  <td>
                    <strong>{{ environment.name }}</strong>
                  </td>
                  <td>{{ environment.migrations }}</td>
                  <td>{{ environment.pending }}</td>
                  <td>
                    <span class="status-pill success">
                      {{ environment.status }}
                    </span>
                  </td>
                  <td>{{ environment.lastUpdate }}</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Recent Executions</h3>
                <p>Latest migration execution requests.</p>
              </div>
            </div>

            <div class="execution-list">
              <div
                v-for="execution in recentExecutions"
                :key="execution.id"
                class="execution-item"
              >
                <div>
                  <strong>{{ execution.environment }} · {{ execution.requestType }}</strong>
                  <span>Requested by {{ execution.requestedBy }}</span>
                </div>
                <div class="execution-meta">
                  <span class="status-pill success">{{ execution.status }}</span>
                  <small>{{ execution.duration }}</small>
                </div>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Environment Comparison</h3>
                <p>Synchronization status between environments.</p>
              </div>
            </div>

            <div class="compare-card">
              <div>
                <strong>DEV → TEST</strong>
                <span>No missing migrations</span>
              </div>
              <span class="status-pill success">In sync</span>
            </div>

            <div class="compare-card">
              <div>
                <strong>TEST → PROD</strong>
                <span>No missing migrations</span>
              </div>
              <span class="status-pill success">In sync</span>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h3>Schema Explorer</h3>
                <p>Browse tables and columns by environment.</p>
              </div>
            </div>

            <div class="schema-list">
              <span>DATABASECHANGELOG</span>
              <span>DATABASECHANGELOGLOCK</span>
              <span>CUSTOMER</span>
              <span>PRODUCT</span>
            </div>
          </article>
        </section>
      </section>
    </main>
  </div>
</template>