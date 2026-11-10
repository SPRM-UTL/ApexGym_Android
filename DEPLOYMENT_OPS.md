# ApexGym Android - Deployment & Operations

## 🚀 Visión General de Despliegue

**Estrategia**: Continuous Delivery con Feature Flags, Staged Rollouts y Rollback Automático
**Artefacto**: Android App Bundle (AAB) → Google Play Console
**Entornos**: Development → Staging → Production (Internal → Closed → Open → Production)

---

## 🏗️ Build Configuration

### Build Variants

```kotlin
// app/build.gradle.kts
android {
    // ...
    
    flavorDimensions += "environment"
    
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "ApexGym DEV")
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.2:3000/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
        }
        
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            resValue("string", "app_name", "ApexGym STAGING")
            buildConfigField("String", "BASE_URL", "\"https://staging-api.apexgym.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
        }
        
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "ApexGym")
            buildConfigField("String", "BASE_URL", "\"https://api.apexgym.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
        }
    }
    
    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            matchingFallbacks = listOf("dev")
        }
        
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks = listOf("prod")
        }
    }
}
```

### Signing Config

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            val keystoreFile = File("keystore/release.keystore")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("KEY_ALIAS") ?: ""
            val keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
        
        create("debug") {
            storeFile = File("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}
```

### ProGuard / R8 Rules

```proguard
# proguard-rules.pro

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Keep Room entities and DAOs
-keep class com.sprm.apexgym.core.db.** { *; }

# Keep Retrofit/Gson models
-keep class com.sprm.apexgym.core.network.client.** { *; }
-keep class com.sprm.apexgym.core.network.api.** { *; }

# Keep Parcelable/Serializable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep Enum values
-keepclassmembers enum * {
    **[] $VALUES;
    public *;
}

# Keep ViewBinding
-keep class **_ViewBinding { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.crashlytics.** { *; }

# Network Security Config
-keep class com.sprm.apexgym.network.** { *; }
```

---

## 🔄 CI/CD Pipeline (GitHub Actions)

### Workflow Principal

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop, release/*]
  pull_request:
    branches: [main, develop]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deploy environment'
        required: true
        type: choice
        options: [staging, production]

env:
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g -Dorg.gradle.parallel=true"
  JAVA_VERSION: "17"

jobs:
  # ============================================================
  # STAGE 1: CODE QUALITY
  # ============================================================
  code-quality:
    name: Code Quality
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
        with:
          api-level: 35
          ndk-version: r26c
          build-tools-version: '35.0.0'
      
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
      
      - name: Run Detekt
        run: ./gradlew detekt
      
      - name: Run Ktlint
        run: ./gradlew ktlintCheck
      
      - name: Run Lint
        run: ./gradlew lintDebug
      
      - name: Upload Lint Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lint-report
          path: app/build/reports/lint-results.html

  # ============================================================
  # STAGE 2: UNIT & INTEGRATION TESTS
  # ============================================================
  unit-tests:
    name: Unit & Integration Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20
    needs: code-quality
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'
      
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --info
      
      - name: Generate Jacoco Report
        run: ./gradlew jacocoTestReport
      
      - name: Check Coverage
        run: |
          COVERAGE=$(xmlstarlet sel -t -v "//report/counter[@type='INSTRUCTION']/@covered" app/build/reports/jacoco/testDebugUnitTest.xml)
          MISSED=$(xmlstarlet sel -t -v "//report/counter[@type='INSTRUCTION']/@missed" app/build/reports/jacoco/testDebugUnitTest.xml)
          TOTAL=$((COVERAGE + MISSED))
          PERCENT=$((COVERAGE * 100 / TOTAL))
          echo "Coverage: ${PERCENT}%"
          if [ $PERCENT -lt 80 ]; then
            echo "Coverage ${PERCENT}% below 80% threshold"
            exit 1
          fi
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          flags: unit
          fail_ci_if_error: true

  # ============================================================
  # STAGE 3: BUILD
  # ============================================================
  build:
    name: Build AAB
    runs-on: ubuntu-latest
    timeout-minutes: 30
    needs: unit-tests
    strategy:
      matrix:
        flavor: [dev, staging, prod]
        buildType: [debug, release]
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'
      
      - name: Build AAB
        run: ./gradlew :app:bundle${{ matrix.flavor }}${{
          matrix.buildType == 'release' && 'Release' || 'Debug' }}
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      
      - name: Upload AAB
        uses: actions/upload-artifact@v4
        with:
          name: app-${{ matrix.flavor }}-${{ matrix.buildType }}
          path: app/build/outputs/bundle/${{ matrix.flavor }}${{
            matrix.buildType == 'release' && 'Release' || 'Debug' }}/*.aab

  # ============================================================
  # STAGE 4: UI TESTS (Device Farm)
  # ============================================================
  ui-tests:
    name: UI Tests (Device Farm)
    runs-on: macos-latest
    timeout-minutes: 45
    needs: build
    if: github.event_name == 'push' || github.event_name == 'workflow_dispatch'
    strategy:
      matrix:
        api-level: [28, 30, 33, 34]
        device: [pixel_6a, galaxy_s23]
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'
      
      - name: Download AAB
        uses: actions/download-artifact@v4
        with:
          name: app-staging-debug
          path: artifacts/
      
      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
          script: ./gradlew connectedStagingDebugAndroidTest
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: ui-test-results-${{ matrix.api-level }}-${{ matrix.device }}
          path: app/build/reports/androidTests/connected/

  # ============================================================
  # STAGE 5: DEPLOY TO PLAY CONSOLE
  # ============================================================
  deploy-staging:
    name: Deploy to Play Console (Internal Testing)
    runs-on: ubuntu-latest
    timeout-minutes: 20
    needs: [build, ui-tests]
    if: github.ref == 'refs/heads/develop' || github.event.inputs.environment == 'staging'
    environment: staging
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Download AAB
        uses: actions/download-artifact@v4
        with:
          name: app-staging-release
          path: artifacts/
      
      - name: Deploy to Play Console Internal Testing
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT }}
          packageName: com.sprm.apexgym.staging
          releaseFiles: artifacts/app-staging-release.aab
          track: internal
          status: completed
          inAppUpdatePriority: 2
      
      - name: Notify Team
        uses: slackapi/slack-github-action@v1.23.0
        with:
          channel-id: ${{ secrets.SLACK_CHANNEL }}
          slack-message: "🚀 ApexGym Staging deployed to Internal Testing track"

  deploy-production:
    name: Deploy to Play Console (Production)
    runs-on: ubuntu-latest
    timeout-minutes: 30
    needs: [build, ui-tests]
    if: github.event_name == 'workflow_dispatch' && github.event.inputs.environment == 'production'
    environment: production
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Download AAB
        uses: actions/download-artifact@v4
        with:
          name: app-prod-release
          path: artifacts/
      
      - name: Deploy to Play Console Production (Staged Rollout)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT }}
          packageName: com.sprm.apexgym
          releaseFiles: artifacts/app-prod-release.aab
          track: production
          userFraction: 0.10  # 10% rollout
          status: completed
          inAppUpdatePriority: 5
          changesNotSentForReview: true
      
      - name: Notify Team
        uses: slackapi/slack-github-action@v1.23.0
        with:
          channel-id: ${{ secrets.SLACK_CHANNEL }}
          slack-message: "🎉 ApexGym v${{ github.ref_name }} deployed to Production (10% rollout)"

  # ============================================================
  # STAGE 6: POST-DEPLOY MONITORING
  # ============================================================
  post-deploy-monitoring:
    name: Post-Deploy Monitoring
    runs-on: ubuntu-latest
    timeout-minutes: 60
    needs: deploy-production
    if: github.event_name == 'workflow_dispatch' && github.event.inputs.environment == 'production'
    steps:
      - name: Wait for Rollout
        run: sleep 3600  # Wait 1 hour for initial metrics
      
      - name: Check Crashlytics
        run: |
          # Query Crashlytics API for crash-free sessions
          # Alert if crash-free sessions < 99.5%
          echo "Checking Crashlytics metrics..."
      
      - name: Check Play Console Metrics
        run: |
          # Query Play Console for ANR rate, crash rate
          # Alert if ANR > 0.47% or Crash > 1%
          echo "Checking Play Console vitals..."
      
      - name: Auto Rollback Decision
        run: |
          # If metrics degraded, halt rollout
          echo "Evaluating rollout health..."
      
      - name: Complete Rollout
        if: success()
        run: |
          # Increase to 100% after 24h if healthy
          echo "Rollout healthy, ready for 100%"
```

---

## 📦 Release Management

### Versioning Strategy (SemVer + Build Number)

```
Formato: MAJOR.MINOR.PATCH-BUILD+FLAVOR

Ejemplos:
  1.0.0-1+prod      # Primera release producción
  1.0.1-2+prod      # Hotfix producción
  1.1.0-3+staging   # Nueva feature en staging
  1.1.0-4+dev       # Desarrollo activo
```

### Version Code Calculation

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = calculateVersionCode()
        versionName = calculateVersionName()
    }
}

fun calculateVersionCode(): Int {
    // MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + BUILD
    val major = 1
    val minor = 0
    val patch = 0
    val build = System.getenv("BUILD_NUMBER")?.toInt() ?: 1
    return major * 1000000 + minor * 10000 + patch * 100 + build
}

fun calculateVersionName(): String {
    val major = 1
    val minor = 0
    val patch = 0
    val flavor = System.getenv("FLAVOR") ?: "dev"
    val build = System.getenv("BUILD_NUMBER") ?: "1"
    return "$major.$minor.$patch-$build+$flavor"
}
```

### Release Branching Strategy

```
main (protected)
  │
  ├── develop (integration branch)
  │     │
  │     ├── feature/AUTH-01-login
  │     ├── feature/STORE-03-cart
  │     └── feature/STAFF-05-empleados
  │
  ├── release/1.0.0 (release candidate)
  │     ├── hotfix/1.0.1-crash-fix
  │     └── hotfix/1.0.2-payment-bug
  │
  └── hotfix/1.0.1-prod (direct from main for emergencies)
```

### Release Checklist

| Paso | Acción | Responsable | Verificación |
|------|--------|-------------|--------------|
| 1 | Create `release/x.y.z` branch | Tech Lead | Branch created |
| 2 | Update version codes | Tech Lead | `versionCode` incrementado |
| 3 | Generate changelog | Tech Lead + PO | `CHANGELOG.md` actualizado |
| 4 | Run full test suite | CI | All gates pass |
| 5 | Deploy to Staging (Internal) | DevOps | Play Console Internal |
| 6 | QA Sign-off (2 days) | QA Lead | Test cases passed |
| 7 | Stakeholder Demo | PO | Features approved |
| 8 | Tag `rc-x.y.z` | Tech Lead | Git tag created |
| 9 | Deploy to Production (10%) | DevOps | Play Console Production |
| 10 | Monitor 24h | DevOps + QA | Crashlytics, Vitals OK |
| 11 | Increase to 50% → 100% | DevOps | Gradual rollout |
| 12 | Merge to `main` + tag `vx.y.z` | Tech Lead | Git history clean |
| 13 | Post-release retrospective | All | Action items documented |

---

## 🏪 Google Play Console Setup

### Tracks Configuration

| Track | Audience | Rollout | Propósito |
|-------|----------|---------|-----------|
| **Internal Testing** | Equipo (20 emails) | 100% | Validación rápida, daily builds |
| **Closed Testing** | Beta testers (50-200) | 100% | UAT, feedback real users |
| **Open Testing** | Público (opcional) | 100% | Stress test, performance |
| **Production** | Todos los usuarios | Staged 10% → 100% | Release oficial |

### Play Console Checklist Pre-Launch

- [ ] **App Signing**: Play App Signing activado
- [ ] **Data Safety Form**: Completado y enviado
- [ ] **Privacy Policy**: URL pública accesible
- [ ] **Target API Level**: 34 (Android 14) mínimo
- [ ] **64-bit Support**: Verificado (ARM64 + x86_64)
- [ ] **App Bundle**: AAB subido (no APK)
- [ ] **Store Listing**: Español + Inglés + Portugués
- [ ] **Screenshots**: Phone (16:9), Tablet (16:10), 7" y 10"
- [ ] **Feature Graphic**: 1024x500
- [ ] **Video Promo**: YouTube link (opcional)
- [ ] **Content Rating**: Cuestionario completado
- [ ] **Ads Declaration**: No ads / No IDFA
- [ ] **Permissions Justification**: Camera, Location, Notifications

### In-App Updates

```kotlin
// core/updates/AppUpdateManager.kt
class AppUpdateManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(context)
    
    fun checkForUpdate(activity: ComponentActivity) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        REQUEST_CODE_UPDATE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("AppUpdate", "Update flow failed", e)
                }
            }
        }
    }
}
```

---

## 📊 Monitoring & Observability

### Firebase Crashlytics

```kotlin
// core/crashlytics/CrashlyticsConfig.kt
class CrashlyticsConfig {
    
