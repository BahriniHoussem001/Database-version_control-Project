<template>
  <section class="content">
    <div class="page-title">
      <div>
        <h2>Settings</h2>
        <p>Environment configuration overview for the MVP.</p>
      </div>
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
  </section>
</template>