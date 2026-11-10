# ApexGym Android - Project Overview

## 📋 Executive Summary

**ApexGym** es una aplicación móvil Android nativa para la gestión integral de gimnasios, diseñada con arquitectura **dual-role** (Cliente + Staff/Recepción). La app permite a los socios gestionar su membresía, acceder al gimnasio vía QR, comprar productos/servicios y recibir notificaciones; mientras que el personal administrativo gestiona empleados, visitas/prospectos (CRM), caja, nóminas y control de accesos.

---

## 🎯 Objetivos del Proyecto

| Objetivo | Descripción | Prioridad |
|----------|-------------|-----------|
| **App Cliente Funcional** | Login/Registro, Home (4 tabs), QR Access, Tienda, Membresía, Perfil | 🔴 Crítica |
| **Módulo Staff Completo** | Empleados (7 sub-módulos), Visitas/CRM (3), Caja (3), Accesos | 🔴 Crítica |
| **Notificaciones Push** | Configuración granular, categorías, horario silencioso, tonos | 🟡 Alta |
| **Arquitectura Escalable** | Clean Architecture, Hilt DI, Navigation, Material3, Coroutines/Flow | 🟡 Alta |
| **CI/CD & Quality** | Gradle 9.x, AGP 9.2, Tests, Lint, Detekt, Build variants | 🟢 Media |

---

## 👥 Stakeholders & Usuarios

| Rol | Descripción | Acceso Principal |
|-----|-------------|------------------|
| **Socio/Cliente** | Usuario final del gimnasio | App Cliente (Home, QR, Tienda, Membresía, Perfil) |
| **Recepcionista** | Control accesos, caja, ventas | Acceso Manual, Corte Caja, Dashboard Recepción |
| **Entrenador/Staff** | Horarios, asistencias, permisos | Empleados → Horarios, Asistencias, Turnos, Permisos |
| **Gerente/Admin** | Nóminas, reportes, CRM, configuración | Nóminas, Visitas/CRM, Config Notificaciones, Reportes |
| **Backend API** | Node.js/Express en `http://192.168.1.2:3000` | REST + Cookies (SessionCookieJar) |

---

## 📱 Funcionalidades por Módulo

### 🔐 **Autenticación (Cliente)**
- Login con email/contraseña → `POST /api/clientes/verificarCredenciales`
- Registro 3 pasos: Cuenta → Datos Personales → Contacto/Emergencia
- Verificación email → `POST /api/clientes/verificarCorreo`
- Logout → `POST /api/usuarios/logout`
- Sesión persistente con cookies (SessionCookieJar)

### 🏠 **Home Fragment (Cliente) - 4 Tabs Bottom Nav**
| Tab | Índice | Contenido Principal | APIs |
|-----|--------|---------------------|------|
| **Inicio** | 0 | Saludo, QR Access, Productos populares, Chips acción | Productos, Servicios, Membresía |
| **Membresía** | 1 | Plan actual, días restantes, historial, planes disponibles | `/api/clientes-membresias/mi-membresia`, `/api/tipos-membresia/membresiasCliente` |
| **Tienda** | 2 | Buscador, Chips categorías, Grid 2-col productos | `/api/productos/productosClientes/traer` |
| **Servicios** | 3 | Lista servicios, buscador, detalle | `/api/servicios/serviciosClientes/traer` |

### 📱 **QR Access (Modal Fullscreen)**
- Generación QR dinámico con timer (45s default)
- Badge membresía activa, nombre socio, ID
- Botones: Generar nuevo, Reportar problema

### 📋 **Bottom Sheets (Cliente)**
- **Notifications**: Lista, marcar leídas, navegar a detalle
- **Profile**: Info usuario, editar perfil, config notificaciones, logout
- **Product Detail**: Imagen, descripción, precio, stock, "Agregar al carrito"
- **Service Detail**: Similar a producto

