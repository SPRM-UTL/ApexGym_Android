# ApexGym Android - Development Plan (6 Experts × 14 Weeks)

## 📅 Cronograma General

| Fase | Semanas | Enfoque | Entregable Clave |
|------|---------|---------|------------------|
| **Fase 0: Foundation** | 1-2 | Setup, Arquitectura, CI/CD, Fixes críticos | Build verde, Hilt, NavGraph, Repos base |
| **Fase 1: Core Cliente** | 3-6 | Auth, Home, Tienda, Membresía, Perfil | App Cliente funcional end-to-end |
| **Fase 2: Core Staff** | 7-10 | Empleados, Visitas/CRM, Accesos, Caja | Módulo Staff funcional |
| **Fase 3: Polish & Launch** | 11-14 | Notificaciones Push, Offline, Tests, Release | App production-ready, Store listing |

---

## 👥 Equipo y Roles

| Experto | Rol | Especialidad | Capacidad/Semana |
|---------|-----|--------------|------------------|
| **E1** | Tech Lead / Android Senior | Arquitectura, Core, Code Review, Mentoring | 8 SP |
| **E2** | Android Senior | Features complejas, Performance, Security | 8 SP |
| **E3** | Android Mid | Features estándar, UI, Testing | 5 SP |
| **E4** | Android Mid | Features estándar, UI, Testing | 5 SP |
| **E5** | QA Engineer | Test Automation, Manual Testing, CI | 5 SP |
| **E6** | DevOps / Backend Liaison | CI/CD, Backend Integration, Infra | 5 SP |

**Capacidad Total/Semana**: ~36 SP  
**Capacidad Total 14 Semanas**: ~504 SP  
**Scope Estimado**: 653 SP → **Requiere priorización estricta (MoSCoW)**

---

## 📋 Sprint Plan Detallado

### SPRINT 0: Foundation (Semanas 1-2) - **16 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S0-01 | Fix all XML syntax errors (15+ layouts) | E1/E2 | 5 | - | `./gradlew assembleDebug` SUCCESS |
| S0-02 | Create BaseFragment (staff compilation) | E1 | 2 | S0-01 | Staff modules compile |
| S0-03 | Unify package names (`com.sprm.apexgym`) | E1 | 2 | S0-01 | Single package, no conflicts |
| S0-04 | Setup Hilt DI (App, Network, Database modules) | E1/E2 | 5 | S0-03 | `@HiltAndroidApp`, modules install |
| S0-05 | Create Navigation Graph (nav_graph.xml) | E2 | 3 | S0-04 | All destinations + actions defined |
| S0-06 | Implement Repository Interfaces (Auth, Product, Membership) | E2 | 5 | S0-04 | Interfaces + DTOs mappers |
| S0-07 | Setup CI/CD Pipeline (GitHub Actions) | E6 | 3 | - | Build + Lint + Test on PR |
| S0-08 | Configure Detekt + Ktlint + Spotless | E6 | 2 | - | Zero violations on CI |
| S0-09 | Baseline Profile Generation | E6 | 2 | S0-01 | `baseline-prof.txt` generated |
| S0-10 | Documentation: Architecture Decision Records (ADR) | E1 | 2 | S0-04 | 5 ADRs minimum |

**Sprint Goal**: Build verde, arquitectura base lista, CI pasando

---

### SPRINT 1: Auth & Onboarding (Semana 3) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S1-01 | LoginFragment + ViewModel + UseCase | E3 | 5 | S0-05, S0-06 | Login flow works, tests pass |
| S1-02 | RegisterFragment (3 pasos) + ViewModel | E3 | 8 | S1-01 | 3-step wizard, validation |
| S1-03 | Email Verification Flow | E4 | 3 | S1-02 | Code input, timer, resend |
| S1-04 | Forgot Password Flow | E4 | 5 | S1-01 | Email → token → reset |
| S1-05 | Splash Screen + Onboarding (3 pages) | E4 | 5 | S1-01 | Permissions request, skip |
| S1-06 | Session Management (CookieJar + Room) | E2 | 5 | S0-06 | Persist/restore session |
| S1-07 | Biometric/PIN Setup (Optional) | E3 | 3 | S1-06 | Crypto + Keystore |
| S1-08 | Unit Tests Auth (ViewModel, UseCase, Repository) | E5 | 2 | S1-01 | 80% coverage |