    companion object {
        fun initialize(context: Context) {
            if (BuildConfig.ENABLE_CRASHLYTICS) {
                FirebaseApp.initializeApp(context)
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                
                // Custom keys para debugging
                FirebaseCrashlytics.getInstance().setCustomKey("flavor", BuildConfig.FLAVOR)
                FirebaseCrashlytics.getInstance().setCustomKey("version", BuildConfig.VERSION_NAME)
            }
        }
        
        fun logUser(userId: String, email: String, role: String) {
            FirebaseCrashlytics.getInstance().setUserId(userId)
            FirebaseCrashlytics.getInstance().setCustomKey("user_email", email)
            FirebaseCrashlytics.getInstance().setCustomKey("user_role", role)
        }
        
        fun logNonFatal(throwable: Throwable, context: Map<String, Any> = emptyMap()) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
            context.forEach { (k, v) -> 
                FirebaseCrashlytics.getInstance().setCustomKey(k, v.toString()) 
            }
        }
    }
}
```

### Alertas Configuradas

| Métrica | Umbral | Acción | Canal |
|---------|--------|--------|-------|
| **Crash-free Sessions** | < 99.5% | PagerDuty + Slack | #alerts-crashlytics |
| **ANR Rate** | > 0.47% | PagerDuty + Slack | #alerts-vitals |
| **Crash Rate** | > 1% | PagerDuty + Slack | #alerts-vitals |
| **Startup Time (p95)** | > 3s | Slack | #alerts-perf |
| **API Error Rate** | > 5% | Slack | #alerts-api |
| **Build Failure** | Any | Slack | #ci-cd |

### Dashboards

| Dashboard | Herramienta | Métricas Clave |
|-----------|-------------|----------------|
| **Release Health** | Firebase Console | Crash-free, ANR, Adoption, Version distribution |
| **Performance** | Firebase Performance | Startup, Screen render, Network latency |
| **Business** | Custom (BigQuery + Looker) | DAU, MAU, Retention, Conversion, Revenue |
| **CI/CD** | GitHub Actions | Build time, Test pass rate, Deploy frequency |

---

## 🔄 Rollback Strategy

### Rollback Automático (CI/CD)

```yaml
# En deploy-production job
- name: Auto Rollback on Metrics Degradation
  if: failure()
  run: |
    # Halt rollout
    gcloud play publish halt --package_name=com.sprm.apexgym --track=production
    
    # Promote previous version
    gcloud play publish promote --package_name=com.sprm.apexgym \
      --track=production --version_code=${{ env.PREVIOUS_VERSION_CODE }}
    
    # Notify
    curl -X POST ${{ secrets.SLACK_WEBHOOK }} -d '{"text":"🔴 AUTO ROLLBACK: Production rollout halted due to metrics degradation"}'
