# ApexGym Android - Team Structure & Organization

## 👥 Equipo Principal (6 Expertos)

| Experto | Rol | Especialidad | Capacidad/Semana | Disponibilidad |
|---------|-----|--------------|------------------|----------------|
| **E1** | **Tech Lead / Android Senior** | Arquitectura, Core, Code Review, Mentoring, Planning | 8 SP | 100% |
| **E2** | **Android Senior** | Features complejas, Performance, Security, Payments, Offline | 8 SP | 100% |
| **E3** | **Android Mid** | UI/UX, Cliente Features, Testing, Store | 5 SP | 100% |
| **E4** | **Android Mid** | UI/UX, Staff Features, CRM, Notifications | 5 SP | 100% |
| **E5** | **QA Engineer** | Test Automation, Manual Testing, CI, Device Farm, Accessibility | 5 SP | 100% |
| **E6** | **DevOps / Backend Liaison** | CI/CD, Infra, Backend Integration, Release, Monitoring | 5 SP | 100% |

**Capacidad Total**: 36 SP/semana | 504 SP en 14 semanas

---

## 🎯 Áreas de Responsabilidad (Ownership)

### E1 - Tech Lead
```
✅ OWNERSHIP:
├── Arquitectura General (Clean Architecture, Hilt, Navigation)
├── Core Modules (Auth, Network, Database, DI)
├── Code Review Standards & Approval
├── Sprint Planning & Capacity Management
├── Technical Decisions (ADRs)
├── Mentoring E3, E4
├── Cross-team Coordination
└── Production Incident Lead

🤝 COLABORA CON:
├── E2: Arquitectura staff modules, performance
├── E3/E4: Code reviews, pair programming
├── E5: Quality gates, test strategy
├── E6: CI/CD, release process
└── PO: Scope, prioritization, technical debt
```

### E2 - Android Senior (Core & Performance)
```
✅ OWNERSHIP:
├── Payment Flows (Single, Mixto, Stripe/MP Integration)
├── Offline Mode & Sync (Room, WorkManager, Conflict Resolution)
├── Performance Optimization (Startup, Memory, Battery)
├── Security (Cert Pinning, Encryption, ProGuard)
├── Staff Complex Modules (Nómina, Horarios, Turnos)
├── Baseline Profiles & Macrobenchmark
└── Legacy Code Refactoring

🤝 COLABORA CON:
├── E1: Arquitectura, code review
├── E3: Store/Payment integration
├── E4: Staff modules integration
├── E5: Performance tests, security tests
├── E6: Backend payment APIs, monitoring
└── Backend: Payment webhooks, reconciliation
```

### E3 - Android Mid (Cliente & Store)
```
✅ OWNERSHIP:
├── Home Fragment (4 tabs, QR, BottomSheets)
├── Store Module (Catalog, Search, Filters, Grid)
├── Cart & Checkout (BottomSheet, Quantity, Totals)
├── Membership (Plans, History, Change/Renew/Cancel)
├── Profile & Settings (Edit, Notifications Config)
├── Auth Flow (Login, Register 3-steps, Verify, Reset)
└── Onboarding & Splash

🤝 COLABORA CON:
├── E1: Architecture, code review
├── E2: Payment integration, performance
├── E4: Shared components, design system
├── E5: UI tests, accessibility
├── E6: API contracts, feature flags
└── UX/UI: Figma handoff, design tokens
```

### E4 - Android Mid (Staff & CRM)
```
✅ OWNERSHIP:
├── Empleados Module (List, Detail, CRUD, Horarios, Asistencias)
├── Turnos, Permisos, Nómina (Staff advanced)
├── Visitas/CRM (Kanban, Detail, Seguimiento, Conversión)
├── Notificaciones (List, Detail, Config, FCM Push)
├── Accesos (Manual, QR Scanner, Capacidad)
├── Caja (Apertura, Movimientos, Corte, Cierre, Dashboard)
└── Staff Navigation & Permissions

🤝 COLABORA CON:
├── E1: Architecture, code review
├── E2: Complex staff logic, performance
├── E3: Shared components, design system
├── E5: UI tests, accessibility
├── E6: API contracts, backend integration
└── UX/UI: Staff workflows, dense UI
```