### 🛒 **Carrito & Pago**
- Carrito bottom sheet con resumen items + totales
- **Pago Mixto**: Repartir monto entre múltiples métodos (efectivo, tarjeta, transferencia, mixto)
- **Pago Único**: Un solo método
- Checkout → Confirmación → Limpieza carrito

### 👨‍💼 **Módulo Staff - Empleados (7 Fragments)**
| Fragment | Función | Estado |
|----------|---------|--------|
| `EmpleadosListFragment` | Lista, buscar, filtrar, stats cards | 🟡 Parcial (TODOs) |
| `EmpleadoDetalleFragment` | Info completa, docs, contactos, acciones | 🟡 Parcial |
| `CrearEditarEmpleadoFragment` | Formulario completo, validaciones, foto | 🟡 Parcial |
| `HorarioEmpleadoFragment` | Semanal/mensual, turnos, descansos, copiar semana | 🟡 Parcial |
| `AsistenciasEmpleadoFragment` | Historial accesos, filtros fecha/tipo, exportar | 🟡 Parcial |
| `TurnosEmpleadoFragment` | Asignar turnos, plantillas, conflictos | 🟡 Parcial |
| `PermisosEmpleadoFragment` | Solicitudes, aprobar/rechazar, tipos (vacaciones, licencia) | 🟡 Parcial |
| `NominaEmpleadoFragment` | Cálculo nómina, horas extra, bonos, PDF | 🟡 Parcial |

### 👥 **Módulo Visitas / CRM (3 Fragments)**
| Fragment | Función |
|----------|---------|
| `VisitasListFragment` | Kanban/Lista, filtros estado/fecha/asesor, stats |
| `VisitaDetalleFragment` | Timeline completo, notas, adjuntos, histórico |
| `CrearEditarVisitaFragment` | Datos prospecto, origen, interés, próximo seguimiento |
| `SeguimientoVisitaFragment` | Tipo contacto (chips), fecha/hora, resultado (dropdown), programar próximo |

### 🔔 **Notificaciones (3 Fragments)**
| Fragment | Función |
|----------|---------|
| `NotificacionesListFragment` | Lista paginada, filtros leídas/no leídas, pull-to-refresh |
| `NotificacionDetalleFragment` | Contenido completo, acciones contextuales |
| `ConfiguracionNotificacionesFragment` | Master toggle, 5 categorías (switches), sonido/vibración/tono, horario No Molestar |

### 🚪 **Accesos y Caja (Recepción - 3 Fragments)**
| Fragment | Función |
|----------|---------|
| `AccesoManualFragment` | Buscar cliente (DNI, nombre, código), validar membresía, permitir/denegar |
| `AsistenciaDetalleBottomSheet` | Detalle acceso individual, registrar salida, ver perfil |
| `CorteCajaFragment` | Resumen financiero, ventas por método, arqueo efectivo, diferencias, finalizar corte |
| `AperturaCajaFragment` | Apertura de caja con monto inicial |
| `CierreCajaFragment` | Cierre con arqueo final |
| `MovimientosCajaFragment` | Historial movimientos, filtrado |
| `NuevoMovimientoSheet` | Registrar ingreso/egreso |
| `ConfiguracionCajaFragment` | Config denominaciones, alertas |
| `HistorialCortesFragment` | Historial cortes de caja |
| `ResumenAperturaFragment` | Resumen apertura |

### 📊 **Otros Fragments Cliente**
| Fragment | Función |
|----------|---------|
| `HistorialComprasFragment` | Historial de compras del socio |
| `HistorialMembresiasFragment` | Historial de planes contratados |
| `HistorialAsistenciasFragment` | Historial de accesos al gimnasio |
| `HistorialCortesFragment` | (Staff) Historial cortes caja |
| `CambiarPlanFragment` | Upgrade/downgrade plan con prorrateo |
| `CancelarMembresiaFragment` | Cancelación con confirmación |
| `RenovarMembresiaFragment` | Renovación plan actual |
| `MembresiaDetalleFragment` | Detalle plan específico |
| `PlanesDisponiblesSheet` | Bottom sheet selección planes |
| `ClienteBusquedaSheet` | Búsqueda cliente para recepción |
| `CarritoBottomSheet` | Carrito persistente |
| `CheckoutFragment` | Proceso checkout |
| `PagoTarjetaFragment` | Pago con tarjeta |
| `PagoMixtoFragment` | Pago dividido |
| `TicketCompraFragment` | Ticket/recibo compra |
| `QRScannerFragment` | Escáner QR (alternativo) |
| `RecepcionDashboardFragment` | Dashboard recepción |