```

### Rollback Manual

```bash
# 1. Ver versión anterior en Play Console
# 2. Halting rollout
gcloud play publish halt --package_name=com.sprm.apexgym --track=production

# 3. Promover versión anterior
gcloud play publish promote --package_name=com.sprm.apexgym \
  --track=production --version_code=12345

# 4. Verificar
gcloud play publish status --package_name=com.sprm.apexgym --track=production
```

### Criterios de Rollback

| Condición | Acción |
|-----------|--------|
| Crash-free sessions < 99% en 1h | Rollback inmediato |
| ANR rate > 1% en 1h | Rollback inmediato |
| Critical bug reportado por > 10 usuarios | Rollback en 30 min |
| Revenue drop > 20% | Rollback inmediato |
| Security vulnerability | Rollback + hotfix en 2h |

---

## 🔐 Secrets Management

### GitHub Secrets

| Secret | Descripción | Rotación |
|--------|-------------|----------|
| `KEYSTORE_PASSWORD` | Password keystore release | 90 días |
| `KEY_ALIAS` | Alias de la key | 90 días |
| `KEY_PASSWORD` | Password de la key | 90 días |
| `PLAY_SERVICE_ACCOUNT` | JSON Service Account Play Console | 90 días |
| `CODECOV_TOKEN` | Token Codecov | 180 días |
| `SLACK_WEBHOOK` | Webhook notificaciones | 180 días |
| `FIREBASE_TOKEN` | Token Firebase CLI | 180 días |
| `SENTRY_DSN` | DSN Sentry (opcional) | 180 días |

### Google Cloud Secret Manager (Alternativa)

```yaml
# Para secrets más sensibles, usar Secret Manager
- name: Access Secrets
  uses: google-github-actions/access-secret-manager@v2
  with:
    secrets: |
      KEYSTORE_PASSWORD: projects/${{ secrets.GCP_PROJECT }}/secrets/keystore-password/versions/latest
      PLAY_SERVICE_ACCOUNT: projects/${{ secrets.GCP_PROJECT }}/secrets/play-service-account/versions/latest
