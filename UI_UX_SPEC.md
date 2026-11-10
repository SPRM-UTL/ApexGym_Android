# ApexGym Android - UI/UX Specification

## 🎨 Design System Overview

### Principios de Diseño

| Principio | Descripción |
|-----------|-------------|
| **Claridad** | Interfaces limpias, jerarquía visual clara, contenido legible |
| **Consistencia** | Patrones repetidos, componentes reutilizables, comportamiento predecible |
| **Eficiencia** | Flujos mínimos, accesos directos, feedback inmediato |
| **Accesibilidad** | Contraste AA, touch targets 48dp+, TalkBack, escalado fuentes |
| **Personalidad** | Brand naranja vibrante, dark mode premium, micro-interacciones |

---

## 🎨 Paleta de Colores (Material3)

### Light Theme

| Token | Hex | Uso | Contraste vs White |
|-------|-----|-----|-------------------|
| `primary` | `#FF6B00` | Botones primarios, FAB, links activos, switches ON | 4.5:1 ✅ |
| `onPrimary` | `#FFFFFF` | Texto sobre primary | - |
| `primaryContainer` | `#FFF3E0` | Chips seleccionados, badges, hover states | 3.2:1 ⚠️ |
| `onPrimaryContainer` | `#E65100` | Texto sobre primaryContainer | 7.1:1 ✅ |
| `secondary` | `#FF9800` | Acentos secundarios, badges warning | 3.1:1 ⚠️ |
| `tertiary` | `#4CAF50` | Success states, rachas, confirmaciones | 4.5:1 ✅ |
| `error` | `#F44336` | Errores, destructivo, denegado | 4.5:1 ✅ |
| `surface` | `#FFFFFF` | Cards, sheets, dialogs | - |
| `onSurface` | `#121214` | Texto principal | 15.3:1 ✅ |
| `onSurfaceVariant` | `#7A8194` | Texto secundario, hints | 4.5:1 ✅ |
| `outline` | `#E4E6EB` | Bordes, divisores, chips no seleccionados | 1.8:1 ❌ |
| `background` | `#F5F6F8` | Pantalla base | - |

### Dark Theme (Principal - App Gym)

| Token | Hex | Uso | Contraste vs White |
|-------|-----|-----|-------------------|
| `primary` | `#FF7A29` | Botones primarios, FAB, links | 4.5:1 ✅ |
| `onPrimary` | `#000000` | Texto sobre primary | - |
| `primaryContainer` | `#FF6B00` | Chips seleccionados | 4.5:1 ✅ |
| `onPrimaryContainer` | `#FFFFFF` | Texto sobre primaryContainer | - |
| `surface` | `#161618` | Cards principales (Home, QR, Membresía) | - |
| `surfaceVariant` | `#1F1F23` | Cards elevadas, bottom sheets | - |
| `surfaceBright` | `#2A2A2F` | Hover/pressed states | - |
| `onSurface` | `#FFFFFF` | Texto principal | 21:1 ✅ |
| `onSurfaceVariant` | `#A6AAB4` | Texto secundario | 7.1:1 ✅ |
| `outline` | `#3A3A44` | Bordes, divisores | 3.9:1 ✅ |
| `background` | `#0F0F1A` | Pantalla base | - |
| `inverseSurface` | `#F5F6F8` | Para overlays claros | - |
| `inverseOnSurface` | `#121214` | Texto sobre inverse | - |

### Semantic Colors (Ambos Temas)

| Token | Light | Dark | Uso |
|-------|-------|------|-----|
| `success` | `#4CAF50` | `#66BB6A` | Éxito, activo, racha |
| `warning` | `#FFC107` | `#FFD54F` | Advertencia, pendiente |
| `info` | `#1976D2` | `#64B5F6` | Información, neutral |
| `error` | `#F44336` | `#EF5350` | Error, denegado, crítico |

---

## 📝 Tipografía (Material3 Type Scale)

### Light Theme

| Style | Font Size | Weight | Line Height | Letter Spacing | Uso |
|-------|-----------|--------|-------------|----------------|-----|
| `Display Large` | 57sp | 400 | 64sp | -0.25sp | Hero screens (no usado) |
| `Display Medium` | 45sp | 400 | 52sp | 0sp | - |
| `Display Small` | 36sp | 400 | 44sp | 0sp | Totales grandes (Home, Caja) |
| `Headline Large` | 32sp | 400 | 40sp | 0sp | Títulos pantalla |
| `Headline Medium` | 28sp | 400 | 36sp | 0sp | Section headers |
| `Headline Small` | 24sp | 400 | 32sp | 0sp | Card titles |
| `Title Large` | 22sp | 500 | 28sp | 0sp | Toolbar titles |
| `Title Medium` | 16sp | 500 | 24sp | 0.15sp | List items, chips |
| `Title Small` | 14sp | 500 | 20sp | 0.1sp | Labels, captions bold |
| `Body Large` | 16sp | 400 | 24sp | 0.5sp | Body text principal |
| `Body Medium` | 14sp | 400 | 20sp | 0.25sp | Body text secundario |
| `Body Small` | 12sp | 400 | 16sp | 0.4sp | Captions, metadata |
| `Label Large` | 14sp | 500 | 20sp | 0.1sp | Botones, tabs |
| `Label Medium` | 12sp | 500 | 16sp | 0.5sp | Chips, badges |
| `Label Small` | 11sp | 500 | 16sp | 0.5sp | Overline, tags |

