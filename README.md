# Sentinel — Incident Management Dashboard

Sentinel is an Angular 20 frontend for real-time incident monitoring. It gives on-call engineers a live view of system health, lets them triage and resolve incidents, and surfaces AI-generated root-cause analysis for every incident reported to the backend.

It's built against the [Incident Management System](https://incident-management-system-uk2o.onrender.com) backend, a Spring Boot API that stores incidents, runs them through Gemini for automatic triage, and exposes aggregate analytics.

## Screenshots

**Dashboard overview**
<img width="395" height="701" alt="Screenshot 2026-08-09 at 12 38 29 PM" src="https://github.com/user-attachments/assets/88471914-1442-42dc-943e-44e79d889f48" />

**Live distribution & trends**
<img width="397" height="702" alt="Screenshot 2026-08-09 at 12 38 43 PM" src="https://github.com/user-attachments/assets/bfccaf49-d4fc-495f-bbcc-c90dce201539" />

**Incident detail**
<img width="398" height="702" alt="Screenshot 2026-08-09 at 12 39 19 PM" src="https://github.com/user-attachments/assets/4059a8c3-6933-450b-96f6-b9f9bf78537c" />

**AI-generated analysis**
<img width="396" height="702" alt="Screenshot 2026-08-09 at 12 39 00 PM" src="https://github.com/user-attachments/assets/e4af9d5a-bfab-46e6-b456-2837cefc2b64" />

## Features

- **Dashboard** — KPI tiles (total, open, critical/high severity, resolved), a severity donut chart, a status donut chart, a 30-day incident volume trend chart, a recent activity feed, and a "Quick incident simulation" panel.
- **Incident simulation** — Fire realistic, randomized scenarios (database failure, CPU spike, memory leak, disk full, network timeout, Kubernetes pod crash) straight at the backend to see the full pipeline run end-to-end, including AI triage.
- **Incidents list** — Paginated, filterable table of all incidents with search, severity filter, and status filter.
- **Incident detail** — Full incident record with status actions (Acknowledge, Mark resolved, Resend notification) and an **AI Analysis** panel — summary, probable root cause, recommended action, and a priority score — generated automatically by Gemini from the incident report.
- **Analytics** — Aggregate views: overview stats, severity/status breakdowns, and daily incident trends.
- **Live connection status** — A "Live monitoring" indicator in the header reflects real-time connectivity to the backend.
- **Responsive layout** — Collapsible sidebar on desktop, slide-out drawer navigation on mobile.

## Tech stack

- [Angular 20](https://angular.dev) with standalone components and signals
- Lazy-loaded, route-level code splitting (`loadComponent`)
- `HttpClient` for API access, RxJS for streams
- Custom lightweight chart components (donut chart, trend chart, bar list) — no charting library dependency
- Karma + Jasmine for unit tests

## Project structure

```
src/app/
├── core/
│   ├── demo/               # Local demo/fallback data
│   ├── models/              # TypeScript interfaces mirroring backend DTOs
│   ├── services/             # IncidentService, AnalyticsService, BackendStatusService
│   └── utils/                # Severity/text formatting helpers
├── features/
│   ├── dashboard/            # KPI tiles, charts, activity feed, simulation panel
│   ├── incidents/             # Filterable/paginated incident list
│   ├── incident-detail/        # Single incident view + AI analysis
│   └── analytics/             # Aggregate analytics views
├── layout/shell/              # App shell: sidebar nav, header, mobile drawer
└── shared/
    ├── components/            # card, kpi-tile, donut-chart, trend-chart, bar-list,
    │                          # incidents-table, pagination, severity/status badges,
    │                          # skeleton, empty-state, error-state, connection-status
    └── icon/                  # Inline SVG icon set
```

## Prerequisites

- [Node.js](https://nodejs.org/) (LTS) and npm
- [Angular CLI](https://angular.dev/tools/cli) 20.x (`npm install -g @angular/cli`) — optional, `npx ng` also works

## Getting started

Install dependencies:

```bash
npm install
```

Start the local development server:

```bash
npm start
# or
ng serve
```

Navigate to `http://localhost:4200/`. The app reloads automatically on source changes.

### Backend connectivity

The app talks to the incident management backend via `environment.apiBaseUrl`. By default this points at the hosted backend:

```
https://incident-management-system-uk2o.onrender.com
```

`proxy.conf.js` proxies `/incidents`, `/analytics`, and `/api` requests during local development while still letting direct browser navigation to routes like `/incidents/:id` fall through to Angular's own routing (see the comments in that file for why a plain path-based proxy doesn't work here).

To point at a different backend (e.g. one running locally), update `apiBaseUrl` in `src/environments/environment.development.ts` and `BACKEND` in `proxy.conf.js`.

## Building

```bash
npm run build
```

Build artifacts are written to `dist/`. The production build is optimized for performance by default.

For a development build with automatic rebuilds:

```bash
npm run watch
```

## Running tests

Unit tests run via [Karma](https://karma-runner.github.io) and Jasmine:

```bash
npm test
```

## Code scaffolding

Generate a new component with the Angular CLI:

```bash
ng generate component component-name
```

See all available schematics:

```bash
ng generate --help
```

## Additional resources

- [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli)