---

## 🏗️ Arquitectura Técnica

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  Fragments (ViewBinding) → BottomSheets → Dialogs           │
│  Navigation Component (NavGraph) + Manual FragmentTransactions│
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                          │
│  Use Cases (pendientes) → Repository Interfaces             │
│  Business Logic: Membership calc, Prorrateo, QR generation  │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                         DATA LAYER                           │
│  RetrofitClient (Singleton) → AuthApiService (Interface)    │
│  OkHttp + LoggingInterceptor + SessionCookieJar (Cookies)   │
│  Room Database (2.6.1) → Entities: Usuario, EstadoCliente  │
│  Data Models (DTOs): 25+ data classes en RetrofitClient.kt │
└─────────────────────────────────────────────────────────────┘
```

### Patrones Implementados
- **ViewBinding** (buildFeatures.viewBinding = true)
- **Navigation Component** (NavGraph + manual transactions para BottomSheets)
- **Material3 Design System** (MaterialCardView, Chips, SwitchMaterial, BottomSheets)
- **Coroutines + lifecycleScope** para async operations
- **LruCache** para bitmap caching (40 items)
- **TransitionManager + AutoTransition/ChangeBounds** para animaciones
- **SessionCookieJar** para manejo de sesión server-side

---

## 🎨 Design System (Material3)

### Colores Principales
| Color | Hex | Uso |
|-------|-----|-----|
| `apex_orange` | `#FFF26513` | Primary, CTAs, accents, active states |
| `apex_orange_light` | `#FFFF7A29` | Hover/pressed states |
| `apex_orange_translucent` | `#26F26513` | Backgrounds, ripples |
| `apex_bg_screen` | `#FFF5F6F8` | Pantalla background (light) |
| `apex_card_dark` | `#FF161618` | Cards dark mode (Home, QR, Membresía) |
| `apex_card_dark_elevated` | `#FF1F1F23` | Cards elevadas |
| `apex_text_primary` | `#FF121214` | Texto principal |
| `apex_text_secondary` | `#FF7A8194` | Texto secundario |
| `apex_text_on_dark_secondary` | `#FFA6AAB4` | Texto sobre dark cards |
| `apex_stroke_light` | `#FFE4E6EB` | Bordes, divisores |
| `apex_nav_inactive` | `#FF9E9EA6` | Nav inactive, placeholders |

### Tipografía
- **Font Family**: System default (Segoe UI / Roboto)
- **Escala**: 8.5sp (nav inactive) → 36sp (totales grandes)
- **Pesos**: Regular, Medium, Bold

### Componentes Reutilizables
- **MaterialCardView** con radius 16-22dp, elevation 1-4dp
- **Chips** (Choice, Filter, Input) con bg selectors
- **SwitchMaterial** con thumb/track tint orange
- **BottomSheetDialogFragment** con bg rounded 24dp top corners
- **FAB** extendida para QR
- **Animated Bottom Nav Pill** (translationX + width animation)

---

## 📊 Estado Actual del Código

### ✅ Completado / Funcional
- [x] Proyecto Gradle configurado (AGP 9.2.1, Kotlin, KSP ready)
- [x] Retrofit + OkHttp + Gson + Room configurados
- [x] API Service con 15+ endpoints definidos
- [x] 25+ DTOs tipados para todas las respuestas
- [x] SessionCookieJar para persistencia de sesión
- [x] Tema Material3 base + colores personalizados
- [x] 40+ Layouts XML (fragment_*.xml) - **mayoría con errores XML**
- [x] HomeFragment completo con 4 tabs, QR, BottomSheets, cache imágenes
- [x] LoginFragment + RegisterFragment (3 pasos) funcionales
- [x] Configuración notificaciones completa (master, 5 cats, sonido, No Molestar)
- [x] PlantUML lifecycle diagram generado

