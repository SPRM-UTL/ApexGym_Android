# ApexGym Android - API Contract

## 📡 Visión General

**Base URL**: `http://192.168.1.2:3000/api`  
**Protocolo**: HTTP/1.1 (cleartext permitido para desarrollo)  
**Autenticación**: Cookie-based Session (SessionCookieJar)  
**Formato**: JSON (Request/Response)  
**Convención**: `responseFlag: 0 = éxito, 1 = error` + `message: String`

---

## 🔐 AUTENTICACIÓN Y SESIÓN

### POST `/clientes/verificarCredenciales` - Login

**Request**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response (Éxito - 200)**
```json
{
  "responseFlag": 0,
  "message": "Login exitoso",
  "data": {
    "idCliente": 12345,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@email.com",
    "telefono": "+5491123456789",
    "fechaNacimiento": "1990-05-15",
    "genero": "M",
    "dni": "30123456",
    "membresia": {
      "idMembresiaCliente": 789,
      "idTipoMembresia": 3,
      "nombrePlan": "Premium Anual",
      "fechaInicio": "2025-01-15",
      "fechaFin": "2026-01-15",
      "diasRestantes": 365,
      "activa": true,
      "precio": 120000,
      "beneficios": ["Acceso ilimitado", "Clases grupales", "Piscina", "Sauna", "Invitados mensuales"]
    },
    "token": "session_token_abc123"
  }
}
```

**Response (Error - 200)**
```json
{
  "responseFlag": 1,
  "message": "Credenciales inválidas",
  "data": null
}
```

### POST `/clientes/registrar` - Registro Paso 1 (Cuenta)

**Request**
```json
{
  "email": "string",
  "password": "string",
  "confirmarPassword": "string"
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Cuenta creada. Verifica tu email.",
  "data": {
    "idCliente": 12345,
    "email": "juan@email.com",
    "codigoVerificacion": "123456",  // Solo en dev
    "expiraEn": 3600
  }
}
```

### POST `/clientes/verificarCorreo` - Verificar Email

**Request**
```json
{
  "email": "string",
  "codigo": "string"  // 6 dígitos
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Email verificado correctamente",
  "data": {
    "idCliente": 12345,
    "emailVerificado": true
  }
}
```

### POST `/usuarios/logout` - Cerrar Sesión

**Request**: Body vacío (cookies automáticas)

**Response**
```json
{
  "responseFlag": 0,
  "message": "Sesión cerrada",
  "data": null
}
```

---

## 👤 PERFIL Y CLIENTE

### GET `/clientes/perfil` - Obtener Perfil