### Dark Theme (Ajustes)

- Mismos tamaños, pesos
- `Body Large`: 16sp → 15sp (legibilidad en dark)
- `Label Large`: 14sp → 13sp (botones más compactos)

### Fuente del Sistema
```xml
<!-- No custom font - usar system default -->
<!-- Android: Roboto / iOS: San Francisco (no aplica) -->
<!-- fallbacks: sans-serif, sans-serif-medium, sans-serif-bold -->
```

---

## 📐 Espaciado y Layout (8dp Grid)

### Spacing Scale

| Token | Valor | Uso |
|-------|-------|-----|
| `space-1` | 4dp | Micro spacing, icon-text gap |
| `space-2` | 8dp | Base unit, padding small |
| `space-3` | 12dp | Padding medium, item gap |
| `space-4` | 16dp | **Standard screen padding**, card padding |
| `space-5` | 20dp | Card internal padding |
| `space-6` | 24dp | Section gap, bottom sheet header |
| `space-8` | 32dp | Large section gap |
| `space-10` | 40dp | Hero sections |
| `space-12` | 48dp | Full screen margins |

### Breakpoints (Responsive)

| Breakpoint | Ancho | Columnas Grid | Uso |
|------------|-------|---------------|-----|
| `compact` | < 600dp | 4 | Phone portrait |
| `medium` | 600-840dp | 8 | Phone landscape / Tablet portrait |
| `expanded` | 840-1200dp | 12 | Tablet landscape |
| `large` | 1200-1600dp | 12 | Desktop |
| `extraLarge` | > 1600dp | 12 | Large desktop |

> **Nota**: App móvil only → `compact` y `medium` principalmente

---

## 🔲 Componentes Base

### 1. MaterialCardView (Estándar App)

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardBackgroundColor="?attr/colorSurface"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutline"
    android:layout_marginHorizontal="16dp"
    android:layout_marginBottom="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">
        <!-- Content -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

**Variantes:**
| Variante | Radius | Elevation | Stroke | Uso |
|----------|--------|-----------|--------|-----|
| `Standard` | 16dp | 2dp | 1dp | Default |
| `Elevated` | 22dp | 4dp | 0dp | Hero cards (Home, QR, Membresía) |
| `Outlined` | 12dp | 0dp | 2dp | Selection cards, planners |
| `Flat` | 8dp | 0dp | 1dp | List items, compact |

### 2. Chips (Filter / Choice / Input)

```xml
<!-- Filter Chip - Categoría Tienda -->
<com.google.android.material.chip.Chip
    style="@style/Widget.Material3.Chip.Filter"
    android:layout_width="wrap_content"
    android:layout_height="32dp"
    android:text="Suplementos"
    android:textSize="12sp"
    app:chipBackgroundColor="@color/chip_bg_selector"
    app:chipCornerRadius="16dp"
    app:chipStrokeWidth="1dp"
    app:chipStrokeColor="@color/chip_stroke_selector"
    android:checkable="true"
    android:clickable="true" />

<!-- Choice Chip - Tipo Contacto CRM -->
<com.google.android.material.chip.Chip
    style="@style/Widget.Material3.Chip.Choice"
    android:layout_width="wrap_content"
    android:layout_height="36dp"
    android:text="Llamada"
    app:chipBackgroundColor="@color/choice_chip_bg_selector"
    app:chipIcon="@drawable/ic_call"
    app:iconTint="@color/choice_chip_icon_selector" />
```

**Selectores de Color (Chip):**

```xml
<!-- res/color/chip_bg_selector.xml -->
<selector>
    <item android:color="?attr/colorPrimaryContainer" android:state_checked="true" />
    <item android:color="?attr/colorSurfaceVariant" />
</selector>

<!-- res/color/chip_stroke_selector.xml -->
<selector>
    <item android:color="?attr/colorPrimary" android:state_checked="true" />
    <item android:color="?attr/colorOutline" />
</selector>
```

### 3. SwitchMaterial (Config Notificaciones)

```xml
<com.google.android.material.switchmaterial.SwitchMaterial
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Notificaciones de membresía"
    android:textSize="14sp"
    android:textColor="?attr/colorOnSurface"
    app:thumbTint="@color/switch_thumb_selector"
    app:trackTint="@color/switch_track_selector"
    app:useMaterial3="true" />
```

### 4. BottomSheetDialogFragment (Estándar)

```kotlin
class BaseBottomSheetFragment : BottomSheetDialogFragment() {
    
    override fun onCreateView(...): View {
        val view = binding.root
        // Rounded top corners 24dp
        (dialog as? BottomSheetDialog)?.setOnShowListener {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
        }
        return view
    }
    
    override fun getTheme() = R.style.BottomSheetTheme
}
```

```xml
<!-- res/drawable/bg_bottom_sheet_rounded.xml -->
<shape>
    <solid android:color="?attr/colorSurface" />
    <corners
        android:topLeftRadius="24dp"
        android:topRightRadius="24dp"
        android:bottomLeftRadius="0dp"
        android:bottomRightRadius="0dp" />
</shape>
```

### 5. FAB Extendida (QR Access)