**Sprint Goal**: Usuario puede registrarse, loguearse, verificar email, recuperar password

---

### SPRINT 2: Home & Navigation Core (Semana 4) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S2-01 | HomeFragment + ViewModel (4 tabs) | E1 | 8 | S0-05 | Bottom nav, pill animation |
| S2-02 | Tab 0: Inicio - Hero, QR FAB, Popular Products | E3 | 5 | S2-01 | All sections render |
| S2-03 | Tab 1: Membresía - Estado, Historial, Planes | E3 | 5 | S2-01 | API integration |
| S2-04 | Tab 2: Tienda - Search, Chips, Grid, Pagination | E4 | 8 | S2-01 | Infinite scroll, cache |
| S2-05 | Tab 3: Profile BottomSheet | E4 | 3 | S2-01 | Edit, settings, logout |
| S2-06 | QR Access Modal (Fullscreen) | E2 | 5 | S2-02 | Timer, regenerate, close |
| S2-07 | BottomSheets: Notifications, Product/Service Detail | E3 | 5 | S2-01 | All sheets functional |
| S2-08 | Image Loading (Coil) + LruCache Migration | E2 | 3 | S2-04 | Coil integrated |
| S2-09 | UI Tests Home (Espresso) | E5 | 4 | S2-01 | Happy paths covered |

**Sprint Goal**: Home completo con 4 tabs funcionales, QR, BottomSheets

---

### SPRINT 3: Tienda, Carrito & Pagos (Semana 5) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S3-01 | Product/Service Detail BottomSheets | E3 | 3 | S2-07 | Images, qty selector, add |
| S3-02 | Carrito BottomSheet (Side Sheet) | E3 | 5 | S3-01 | Persistent, qty, delete |
| S3-03 | Checkout Flow - Pago Único | E4 | 8 | S3-02 | Method selection, confirm |
| S3-04 | Checkout Flow - Pago Mixto | E4 | 8 | S3-03 | Split payment validation |
| S3-05 | Payment Integration (Stripe/MP SDK) | E2 | 5 | S3-03 | Token generation |
| S3-06 | Purchase Confirmation + Ticket PDF | E4 | 3 | S3-04 | Download/share ticket |
| S3-07 | Historial Compras Fragment | E3 | 3 | S3-06 | Paginated, filter, detail |
| S3-08 | Unit/UI Tests Store | E5 | 1 | S3-01 | Critical paths |

**Sprint Goal**: Compra end-to-end: catálogo → carrito → pago (único/mixto) → ticket

---

### SPRINT 4: Membresía & Perfil Completo (Semana 6) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S4-01 | Membresía Detalle + Historial Planes | E3 | 3 | S2-03 | Expandable history |
| S4-02 | Cambiar Plan (Upgrade/Downgrade + Prorrateo) | E2 | 8 | S4-01 | Calc logic, API, confirm |
| S4-03 | Renovar Membresía | E3 | 5 | S4-02 | Same plan renewal |
| S4-04 | Cancelar Membresía | E3 | 3 | S4-01 | Double confirm, policy |
| S4-05 | Planes Disponibles BottomSheet | E4 | 3 | S2-03 | Compare, select |
| S4-06 | Perfil - Editar Datos + Foto | E4 | 5 | S1-01 | Image picker, upload |
| S4-07 | Perfil - Cambiar Password | E4 | 2 | S1-04 | Current + new + confirm |
| S4-08 | Config Notificaciones Completa | E3 | 5 | S2-05 | Master, 5 cats, sound, DND |
| S4-09 | Historial Accesos (Cliente) | E3 | 2 | S4-01 | List + filters |

**Sprint Goal**: Gestión completa de membresía y perfil de usuario

---

