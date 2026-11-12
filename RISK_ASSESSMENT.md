# ApexGym Android - Risk Assessment & Mitigation

## 🎯 Resumen Ejecutivo de Riesgos

| Nivel | Cantidad | Descripción |
|-------|----------|-------------|
| **Crítico** | 4 | Bloquean el proyecto si no se resuelven ya |
| **Alto** | 8 | Impactan timeline/costo significativamente |
| **Medio** | 12 | Requieren atención proactiva |
| **Bajo** | 6 | Monitoreo pasivo |

**Total Riesgos Identificados**: 30

---

## 🔴 RIESGOS CRÍTICOS (Requieren Acción Inmediata)

### RISK-001: Errores XML en Build (BLOQUEADOR ACTUAL)
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 100% (Ya ocurre) |
| **Impacto** | Build falla, no se puede testear ni desplegar |
| **Causa Raíz** | Generación de código automática omitió `</LinearLayout>` en 15+ layouts |
| **Detección** | `./gradlew assembleDebug` falla secuencialmente |
| **Mitigación Inmediata** | Fix manual de cada archivo (patrón identificado: LinearLayout interno de MaterialCardView) |
| **Prevención Futura** | Lint check custom + CI validation + Code review obligatorio en XML |
| **Owner** | E1 + E2 | 
| **Timeline** | **Semana 1 - Día 1-2** |
| **Contingencia** | Si >20 archivos: script automatizado de fix |

### RISK-002: BaseFragment No Existe (Compilation Error Staff)
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 100% (Referenciado en 8+ fragments staff) |
| **Impacto** | Módulo staff no compila, bloquea Sprints 5-9 |
| **Causa Raíz** | Arquitectura planificada pero no implementada |
| **Mitigación** | Crear `BaseFragment` abstracto con ViewBinding, lifecycle, error handling base |
| **Owner** | E1 |
| **Timeline** | **Semana 1 - Día 3** |
| **Esfuerzo** | 2 SP |

### RISK-003: Inconsistencia Package Names (`com.sprm.apexgym` vs `com.apexgym`)
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 100% (Ya existe) |
| **Impacto** | Hilt/DI falla, Navigation ambiguo, Build variants rotos |
| **Causa Raíz** | Módulos cliente/staff desarrollados por equipos separados |
| **Mitigación** | Refactor global a `com.sprm.apexgym` (paquete principal cliente) |
| **Riesgo Refactor** | Romper imports, Hilt bindings, Navigation deep links |
| **Estrategia** | 1. Backup branch 2. Refactor automatizado (Android Studio) 3. Full rebuild 4. Tests |
| **Owner** | E1 |
| **Timeline** | **Semana 1 - Día 4-5** |
| **Esfuerzo** | 2 SP |

### RISK-004: Backend API No Disponible / Cambios Sin Aviso
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 70% (Equipo backend separado, timeline incierto) |
| **Impacto** | Bloquea integración real, tests de integración, UAT |
| **Causa Raíz** | Dependencia externa, falta de contrato versionado |
| **Mitigación** | 1. MockWebServer para todos los endpoints 2. Contract tests (Pact) 3. E6 como liaison dedicado 4. API versioning en headers |
| **Owner** | E6 (Liaison) + E5 (Contract Tests) |
| **Timeline** | **Continuo desde Semana 1** |
| **Esfuerzo** | 8 SP (setup inicial) + 2 SP/sprint maintenance |

---

## 🟠 RIESGOS ALTOS

### RISK-005: Complejidad Pago Mixto (Split Payments)
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 60% |
| **Impacto** | Sprint 3 overflow, UX confusa, edge cases financieros |
| **Complejidad** | Validación suma = total, múltiples tokens, rollback parcial, conciliación |
| **Mitigación** | 1. Spike técnico Semana 2 (2 SP) 2. Simplificar MVP: solo 2 métodos 3. Delegar lógica compleja a backend 4. Tests exhaustivos edge cases |
| **Owner** | E2 (Lead) + E4 |
| **Timeline** | **Semana 2 (Spike) → Semana 5 (Implementación)** |
| **Plan B** | Solo Pago Único en MVP, Mixto en v1.1 |

### RISK-006: Módulos Staff Subestimados (Empleados, Visitas, Caja)
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 80% |
| **Impacto** | Sprints 5-9 overflow, calidad reducida, technical debt |
| **Evidencia** | 8 fragments empleados + 4 visitas + 9 caja = 21 fragments solo staff |
| **Mitigación** | 1. Priorización MoSCoW estricta 2. E1+E2 pair programming en staff 3. Componentes reutilizables (BaseListFragment, BaseFormFragment) 4. Scope fijo por sprint, no scope creep |
| **Owner** | E1 (Planning) |
| **Timeline** | **Revisión cada Sprint Planning** |

