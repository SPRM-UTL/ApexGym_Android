# ApexGym Android - Technical Architecture

## 🏛️ Visión General de la Arquitectura

ApexGym sigue una **arquitectura por capas (Layered Architecture)** con separación clara entre Presentation, Domain y Data, evolucionando hacia **Clean Architecture** con inyección de dependencias (Hilt).

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │   Fragments     │  │  ViewModels     │  │  Adapters/      │     │
│  │ (ViewBinding)   │──│ (StateFlow/     │  │  ViewHolders    │     │
│  │                 │  │  LiveData)      │  │                 │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │              │
│           ▼                    ▼                    ▼              │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │              Navigation Controller                          │  │
│  │  (NavController + Manual FragmentTransaction for Sheets)   │  │
│  └─────────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬──────────────────────────────────┘
                                 │ Use Cases / Repository Interfaces
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │   Use Cases     │  │  Repository     │  │  Domain Models  │     │
│  │ (Interactors)   │──│  Interfaces     │  │  (Entities)     │     │
│  │                 │  │                 │  │                 │     │
│  └─────────────────┘  └────────┬────────┘  └─────────────────┘     │
│                                │                                    │
└────────────────────────────────┼────────────────────────────────────┘
                                 │ Implementation
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │  Repository     │  │  Data Sources   │  │  Data Models    │     │
│  │  Implementations│──│                 │  │  (DTOs/Entities)│     │
│  │                 │  │  • Remote       │  │                 │     │
│  └─────────────────┘  │  • Local (Room) │  └─────────────────┘     │
│                       │  • Cache        │                          │
│                       └─────────────────┘                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Módulos y Paquetes

### Estructura Actual (Requiere Refactor)

```
app/
├── com.sprm.apexgym/          # Cliente (Principal)
│   ├── core/
│   │   ├── db/
│   │   │   └── models/        # Room Entities: Usuario, EstadoCliente
│   │   ├── network/
│   │   │   ├── api/           # AuthApiService (Interface)
│   │   │   └── client/        # RetrofitClient, SessionCookieJar, DTOs
│   │   └── di/                # ❌ Pendiente: Hilt Modules
│   └── feature/
│       ├── auth/              # Login, Registro (3 pasos)
│       └── main/              # Home, MainActivity, BottomSheets
│
└── com.apexgym/               # Staff (Separado - Unificar)
    └── feature/
        ├── empleados/         # 8 Fragments
        ├── visitas/           # 4 Fragments
        └── notificaciones/    # 3 Fragments
```

### Estructura Objetivo (Clean Architecture)

```
app/
├── core/
│   ├── common/                # Constants, Extensions, Utils
│   ├── data/
│   │   ├── local/             # Room DAO, Database, Entities
│   │   ├── remote/            # Retrofit Service, DTOs, Interceptors
│   │   ├── repository/        # Repository Implementations
│   │   └── mapper/            # DTO ↔ Domain Mappers
│   ├── domain/
│   │   ├── model/             # Domain Entities
│   │   ├── repository/        # Repository Interfaces
│   │   └── usecase/           # Use Cases (Interactors)
│   └── di/                    # Hilt Modules (App, Network, Database, Feature)
│
├── feature/
│   ├── auth/                  # Login, Register, Verification
│   │   ├── data/              # AuthRepositoryImpl, AuthDataSource
│   │   ├── domain/            # LoginUseCase, RegisterUseCase
│   │   └── presentation/      # LoginFragment, LoginViewModel
│   ├── home/                  # HomeFragment (4 tabs)
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/      # HomeFragment, HomeViewModel, Adapters
│   ├── membership/            # Membresía, Planes, Cambios
│   ├── store/                 # Tienda, Productos, Servicios, Carrito
│   ├── profile/               # Perfil, Config, Notificaciones
│   ├── staff/
│   │   ├── empleados/         # 7 sub-features
│   │   ├── visitas/           # CRM Prospectos
│   │   ├── caja/              # Caja, Cortes, Movimientos
│   │   └── accesos/           # Control acceso manual
│   └── notifications/         # Push, Config, Historial
│
└── app/                       # Application Class, Navigation Graph
```

---

## 🔄 Flujo de Datos (Data Flow)

### Patrón Unidireccional (MVI-inspired)