### E5 - QA Engineer
```
✅ OWNERSHIP:
├── Test Strategy & Pyramid Implementation
├── Unit Test Coverage (≥80%) & Quality
├── Integration Tests (MockWebServer, Room, Pact)
├── UI Tests (Espresso/Compose, Device Farm)
├── Contract Tests (Pact Consumer-Driven)
├── Accessibility Testing (TalkBack, Contrast, Scaling)
├── Performance Testing (Startup, Scroll, Memory)
├── Security Testing (OWASP, Dependency Check)
├── CI/CD Quality Gates (Coverage, Lint, Detekt)
├── Test Data Management & Test Accounts
├── Release QA Sign-off Process
├── Bug Triage & Regression Testing
└── Exploratory Testing Sessions

🤝 COLABORA CON:
├── E1: Quality gates, definition of done
├── E2: Performance benchmarks, security audit
├── E3/E4: Feature testing, testability reviews
├── E6: CI/CD pipeline, device farm, monitoring
├── PO: Acceptance criteria, UAT coordination
└── Support: Production bug reproduction
```

### E6 - DevOps / Backend Liaison
```
✅ OWNERSHIP:
├── CI/CD Pipeline (GitHub Actions, Gradle, Signing)
├── Build Variants & Flavors (Dev, Staging, Prod)
├── Google Play Console Management (Tracks, Rollouts)
├── Release Process & Automation
├── Monitoring & Alerting (Crashlytics, Vitals, Performance)
├── Backend API Integration & Contract Testing
├── Feature Flags & Remote Config (Firebase)
├── Infrastructure (Secrets, Keystore, Certificates)
├── Dependency Management (Version Catalog, Updates)
├── Device Farm (Firebase Test Lab, Matrix)
├── Documentation (Runbooks, ADRs, Handoff)
└── Security Scanning (Dependency Check, SAST)

🤝 COLABORA CON:
├── E1: Release process, architecture decisions
├── E2: Performance monitoring, backend contracts
├── E3/E4: API contracts, feature flags, deploy
├── E5: CI/CD quality gates, device farm, test data
├── Backend Team: API design, contracts, deploy coordination
├── PO: Release planning, stakeholder communication
└── Support: Production incidents, rollback
```

---

## 🔄 Ceremonias & Rituales

### Sprint Ceremonies (2 semanas por sprint)

| Ceremonia | Frecuencia | Duración | Participantes | Facilitador |
|-----------|------------|----------|---------------|-------------|
| **Sprint Planning** | Inicio sprint | 2h | Todo el equipo | E1 (Tech Lead) |
| **Daily Standup** | Diario | 15 min | Todo el equipo | Rotativo |
| **Backlog Refinement** | 2x/sprint | 1h | E1, E2, PO | E1 |
| **Code Review** | Continuo | < 4h/PR | E1/E2 + Author | - |
| **Sprint Review** | Fin sprint | 1h | Todo + Stakeholders | E1 + PO |
| **Retrospective** | Fin sprint | 1h | Todo el equipo | Rotativo (Scrum Master) |
| **Tech Sync** | Semanal | 30 min | E1, E2, E6 | E1 |

### Reuniones Adicionales

| Reunión | Frecuencia | Duración | Participantes | Propósito |
|---------|------------|----------|---------------|-----------|
| **Architecture Review** | Quincenal | 1h | E1, E2, E6 | Decisiones técnicas grandes |
| **Design Handoff** | Por feature | 30 min | E3/E4, UX/UI | Figma → Code alignment |
| **API Contract Sync** | Semanal | 30 min | E2/E4, E6, Backend | Contract alignment |
| **Release Readiness** | Pre-release | 1h | E1, E5, E6, PO | Go/No-go decision |
| **Incident Retrospective** | Post-incidente | 1h | Involved + E1 | Root cause, action items |

---

## 📋 RACI Matrix por Área

| Actividad | E1 (TL) | E2 (Sr) | E3 (Mid) | E4 (Mid) | E5 (QA) | E6 (DevOps) | PO |
|-----------|---------|---------|----------|----------|---------|-------------|-----|
| **Arquitectura** | R/A | C | I | I | I | C | I |
| **Sprint Planning** | R/A | C | C | C | C | C | R |
| **Code Review** | A | R | R | R | C | C | I |
| **Unit Tests** | A | R | R | R | R | I | I |
| **Integration Tests** | A | R | R | R | R | C | I |
| **UI Tests** | A | C | R | R | R | C | I |
| **Accessibility** | A | C | R | R | R | I | I |
| **Performance** | A | R | C | C | R | R | I |
| **Security** | A | R | C | C | R | R | I |
| **CI/CD Pipeline** | C | C | I | I | C | R/A | I |
| **Release Process** | A | C | I | I | R | R | A |
| **Backend Integration** | C | R | C | R | I | R | I |
| **Feature Flags** | C | C | C | C | I | R | A |
| **Documentation** | R | C | R | R | R | R | A |
| **Mentoring** | R | R | - | - | - | - | - |
| **Stakeholder Communication** | R | I | I | I | I | C | R/A |