```xml
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/fabQrAccess"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Acceso QR"
    android:icon="@drawable/ic_qr_code"
    app:iconTint="@color/white"
    app:backgroundTint="@color/apex_orange"
    app:extendedPaddingStart="24dp"
    app:extendedPaddingEnd="24dp"
    app:shapeAppearanceOverlay="@style/ShapeAppearance.FAB" />
```

### 6. Bottom Navigation (Custom Animated Pill)

```xml
<!-- Custom implementation - NO BottomNavigationView -->
<FrameLayout
    android:id="@+id/bottomNavContainer"
    android:layout_width="match_parent"
    android:layout_height="72dp"
    android:background="?attr/colorSurface">
    
    <!-- Animated Pill Indicator -->
    <View
        android:id="@+id/navPillIndicator"
        android:layout_width="80dp"
        android:layout_height="48dp"
        android:layout_gravity="bottom|start"
        android:layout_marginStart="12dp"
        android:layout_marginBottom="12dp"
        android:background="@drawable/bg_nav_pill" />
    
    <!-- Tab Buttons -->
    <LinearLayout
        android:id="@+id/llBottomNavTabs"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:weightSum="4">
        
        <!-- 4 tabs con ImageView + TextView -->
    </LinearLayout>
</FrameLayout>
```

**Animación Pill (ValueAnimator + TransitionManager):**
```kotlin
private fun animatePillToTab(tabIndex: Int) {
    val targetTab = tabs[tabIndex]
    val targetX = targetTab.x + (targetTab.width - pillWidth) / 2
    val targetWidth = calculatePillWidth(tabIndex)
    
    TransitionManager.beginDelayedTransition(navContainer, AutoTransition().apply {
        duration = 300
        interpolator = FastOutSlowInInterpolator()
    })
    
    ValueAnimator.ofFloat(pill.translationX, targetX).apply {
        duration = 300
        addUpdateListener { pill.translationX = it.animatedValue as Float }
        start()
    }
    
    ValueAnimator.ofInt(pill.layoutParams.width, targetWidth).apply {
        duration = 300
        addUpdateListener { pill.layoutParams.width = it.animatedValue as Int; pill.requestLayout() }
        start()
    }
}
```

---

## 📱 Pantallas Clave - Especificaciones Detalladas

### 1. Splash / Onboarding

| Elemento | Especificación |
|----------|----------------|
| Duración Splash | 1.5s min, 3s max (carga async) |
| Animación Logo | Scale 0.8→1.0 + Fade in (600ms) |
| Onboarding | 3 páginas: Bienvenida, Permisos, QR Demo |
| Page Indicator | 3 dots, active naranja, inactive gris |
| Skip Button | Top-right, "Omitir", 14sp medium |
| Next/Get Started | Bottom, FAB extendida naranja |

### 2. Login Screen

| Elemento | Especificación |
|----------|----------------|
| Layout | Centered card (max 360dp width), 24dp screen margin |
| Logo | 80dp, centered, 32dp margin bottom |
| Title | "Bienvenido", Headline Medium, 16dp margin bottom |
| Subtitle | "Inicia sesión para continuar", Body Medium, secondary |
| Email Field | OutlinedBox, hint "Email", keyboardType email, 12dp gap |
| Password Field | OutlinedBox, hint "Contraseña", toggle visibility, 12dp gap |
| Forgot Password | TextButton, Label Medium, right aligned, 16dp margin top |
| Login Button | FilledButton, full width, 48dp height, "Entrar", 24dp margin top |
| Divider | "o continua con", 16dp margin vertical |
| Social Buttons | OutlinedButton icons (Google, Apple), 12dp gap |
| Register Link | "¿No tienes cuenta? Regístrate", centered, 24dp margin top |

**Estados Validación:**
- Email inválido: Error text rojo, stroke rojo, shake animation
- Password vacío: Error text, stroke rojo
- Loading: Button disabled, circular progress, texto "Entrando..."
- Error API: Snackbar abajo + shake card

### 3. Home Fragment (Cliente) - 4 Tabs

#### Tab 0: INICIO

```
┌─────────────────────────────────────┐
│ Header (72dp)                       │
│ ┌─────────────────────────────────┐ │
│ │ 👋 Hola, Juan          [🔔] [👤] │ │  ← Toolbar con saludo + acciones
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ QR Access Card (Hero - Elevated)    │
│ ┌─────────────────────────────────┐ │
│ │  ┌────┐  ACCESO AL GIMNASIO     │ │
│ │  │ QR │  Tu pase de entrada     │ │
│ │  └────┘  [Generar QR] [Escanear]│ │  ← FAB extendida + botón outline
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Productos Populares (Horizontal)    │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐        │
│ │Img │ │Img │ │Img │ │Img │  →     │  ← RecyclerView horizontal, snap
│ │Prot│ │Cre │ │Bar │ │Be │        │
│ │$45K│ │$8K │ │$3K │ │$12K│        │
│ └────┘ └────┘ └────┘ └────┘        │
├─────────────────────────────────────┤
│ Action Chips (Row)                  │
│ [💳 Membresía] [🛒 Tienda] [📋 Servicios] │  ← Navigan a tabs 1, 2, 3
└─────────────────────────────────────┘
```

#### Tab 1: MEMBRESÍA