```
┌─────────────┐     Intent/Event      ┌─────────────┐
│   Fragment  │ ────────────────────▶ │  ViewModel  │
│   (UI)      │                       │             │
└─────────────┘                       └──────┬──────┘
                                             │
                                             ▼ Use Case
                                    ┌────────────────┐
                                    │  Repository    │
                                    │  Interface     │
                                    └───────┬────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    ▼                       ▼                       ▼
            ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
            │   Remote    │         │    Local    │         │   Cache     │
            │  DataSource │         │  DataSource │         │  DataSource │
            │  (Retrofit) │         │   (Room)    │         │  (LruCache) │
            └──────┬──────┘         └──────┬──────┘         └──────┬──────┘
                   │                       │                       │
                   └───────────────────────┼───────────────────────┘
                                           ▼
                                    ┌────────────────┐
                                    │  Mapper        │
                                    │  DTO → Domain  │
                                    └───────┬────────┘
                                            │
                                            ▼ Result (StateFlow)
                                    ┌────────────────┐
                                    │   ViewModel    │
                                    │  _uiState      │
                                    └───────┬────────┘
                                            │ State
                                            ▼
                                    ┌────────────────┐
                                    │   Fragment     │
                                    │  collect()     │
                                    └────────────────┘
```

### Estados de UI (Sealed Class Pattern)

```kotlin
// core/common/ui/UiState.kt
sealed interface UiState<out T> {
    data class Success<T>(val data: T) : UiState<T>
    data class Loading<T>(val message: String? = null) : UiState<T>
    data class Error<T>(val message: String, val throwable: Throwable? = null) : UiState<T>
    data class Empty<T>(val message: String) : UiState<T>
}

// Uso en ViewModel
val uiState: MutableStateFlow<UiState<HomeData>> = MutableStateFlow(UiState.Loading())
```

---

## 🌐 Capa de Red (Network Layer)

### RetrofitClient (Singleton Actual)

```kotlin
// core/network/client/RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.2:3000/"
    
    val apiService: AuthApiService by lazy {
        OkHttpClient.Builder()
            .cookieJar(SessionCookieJar())           // Cookies para sesión
            .addInterceptor(LoggingInterceptor())    // Logs BODY/HEADERS
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            .let { client ->
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthApiService::class.java)
            }
    }
}
```

### SessionCookieJar (Persistencia Sesión)

```kotlin
class SessionCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<HttpUrl, List<Cookie>>()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url] = cookies
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url] ?: emptyList()
    }
}
```

### Endpoints Definidos (AuthApiService)

| Método | Endpoint | Descripción | Response |
|--------|----------|-------------|----------|
| POST | `/api/clientes/verificarCredenciales` | Login cliente | `ApiResponseLogin` |
| POST | `/api/clientes/registrar` | Registro paso 1 | `ApiResponseRegistro` |
| POST | `/api/clientes/verificarCorreo` | Verificar email | `ApiResponseVerificarCorreo` |
| POST | `/api/usuarios/logout` | Cerrar sesión | `ApiResponseLogout` |
| GET | `/api/productos/productosClientes/traer` | Catálogo productos | `ApiResponseProductos` |
| GET | `/api/servicios/serviciosClientes/traer` | Catálogo servicios | `ApiResponseServicios` |
| GET | `/api/tipos-membresia/membresiasCliente` | Planes disponibles | `ApiResponseTiposMembresia` |
| GET | `/api/clientes-membresias/mi-membresia` | Membresía activa | `ApiResponseMembresiaCliente` |
| GET | `/api/clientes/perfil` | Perfil usuario | `ApiResponsePerfil` |
| POST | `/api/clientes/actualizarPerfil` | Actualizar perfil | `ApiResponseActualizarPerfil` |
| POST | `/api/clientes/actualizarContacto` | Actualizar contacto | `ApiResponseActualizarContacto` |

### DTOs Principales (25+ en RetrofitClient.kt)

```kotlin
// Modelos de respuesta unificados
data class ApiResponseBase(
    val responseFlag: Int,      // 0 = éxito, 1 = error
    val message: String?        // Mensaje legible
)

data class ApiResponseLogin(
    override val responseFlag: Int,
    override val message: String?,
    val data: LoginData?        // Usuario, token, membresía
) : ApiResponseBase(responseFlag, message)

data class LoginData(
    val idCliente: Int,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val membresia: MembresiaCliente?
)

// ... 20+ DTOs más para Productos, Servicios, Membresías, Notificaciones, etc.
```

---