**Headers**: Cookie session

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "idCliente": 12345,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@email.com",
    "telefono": "+5491123456789",
    "fechaNacimiento": "1990-05-15",
    "genero": "M",
    "dni": "30123456",
    "direccion": "Av. Corrientes 1234, CABA",
    "codigoPostal": "1043",
    "contactoEmergencia": {
      "nombre": "María Pérez",
      "telefono": "+5491198765432",
      "parentesco": "Madre"
    },
    "fotoUrl": "https://cdn.apexgym.com/avatars/12345.jpg",
    "fechaRegistro": "2024-01-15T10:30:00Z",
    "ultimoAcceso": "2025-11-05T08:15:00Z"
  }
}
```

### POST `/clientes/actualizarPerfil` - Actualizar Perfil

**Request**
```json
{
  "nombre": "string",
  "apellido": "string",
  "fechaNacimiento": "YYYY-MM-DD",
  "genero": "M|F|O",
  "dni": "string",
  "direccion": "string",
  "codigoPostal": "string",
  "contactoEmergencia": {
    "nombre": "string",
    "telefono": "string",
    "parentesco": "string"
  }
}
```

### POST `/clientes/actualizarContacto` - Actualizar Email/Teléfono

**Request**
```json
{
  "email": "string?",
  "telefono": "string?"
}
```

### POST `/clientes/cambiarPassword` - Cambiar Contraseña

**Request**
```json
{
  "passwordActual": "string",
  "passwordNuevo": "string",
  "confirmarPassword": "string"
}
```

---

## 📦 PRODUCTOS Y SERVICIOS

### GET `/productos/productosClientes/traer` - Catálogo Productos

**Query Params**: `categoria?: string`, `buscar?: string`, `pagina?: int`, `limite?: int`

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "productos": [
      {
        "idProducto": 101,
        "nombre": "Proteína Whey 2kg",
        "descripcion": "Proteína de suero aislada, 25g por servicio",
        "precio": 45000,
        "stock": 15,
        "imagenUrl": "https://cdn.apexgym.com/productos/101.jpg",
        "categoria": "Suplementos",
        "subcategoria": "Proteínas",
        "destacado": true,
        "marca": "Optimum Nutrition"
      }
    ],
    "categorias": [
      {"id": 1, "nombre": "Suplementos", "icono": "💊"},
      {"id": 2, "nombre": "Ropa", "icono": "👕"},
      {"id": 3, "nombre": "Accesorios", "icono": "🎒"},
      {"id": 4, "nombre": "Bebidas", "icono": "🥤"}
    ],
    "paginacion": {
      "paginaActual": 1,
      "totalPaginas": 5,
      "totalItems": 98,
      "itemsPorPagina": 20
    }
  }
}
```

### GET `/servicios/serviciosClientes/traer` - Catálogo Servicios

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "servicios": [
      {
        "idServicio": 201,
        "nombre": "Entrenamiento Personal",
        "descripcion": "Sesión 1-on-1 con entrenador certificado",
        "duracionMinutos": 60,
        "precio": 8000,
        "imagenUrl": "https://cdn.apexgym.com/servicios/201.jpg",
        "categoria": "Entrenamiento",
        "entrenadoresDisponibles": [
          {"id": 5, "nombre": "Carlos Rodríguez", "especialidad": "Fuerza"},
          {"id": 8, "nombre": "Laura Gómez", "especialidad": "Funcional"}
        ],
        "requiereReserva": true,
        "cupoMaximo": 1
      },
      {
        "idServicio": 202,
        "nombre": "Clase Yoga",
        "descripcion": "Clase grupal de yoga vinyasa",
        "duracionMinutos": 75,
        "precio": 3500,
        "imagenUrl": "https://cdn.apexgym.com/servicios/202.jpg",
        "categoria": "Clases Grupales",
        "entrenadoresDisponibles": [
          {"id": 12, "nombre": "Ana Martínez", "especialidad": "Yoga"}
        ],
        "requiereReserva": true,
        "cupoMaximo": 20
      }
    ],
    "categorias": [
      {"id": 1, "nombre": "Entrenamiento Personal"},
      {"id": 2, "nombre": "Clases Grupales"},
      {"id": 3, "nombre": "Nutrición"},
      {"id": 4, "nombre": "Fisioterapia"}
    ]
  }
}
```

---

## 🏷️ MEMBRESÍAS Y PLANES

### GET `/tipos-membresia/membresiasCliente` - Planes Disponibles

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "planes": [
      {
        "idTipoMembresia": 1,
        "nombre": "Básico Mensual",
        "descripcion": "Acceso a sala de musculación y cardio",
        "duracionDias": 30,
        "precio": 15000,
        "beneficios": ["Sala musculación", "Sala cardio", "Vestidores"],
        "popular": false,
        "color": "#2196F3"
      },
      {
        "idTipoMembresia": 2,
        "nombre": "Full Mensual",
        "descripcion": "Acceso completo + clases grupales",
        "duracionDias": 30,
        "precio": 25000,
        "beneficios": ["Todo Básico", "Clases grupales ilimitadas", "Piscina", "1 invitado/mes"],
        "popular": true,
        "color": "#4CAF50"
      },
      {
        "idTipoMembresia": 3,
        "nombre": "Premium Anual",
        "descripcion": "Todo incluido + beneficios exclusivos",
        "duracionDias": 365,
        "precio": 120000,
        "beneficios": ["Todo Full", "Sauna", "Spa", "3 invitados/mes", "Entrenador 1x/mes", "Nutricionista 1x/mes", "Merchandising 10% desc"],
        "popular": false,
        "color": "#FF6B00"
      }
    ]
  }
}
```