```
┌─────────────────────────────────────┐
│ Estado Actual Card (Elevated Dark)  │
│ ┌─────────────────────────────────┐ │
│ │ PREMIUM ANUAL        🟢 ACTIVA  │ │
│ │ ┌─────────────────────────────┐ │ │
│ │ │        71 días restantes    │ │ │  ← Progress circular 120dp
│ │ │        ████████░░ 80%       │ │ │
│ │ └─────────────────────────────┘ │ │
│ │ Vence: 15 ene 2026  |  $120.000 │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Beneficios (Grid 2x3)               │
│ ┌─────────────┐ ┌─────────────┐     │
│ │ 🏋️ Acceso   │ │ 🏊 Piscina  │     │
│ │ ilimitado   │ │ y Sauna     │     │
│ └─────────────┘ └─────────────┘     │
│ ┌─────────────┐ ┌─────────────┐     │
│ │ 👥 3 invit. │ │ 🥗 Nutri.   │     │
│ │ /mes        │ │ 1x/mes      │     │
│ └─────────────┘ └─────────────┘     │
├─────────────────────────────────────┤
│ Historial Planes (Expandible)       │
│ ▼ Premium Anual    Ene 2025 - Ene 2026  $120.000  ACTIVA │
│   Full Mensual     Dic 2024 - Ene 2025  $25.000   VENCIDA │
├─────────────────────────────────────┤
│ Planes Disponibles                  │
│ [Ver todos los planes ▸]            │  ← Abre BottomSheet
└─────────────────────────────────────┘
```

#### Tab 2: TIENDA

```
┌─────────────────────────────────────┐
│ Search Bar (48dp)                   │
│ [🔍 Buscar productos...       ]     │
├─────────────────────────────────────┤
│ Category Chips (Horizontal Scroll)  │
│ [Todos] [Suplementos] [Ropa] [Acc.] [Bebidas] →
├─────────────────────────────────────┤
│ Products Grid (2 cols, 16dp gap)    │
│ ┌──────────────┐ ┌──────────────┐   │
│ │   [IMG]      │ │   [IMG]      │   │
│ │ Proteína     │ │ Creatina     │   │
│ │ $45.000      │ │ $28.000      │   │
│ │ Stock: 15    │ │ Stock: 8     │   │
│ │ [+ Agregar]  │ │ [+ Agregar]  │   │
│ └──────────────┘ └──────────────┘   │
│        ↓ Scroll vertical             │
└─────────────────────────────────────┘
```

#### Tab 3: PERFIL (via Avatar Click → BottomSheet)