## 💾 Capa de Datos Local (Room Database)

### Configuración Actual

```kotlin
// core/db/AppDatabase.kt
@Database(entities = [Usuario::class, EstadoCliente::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun estadoClienteDao(): EstadoClienteDao
}
```

### Entidades

```kotlin
// core/db/models/Usuario.kt
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: Int,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val token: String?,           // Session token
    val fechaRegistro: Long,
    val ultimoAcceso: Long
)

// core/db/models/EstadoCliente.kt
@Entity(tableName = "estado_cliente")
data class EstadoCliente(
    @PrimaryKey val idCliente: Int,
    val membresiaActiva: Boolean,
    val diasRestantes: Int,
    val planActual: String?,
    val fechaVencimiento: Long,
    val ultimaSincronizacion: Long
)
```

### DAOs

```kotlin
@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)
    
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuarioById(id: Int): Usuario?
    
    @Query("DELETE FROM usuarios")
    suspend fun clearAll()
}

@Dao
interface EstadoClienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEstado(estado: EstadoCliente)
    
    @Query("SELECT * FROM estado_cliente WHERE idCliente = :id")
    suspend fun getEstadoByClienteId(id: Int): EstadoCliente?
}
```

---

## 🧭 Navegación

### Actual: FragmentTransaction Manual + BottomSheets

```kotlin
// MainActivity.kt - Navegación principal
supportFragmentManager.beginTransaction()
    .replace(R.id.fragment_container, HomeFragment())
    .commit()

// BottomSheets (no van al back stack)
NotificationsBottomSheetFragment().show(
    childFragmentManager, 
    "notifications_sheet"
)
```

### Objetivo: Navigation Component (NavGraph)

```xml
<!-- res/navigation/nav_graph.xml -->
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    app:startDestination="@id/loginFragment">
    
    <fragment
        android:id="@+id/loginFragment"
        android:name="com.sprm.apexgym.feature.auth.LoginFragment">
        <action android:id="@+id/action_login_to_home"
            app:destination="@id/homeFragment" />
        <action android:id="@+id/action_login_to_register"
            app:destination="@id/registerFragment" />
    </fragment>
    
    <fragment
        android:id="@+id/homeFragment"
        android:name="com.sprm.apexgym.feature.home.HomeFragment">
        <!-- BottomSheets como destinations -->
        <action android:id="@+id/action_home_to_notifications"
            app:destination="@id/notificationsBottomSheet" />
        <action android:id="@+id/action_home_to_profile"
            app:destination="@id/profileBottomSheet" />
        <action android:id="@+id/action_home_to_productDetail"
            app:destination="@id/productDetailBottomSheet" />
    </fragment>
    
    <!-- Dialog Destinations para BottomSheets -->
    <dialog
        android:id="@+id/notificationsBottomSheet"
        android:name="com.sprm.apexgym.feature.home.NotificationsBottomSheetFragment" />
    <dialog
        android:id="@+id/profileBottomSheet"
        android:name="com.sprm.apexgym.feature.profile.ProfileBottomSheetFragment" />
    
    <!-- Staff Navigation (Nested Graph) -->
    <include app:graph="@navigation/nav_graph_staff" />
</navigation>
```

---

## ⚙️ Inyección de Dependencias (Hilt - Pendiente)

### Módulos Planificados

```kotlin
// core/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "apexgym.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideUsuarioDao(database: AppDatabase): UsuarioDao = database.usuarioDao()
    
    @Provides
    fun provideEstadoClienteDao(database: AppDatabase): EstadoClienteDao = database.estadoClienteDao()
}

// core/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(SessionCookieJar())
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = 
        retrofit.create(AuthApiService::class.java)
}

// core/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    
    @Binds
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
    
    // ... más repositories
}
```

### Anotaciones en Clases

```kotlin
// Application Class
@HiltAndroidApp
class ApexGymApplication : Application()

// Activities
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

// Fragments
@AndroidEntryPoint
class HomeFragment : Fragment()

// ViewModels
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val getMembershipUseCase: GetMembershipUseCase
) : ViewModel()
```

---

## 🎨 Sistema de Diseño (Material3 Theming)

### Tema Base (themes.xml)