### GET `/clientes-membresias/mi-membresia` - Membresía Activa

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "idMembresiaCliente": 789,
    "idCliente": 12345,
    "idTipoMembresia": 3,
    "nombrePlan": "Premium Anual",
    "fechaInicio": "2025-01-15",
    "fechaFin": "2026-01-15",
    "diasRestantes": 71,
    "activa": true,
    "estado": "ACTIVA",
    "precioPagado": 120000,
    "metodoPago": "TARJETA",
    "ultimoPago": "2025-01-15T10:30:00Z",
    "beneficios": ["Acceso ilimitado", "Clases grupales", "Piscina", "Sauna", "Invitados mensuales"],
    "historial": [
      {
        "id": 789,
        "plan": "Premium Anual",
        "fechaInicio": "2025-01-15",
        "fechaFin": "2026-01-15",
        "precio": 120000,
        "estado": "ACTIVA"
      },
      {
        "id": 456,
        "plan": "Full Mensual",
        "fechaInicio": "2024-12-01",
        "fechaFin": "2025-01-15",
        "precio": 25000,
        "estado": "VENCIDA"
      }
    ]
  }
}
```

### POST `/clientes-membresias/solicitarCambio` - Solicitar Cambio Plan

**Request**
```json
{
  "idTipoMembresiaNuevo": 2,
  "fechaEfectiva": "2025-12-01"  // Opcional, default = inmediato
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Cambio solicitado. Diferencia a pagar: $15,000",
  "data": {
    "idSolicitud": 1001,
    "planActual": "Premium Anual",
    "planNuevo": "Full Mensual",
    "fechaEfectiva": "2025-12-01",
    "prorrateo": {
      "diasRestantesActual": 71,
      "valorDiarioActual": 328.77,
      "valorResidual": 23342.47,
      "precioPlanNuevo": 25000,
      "diferencia": 1657.53,
      "tipo": "UPGRADE"
    },
    "estado": "PENDIENTE_PAGO"
  }
}
```

### POST `/clientes-membresias/renovar` - Renovar Membresía

**Request**
```json
{
  "idTipoMembresia": 3,
  "metodoPago": "TARJETA",
  "tokenPago": "stripe_token_xyz"
}
```

### POST `/clientes-membresias/cancelar` - Cancelar Membresía

**Request**
```json
{
  "motivo": "string",
  "confirmar": true
}
```

---

## 🛒 CARRITO Y PAGOS

### POST `/pedidos/crear` - Crear Pedido (Checkout)

**Request**
```json
{
  "items": [
    {
      "tipo": "PRODUCTO|SERVICIO",
      "id": 101,
      "cantidad": 2,
      "precioUnitario": 45000
    }
  ],
  "metodoPago": "MIXTO",
  "pagos": [
    {"metodo": "EFECTIVO", "monto": 50000},
    {"metodo": "TARJETA", "monto": 40000, "token": "stripe_token_xyz"},
    {"metodo": "SALDO", "monto": 10000}
  ],
  "observaciones": "string"
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Pedido creado exitosamente",
  "data": {
    "idPedido": 5001,
    "numeroOrden": "AG-20251105-001",
    "fecha": "2025-11-05T14:30:00Z",
    "subtotal": 90000,
    "impuestos": 18900,
    "descuento": 0,
    "total": 108900,
    "estado": "PAGADO",
    "items": [...],
    "pagos": [...],
    "ticketUrl": "https://cdn.apexgym.com/tickets/AG-20251105-001.pdf"
  }
}
```

### GET `/pedidos/historial` - Historial Compras

**Query**: `pagina`, `limite`, `fechaDesde`, `fechaHasta`, `tipo`

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "pedidos": [
      {
        "idPedido": 5001,
        "numeroOrden": "AG-20251105-001",
        "fecha": "2025-11-05T14:30:00Z",
        "total": 108900,
        "estado": "PAGADO",
        "itemsCount": 3,
        "ticketUrl": "https://cdn.apexgym.com/tickets/AG-20251105-001.pdf"
      }
    ],
    "paginacion": {...}
  }
}
```