```
┌─────────────────────────────────────┐
│ Header (56dp)                       │
│ ┌─────────────────────────────────┐ │
│ │           [👤]  Juan Pérez      │ │
│ │        juan@email.com           │ │
│ │        Socio #12345             │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Menu Items (List)                   │
│ ┌─────────────────────────────────┐ │
│ │ ✏️  Editar perfil          ▸    │ │
│ ├─────────────────────────────────┤ │
│ │ 🔔  Notificaciones         ▸    │ │
│ ├─────────────────────────────────┤ │
│ │ 🎨  Tema: Oscuro           ▸    │ │
│ ├─────────────────────────────────┤ │
│ │ 🌐  Idioma: Español        ▸    │ │
│ ├─────────────────────────────────┤ │
│ │ 📱  Dispositivos           ▸    │ │
│ ├─────────────────────────────────┤ │
│ │ 🗑️  Eliminar cuenta        ▸    │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Logout Button (Red)                 │
│ ┌─────────────────────────────────┐ │
│ │      Cerrar sesión              │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### 4. QR Access Modal (Fullscreen desde Home)

```
┌─────────────────────────────────────┐
│ ┌─────────────────────────────────┐ │
│ │  ✕  (Close - top right)         │ │
│ └─────────────────────────────────┘ │
│                                     │
│        ┌─────────────────┐          │
│        │   APEX GYM      │          │
│        │  ┌─────────┐    │          │
│        │  │   QR    │    │          │  ← QR Code 280x280dp
│        │  │  CODE   │    │          │
│        │  └─────────┘    │          │
│        │                 │          │
│        │  JUAN PÉREZ     │          │
│        │  Premium Anual  │          │  ← Badge membresía
│        │  Socio #12345   │          │
│        └─────────────────┘          │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  ⏱  00:45  (Countdown)      │    │  ← Timer circular 45s
│  └─────────────────────────────┘    │
│                                     │
│  [Generar nuevo QR]  [Reportar]     │  ← 2 botones full width
│                                     │
│  ⚠️ El código expira en 45 segundos │
└─────────────────────────────────────┘
```

**Animación Entrada:** Slide up desde bottom (300ms) + Fade in background
**Animación Salida:** Slide down + Fade out

### 5. Product Detail BottomSheet

```
┌─────────────────────────────────────┐
│ Drag Handle (4dp)                   │
├─────────────────────────────────────┤
│ Image (16:9, 200dp height)          │
│ ┌─────────────────────────────────┐ │
│ │         [PRODUCT IMAGE]         │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Title + Price                       │
│ Proteína Whey 2kg          $45.000  │
├─────────────────────────────────────┤
│ Description                         │
│ Proteína de suero aislada...        │
├─────────────────────────────────────┤
│ Stock Badge                         │
│ 🟢 Stock: 15 unidades disponibles   │
├─────────────────────────────────────┤
│ Quantity Selector                   │
│ Cantidad:  [−]  1  [+]              │
├─────────────────────────────────────┤
│ [Agregar al carrito - $45.000]      │  ← Filled Button full width
└─────────────────────────────────────┘
```

### 6. Carrito BottomSheet (Side Sheet - Right)

```
┌─────────────────────────────────────┐
│ ┌─────────────────────────────────┐ │
│ │  🛒 Mi Carrito           ✕     │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Item 1                              │
│ ┌────┐ Proteína Whey 2kg    ×2      │
│ │Img │ $45.000 c/u                 │
│ │    │ [−] 2 [+]    [🗑️]          │
│ └────┘                              │
├─────────────────────────────────────┤
│ Item 2                              │
│ ┌────┐ Creatina 300g         ×1     │
│ │Img │ $28.000                     │
│ │    │ [−] 1 [+]    [🗑️]          │
│ └────┘                              │
├─────────────────────────────────────┤
│ Resumen (Sticky Bottom)             │
│ Subtotal (2 items)      $118.000    │
│ Envío                          $0   │
│ ─────────────────────────────────   │
│ TOTAL                   $118.000    │
│                                     │
│ [Continuar al pago]                 │  ← Filled Button
└─────────────────────────────────────┘
```

### 7. Configuración Notificaciones

```
┌─────────────────────────────────────┐
│ Toolbar: "Notificaciones" [✕]       │
├─────────────────────────────────────┤
│ Master Toggle                       │
│ ┌─────────────────────────────────┐ │
│ │ 🔔  Todas las notificaciones  ● │ │  ← Switch ON
│ │     Activar/desactivar todas    │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Categorías (Card)                   │
│ ┌─────────────────────────────────┐ │
│ │ 📋 Categorías                   │ │
│ ├─────────────────────────────────┤ │
│ │ 🏷️ Membresía              ●     │ │
│ │    Vencimientos, renovaciones   │ │
│ ├─────────────────────────────────┤ │
│ │ 🛒 Compras                  ●   │ │
│ │    Confirmaciones, tickets      │ │
│ ├─────────────────────────────────┤ │
│ │ 🎁 Promociones               ○  │ │
│ │    Ofertas, descuentos          │ │
│ ├─────────────────────────────────┤ │
│ │ ⚙️ Sistema                   ●  │ │
│ │    Mantenimiento, actualizaciones│ │
│ ├─────────────────────────────────┤ │
│ │ ⏰ Recordatorios              ●  │ │
│ │    Clases, entrenamientos       │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Sonido y Vibración (Card)           │
│ ┌─────────────────────────────────┐ │
│ │ 🔊 Sonido y vibración           │ │
│ ├─────────────────────────────────┤ │
│ │ 🔔 Sonido                    ●  │ │
│ │    Tono: [Notificación por defecto ▼] │
│ ├─────────────────────────────────┤ │
│ │ 📳 Vibración                 ●  │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ No Molestar (Card)                  │
│ ┌─────────────────────────────────┐ │
│ │ 🌙 No molestar                  │ │
│ ├─────────────────────────────────┤ │
│ │ Activar horario           ●     │ │
│ ├─────────────────────────────────┤ │
│ │ Desde: [22:00 ▼]  Hasta: [08:00 ▼] │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 👨‍💼 Staff Screens - Especificaciones

### 8. Empleados List

```
┌─────────────────────────────────────┐
│ Toolbar: "Empleados" [+]            │
├─────────────────────────────────────┤
│ Search + Filter Bar (56dp)          │
│ [🔍 Buscar...] [🔽 Filtros ▼]       │
├─────────────────────────────────────┤
│ Stats Cards (Horizontal Scroll)     │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
│ │ Total│ │Activos│ │Nuevos│ │Licen.│ │
│ │  25  │ │  22  │ │  3   │ │  1   │ │
│ └──────┘ └──────┘ └──────┘ └──────┘ │
├─────────────────────────────────────┤
│ List (RecyclerView)                 │
│ ┌─────────────────────────────────┐ │
│ │ [👤] Carlos Rodríguez    ENTREN │ │
│ │    carlos@apexgym.com  ACTIVO   │ │
│ │    Legajo: EMP-001  ▸           │ │
│ ├─────────────────────────────────┤ │
│ │ [👤] María Gómez         RECEP  │ │
│ │    maria@apexgym.com   ACTIVO   │ │
│ │    Legajo: EMP-002  ▸           │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Empty State                         │
│ [Ilustración]                       │
│ "No hay empleados"                  │
│ "Agrega tu primer empleado" [+]     │
└─────────────────────────────────────┘
```

### 9. Acceso Manual (Recepción)