```

---

## 📱 Device Farm & Testing Infrastructure

### Firebase Test Lab Matrix

```yaml
# Configuración device matrix para UI tests
testMatrix:
  androidMatrix:
    androidModelIds:
      - "pixel6a"
      - "pixel7pro"
      - "galaxyS23"
      - "galaxyA54"
    androidVersionIds:
      - "28"
      - "30"
      - "33"
      - "34"
    locales:
      - "es_ES"
      - "en_US"
      - "pt_BR"
    orientations:
      - "portrait"
      - "landscape"
```

### Costos Estimados (Firebase Test Lab)

| Dispositivos | Tests/Device | Duración | Costo/Run |
|--------------|--------------|----------|-----------|
| 4 models × 4 APIs × 3 locales = 48 | 10 tests | 15 min | ~$24/run |
| **Mensual (daily)** | | | **~$720/mes** |

---

## 📋 Runbooks Operacionales

### Runbook: Deploy to Production

```markdown
# Runbook: Production Deploy

## Pre-requisitos
- [ ] Release branch `release/x.y.z` merged to `main`
- [ ] Tag `vx.y.z` created
- [ ] Changelog publicado en GitHub Releases
- [ ] QA sign-off documentado
- [ ] Stakeholder approval (email/Slack)

## Pasos
1. Trigger workflow `Deploy to Production` en GitHub Actions
2. Seleccionar `environment: production`
3. Confirmar versión (auto-detectada del tag)
4. Ejecutar workflow
5. Monitorear logs de deploy
6. Verificar en Play Console: Artifact subido, track: production, rollout: 10%
7. Postear en #releases: "🚀 v{x.y.z} deployed to Production (10%)"