**R** = Responsible (Hace el trabajo)  
**A** = Accountable (Decide y aprueba)  
**C** = Consulted (Opina antes)  
**I** = Informed (Enterado después)

---

## 🛠️ Herramientas de Colaboración

### Comunicación
| Herramienta | Uso | Canales Principales |
|-------------|-----|---------------------|
| **Slack** | Comunicación diaria | #apexgym-dev, #apexgym-releases, #apexgym-alerts, #apexgym-random |
| **Zoom/Meet** | Ceremonias, Pair Programming | Sprint meetings, 1:1s |
| **Loom** | Async demos, code walkthroughs | PR reviews complejos |

### Gestión de Trabajo
| Herramienta | Uso |
|-------------|-----|
| **GitHub Projects** | Sprint board, Issues, Milestones |
| **GitHub Issues** | Bugs, Tasks, Epics |
| **GitHub Discussions** | RFCs, ADRs, Technical decisions |
| **Confluence/Notion** | Documentation, Runbooks, Wiki |

### Código & CI/CD
| Herramienta | Uso |
|-------------|-----|
| **GitHub** | Source control, PRs, Actions |
| **Gradle** | Build system |
| **Hilt** | Dependency Injection |
| **Firebase** | Crashlytics, Analytics, Test Lab, Remote Config |
| **Play Console** | App distribution, Vitals |
| **Codecov** | Coverage reporting |
| **SonarQube** | Code quality (opcional) |

### Diseño & Especificaciones
| Herramienta | Uso |
|-------------|-----|
| **Figma** | UI/UX Design, Design System, Handoff |
| **PlantUML** | Architecture diagrams, Flow diagrams |
| **Postman/Swagger** | API Documentation, Testing |

---

## 📈 Career Growth & Skill Development

### Competencias por Rol

#### Tech Lead (E1)
| Técnicas | Blandas | Objetivo 6 meses |
|----------|---------|------------------|
| Clean Architecture, DDD, Modularization | Leadership, Coaching, Decision Making | Mentorear 2 mids a Senior |
| Hilt, Navigation, Compose (futuro) | Communication, Conflict Resolution | Liderar migración a Compose |
| Performance, Security, Observability | Strategic Planning, Stakeholder Mgmt | Definir tech roadmap 2025 |

#### Android Senior (E2)
| Técnicas | Blandas | Objetivo 6 meses |
|----------|---------|------------------|
| Payments, Offline, Sync, Security | Technical Mentoring, Code Review | Liderar módulo pagos end-to-end |
| Macrobenchmark, Baseline Profiles | Knowledge Sharing, Documentation | Workshop performance mensual |
| Advanced Kotlin (Coroutines, Flow, KSP) | Cross-team Collaboration | Contribuir a librerías internas |

#### Android Mid (E3, E4)
| Técnicas | Blandas | Objetivo 6 meses |
|----------|---------|------------------|
| MVVM, ViewBinding, Coroutines, Flow | Communication, Estimation | Autonomous feature delivery |
| Material3, Motion, Animations | Pair Programming, Code Review | Mentorear junior (futuro) |
| Testing (Unit, UI, Contract) | Time Management, Prioritization | ≥ 85% coverage en features own |

#### QA Engineer (E5)
| Técnicas | Blandas | Objetivo 6 meses |
|----------|---------|------------------|
| Test Automation (Espresso, Compose, Detox) | Advocacy, Bug Reporting | Zero critical bugs en producción |
| CI/CD Quality Gates, Contract Testing | Collaboration, Teaching | Quality workshop mensual |
| Accessibility, Performance, Security | Process Improvement | a11y certification |

#### DevOps (E6)
| Técnicas | Blandas | Objetivo 6 meses |
|----------|---------|------------------|
| GitHub Actions, Gradle, Fastlane | Communication, Incident Response | Zero-downtime deployments |
| GCP/Firebase, Play Console API | Documentation, Runbooks | Full automation release |
| Monitoring, Alerting, SLOs | Cross-team Enablement | Self-service infra for devs |

---