### SPRINT 5: Staff - Empleados Core (Semana 7) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S5-01 | EmpleadosListFragment + ViewModel + Adapter | E1 | 8 | S0-05 | Search, filter, stats |
| S5-02 | EmpleadoDetalleFragment | E3 | 5 | S5-01 | Info, docs, actions |
| S5-03 | CrearEditarEmpleadoFragment (Formulario) | E3 | 8 | S5-01 | Validations, photo, roles |
| S5-04 | HorarioEmpleadoFragment (Semanal/Mensual) | E2 | 8 | S5-01 | Shifts, breaks, copy week |
| S5-05 | AsistenciasEmpleadoFragment | E4 | 5 | S5-01 | History, filters, export |
| S5-06 | Staff Navigation Graph (Nested) | E1 | 2 | S0-05 | Separate nav graph |

**Sprint Goal**: CRUD Empleados + Horarios + Asistencias funcional

---

### SPRINT 6: Staff - Empleados Avanzado + Visitas (Semana 8) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S6-01 | TurnosEmpleadoFragment | E2 | 5 | S5-04 | Assign, templates, conflicts |
| S6-02 | PermisosEmpleadoFragment | E3 | 5 | S5-01 | Requests, approve/reject |
| S6-03 | NominaEmpleadoFragment | E2 | 8 | S5-01 | Calc, PDF, history |
| S6-04 | VisitasListFragment (Kanban) | E1 | 8 | S0-05 | Drag-drop, filters, stats |
| S6-05 | CrearEditarVisitaFragment | E3 | 5 | S6-04 | Form, assign advisor |
| S6-06 | VisitaDetalleFragment (Timeline) | E3 | 5 | S6-04 | Notes, attachments, history |

**Sprint Goal**: Nómina, Turnos, Permisos + CRM Visitas (Kanban + Detalle)

---

### SPRINT 7: Staff - Visitas Seguimiento + Notificaciones (Semana 9) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S7-01 | SeguimientoVisitaFragment | E3 | 8 | S6-04 | Contact types, result, schedule |
| S7-02 | Convertir Visita → Socio | E4 | 3 | S7-01 | Pre-fill registration |
| S7-03 | CRM Reports (Funnel, Forecast) | E2 | 5 | S6-04 | Charts, export |
| S7-04 | NotificacionesListFragment + ViewModel | E3 | 5 | S4-08 | Pagination, swipe actions |
| S7-05 | NotificacionDetalleFragment | E4 | 3 | S7-04 | Actions, mark read |
| S7-06 | Push Notifications (FCM Setup) | E6 | 8 | S4-08 | Token, foreground/background |
| S7-07 | Deep Links (QR, Membership, Store) | E2 | 4 | S2-01 | `apexgym://` schemes |

**Sprint Goal**: CRM completo + Push Notifications + Deep Links

---

### SPRINT 8: Staff - Accesos & Caja (Semana 10) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S8-01 | AccesoManualFragment | E1 | 8 | S0-05 | Search, validate, allow/deny |
| S8-02 | AsistenciaDetalleBottomSheet | E3 | 3 | S8-01 | Photo, check-out, profile |
| S8-03 | QR Scanner (Acceso) | E2 | 5 | S8-01 | CameraX, scan, validate |
| S8-04 | Control Capacidad (Real-time) | E2 | 5 | S8-01 | Counter, alerts |
| S8-05 | AperturaCajaFragment | E3 | 5 | S0-05 | Denominations, sign |
| S8-06 | MovimientosCajaFragment + Sheet | E3 | 5 | S8-05 | In/Out, categories, proof |
| S8-07 | CorteCajaFragment | E2 | 8 | S8-05 | Arqueo, diff, PDF report |

**Sprint Goal**: Control de accesos manual/QR + Caja completa (apertura, movimientos, corte)

---

### SPRINT 9: Caja Avanzado + Dashboard Recepción (Semana 11) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S9-01 | CierreCajaFragment | E3 | 5 | S8-07 | Final arqueo, sign, PDF |
| S9-02 | ConfiguracionCajaFragment | E4 | 3 | S8-05 | Denominations, alerts |
| S9-03 | HistorialCortesFragment | E4 | 3 | S8-07 | List, filter, reprint |
| S9-04 | ResumenAperturaFragment | E4 | 2 | S8-05 | Quick view |
| S9-05 | RecepcionDashboardFragment | E1 | 8 | S8-01, S8-05 | Real-time metrics |
| S9-06 | Offline Mode (Room Cache + Sync) | E2 | 8 | S0-06 | Queue, retry, conflict |
| S9-07 | WorkManager Background Sync | E2 | 2 | S9-06 | Periodic sync |