```xml
<!-- values/themes.xml -->
<style name="Theme.ApexGym" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Colores principales -->
    <item name="colorPrimary">@color/apex_orange</item>
    <item name="colorPrimaryContainer">@color/apex_orange_light</item>
    <item name="colorOnPrimary">@color/white</item>
    <item name="colorOnPrimaryContainer">@color/apex_text_primary</item>
    
    <!-- Surface / Cards -->
    <item name="colorSurface">@color/apex_card_dark</item>
    <item name="colorSurfaceVariant">@color/apex_card_dark_elevated</item>
    <item name="colorOnSurface">@color/apex_text_primary</item>
    <item name="colorOnSurfaceVariant">@color/apex_text_on_dark_secondary</item>
    
    <!-- Background -->
    <item name="colorBackground">@color/apex_bg_screen</item>
    <item name="colorOnBackground">@color/apex_text_primary</item>
    
    <!-- Outline / Border -->
    <item name="colorOutline">@color/apex_stroke_light</item>
    
    <!-- Shapes -->
    <item name="shapeAppearanceCornerSmall">@style/ShapeAppearance.App.Small</item>
    <item name="shapeAppearanceCornerMedium">@style/ShapeAppearance.App.Medium</item>
    <item name="shapeAppearanceCornerLarge">@style/ShapeAppearance.App.Large</item>
    
    <!-- Typography -->
    <item name="textAppearanceHeadlineLarge">@style/TextAppearance.App.HeadlineLarge</item>
    <item name="textAppearanceTitleLarge">@style/TextAppearance.App.TitleLarge</item>
    <item name="textAppearanceBodyLarge">@style/TextAppearance.App.BodyLarge</item>
    <item name="textAppearanceLabelLarge">@style/TextAppearance.App.LabelLarge</item>
</style>

<!-- Shapes personalizados -->
<style name="ShapeAppearance.App.Small" parent="ShapeAppearance.Material3.Corner.Small">
    <item name="cornerSize">8dp</item>
</style>
<style name="ShapeAppearance.App.Medium" parent="ShapeAppearance.Material3.Corner.Medium">
    <item name="cornerSize">16dp</item>
</style>
<style name="ShapeAppearance.App.Large" parent="ShapeAppearance.Material3.Corner.Large">
    <item name="cornerSize">24dp</item>
</style>
```

### Colores (colors.xml - Extracto)

```xml
<resources>
    <!-- Brand -->
    <color name="apex_orange">#FFF26513</color>
    <color name="apex_orange_light">#FFFF7A29</color>
    <color name="apex_orange_translucent">#26F26513</color>
    
    <!-- Backgrounds -->
    <color name="apex_bg_screen">#FFF5F6F8</color>
    <color name="apex_card_dark">#FF161618</color>
    <color name="apex_card_dark_elevated">#FF1F1F23</color>
    
    <!-- Text -->
    <color name="apex_text_primary">#FF121214</color>
    <color name="apex_text_secondary">#FF7A8194</color>
    <color name="apex_text_on_dark_secondary">#FFA6AAB4</color>
    <color name="white">#FFFFFFFF</color>
    
    <!-- Borders / Dividers -->
    <color name="apex_stroke_light">#FFE4E6EB</color>
    <color name="apex_nav_inactive">#FF9E9EA6</color>
    
    <!-- Semantic -->
    <color name="success_green">#FF4CAF50</color>
    <color name="error_red">#FFF44336</color>
    <color name="warning_amber">#FFFFC107</color>
    <color name="info_blue">#FF1976D2</color>
</resources>
```

---

## 🔧 Configuración Build (Gradle)

### build.gradle.kts (Root)

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.14" apply false
    id("com.google.dagger.hilt") version "2.51.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### app/build.gradle.kts

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt")
    id("kotlinx-serialization")
}