## 🎓 Onboarding Plan (Nuevos Miembros)

### Semana 1: Foundation
- [ ] Setup entorno (Android Studio, JDK 17, Gradle, Git)
- [ ] Clone repo, build & run dev variant
- [ ] Arquitectura walkthrough (E1)
- [ ] Codebase tour: Core, Features, Tests
- [ ] CI/CD pipeline walkthrough (E6)
- [ ] Testing strategy & tools (E5)
- [ ] Design system & Figma (E3/E4)
- [ ] Pair programming con buddy asignado

### Semana 2: First Contribution
- [ ] Asignar issue "Good First Issue" (bug pequeño o test)
- [ ] PR completo: code + tests + docs
- [ ] Code review experience (both sides)
- [ ] Deploy a staging via CI/CD
- [ ] Manual testing en device farm

### Mes 1: Autonomous Delivery
- [ ] Feature pequeña end-to-end (3-5 SP)
- [ ] Code review de peers
- [ ] Participar en todas las ceremonias
- [ ] Contribuir a documentación
- [ ] 1:1 con Tech Lead (feedback)

---

## 💰 Budget & Resource Planning

### Costos de Equipo (Mensual Estimado)

| Rol | Salario Base | Beneficios | Total Mensual | Anual |
|-----|--------------|------------|---------------|-------|
| Tech Lead (E1) | $4,500 | $900 | $5,400 | $64,800 |
| Android Senior (E2) | $4,000 | $800 | $4,800 | $57,600 |
| Android Mid (E3) | $3,000 | $600 | $3,600 | $43,200 |
| Android Mid (E4) | $3,000 | $600 | $3,600 | $43,200 |
| QA Engineer (E5) | $3,200 | $640 | $3,840 | $46,080 |
| DevOps (E6) | $3,500 | $700 | $4,200 | $50,400 |
| **TOTAL** | **$21,200** | **$4,240** | **$25,440** | **$305,280** |

> **Nota**: Valores referencia mercado LatAm/Remote USD. Ajustar según ubicación real.

### Budget Infraestructura (Mensual)

| Concepto | Costo | Anual |
|----------|-------|-------|
| Firebase Blaze (Crashlytics, Test Lab, Analytics) | $100 | $1,200 |
| GitHub Actions (minutos build) | $50 | $600 |
| Codecov/SonarQube | $20 | $240 |
| Slack Pro (6 usuarios) | $48 | $576 |
| PagerDuty (opcional, 2 users) | $50 | $600 |
| Figma Professional (3 editors) | $45 | $540 |
| **TOTAL INFRA** | **$313** | **$3,756** |

### Budget Total Proyecto (14 semanas ≈ 3.5 meses)

| Categoría | Costo |
|-----------|-------|
| Equipo (3.5 meses) | ~$89,000 |
| Infraestructura (3.5 meses) | ~$1,100 |
| Contingencia (10%) | ~$9,000 |
| **TOTAL PROYECTO** | **~$99,100** |

---

## 📊 Métricas de Salud del Equipo

### Weekly Health Check (Retro Breve)

| Métrica | Target | Herramienta |
|---------|--------|-------------|
| **Sprint Velocity** | 36 SP ± 10% | GitHub Projects |
| **PR Cycle Time** | < 24h | GitHub Insights |
| **Code Review Time** | < 4h | GitHub Insights |
| **Build Success Rate** | > 95% | GitHub Actions |
| **Test Pass Rate** | 100% main branch | CI Dashboard |
| **Critical Bugs Open** | 0 | GitHub Issues |
| **Team Happiness** | > 4/5 | Anonymous Survey (Sprint) |
| **Knowledge Sharing** | 1 session/sprint | Calendar |

### Quarterly Reviews

| Revisión | Frecuencia | Participantes | Output |
|----------|------------|---------------|--------|
| **Performance Review** | Trimestral | E1 + Cada miembro | Goals, Feedback, Growth Plan |
| **Tech Radar Update** | Trimestral | E1, E2, E6 | Adopt/Trial/Assess/Hold |
| **Process Retrospective** | Trimestral | Todo el equipo | Process Improvements |
| **Architecture Review** | Semestral | E1, E2, E6, Backend | Strategic Decisions |

---

## 🚨 Escalation & Conflict Resolution

### Escalation Path

```
Individual Contributor (E3/E4/E5)
    ↓ (Bloqueo técnico > 4h, decisión arquitectura)
Tech Lead (E1)
    ↓ (Impacto timeline > 2 días, scope change)
Product Owner
    ↓ (Budget, timeline, strategic)
Steering Committee / Stakeholders
```