**Sprint Goal**: Caja completa + Dashboard recepción + Modo offline

---

### SPRINT 10: Polish, Performance, Accessibility (Semana 12) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S10-01 | Accessibility Audit & Fixes (a11y) | E5 | 8 | All | TalkBack, contrast, scaling |
| S10-02 | Performance Profiling (Startup, Scroll, Memory) | E1 | 5 | All | <2s startup, 60fps, <150MB |
| S10-03 | Baseline Profiles Integration | E6 | 3 | S0-09 | Profile installed |
| S10-04 | Animations Polish (Micro-interactions) | E3 | 5 | All | Consistent 300ms, reduced motion |
| S10-05 | Empty/Error/Loading States (All screens) | E4 | 5 | All | Illustrations, retry, messages |
| S10-06 | Tablet/Landscape Layouts (w600dp) | E3 | 5 | S2-01 | Two-pane, rail nav |
| S10-07 | Dark Mode Consistency Audit | E4 | 3 | All | No hardcoded colors |

**Sprint Goal**: App pulida, accesible, performante, responsive

---

### SPRINT 11: Testing Hardening (Semana 13) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S11-01 | Unit Tests Coverage ≥ 80% (ViewModels, UseCases, Repos) | E5 | 8 | All | Jacoco report |
| S11-02 | UI Tests Critical Paths (Auth, Home, Store, Staff) | E5 | 8 | All | Espresso/Compose |
| S11-03 | Integration Tests (API + DB) | E5 | 5 | S0-06 | MockWebServer + Room |
| S11-04 | Performance Tests (Startup, Scroll, DB) | E1 | 5 | S10-02 | Benchmarks |
| S11-05 | Security Audit (Network, Storage, Crypto) | E2 | 5 | All | No secrets, cert pinning |
| S11-06 | Monkey/Stress Testing (2h) | E5 | 3 | All | No ANR/crash |
| S11-07 | Device Farm Testing (4 devices) | E5 | 2 | All | Firebase Test Lab |

**Sprint Goal**: Quality gates passed, release candidate ready

---

### SPRINT 12: Release Preparation (Semana 14) - **36 SP**

| ID | Task | Owner | SP | Dependencias | Done Criteria |
|----|------|-------|----|--------------|---------------|
| S12-01 | Release Build Configuration (Signing, ProGuard) | E6 | 3 | S0-07 | AAB signed, optimized |
| S12-02 | Play Console Setup (Listing, Screenshots, Privacy) | E6 | 5 | S12-01 | Store listing complete |
| S12-03 | Internal Testing Track Release | E6 | 2 | S12-02 | QA + Stakeholders |
| S12-04 | Closed Testing Track (50 users) | E6 | 3 | S12-03 | Feedback collected |
| S12-05 | Production Release (Staged Rollout 10% → 100%) | E6 | 3 | S12-04 | Monitored rollout |
| S12-06 | Post-Launch Monitoring (Crashlytics, Analytics) | E6 | 2 | S12-05 | Alerts configured |
| S12-07 | Documentation Handoff (Wiki, Runbooks) | E1 | 5 | All | Confluence updated |
| S12-08 | Retrospective + Lessons Learned | All | 3 | All | Action items |
| S12-09 | Buffer / Bug Fixes | All | 5 | All | Zero critical bugs |

**Sprint Goal**: App publicada en Play Store, monitoreada, documentada

---

## 📊 Capacity Planning por Sprint