### RISK-007: FCM Push Notifications + Deep Links
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 50% |
| **Impacto** | Notificaciones no llegan, deep links rotos, mala UX |
| **Complejidad** | Foreground/background handling, token refresh, payload parsing, navigation state restoration |
| **Mitigación** | 1. E6 dedicated Semana 9 2. Test devices físicos (no emulador) 3. Firebase Console testing 4. Fallback local notifications |
| **Owner** | E6 |
| **Timeline** | **Semana 9 (3 SP spike + 5 SP implementación)** |

### RISK-008: Modo Offline + Sync Background
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 60% |
| **Impacto** | Data loss, conflictos sync, UX inconsistente |
| **Complejidad** | Room cache, mutation queue, conflict resolution, WorkManager scheduling, network awareness |
| **Mitigación** | 1. Scope reducido: solo cache lectura (GET) en MVP 2. Mutations offline → v1.1 3. WorkManager simple periodic sync 4. Clear UI indicators (offline badge) |
| **Owner** | E2 |
| **Timeline** | **Semana 11 (8 SP)** |

### RISK-009: Performance - App Startup > 2s / Scroll Jank
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 40% |
| **Impacto** | Malas reviews, uninstall rate, Play Store ranking |
| **Causas** | Hilt initialization, Room DB creation, Network calls en startup, Heavy HomeFragment |
| **Mitigación** | 1. Baseline Profiles (Sprint 0) 2. Lazy initialization (Hilt EntryPoints) 3. Startup profiling (Macrobenchmark) 4. HomeFragment: defer non-critical loads 5. Paging 3 para listas grandes |
| **Owner** | E1 + E6 |
| **Timeline** | **Sprint 10 (5 SP) + Continuo** |

### RISK-010: Accesibilidad (a11y) - No Cumple Estándares
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 50% |
| **Impacto** | Exclusión usuarios, legal compliance, Play Store removal risk |
| **Gaps Actuales** | Sin content descriptions, contrast hardcoded, touch targets < 48dp, no TalkBack testing |
| **Mitigación** | 1. Sprint 10 dedicado (8 SP) 2. Checklist obligatorio en DoD 3. Automated testing (Accessibility Scanner) 4. Manual TalkBack testing en device farm |
| **Owner** | E5 (QA Lead) |
| **Timeline** | **Sprint 10** |

### RISK-011: Seguridad - Almacenamiento Tokens / Cert Pinning
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 30% |
| **Impacto** | Data breach, GDPR fines, reputational damage |
| **Vulnerabilidades** | Tokens en SharedPrefs (no EncryptedSharedPreferences), cleartext HTTP (dev), no certificate pinning |
| **Mitigación** | 1. EncryptedSharedPreferences para tokens 2. Network Security Config (prod: HTTPS only) 3. Certificate Pinning (OkHttp) 4. ProGuard/R8 ofuscación 5. Security audit Sprint 11 |
| **Owner** | E2 |
| **Timeline** | **Sprint 0 (Config) + Sprint 11 (Audit)** |

### RISK-012: Certificate Pinning / Network Security Config
| Aspecto | Detalle |
|---------|---------|
| **Probabilidad** | 40% |
| **Impacto** | MITM attacks, data interception |
| **Estado Actual** | `cleartextTrafficPermitted="true"` para IP local |
| **Mitigación** | Config separada debug vs release, pinning en release, rotation strategy |
| **Owner** | E6 |
| **Timeline** | **Sprint 12 (Pre-release)** |

---

## 🟡 RIESGOS MEDIOS

### RISK-013: Scope Creep - Stakeholders Piden Features Extra
| Probabilidad | 70% | Impacto | Timeline延期, team burnout |
| Mitigación | Change control process, PO gatekeeper, MoSCoW rígido, buffer 10% |

### RISK-014: Team Capacity - Enfermedades / Rotación
| Probabilidad | 30% | Impacto | Velocity drop 20-30% |
| Mitigación | Cross-training (E3/E4 intercambiables), documentation, pair programming, buffer sprints |

### RISK-015: Device Fragmentation - Bugs en Dispositivos Específicos
| Probabilidad | 50% | Impacto | Crashes en Samsung/Xiaomi/Huawei |
| Mitigación | Device farm (Firebase Test Lab) 4 dispositivos mínimo, Samsung Remote Test Lab |