## Post-Deploy (Próximas 24h)
- [ ] Verificar Crashlytics cada 2h
- [ ] Verificar Play Console Vitals cada 4h
- [ ] Aumentar rollout: 10% → 25% → 50% → 100% (si métricas OK)
- [ ] Comunicar a equipo: "✅ v{x.y.z} fully rolled out"

## Rollback Trigger
SI: Crash-free < 99% O ANR > 1% O Critical bug reportado
ENTONCES: Ejecutar rollback manual (ver sección Rollback)
```

### Runbook: Hotfix Production

```markdown
# Runbook: Hotfix Production

## Cuándo
- Crash crítico afectando > 5% usuarios
- Bug de seguridad
- Bug de pagos/financiero
- Data loss

## Proceso (Target: < 2 horas)
1. Crear branch `hotfix/x.y.z+1` desde `main` (tag actual producción)
2. Fix + test + code review (expedited)
3. Merge a `main` + tag `vx.y.z+1`
4. Trigger deploy production workflow
5. Rollout 100% inmediato (skip staged para hotfix crítico)
6. Verificar fix en 30 min
7. Comunicar: "🔧 Hotfix v{x.y.z+1} deployed - {descripción fix}"
```

---

## 💰 Costos Operacionales Estimados (Mensual)

| Servicio | Costo Estimado | Notas |
|----------|----------------|-------|
| **Firebase (Blaze Plan)** | $50-150 | Crashlytics, Analytics, Performance, Test Lab |
| **Google Play Console** | $25 (one-time) | Registro desarrollador |
| **GitHub Actions** | $0-50 | Minutos build (incluidos en plan) |
| **Codecov** | $0-10 | Gratis para open source / equipo pequeño |
| **Slack (Pro)** | $8/usuario | Notificaciones |
| **PagerDuty** | $25/usuario | Alerting (opcional) |
| **Sentry** | $0-80 | Error tracking (alternativa Crashlytics) |
| **Total Estimado** | **$100-400/mes** | Según escala y herramientas |

---

## 🔐 Seguridad en Despliegue

### Checklist Seguridad Pre-Release

- [ ] **Certificate Pinning** configurado para producción
- [ ] **Network Security Config**: `cleartextTrafficPermitted="false"` en prod
- [ ] **ProGuard/R8** ofuscación activada
- [ ] **Keystore** almacenado seguro (no en repo)
- [ ] **Secrets** en GitHub Secrets / Secret Manager (no en código)
- [ ] **Dependencies** escaneadas (OWASP Dependency Check)
- [ ] **Permissions** mínimas necesarias en Manifest
- [ ] **Debug flags** deshabilitados (`debuggable=false`, `ENABLE_LOGGING=false`)
- [ ] **App Signing** por Play Console (managed)
- [ ] **Data Safety Form** preciso y actualizado

### Dependency Scanning

```yaml
# .github/workflows/dependency-scan.yml
- name: OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'ApexGym'
    path: '.'
    format: 'HTML'
    out: 'reports'
    args: >
      --failOnCVSS 7
      --suppression suppression.xml