### Conflict Resolution Framework

1. **Datos sobre opiniones**: Métricas, benchmarks, user research
2. **Time-boxed discussion**: 30 min máximo, luego decide Owner (RACI)
3. **Disagree and Commit**: Si no hay consenso, A (Accountable) decide
4. **Documentar**: ADR para decisiones técnicas relevantes
5. **Revisar**: En retrospectiva, evaluar outcome

---

## 📅 Calendar & Time Management

### Core Hours (Overlap Obligatorio)

| Zona Horaria | Core Hours | Disponibilidad |
|--------------|------------|----------------|
| **ART (UTC-3)** | 10:00 - 16:00 | Todo el equipo |
| **EST (UTC-5)** | 08:00 - 14:00 | Si aplica remoto |
| **CET (UTC+1)** | 14:00 - 20:00 | Si aplica remoto |

### Time Blocking Recomendado

| Bloque | Actividad | Duración |
|--------|-----------|----------|
| **Deep Work** | 09:00-12:00 | Feature development, code review |
| **Ceremonies** | 10:00-11:30 | Standup, Refinement, Planning |
| **Collaboration** | 14:00-16:00 | Pair programming, Syncs, PR reviews |
| **Admin/Learning** | 16:00-17:00 | Docs, Learning, 1:1s |

### No-Meeting Days

- **Miércoles**: No meetings (excepto Daily Standup 15 min)
- **Viernes PM**: No meetings después de 14:00 (Wrap up week)

---

## 🎉 Team Culture & Values

### Valores del Equipo

| Valor | Comportamiento Esperado |
|-------|------------------------|
| **Calidad sobre Velocidad** | No mergear sin tests, code review, lint clean |
| **Ownership End-to-End** | Desde diseño hasta producción + monitoring |
| **Aprendizaje Continuo** | 10% tiempo para learning, sharing, experiments |
| **Transparencia Radical** | Issues públicos, decisions documentadas, metrics visibles |
| **Empatía Usuario** | Dogfooding semanal, user feedback sessions |
| **Mejora Incremental** | Boy Scout Rule: dejar código mejor de como se encontró |

### Rituales Culturales

| Ritual | Frecuencia | Descripción |
|--------|------------|-------------|
| **Friday Demo** | Semanal | 15 min: cada uno muestra lo que hizo |
| **Learning Friday** | Mensual | 2h: workshop, tech talk, course |
| **Bug Bash** | Por release | 2h: todo el equipo caza bugs en staging |
| **Architecture Kata** | Quincenal | 1h: diseño solución a problema imaginario |
| **Retro Action Items** | Sprint | Tracking visible de mejoras comprometidas |

---

## 📞 Contactos Clave Externos

| Rol | Nombre | Organización | Contacto | Responsabilidad |
|-----|--------|--------------|----------|-----------------|
| **Product Owner** | [Nombre] | ApexGym | po@apexgym.com | Prioridades, Scope, Acceptance |
| **Backend Lead** | [Nombre] | ApexGym Backend | backend@apexgym.com | API Contracts, Deploy Coord |
| **UX/UI Designer** | [Nombre] | Design Team | design@apexgym.com | Figma, Design System, Handoff |
| **DevOps Platform** | [Nombre] | Infra Team | platform@apexgym.com | K8s, Secrets, DNS, SSL |
| **Security Officer** | [Nombre] | Security | security@apexgym.com | Audits, Compliance, Incidents |
| **Data Analyst** | [Nombre] | Analytics | data@apexgym.com | Events, Dashboards, Insights |

---

## 📝 Team Charter - Acuerdo de Trabajo

> **Nos comprometemos a:**
> 1. **Entregar valor incremental** cada sprint con calidad
> 2. **Comunicar bloqueos temprano** (Daily Standup + Slack)
> 3. **Revisar código de pares** con respeto y rigor (< 4h)
> 4. **Escribir tests** para toda lógica nueva (TDD cuando posible)
> 5. **Documentar decisiones** (ADRs) y conocimiento (Wiki)
> 6. **Mejorar continuamente** (Retrospectivas + Action Items)
> 7. **Cuidar la salud del equipo** (No meetings Wed, Core Hours, PTO respect)
> 8. **Celebrar logros** (Demos, Releases, Learning)

**Firmado por el equipo**: _________________ **Fecha**: _________________