---

## 🔔 NOTIFICACIONES

### GET `/notificaciones` - Lista Notificaciones

**Query**: `pagina`, `limite`, `leidas?: boolean`, `categoria?: string`

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "notificaciones": [
      {
        "idNotificacion": 10001,
        "titulo": "Tu membresía vence en 3 días",
        "mensaje": "Renueva tu plan Premium para no perder beneficios",
        "categoria": "MEMBRESIA",
        "leida": false,
        "fecha": "2025-11-05T09:00:00Z",
        "accion": {
          "tipo": "NAVEGAR",
          "destino": "MEMBRESIA_DETALLE",
          "params": {"idMembresiaCliente": 789}
        },
        "imagenUrl": null
      }
    ],
    "totalNoLeidas": 5,
    "paginacion": {...}
  }
}
```

### POST `/notificaciones/{id}/leer` - Marcar como Leída

**Response**
```json
{
  "responseFlag": 0,
  "message": "Marcada como leída",
  "data": {"idNotificacion": 10001, "leida": true}
}
```

### POST `/notificaciones/leer-todas` - Marcar Todas como Leídas

### GET `/notificaciones/configuracion` - Obtener Config Notificaciones

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "masterActivo": true,
    "categorias": {
      "MEMBRESIA": true,
      "COMPRAS": true,
      "PROMOCIONES": false,
      "SISTEMA": true,
      "RECORDATORIOS": true
    },
    "sonido": "notification_sound_default",
    "vibracion": true,
    "led": false,
    "noMolestar": {
      "activo": true,
      "inicio": "22:00",
      "fin": "08:00"
    }
  }
}
```

### PUT `/notificaciones/configuracion` - Actualizar Config

**Request**: Mismo estructura que GET response

---

## 🚪 ACCESOS (STAFF)

### POST `/accesos/validar` - Validar Acceso Manual

**Request**
```json
{
  "tipoBusqueda": "DNI|NOMBRE|CODIGO|QR",
  "valor": "30123456",
  "tipoAcceso": "ENTRADA|SALIDA"
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Acceso permitido",
  "data": {
    "idCliente": 12345,
    "nombre": "Juan Pérez",
    "fotoUrl": "https://cdn.apexgym.com/avatars/12345.jpg",
    "membresia": {
      "plan": "Premium Anual",
      "diasRestantes": 71,
      "activa": true
    },
    "acceso": {
      "idAcceso": 50001,
      "tipo": "ENTRADA",
      "fechaHora": "2025-11-05T08:15:00Z",
      "permitido": true,
      "motivoDenegacion": null
    }
  }
}
```

**Response (Denegado)**
```json
{
  "responseFlag": 0,
  "message": "Acceso denegado",
  "data": {
    "idCliente": 12345,
    "nombre": "Juan Pérez",
    "membresia": {
      "plan": "Básico Mensual",
      "diasRestantes": 0,
      "activa": false
    },
    "acceso": {
      "idAcceso": 50002,
      "tipo": "ENTRADA",
      "fechaHora": "2025-11-05T08:15:00Z",
      "permitido": false,
      "motivoDenegacion": "MEMBRESIA_VENCIDA"
    }
  }
}
```

### GET `/accesos/historial` - Historial Accesos (Staff)