| Sprint | Semanas | E1 (TL) | E2 (Sr) | E3 (Mid) | E4 (Mid) | E5 (QA) | E6 (DevOps) | Total SP | Buffer |
|--------|---------|---------|---------|----------|----------|---------|-------------|----------|--------|
| 0 | 1-2 | 8 | 10 | 3 | 3 | 2 | 10 | 36 | 10% |
| 1 | 3 | 3 | 5 | 13 | 11 | 4 | 0 | 36 | 10% |
| 2 | 4 | 8 | 8 | 10 | 8 | 2 | 0 | 36 | 10% |
| 3 | 5 | 3 | 5 | 8 | 11 | 1 | 8 | 36 | 10% |
| 4 | 6 | 3 | 8 | 8 | 8 | 3 | 0 | 36 | 10% |
| 5 | 7 | 10 | 3 | 8 | 8 | 3 | 0 | 36 | 15% |
| 6 | 8 | 8 | 13 | 8 | 5 | 2 | 0 | 36 | 15% |
| 7 | 9 | 4 | 12 | 8 | 8 | 2 | 2 | 36 | 15% |
| 8 | 10 | 8 | 13 | 8 | 5 | 2 | 0 | 36 | 20% |
| 9 | 11 | 8 | 10 | 5 | 8 | 2 | 3 | 36 | 20% |
| 10 | 12 | 5 | 3 | 10 | 8 | 8 | 2 | 36 | 20% |
| 11 | 13 | 5 | 5 | 3 | 3 | 20 | 0 | 36 | 10% |
| 12 | 14 | 5 | 3 | 3 | 3 | 3 | 19 | 36 | 5% |

**Total Asignado**: 468 SP de 504 SP capacidad (93% utilización)
**Buffer Global**: ~36 SP para imprevistos, bugs, scope creep

---

## 🎯 Hitos (Milestones)

| Hito | Semana | Criterio de Éxito |
|------|--------|-------------------|
| **M0: Foundation Ready** | 2 | Build verde, Hilt, NavGraph, CI pasando, 0 lint errors |
| **M1: Auth Complete** | 3 | Login, Registro 3 pasos, Verificación, Password reset funcionando |
| **M2: Cliente Core** | 6 | Home 4 tabs, Tienda/Carrito/Pago, Membresía, Perfil - E2E |
| **M3: Staff Core** | 8 | Empleados CRUD, Horarios, Asistencias, Turnos, Permisos, Nómina |
| **M4: CRM Complete** | 9 | Visitas Kanban, Seguimiento, Conversión, Reportes |
| **M5: Operaciones** | 11 | Accesos, Caja completa, Dashboard, Offline mode |
| **M6: Quality Gate** | 13 | Tests ≥80%, Performance OK, Security OK, a11y OK |
| **M7: Production Release** | 14 | App en Play Store, rollout 100%, monitoring activo |

---

## ⚠️ Dependencias Críticas (Critical Path)

```
S0-01 (XML Fixes) 
    → S0-02 (BaseFragment)
        → S0-03 (Package Unify)
            → S0-04 (Hilt) → S0-05 (NavGraph) → S0-06 (Repositories)
                → S1-01 (Login) → S1-02 (Register) → S2-01 (Home)
                    → S2-02/03/04/05 (Tabs) → S3-01/02/03/04 (Store)
                        → S4-01/02/03/04 (Membresía)
                            → S5-01 (Empleados) → S5-04 (Horarios)
                                → S6-01/02/03 (Turnos/Permisos/Nómina)
                                    → S6-04 (Visitas) → S7-01 (Seguimiento)
                                        → S8-01 (Accesos) → S8-05 (Caja)
                                            → S9-05 (Dashboard) → S10 (Polish)
                                                → S11 (Tests) → S12 (Release)
```

**Riesgo**: Cualquier delay en S0-01 a S0-06 impacta TODO el plan
**Mitigación**: E1+E2 full-time en Foundation (Sprint 0)

---

## 📈 Métricas de Seguimiento (KPIs)

| Métrica | Target | Frecuencia | Herramienta |
|---------|--------|------------|-------------|
| **Velocity** | 36 SP/sprint | Semanal | GitHub Projects |
| **Build Success Rate** | 100% main branch | Por PR | GitHub Actions |
| **Test Coverage** | ≥ 80% | Semanal | Jacoco + SonarQube |
| **Critical Bugs Open** | 0 | Diario | GitHub Issues |
| **Lint/Detekt Violations** | 0 new | Por PR | CI |
| **App Startup (Cold)** | < 2.0s | Sprint 10, 12 | Macrobenchmark |
| **Frame Drops (Scroll)** | < 5% frames > 16ms | Sprint 10 | Perfetto |
| **Crash Rate (Pre-launch)** | < 0.1% | Sprint 12 | Crashlytics |
| **API Latency (p95)** | < 500ms | Continuo | Backend monitoring |

---

## 🔄 Ceremonias Ágiles