### 🟡 Parcial / Con TODOs
- [ ] **Todos los fragments Staff** (Empleados, Visitas, Caja) - Solo esqueletos con TODOs
- [ ] **Navigation Graph** - No existe nav_graph.xml, navegación manual
- [ ] **ViewModels** - Solo RegistroViewModel existe, resto usan lógica en Fragment
- [ ] **Adapters/RecyclerViews** - Pendientes en todos los list fragments
- [ ] **Repository Layer** - No existe, llamadas directas a Retrofit en Fragments
- [ ] **Use Cases** - No existen
- [ ] **Hilt/Dagger DI** - Anotaciones @AndroidEntryPoint pero no hay módulos
- [ ] **Tests** - Solo plantillas ExampleUnitTest/ExampleInstrumentedTest

### 🔴 Crítico - Bloqueadores
- [ ] **Errores XML en 15+ layouts** - LinearLayout sin cerrar en MaterialCardView
- [ ] **Build falla** - `./gradlew assembleDebug` falla por XML malformados
- [ ] **BaseFragment** - Referenciado pero no existe (compilation error en staff fragments)
- [ ] **ViewBinding** - Habilitado pero no usado consistentemente

---

## 🔗 Dependencias Externas

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| Android Gradle Plugin | 9.2.1 | Build system |
| Kotlin | 1.9.x (implícito) | Lenguaje |
| Retrofit | 2.9.0 | HTTP Client |
| OkHttp | 4.11.0 | Network layer + logging |
| Gson | 2.9.0 (via converter) | JSON parsing |
| Room | 2.6.1 | Local DB |
| Material3 | 1.14.0 | UI Components |
| Lifecycle/ViewModel | 2.6.1 | Architecture |
| ConstraintLayout | 2.2.2 | Layouts |
| Coroutines | 1.7.x (via KTX) | Async |

---

## 📁 Estructura de Paquetes

```
com.sprm.apexgym (package principal - Cliente)
├── core
│   ├── db/models/          # Room entities: Usuario, EstadoCliente
│   ├── network/
│   │   ├── api/            # AuthApiService (15 endpoints)
│   │   └── client/         # RetrofitClient, SessionCookieJar, 25+ DTOs
│   └── (di/ - pendiente Hilt modules)
├── feature
│   ├── auth/               # Login, Registro (3 pasos), Cliente, Contacto
│   └── main/               # Home, MainActivity, BottomSheets (8)
│
com.apexgym (package staff - features separadas)
├── feature
│   ├── empleados/          # 8 fragments (List, Detalle, CRUD, Horario, Asistencias, Turnos, Permisos, Nómina)
│   ├── visitas/            # 4 fragments (List, Detalle, Crear/Editar, Seguimiento)
│   └── notificaciones/     # 3 fragments (List, Detalle, Config)
```

> **Nota**: Hay inconsistencia en package names (`com.sprm.apexgym` vs `com.apexgym`). Requiere unificación.

---

## 🚀 Próximos Pasos Inmediatos

1. **Fix XML Syntax Errors** (Bloqueador #1) - 15+ layouts con LinearLayout sin cerrar
2. **Create BaseFragment** - Para que compile módulo staff
3. **Unificar Package Names** - `com.sprm.apexgym` everywhere
4. **Crear Navigation Graph** - Centralizar navegación
5. **Implementar ViewModels + Repository** - Separar lógica de UI
6. **Completar Staff Fragments** - Adapters, ViewModels, lógica real
7. **Configurar Hilt DI** - Módulos Application, Network, Database
8. **Tests Unitarios + UI** - Coverage mínimo 70%
9. **CI/CD Pipeline** - GitHub Actions / GitLab CI
10. **Documentación Técnica** - Este set de .md files