**Query**: `fechaDesde`, `fechaHasta`, `idCliente`, `tipo`, `pagina`, `limite`

---

## 💰 CAJA (STAFF)

### POST `/caja/apertura` - Apertura de Caja

**Request**
```json
{
  "montoInicial": 50000,
  "denominaciones": {
    "1000": 20,
    "500": 30,
    "200": 25,
    "100": 40,
    "50": 50,
    "20": 30,
    "10": 20,
    "5": 15,
    "2": 10,
    "1": 5
  },
  "responsable": "Juan Pérez",
  "observaciones": "Apertura turno mañana"
}
```

### POST `/caja/movimiento` - Registrar Movimiento

**Request**
```json
{
  "tipo": "INGRESO|EGRESO",
  "concepto": "Venta producto | Pago proveedor | Gasto operativo",
  "monto": 15000,
  "metodoPago": "EFECTIVO|TARJETA|TRANSFERENCIA|MIXTO",
  "referencia": "ID_PEDIDO_5001",
  "comprobanteUrl": "https://...",
  "observaciones": "string"
}
```

### POST `/caja/corte` - Realizar Corte de Caja

**Request**
```json
{
  "arqueo": {
    "1000": 25,
    "500": 35,
    "200": 20,
    "100": 45,
    "50": 55,
    "20": 35,
    "10": 25,
    "5": 20,
    "2": 15,
    "1": 10
  },
  "observaciones": "Diferencia $500 por redondeo"
}
```

**Response**
```json
{
  "responseFlag": 0,
  "message": "Corte realizado",
  "data": {
    "idCorte": 2001,
    "fecha": "2025-11-05T22:00:00Z",
    "responsable": "Juan Pérez",
    "ventasTotales": 1250000,
    "ventasPorMetodo": {
      "EFECTIVO": 450000,
      "TARJETA": 600000,
      "TRANSFERENCIA": 150000,
      "MIXTO": 50000
    },
    "arqueoSistema": 450000,
    "arqueoFisico": 450500,
    "diferencia": 500,
    "diferenciaJustificada": true,
    "estado": "CERRADO",
    "reporteUrl": "https://cdn.apexgym.com/cortes/corte_2001.pdf"
  }
}
```

### GET `/caja/historial` - Historial Cortes

### GET `/caja/resumen` - Resumen Caja Actual

---

## 👨‍💼 EMPLEADOS (STAFF)

### GET `/empleados` - Lista Empleados