### RISK-016: Android Version Compatibility (API 28-35)
| Probabilidad | 40% | Impacto | Behavior differences, deprecated APIs |
| Mitigación | CompileSdk 35, minSdk 28, test matrix API 28/30/33/35, Jetpack libraries |

### RISK-017: Memory Leaks - Fragments + Coroutines + Image Loading
| Probabilidad | 50% | Impacto | OOM crashes, ANR |
| Mitigación | LeakCanary en debug, lifecycleScope obligatorio, Coil lifecycle-aware, profile memory |

### RISK-018: Database Migration - Room Schema Changes
| Probabilidad | 30% | Impacto | Crashes en update, data loss |
| Mitigación | Export schema, test migrations, fallbackToDestructiveMigration solo dev |

### RISK-019: Third-party SDK Issues (Stripe, MP, Coil, Firebase)
| Probabilidad | 30% | Impacto | Build breaks, runtime crashes, version conflicts |
| Mitigación | Pin versions, lock files, test updates en branch separado, alternatives ready |

### RISK-020: Gradle / AGP / Kotlin Version Conflicts
| Probabilidad | 20% | Impacto | Build breaks, plugin incompatibilities |
| Mitigación | Version catalog (libs.versions.toml), AGP 9.2.1 stable, Kotlin 1.9.22 |

### RISK-021: Play Store Policy Changes / Rejection
| Probabilidad | 25% | Impacto | Launch delay, appeal process |
| Mitigación | Pre-launch checklist, Privacy Policy, Data Safety Form, Target API 35, 64-bit |

### RISK-022: Legal/Compliance - GDPR, LGPD, Data Retention
| Probabilidad | 20% | Impacto | Fines, legal issues |
| Mitigación | Data deletion feature, consent management, DPO consultation, audit trail |

### RISK-023: CI/CD Pipeline Failures / Flaky Tests
| Probabilidad | 40% | Impacto | Delayed merges, false confidence |
| Mitigación | Test quarantine, retry logic, dedicated CI maintenance (E6), fast feedback |

### RISK-024: Documentation Debt - Handoff Incompleto
| Probabilidad | 50% | Impacto | Onboarding lento, bus factor |
| Mitigación | Documentation as Code, ADRs, runbooks, Sprint 12 dedicated (5 SP) |

---

## 🟢 RIESGOS BAJOS

### RISK-025: Design System Inconsistencies
| Mitigación | Design tokens, Figma sync, component library, code review checklist |

### RISK-026: Internationalization (i18n) Not Ready
| Mitigación | String resources externalizados, pseudolocalization testing, v1.1 scope |

### RISK-027: Analytics / Crashlytics Not Configured
| Mitigación | Sprint 10 setup, custom events defined, dashboard ready |

### RISK-028: App Bundle / Dynamic Delivery Issues
| Mitigación | Test bundletool, Play Internal Testing, split APKs verification |

### RISK-029: Battery / Network Optimization (Doze, App Standby)
| Mitigación | WorkManager constraints, FCM high priority, foreground service para sync crítico |

### RISK-030: Competitor Feature Parity Pressure
| Mitigación | Focus on core value, differentiable UX, iterative releases |

---

## 📊 Matriz de Riesgo (Probabilidad × Impacto)

```
IMPACTO
  ↑
  │  CRÍTICO  │ R001  R002  R003  R004  │
  │           │ R005  R006  R007  R008  │  ALTO
  │           │ R009  R010  R011  R012  │
  ├───────────┼─────────────────────────┤
  │           │ R013  R014  R015  R016  │  MEDIO
  │           │ R017  R018  R019  R020  │
  │           │ R021  R022  R023  R024  │
  ├───────────┼─────────────────────────┤
  │           │ R025  R026  R027  R028  │  BAJO
  │           │ R029  R030              │
  └───────────┴─────────────────────────┘
    BAJA      MEDIA      ALTA      CRÍTICA
                    PROBABILIDAD
```

---

## 🛡️ Plan de Mitigación por Prioridad

### Inmediato (Semana 1)
| Acción | Riesgos | Owner | Esfuerzo |
|--------|---------|-------|----------|
| Fix XML errors (script + manual) | R001 | E1+E2 | 5 SP |
| Crear BaseFragment | R002 | E1 | 2 SP |
| Unificar package names | R003 | E1 | 2 SP |
| Setup Hilt + NavGraph + Repos | R004, R006 | E1+E2 | 13 SP |
| Configurar MockWebServer + Contract Tests | R004 | E5+E6 | 8 SP |
| Network Security Config (debug/release) | R011, R012 | E6 | 2 SP |