android {
    namespace = "com.sprm.apexgym"
    compileSdk = 35  // Updated from 34 (37.1 was preview)
    
    defaultConfig {
        applicationId = "com.sprm.apexgym"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = false  // Solo ViewBinding
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    
    packagingOptions {
        resources.excludes += "META-INF/*.kotlin_module"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Material3
    implementation("com.google.android.material:material:1.14.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Coroutines + Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Serialization (para DTOs futuros)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Image Loading (Coil - pendiente)
    // implementation("io.coil-kt:coil:2.5.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.51.1")
}
```

### libs.versions.toml (Version Catalog - Recomendado)

```toml
# gradle/libs.versions.toml
[versions]
agp = "9.2.1"
kotlin = "1.9.22"
ksp = "1.9.22-1.0.14"
compileSdk = 35
minSdk = 28
targetSdk = 35
coreKtx = "1.12.0"
lifecycle = "2.7.0"
activity = "1.8.2"
fragment = "1.6.2"
material = "1.14.0"
navigation = "2.7.6"
room = "2.6.1"
retrofit = "2.9.0"
okhttp = "4.11.0"
hilt = "2.51.1"
coroutines = "1.7.3"
serialization = "1.6.3"
junit = "4.13.2"
mockito = "5.11.0"
espresso = "3.5.1"
coil = "2.5.0"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-ktx = { module = "androidx.activity:activity-ktx", version.ref = "activity" }
androidx-fragment-ktx = { module = "androidx.fragment:fragment-ktx", version.ref = "fragment" }
material3 = { module = "com.google.android.material:material", version.ref = "material" }
navigation-fragment-ktx = { module = "androidx.navigation:navigation-fragment-ktx", version.ref = "navigation" }
navigation-ui-ktx = { module = "androidx.navigation:navigation-ui-ktx", version.ref = "navigation" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-fragment = { module = "androidx.hilt:hilt-navigation-fragment", version.ref = "hilt" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
coil = { module = "io.coil-kt:coil", version.ref = "coil" }
junit = { module = "junit:junit", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockito" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt", version.ref = "hilt" }
kotlin-serialization = { id = "kotlinx-serialization" }
```

---

## 🔐 Seguridad

### Network Security Config

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.1.2</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>  <!-- Emulador -->
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

### AndroidManifest.xml (Extracto)

```xml
<application
    android:name=".ApexGymApplication"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:networkSecurityConfig="@xml/network_security_config"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.ApexGym"
    tools:targetApi="31">
    
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:launchMode="singleTop"
        android:theme="@style/Theme.ApexGym.NoActionBar"
        android:windowSoftInputMode="adjustResize">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />  <!-- QR Scanner -->
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
</application>
```

---

## ⚡ Rendimiento y Optimizaciones

### LruCache para Imágenes (HomeFragment actual)

```kotlin
private val imageCache = object : LruCache<String, Bitmap>(40) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount / 1024  // KB
    }
}

private fun loadImage(url: String, imageView: ImageView) {
    imageCache.get(url)?.let { imageView.setImageBitmap(it) } ?: run {
        // Cargar desde red + cache
        lifecycleScope.launch { /* ... */ }
    }
}
```

### Optimizaciones Pendientes

| Optimización | Estado | Prioridad |
|--------------|--------|-----------|
| Coil/Glide para imágenes | ❌ No implementado | 🟡 Alta |
| Paging 3 para listas grandes | ❌ No implementado | 🟡 Media |
| WorkManager para sync background | ❌ No implementado | 🟡 Media |
| Baseline Profiles | ❌ No configurado | 🟢 Baja |
| R8/ProGuard optimizado | ✅ Configurado básico | 🟢 Baja |
| App Bundle (AAB) | ✅ Soportado | 🟢 Baja |

---

## 📱 Compatibilidad y Versiones

| Componente | Versión Mínima | Versión Objetivo | Notas |
|------------|----------------|------------------|-------|
| Android SDK | API 28 (Android 9) | API 35 (Android 15) | 95%+ dispositivos activos |
| Kotlin | 1.9.22 | 1.9.22 | LTS |
| AGP | 9.2.1 | 9.2.1 | Stable |
| Gradle | 8.7+ | 8.7+ | Compatible con AGP 9.2 |
| Java | 17 | 17 | Requerido por AGP 9.x |
| Material3 | 1.14.0 | 1.14.0 | Latest stable |
| Room | 2.6.1 | 2.6.1 | KSP support |

---

## 🚀 Próximos Pasos Arquitectura

1. **Migrar a libs.versions.toml** - Centralizar versiones
2. **Implementar Hilt DI** - 4 módulos (App, Network, Database, Features)
3. **Crear Repository Layer** - Interfaces + Implementaciones
4. **Implementar Use Cases** - Casos de uso por feature
5. **Migrar a Navigation Component** - nav_graph.xml + Deep Links
6. **Unificar paquetes** - `com.sprm.apexgym` everywhere
7. **Implementar ViewModels** - StateFlow + UiState sealed class
8. **Agregar Coil** - Image loading con cache automático
9. **Configurar Baseline Profiles** - Startup performance
10. **Documentar decisiones arquitectónicas** - ADR (Architecture Decision Records)