**Query**: `buscar`, `estado`, `rol`, `sede`, `pagina`, `limite`

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "empleados": [
      {
        "id": 1,
        "legajo": "EMP-001",
        "nombre": "Carlos",
        "apellido": "Rodríguez",
        "dni": "25123456",
        "email": "carlos@apexgym.com",
        "telefono": "+5491133334444",
        "rol": "ENTRENADOR",
        "sede": "Caballito",
        "estado": "ACTIVO",
        "fechaIngreso": "2023-03-15",
        "fotoUrl": "https://cdn.apexgym.com/staff/1.jpg"
      }
    ],
    "stats": {
      "total": 25,
      "activos": 22,
      "inactivos": 2,
      "vacaciones": 1,
      "nuevosMes": 3
    },
    "paginacion": {...}
  }
}
```

### GET `/empleados/{id}` - Detalle Empleado

### POST `/empleados` - Crear Empleado

### PUT `/empleados/{id}` - Actualizar Empleado

### GET `/empleados/{id}/horario` - Horario Empleado

### GET `/empleados/{id}/asistencias` - Asistencias Empleado

### GET `/empleados/{id}/nomina` - Nómina Empleado

---

## 👥 VISITAS / CRM (STAFF)

### GET `/visitas` - Lista Visitas

**Query**: `estado`, `asesorId`, `fechaDesde`, `fechaHasta`, `origen`, `pagina`, `limite`

**Response**
```json
{
  "responseFlag": 0,
  "message": "OK",
  "data": {
    "visitas": [
      {
        "idVisita": 3001,
        "prospecto": {
          "nombre": "Roberto",
          "apellido": "García",
          "telefono": "+5491155556666",
          "email": "roberto@email.com"
        },
        "origen": "WEB",
        "interesInicial": "Membresía Full + Entrenador",
        "estado": "CALIFICADO",
        "asesor": {"id": 5, "nombre": "Carlos Rodríguez"},
        "fechaCreacion": "2025-10-20T14:00:00Z",
        "ultimoContacto": "2025-11-02T10:30:00Z",
        "proximoSeguimiento": "2025-11-06T15:00:00Z",
        "valorEstimado": 25000,
        "probabilidad": 70
      }
    ],
    "kanbanStats": {
      "NUEVO": 12,
      "CONTACTADO": 8,
      "CALIFICADO": 5,
      "PROPUESTA": 3,
      "CERRADO_GANADO": 15,
      "CERRADO_PERDIDO": 4
    },
    "paginacion": {...}
  }
}
```

### POST `/visitas` - Crear Visita

### GET `/visitas/{id}` - Detalle Visita (Timeline completo)

### POST `/visitas/{id}/seguimiento` - Registrar Seguimiento

**Request**
```json
{
  "tipoContacto": "LLAMADA|WHATSAPP|EMAIL|PRESENCIAL|VIDEO",
  "fechaHora": "2025-11-05T15:00:00Z",
  "resultado": "INTERESTADO|NO_CONTESTA|NO_INTERESADO|REAGENDAR|CERRADO",
  "observaciones": "Le interesa plan Full, consultar por piscina",
  "proximoSeguimiento": "2025-11-08T10:00:00Z"
}
```

---

## 📊 REPORTES Y DASHBOARDS

### GET `/reportes/dashboard-recepcion` - Dashboard Recepción

### GET `/reportes/dashboard-gerente` - Dashboard Gerente

### GET `/reportes/ventas` - Reporte Ventas

### GET `/reportes/membresias` - Reporte Membresías

### GET `/reportes/asistencias` - Reporte Asistencias

---

## 🔄 CÓDIGOS DE ERROR ESTÁNDAR

| responseFlag | message (ejemplos) | Significado |
|--------------|-------------------|-------------|
| 0 | "OK", "Login exitoso", "Creado" | Éxito |
| 1 | "Credenciales inválidas", "Email ya registrado", "Stock insuficiente" | Error de negocio |
| 2 | "Sesión expirada", "Token inválido", "No autorizado" | Error autenticación |
| 3 | "Validación fallida: email inválido", "Campo requerido: password" | Error validación |
| 4 | "Recurso no encontrado", "Cliente no existe" | Not found |
| 5 | "Error interno del servidor", "Base de datos no disponible" | Error servidor |
| 6 | "Permisos insuficientes", "Rol no autorizado" | Forbidden |

---

## 📋 HEADERS COMUNES

**Request**
```
Content-Type: application/json
Accept: application/json
Cookie: session_id=abc123; path=/; HttpOnly
User-Agent: ApexGym/1.0 (Android; API 35)
```

**Response**
```
Content-Type: application/json
Set-Cookie: session_id=new_token; Path=/; HttpOnly; SameSite=Lax
X-Response-Time: 145ms
```

---

## 🧪 EJEMPLOS DE TESTING (cURL)

### Login
```bash
curl -X POST http://192.168.1.2:3000/api/clientes/verificarCredenciales \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@email.com","password":"MiPass123!"}' \
  -c cookies.txt -v
```

### Obtener Perfil (con cookies)
```bash
curl -X GET http://192.168.1.2:3000/api/clientes/perfil \
  -b cookies.txt -v
```

### Catálogo Productos
```bash
curl -X GET "http://192.168.1.2:3000/api/productos/productosClientes/traer?categoria=Suplementos&pagina=1&limite=20" \
  -b cookies.txt