```
┌─────────────────────────────────────┐
│ Toolbar: "Acceso Manual" [✕]        │
├─────────────────────────────────────┤
│ Search Card                         │
│ ┌─────────────────────────────────┐ │
│ │ Buscar socio                    │ │
│ ├─────────────────────────────────┤ │
│ │ [DNI ▼]  [30123456________] [🔍]│ │  ← Spinner tipo búsqueda
│ ├─────────────────────────────────┤ │
│ │ Accesos rápidos:                │ │
│ │ [📷 Escanear QR] [📋 Código]    │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Resultado (si encontrado)           │
│ ┌─────────────────────────────────┐ │
│ │ [👤] JUAN PÉREZ                 │ │
│ │    Premium Anual  |  71 días    │ │
│ │    🟢 MEMBRESÍA ACTIVA          │ │
│ ├─────────────────────────────────┤ │
│ │ [✅ PERMITIR ENTRADA]  [❌ DENEGAR]│ │  ← 2 botones grandes
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Resultado (si denegado)             │
│ ┌─────────────────────────────────┐ │
│ │ [👤] JUAN PÉREZ                 │ │
│ │    Básico Mensual  |  0 días    │ │
│ │    🔴 MEMBRESÍA VENCIDA         │ │
│ │    Motivo: Pago pendiente       │ │
│ ├─────────────────────────────────┤ │
│ │ [❌ DENEGAR ACCESO]             │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### 10. Corte de Caja

```
┌─────────────────────────────────────┐
│ Toolbar: "Corte de Caja" [✕]        │
├─────────────────────────────────────┤
│ Resumen Financiero (Card Grid)      │
│ ┌─────────────┐ ┌─────────────┐     │
│ │ Ventas Tot. │ │  Efectivo   │     │
│ │ $1.250.000  │ │ $  450.000  │     │
│ └─────────────┘ └─────────────┘     │
│ ┌─────────────┐ ┌─────────────┐     │
│ │   Tarjeta   │ │ Transfer.   │     │
│ │ $  600.000  │ │ $  150.000  │     │
│ └─────────────┘ └─────────────┘     │
├─────────────────────────────────────┤
│ Arqueo Efectivo (Denominaciones)    │
│ ┌─────────────────────────────────┐ │
│ │ Denominación  Cant.  Subtotal   │ │
│ ├─────────────────────────────────┤ │
│ │ $1.000      [ 25 ]   $25.000    │ │  ← EditText cantidad
│ │ $  500      [ 35 ]   $17.500    │ │
│ │ $  200      [ 20 ]   $ 4.000    │ │
│ │ ...                              │ │
│ ├─────────────────────────────────┤ │
│ │ TOTAL ARQUEO:          $450.500 │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Comparación                         │
│ Sistema: $450.000  |  Físico: $450.500  |  Diff: +$500 ⚠️ │
├─────────────────────────────────────┤
│ Observaciones                       │
│ [________________________________]  │
├─────────────────────────────────────┤
│ [Finalizar Corte] [Cancelar]        │
└─────────────────────────────────────┘
```

---

## ♿ Accesibilidad (a11y)

### Checklist Obligatorio

| Criterio | Implementación | Test |
|----------|----------------|------|
| **Contraste Texto** | Mínimo 4.5:1 (AA) para body, 3:1 para large text | Color contrast analyzer |
| **Contraste UI** | 3:1 para bordes, iconos, estados focus | Manual + automated |
| **Touch Targets** | Mínimo 48x48dp (Material3 default) | Layout Inspector |
| **Content Descriptions** | Todos ImageView, IconButton, FAB, chips | TalkBack navigation |
| **Focus Order** | Logical top-to-bottom, left-to-right | Tab navigation |
| **Font Scaling** | `sp` units, max 200% escala, no truncado crítico | Settings > Display > Font size |
| **Touch Delegation** | `android:importantForAccessibility="no"` en decorativos | TalkBack |
| **Live Regions** | `android:accessibilityLiveRegion="polite"` en toasts, loading | TalkBack announcements |
| **Semantic Headings** | `android:accessibilityHeading="true"` en section titles | TalkBack rotor |
| **Switch Labels** | SwitchMaterial incluye label automático | TalkBack |

### Ejemplos Content Descriptions

```xml
<!-- FAB QR -->
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:contentDescription="Generar código QR de acceso al gimnasio"
    ... />

<!-- Chip Categoría -->
<com.google.android.material.chip.Chip
    android:contentDescription="Filtro categoría Suplementos, no seleccionado"
    ... />

<!-- Switch Notificación -->
<com.google.android.material.switchmaterial.SwitchMaterial
    android:text="Notificaciones de membresía"
    <!-- Label automático usado como contentDescription -->
    ... />

<!-- QR Code Image -->
<ImageView
    android:contentDescription="Código QR de acceso para Juan Pérez, expira en 45 segundos"
    ... />

<!-- Stat Card -->
<LinearLayout
    android:accessibilityHeading="true"
    android:contentDescription="Resumen de accesos: 247 total, 18 este mes, 12 días consecutivos"
    ... />
```

---

## 🌙 Dark Mode Implementation

### Forzar Dark Mode (App Gimnasio - Uso nocturno)

```xml
<!-- values/themes.xml -->
<style name="Theme.ApexGym" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Forzar dark para consistencia brand -->
    <item name="android:forceDarkAllowed">true</item>
</style>
```

```kotlin
// MainActivity.kt - AppCompatDelegate
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Forzar dark mode siempre
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        // ...
    }
}
```

### Colores Adaptativos (ColorStateList)

```xml
<!-- res/color/text_primary_selector.xml -->
<selector>
    <item android:color="?attr/colorOnSurface" android:state_enabled="true" />
    <item android:color="?attr/colorOnSurface" android:alpha="0.38" android:state_enabled="false" />
</selector>