### Corto Plazo (Sprints 1-4)
| Acción | Riesgos | Owner |
|--------|---------|-------|
| Spike Pago Mixto | R005 | E2 |
| Componentes base Staff (BaseList, BaseForm) | R006 | E1 |
| Baseline Profiles generation | R009 | E6 |
| EncryptedSharedPreferences | R011 | E2 |
| LeakCanary + Memory profiling | R017 | E1 |

### Mediano Plazo (Sprints 5-9)
| Acción | Riesgos | Owner |
|--------|---------|-------|
| FCM + Deep Links implementation | R007 | E6 |
| Offline mode (read-only cache) | R008 | E2 |
| Device farm testing setup | R015 | E5 |
| Accessibility audit preparation | R010 | E5 |

### Largo Plazo (Sprints 10-14)
| Acción | Riesgos | Owner |
|--------|---------|-------|
| Full a11y compliance | R010 | E5 |
| Performance optimization | R009 | E1+E6 |
| Security audit completo | R011, R012 | E2 |
| Play Store pre-launch checklist | R021 | E6 |
| Documentation handoff | R024 | E1 |

---

## 📋 Risk Register - Tracking Template

| ID | Riesgo | Prob | Impact | Score | Estado | Owner | Mitigación | Próxima Revisión |
|----|--------|------|--------|-------|--------|-------|------------|------------------|
| R001 | XML Build Errors | 100% | Crítico | 100 | 🔴 ACTIVO | E1+E2 | Fix manual + script | Daily standup |
| R002 | BaseFragment missing | 100% | Crítico | 100 | 🔴 ACTIVO | E1 | Create abstract base | Día 3 Semana 1 |
| R003 | Package mismatch | 100% | Crítico | 100 | 🔴 ACTIVO | E1 | Refactor global | Día 4-5 Semana 1 |
| R004 | Backend unavailable | 70% | Crítico | 70 | 🟡 MONITOREO | E6 | MockServer + Contract tests | Weekly |
| R005 | Pago Mixto complexity | 60% | Alto | 48 | 🟡 PLANIFICADO | E2 | Spike Semana 2 | Sprint 2 Planning |
| R006 | Staff modules scope | 80% | Alto | 64 | 🟡 MONITOREO | E1 | MoSCoW + base components | Cada Sprint Review |
| R007 | FCM + Deep Links | 50% | Alto | 40 | 🟢 PLANIFICADO | E6 | Dedicated Sprint 9 | Sprint 8 Planning |
| R008 | Offline mode | 60% | Alto | 48 | 🟢 SCOPE REDUCIDO | E2 | Read-only cache MVP | Sprint 10 Planning |
| R009 | Performance | 40% | Alto | 32 | 🟢 MONITOREO | E1 | Baseline profiles + profiling | Sprint 10 |
| R010 | Accessibility | 50% | Alto | 40 | 🟢 PLANIFICADO | E5 | Sprint 10 dedicated | Sprint 10 |

**Score = Probabilidad × Impacto (1-5 scale)**
- 80-100: Crítico - Acción inmediata
- 40-79: Alto - Planificación activa
- 20-39: Medio - Monitoreo
- <20: Bajo - Conciencia pasiva

---

## 🚨 Escalation Path

```
Desarrollador (E3/E4) 
    → Tech Lead (E1) [Bloqueo técnico < 4h]
        → Product Owner [Scope/Priority decision]
            → Stakeholders [Timeline/Budget impact]
                → Steering Committee [Project continuity]
```

**SLA de Respuesta:**
- Crítico: 2 horas
- Alto: 4 horas  
- Medio: 24 horas
- Bajo: Próxima ceremonia

---

## 📈 Risk Burn-down Target

| Semana | Riesgos Críticos Abiertos | Riesgos Altos Abiertos |
|--------|---------------------------|------------------------|
| 1 | 4 → 0 | 8 → 6 |
| 2 | 0 | 6 → 5 |
| 3 | 0 | 5 → 4 |
| 4 | 0 | 4 → 3 |
| 5 | 0 | 3 → 3 |
| 6 | 0 | 3 → 2 |
| 7 | 0 | 2 → 2 |
| 8 | 0 | 2 → 1 |
| 9 | 0 | 1 → 1 |
| 10 | 0 | 1 → 0 |
| 11 | 0 | 0 |
| 12 | 0 | 0 |
| 13 | 0 | 0 |
| 14 | 0 | 0 |

**Meta**: Cero riesgos críticos/altos abiertos al Sprint 10 (Quality Gate)