| Ceremonia | Frecuencia | Duración | Participantes | Objetivo |
|-----------|------------|----------|---------------|----------|
| **Sprint Planning** | Inicio sprint | 2h | Todo el equipo | Commit sprint goal, assign tasks |
| **Daily Standup** | Diario | 15 min | Todo el equipo | Sync, blockers, adjust |
| **Backlog Refinement** | 2x/sprint | 1h | E1, E2, PO | Groom next sprint, estimate |
| **Code Review** | Continuo | < 4h | E1/E2 + author | Quality, knowledge sharing |
| **Sprint Review** | Fin sprint | 1h | Todo + Stakeholders | Demo, feedback |
| **Retrospective** | Fin sprint | 1h | Todo el equipo | Improve process |
| **Tech Sync** | Semanal | 30 min | E1, E2, E6 | Architecture, infra, blockers |

---

## 📦 Entregables por Sprint

| Sprint | Entregable Principal | Artefactos |
|--------|---------------------|------------|
| 0 | **Foundation** | Build ok, Hilt, NavGraph, CI, ADRs, Baseline Profile |
| 1 | **Auth Module** | Login, Register, Verify, Reset - tested |
| 2 | **Home Module** | 4 tabs, QR, BottomSheets - tested |
| 3 | **Store Module** | Catalog, Cart, Checkout (Single/Mixto), Ticket - tested |
| 4 | **Membership + Profile** | Full membership mgmt, profile settings - tested |
| 5 | **Staff: Empleados Core** | List, Detail, CRUD, Horarios, Asistencias - tested |
| 6 | **Staff: Empleados Av + Visitas** | Turnos, Permisos, Nómina, CRM List/Detail - tested |
| 7 | **Staff: CRM + Push** | Seguimiento, Conversión, Reports, FCM - tested |
| 8 | **Staff: Accesos + Caja** | Manual/QR Access, Caja Aperture/Movimientos/Corte - tested |
| 9 | **Staff: Caja Complete + Dashboard** | Cierre, Config, Historial, Dashboard, Offline - tested |
| 10 | **Polish** | a11y, Performance, Responsive, Animations - audited |
| 11 | **Testing Hardening** | Unit/UI/Integration/Perf/Security tests - passing |
| 12 | **Release** | AAB signed, Play Console, Rollout, Monitoring, Docs |

---

## 🚨 Risk Mitigation por Sprint

| Sprint | Riesgo Principal | Mitigación |
|--------|------------------|------------|
| 0 | XML fixes tardan más | E1+E2 pair programming, parallelize |
| 0 | Hilt setup complejo | Usar template probado, E6 ayuda |
| 1-2 | API Backend no listo | MockWebServer, contract tests, E6 liaison |
| 3 | Pagos mixto complejos | Spike semana 2, simplificar MVP |
| 5-6 | Staff modules grandes | Dividir en sub-tareas, E1 oversight |
| 7 | FCM setup + deep links | E6 dedicated, test devices ready |
| 8-9 | Caja logic financiera | Pair con domain expert, tests exhaustivos |
| 10 | Performance regressions | Benchmarks en CI, regression detection |
| 11 | Test coverage gap | Test-first en features nuevas, E5 dedicated |
| 12 | Play Store rejection | Pre-launch checklist, internal testing |

---

## 📝 Definition of Ready (DoR) - Para tomar tarea

- [ ] Historia de usuario clara con criterios de aceptación
- [ ] Diseños UI/UX aprobados (Figma link)
- [ ] Contrato API definido y documentado
- [ ] Dependencias técnicas resueltas o identificadas
- [ ] Estimación en Story Points acordada
- [ ] Test cases definidos (QA)

## 📝 Definition of Done (DoD) - Para cerrar tarea

- [ ] Código implementado + revisado (PR approved)
- [ ] Unit tests ≥ 80% coverage (ViewModel, UseCase, Repository)
- [ ] UI tests happy path + 2 edge cases
- [ ] Lint/Detekt clean (0 new violations)
- [ ] Accesibilidad verificada (TalkBack, contraste, scaling)
- [ ] Performance OK (no leaks, 60fps scroll)
- [ ] Documentación actualizada (README, API contracts)
- [ ] Deploy a staging/device farm verificado
- [ ] PO/QA sign-off