```

### Crear Pedido
```bash
curl -X POST http://192.168.1.2:3000/api/pedidos/crear \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "items":[{"tipo":"PRODUCTO","id":101,"cantidad":1,"precioUnitario":45000}],
    "metodoPago":"TARJETA",
    "pagos":[{"metodo":"TARJETA","monto":54450,"token":"stripe_token_xyz"}]
  }'
```

---

## 📝 NOTAS DE IMPLEMENTACIÓN ANDROID

### Retrofit Interface (AuthApiService.kt)

```kotlin
interface AuthApiService {
    @POST("clientes/verificarCredenciales")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponseLogin>
    
    @POST("clientes/registrar")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponseRegistro>
    
    @POST("clientes/verificarCorreo")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<ApiResponseVerificarCorreo>
    
    @POST("usuarios/logout")
    suspend fun logout(): Response<ApiResponseBase>
    
    @GET("clientes/perfil")
    suspend fun getProfile(): Response<ApiResponsePerfil>
    
    @POST("clientes/actualizarPerfil")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponseActualizarPerfil>
    
    @GET("productos/productosClientes/traer")
    suspend fun getProducts(
        @Query("categoria") categoria: String? = null,
        @Query("buscar") buscar: String? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("limite") limite: Int = 20
    ): Response<ApiResponseProductos>
    
    // ... más endpoints
}
```

### Interceptor de Logging (Debug)

```kotlin
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        Log.d("API", "→ ${request.method} ${request.url}")
        request.headers.forEach { (k, v) -> Log.d("API", "  $k: $v") }
        request.body?.let { Log.d("API", "  Body: ${it.toString()}") }
        
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime
        
        Log.d("API", "← ${response.code} ${request.url} (${duration}ms)")
        response.body?.let { body ->
            val content = body.string()
            Log.d("API", "  Response: $content")
            return response.newBuilder().body(ResponseBody.create(content, body.contentType())).build()
        }
        return response
    }
}
```

### Manejo de Errores Unificado

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String, val throwable: Throwable? = null) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            response.body()?.let { body ->
                if (body is ApiResponseBase) {
                    when (body.responseFlag) {
                        0 -> ApiResult.Success(body.data as T)
                        1 -> ApiResult.Error(1, body.message ?: "Error desconocido")
                        2 -> ApiResult.Error(2, body.message ?: "Sesión expirada")
                        else -> ApiResult.Error(body.responseFlag, body.message ?: "Error")
                    }
                } else {
                    ApiResult.Success(body)
                }
            } ?: ApiResult.Error(5, "Respuesta vacía")
        } else {
            ApiResult.Error(response.code(), "HTTP ${response.code()}: ${response.message()}")
        }
    } catch (e: IOException) {
        ApiResult.Error(0, "Sin conexión a internet", e)
    } catch (e: Exception) {
        ApiResult.Error(5, "Error inesperado: ${e.message}", e)
    }
}
```

---

## 🔄 VERSIONADO Y CHANGELOG

| Versión | Fecha | Cambios |
|---------|-------|---------|
| v1.0.0 | 2025-11-05 | Contrato inicial - Auth, Productos, Servicios, Membresías, Pedidos, Notificaciones, Accesos, Caja, Empleados, Visitas |
| v1.1.0 | TBD | Push notifications (FCM), Webhooks, WebSockets tiempo real |
| v2.0.0 | TBD | Multi-sede, Marketplace, Suscripciones recurrentes, API pública partners |

---

## 📞 CONTACTO BACKEND

- **Equipo Backend**: Node.js/Express + PostgreSQL
- **Puerto**: 3000
- **Documentación**: Swagger en `http://192.168.1.2:3000/docs`
- **Repositorio**: `git@github.com:apexgym/backend.git`
- **Contacto**: backend@apexgym.com