<!-- res/color/icon_tint_selector.xml -->
<selector>
    <item android:color="?attr/colorPrimary" android:state_checked="true" />
    <item android:color="?attr/colorOnSurfaceVariant" />
</selector>
```

---

## 📱 Responsive Layouts

### Phone Portrait (< 600dp) - Default

- Bottom Navigation: 4 tabs visibles
- Grid Productos: 2 columnas
- Cards: margin 16dp, padding 20dp
- Bottom Sheets: 90% screen height max

### Phone Landscape / Tablet (600-840dp)

```xml
<!-- layout-w600dp/fragment_home.xml -->
<LinearLayout android:orientation="horizontal">
    <!-- Side Navigation Rail (izquierda) -->
    <NavigationRail
        android:layout_width="72dp"
        android:layout_height="match_parent"
        app:menu="@menu/nav_rail_menu" />
    
    <!-- Content Principal -->
    <FragmentContainerView
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1" />
</LinearLayout>
```

- Navigation Rail reemplaza Bottom Nav
- Grid Productos: 3-4 columnas
- Two-pane layouts en Master-Detail (Empleados, Visitas)

---

## 🎬 Motion & Animations

### Duraciones Estándar

| Tipo | Duración | Easing | Uso |
|------|----------|--------|-----|
| `fast` | 150ms | `FastOutSlowIn` | Ripples, hover, press |
| `medium` | 300ms | `FastOutSlowIn` | Navigation, tabs, sheets |
| `slow` | 500ms | `FastOutLinearIn` | Hero transitions, fullscreen modals |
| `extraSlow` | 800ms | `LinearOutSlowIn` | Onboarding, splash |

### Animaciones Clave

```kotlin
// 1. Bottom Nav Pill (300ms)
TransitionManager.beginDelayedTransition(container, AutoTransition().apply {
    duration = 300
    interpolator = FastOutSlowInInterpolator()
})

// 2. QR Modal Entry (300ms slide + fade)
val slideUp = TranslationY(screenHeight).also { duration = 300 }
val fadeIn = Alpha(0f, 1f).also { duration = 300 }
AnimatorSet().playTogether(slideUp, fadeIn).start()

// 3. Card Expand (CollapsingToolbar style)
val expand = ObjectAnimator.ofFloat(card, "translationY", 0f, -expandedHeight)
expand.duration = 400
expand.interpolator = FastOutSlowInInterpolator()