```

---

## 📝 Documentación de Handoff

### Entregables al Equipo de Operaciones

| Documento | Ubicación | Actualización |
|-----------|-----------|---------------|
| **Runbooks** | `/docs/runbooks/` | Por release |
| **Architecture Decision Records** | `/docs/adr/` | Por decisión |
| **API Contracts** | `/docs/api/` | Por cambio breaking |
| **Database Schema** | `/docs/db/` | Por migración |
| **Feature Flags** | `/docs/flags/` | Por feature |
| **Rollback Procedures** | `/docs/runbooks/rollback.md` | Por release |
| **Incident Response** | `/docs/incidents/` | Por incidente |
| **Capacity Planning** | `/docs/capacity/` | Trimestral |

### Contactos de Escalación

| Rol | Nombre | Slack | Email | Teléfono |
|-----|--------|-------|-------|----------|
| **Tech Lead** | E1 | @tech-lead | techlead@apexgym.com | +54 9 11 XXXX-XXXX |
| **DevOps** | E6 | @devops | devops@apexgym.com | +54 9 11 XXXX-XXXX |
| **QA Lead** | E5 | @qa-lead | qa@apexgym.com | +54 9 11 XXXX-XXXX |
| **Product Owner** | PO | @po | po@apexgym.com | +54 9 11 XXXX-XXXX |
| **Backend Lead** | BL | @backend | backend@apexgym.com | +54 9 11 XXXX-XXXX |