// 4. Shimmer Loading (1.5s loop)
val shimmer = ValueAnimator.ofFloat(0f, 1f).apply {
    duration = 1500
    repeatCount = ValueAnimator.INFINITE
    repeatMode = ValueAnimator.RESTART
    interpolator = LinearInterpolator()
}
```

### Reduced Motion (Respeto preferencias usuario)

```kotlin
// En BaseFragment/Activity
private fun isReduceMotionEnabled(): Boolean {
    return (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
        .isReduceMotionEnabled
}

// Aplicar
if (!isReduceMotionEnabled()) {
    animatePillToTab(tabIndex)
} else {
    pill.translationX = targetX
    pill.layoutParams.width = targetWidth
}
```

---

## 🧪 UI Testing Checklist

### Device Matrix (Mínimo)

| Device | API | Screen | Density | Orientación |
|--------|-----|--------|---------|-------------|
| Pixel 6a | 34 | 6.1" | 440 dpi | Portrait |
| Pixel 7 Pro | 34 | 6.7" | 480 dpi | Portrait + Landscape |
| Galaxy A54 | 33 | 6.4" | 400 dpi | Portrait |
| Tablet (Genérico) | 31 | 10.1" | 240 dpi | Landscape |
| Foldable (Genérico) | 34 | 7.6" | 380 dpi | Both |

### Test Cases Críticos

| Screen | Test Case | Expected |
|--------|-----------|----------|
| Login | Email inválido | Error inline + stroke rojo |
| Login | Password vacío | Error inline |
| Login | Credenciales incorrectas | Snackbar "Credenciales inválidas" |
| Home | Swipe tabs | Pill animation suave 300ms |
| Home | Click FAB QR | Modal fullscreen slide up |
| QR Modal | Timer 45s | Countdown visual + regenerar |
| Tienda | Search + filter | Resultados filtrados tiempo real |
| Tienda | Agregar al carrito | Badge carrito actualizado |
| Carrito | Modificar cantidad | Total recalculado instantáneo |
| Checkout | Pago mixto válido | Confirmación + ticket |
| Notificaciones | Master OFF | Todos switches deshabilitados |
| Notificaciones | No Molestar ON | Push silenciados en horario |
| Acceso Manual | DNI válido + activa | Botón verde "PERMITIR" |
| Acceso Manual | DNI válido + vencida | Botón rojo "DENEGAR" + motivo |
| Empleados | Filtro ACTIVO | Solo activos en lista |
| Corte Caja | Arqueo = Sistema | Diff $0, botón habilitado |

---

## 📦 Assets y Recursos

### Iconografía

| Set | Fuente | Formato | Tamaños |
|-----|--------|---------|---------|
| System Icons | Material Symbols (Outlined) | XML Vector | 24dp default |
| Brand Icons | Custom (Figma) | SVG → XML | 24dp, 32dp, 48dp |
| Illustrations | Custom (undraw.co style) | SVG → XML | 80dp, 120dp, 200dp |

### Imágenes

| Tipo | Formato | Compresión | Naming |
|------|---------|------------|--------|
| Productos | WebP (lossless) | 85% quality | `prod_{id}_{variant}.webp` |
| Servicios | WebP | 80% | `serv_{id}.webp` |
| Avatars | WebP | 75% | `avatar_{userId}.webp` |
| Placeholders | Vector (XML) | N/A | `ic_placeholder_{type}.xml` |

### Loading Placeholders (Shimmer)

```xml
<!-- res/layout/item_product_shimmer.xml -->
<com.google.android.material.card.MaterialCardView ...>
    <LinearLayout android:orientation="vertical" android:padding="12dp">
        <View android:layout_width="match_parent" android:layout_height="160dp"
            android:background="@drawable/shimmer_bg" />
        <Space android:layout_height="8dp" />
        <View android:layout_width="60%" android:layout_height="16dp"
            android:background="@drawable/shimmer_bg" />
        <Space android:layout_height="4dp" />
        <View android:layout_width="40%" android:layout_height="12dp"
            android:background="@drawable/shimmer_bg" />
        <Space android:layout_height="8dp" />
        <View android:layout_width="80dp" android:layout_height="20dp"
            android:background="@drawable/shimmer_bg" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

```xml
<!-- res/drawable/shimmer_bg.xml -->
<shape>
    <gradient
        android:startColor="#3A3A44"
        android:centerColor="#4A4A54"
        android:endColor="#3A3A44"
        android:angle="0"
        android:type="linear" />
    <corners android:radius="8dp" />
</shape>
```

---

## 📋 Design Tokens Export (Para Handoff)

### Figma/Design System Tokens

```json
{
  "color": {
    "primary": { "value": "#FF6B00", "type": "color" },
    "primaryContainer": { "value": "#FFF3E0", "type": "color" },
    "surface": { "value": "#161618", "type": "color" },
    "surfaceVariant": { "value": "#1F1F23", "type": "color" },
    "background": { "value": "#0F0F1A", "type": "color" },
    "onSurface": { "value": "#FFFFFF", "type": "color" },
    "onSurfaceVariant": { "value": "#A6AAB4", "type": "color" },
    "outline": { "value": "#3A3A44", "type": "color" },
    "success": { "value": "#4CAF50", "type": "color" },
    "warning": { "value": "#FFC107", "type": "color" },
    "error": { "value": "#F44336", "type": "color" }
  },
  "spacing": {
    "1": { "value": "4dp", "type": "dimension" },
    "2": { "value": "8dp", "type": "dimension" },
    "3": { "value": "12dp", "type": "dimension" },
    "4": { "value": "16dp", "type": "dimension" },
    "5": { "value": "20dp", "type": "dimension" },
    "6": { "value": "24dp", "type": "dimension" },
    "8": { "value": "32dp", "type": "dimension" }
  },
  "typography": {
    "displaySmall": { "fontSize": "36sp", "fontWeight": "400", "lineHeight": "44sp" },
    "headlineLarge": { "fontSize": "32sp", "fontWeight": "400", "lineHeight": "40sp" },
    "headlineMedium": { "fontSize": "28sp", "fontWeight": "400", "lineHeight": "36sp" },
    "headlineSmall": { "fontSize": "24sp", "fontWeight": "400", "lineHeight": "32sp" },
    "titleLarge": { "fontSize": "22sp", "fontWeight": "500", "lineHeight": "28sp" },
    "titleMedium": { "fontSize": "16sp", "fontWeight": "500", "lineHeight": "24sp" },
    "titleSmall": { "fontSize": "14sp", "fontWeight": "500", "lineHeight": "20sp" },
    "bodyLarge": { "fontSize": "16sp", "fontWeight": "400", "lineHeight": "24sp" },
    "bodyMedium": { "fontSize": "14sp", "fontWeight": "400", "lineHeight": "20sp" },
    "bodySmall": { "fontSize": "12sp", "fontWeight": "400", "lineHeight": "16sp" },
    "labelLarge": { "fontSize": "14sp", "fontWeight": "500", "lineHeight": "20sp" },
    "labelMedium": { "fontSize": "12sp", "fontWeight": "500", "lineHeight": "16sp" },
    "labelSmall": { "fontSize": "11sp", "fontWeight": "500", "lineHeight": "16sp" }
  },
  "borderRadius": {
    "small": { "value": "8dp", "type": "dimension" },
    "medium": { "value": "16dp", "type": "dimension" },
    "large": { "value": "24dp", "type": "dimension" },
    "full": { "value": "9999dp", "type": "dimension" }
  },
  "elevation": {
    "level0": { "value": "0dp", "type": "dimension" },
    "level1": { "value": "2dp", "type": "dimension" },
    "level2": { "value": "4dp", "type": "dimension" },
    "level3": { "value": "8dp", "type": "dimension" },
    "level4": { "value": "16dp", "type": "dimension" }
  }
}
```

---

## 📝 Changelog UI/UX

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0.0 | 2025-11-05 | Spec inicial: Design System, 10 pantallas clave, Staff screens, a11y, motion, responsive, tokens |
| 1.1.0 | TBD | Onboarding flow, Splash, Empty states completos, Tablet layouts, Foldable support |
| 2.0.0 | TBD | Design System v2: New brand, Motion 2.0, Compose migration |