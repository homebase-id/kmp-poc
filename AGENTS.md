# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project targeting Android and iOS, built with Compose Multiplatform. The application implements a YouAuth authentication flow using OAuth2-like browser-based authentication with deeplinks.

**Package:** `id.homebase.homebasekmppoc`

## Current Development Context

### App Architecture

The application uses a **drawer-based navigation** with six main pages (defined in `App.kt`):

1. **Owner** - `OwnerPage(authenticationManager)` - Main owner authentication page
2. **Domain** - `DomainPage(domainYouAuthManager)` - Domain-based YouAuth authentication
3. **App** - `AppPage(appYouAuthManager)` - App-based YouAuth authentication with permissions
4. **db** - `DbPage()` - Database operations testing
5. **ws** - `WebsocketPage(wsAuthenticationManager)` - WebSocket connectivity testing
6. **Video** - `VideoPlayerTestPage(videoAuthenticationManager)` - Video player testing

### Project Structure

```
composeApp/src/commonMain/kotlin/id/homebase/homebasekmppoc/
├── App.kt                    # Main entry point with drawer navigation
├── lib/                      # Shared library code (reusable components)
│   ├── core/                 # Core utilities (SecureByteArray, etc.)
│   ├── crypto/               # Cryptography (ECC, AES, HKDF, etc.)
│   ├── drives/               # Drive API models and queries
│   ├── http/                 # HTTP utilities (UriBuilder, etc.)
│   ├── image/                # Image processing utilities
│   └── serialization/        # JSON serialization (OdinSystemSerializer)
├── prototype/                # Feature implementations  
│   ├── lib/                  # Feature-specific libraries
│   │   ├── authentication/   # AuthenticationManager, AuthState
│   │   ├── youauth/          # YouAuthManager, YouAuthCallbackRouter
│   │   ├── drives/           # DriveQueryProvider
│   │   ├── database/         # Database operations
│   │   ├── http/             # HTTP client creation
│   │   ├── video/            # Video handling
│   │   └── websockets/       # WebSocket client
│   └── ui/                   # UI pages and components
│       ├── app/              # App page with permissions
│       ├── domain/           # Domain authentication page
│       ├── driveFetch/       # DriveFetchPage & DriveFetchList
│       ├── owner/            # Owner authentication page
│       ├── db/               # Database testing page
│       ├── ws/               # WebSocket testing page
│       └── video/            # Video player testing page
└── ui/                       # Legacy/shared UI components
```

### YouAuth Authentication System

The app implements two types of YouAuth flows managed by `YouAuthManager`:

1. **Domain Authentication** (in Domain tab):
   - Basic domain authentication without app permissions
   - Uses `clientType = ClientType.domain`

2. **App Authentication** (in App tab):
   - Full app authentication with permission requests
   - Uses `clientType = ClientType.app` with `YouAuthAppParameters`
   - Requests specific drive permissions (e.g., photo albums, channels)

**Authentication State Flow** (`AuthState` sealed class):
- `Unauthenticated` - Initial state
- `Authenticating` - Browser launched, waiting for callback
- `Authenticated(identity, clientAuthToken, sharedSecret)` - Successfully authenticated
- `Error(message)` - Authentication failed

**Key Components:**
- `YouAuthManager` - Manages auth flow lifecycle per page
- `YouAuthCallbackRouter` - Routes deeplink callbacks to correct manager instance
- `AuthenticationManager` - Alternative authentication for Owner/WS/Video pages

### Drive Fetch Feature (Recently Implemented)

Located in `prototype/ui/driveFetch/`:

- **`DriveFetchPage.kt`** - Main page that:
  - Requires prior authentication from App tab
  - Fetches files from authenticated user's drive
  - Uses `DriveQueryProvider.create().queryBatch()` to fetch data
  - Displays results in `DriveFetchList`

- **`DriveFetchList.kt`** - Display component for drive items:
  - `DriveFetchList` - LazyColumn of file headers
  - `DriveFetchItemCard` - Card displaying file ID and content

**Usage Flow:**
1. Authenticate in App tab (YouAuth with app permissions)
2. Navigate to use Drive Fetch functionality
3. Click "Fetch Files" to call `queryBatch` API
4. Results displayed as list of `SharedSecretEncryptedFileHeader`

### Known Issues & Recent Work

- **YouAuth callback routing** - Fixed "lateinit property instance has not been initialized" by implementing `YouAuthCallbackRouter` to properly route callbacks to the correct `YouAuthManager` instance
- **State persistence** - `YouAuthManager` instances are hoisted to `App.kt` level to survive tab navigation
- **Token exchange** - Sometimes returns 404, related to `exchangeSecretDigest` encoding compatibility across platforms (marked as TODO in code)

## Build Commands

### Android

```bash
gradlew :composeApp:assembleDebug          # Build debug APK
gradlew :composeApp:assembleRelease        # Build release APK
gradlew build                               # Build entire project
```

### Testing

```bash
gradlew test                                # Run all tests
gradlew :composeApp:testDebugUnitTest      # Run Android unit tests
gradlew test --tests "id.homebase.homebasekmppoc.ComposeAppCommonTest.example"  # Run specific test
```

### Other Commands

```bash
gradlew lint                                # Lint code
gradlew lintFix                             # Auto-fix lint issues
gradlew clean                               # Clean build artifacts
```

## Architecture

### Multiplatform Structure

The project follows standard KMP structure with platform-specific and shared code:

- **`composeApp/src/commonMain/kotlin`**: Shared code for all platforms

  - Core UI components (Compose Multiplatform)
  - Business logic
  - YouAuth authentication flow
  - Cryptography utilities (in `crypto/` folder)

- **`composeApp/src/androidMain/kotlin`**: Android-specific implementations

  - `MainActivity.kt`: Entry point with deeplink handling
  - `Platform.android.kt`: Android platform implementations (Custom Tabs)

- **`composeApp/src/iosMain/kotlin`**: iOS-specific implementations

  - `MainViewController.kt`: iOS entry point with ASWebAuthenticationSession
  - `Platform.ios.kt`: iOS platform implementations

- **`iosApp/`**: Native iOS application wrapper (SwiftUI entry point)

### Expect/Actual Pattern

The project uses Kotlin's `expect`/`actual` mechanism for platform-specific implementations.

```kotlin
// commonMain
expect fun getPlatform(): Platform

// androidMain and iosMain provide actual implementations
```

### YouAuth Authentication Flow

The app implements browser-based OAuth2-like authentication:

1. User enters their Odin Identity domain
2. App launches in-app browser to `https://{domain}/api/v1/kmp/auth`
3. Backend redirects to `youauth://callback?code={authCode}`
4. App receives deeplink and processes the auth code

**Deeplink Configuration:**

- **Android**: Intent filters in `AndroidManifest.xml` for `youauth://` scheme
- **iOS**: URL scheme in `Info.plist` for `youauth://` scheme

### UI Architecture

The app uses a tab-based navigation with three main screens:

- **Home**: Main landing page
- **Domain**: Authentication/domain entry
- **App**: Application features

Main UI entry point: `App.kt` with `PrimaryTabRow` for navigation.

### Cryptography Implementation

The project uses [cryptography-kotlin](https://github.com/whyoleg/cryptography-kotlin) for cross-platform cryptographic operations.

**Cryptography Provider:**

- Uses `cryptography-provider-optimal` which automatically selects the best provider per platform:
  - **Android**: JDK provider
  - **iOS**: CryptoKit provider (with Apple CommonCrypto fallback)

**Key Implementation Notes:**

- All crypto operations are `suspend` functions (async)
- ECC operations support P-256 and P-384 curves
- Keys can be encoded/decoded in DER, RAW (uncompressed EC points) formats
- JWK format is handled manually as CryptoKit doesn't support it natively
- HKDF uses SHA-256 for key derivation
- No platform-specific native code required

## Dependencies

### Dependency Management Structure (STRICT)

**All dependencies MUST follow this structure:**

#### Step 1: Add Version to `gradle/libs.versions.toml`

```toml
[versions]
# Existing versions...
kotlin = "2.2.21"
compose = "1.9.3"

# Add your new version here (alphabetically)
newLibrary = "1.0.0"

[libraries]
# Existing libraries...
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

# Add your new library here (alphabetically)
new-library-core = { module = "com.example:library-core", version.ref = "newLibrary" }
new-library-android = { module = "com.example:library-android", version.ref = "newLibrary" }
```

**Format:** `libraryName = { module = "group:artifact", version.ref = "versionName" }`

#### Step 2: Add to `composeApp/build.gradle.kts`

The dependency location depends on **platform support**:

```kotlin
kotlin {
    sourceSets {
        // 1. COMMON (All Platforms) - Most dependencies go here
        commonMain.dependencies {
            implementation(libs.new.library.core)  // Note: use dots, not hyphens
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        // 2. ANDROID ONLY
        androidMain.dependencies {
            implementation(libs.new.library.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
        }

        // 3. IOS ONLY
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // 4. DESKTOP ONLY
        desktopMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutinesSwing)
        }

        // 5. COMMON TEST (Test dependencies for all platforms)
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        // 6. ANDROID UNIT TEST (Android-specific tests)
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
        }

        // 7. IOS TEST
        iosTest.dependencies {
            // iOS-specific test dependencies
        }

        // 8. DESKTOP TEST
        val desktopTest by getting {
            dependencies {
                // Desktop-specific test dependencies
            }
        }
    }
}
```

### Dependency Decision Tree

**Question: Where should I add this dependency?**

```
Does the library support Kotlin Multiplatform?
├─ YES
│  └─ Add to commonMain.dependencies
│     Example: kotlinx-datetime, kotlinx-serialization, ktor-client-core
│
└─ NO - Platform-specific only
   ├─ Android only (Java/Android library)
   │  └─ Add to androidMain.dependencies
   │     Example: androidx.browser, androidx.activity-compose
   │
   ├─ iOS only (Swift/ObjC library via cinterop)
   │  └─ Add to iosMain.dependencies
   │     Example: Platform.UIKit, Platform.Foundation
   │
   └─ Desktop only (JVM library)
      └─ Add to desktopMain.dependencies
         Example: kotlinx-coroutines-swing, specific desktop libs
```

### Common Dependencies Reference

#### Currently Used (DO NOT DUPLICATE)

```toml
# Core
kotlin = "2.2.21"
compose = "1.9.3"
kotlinx-coroutines = "1.10.2"
kotlinx-datetime = "0.7.1"
kotlinx-io = "0.8.0"

# Android
androidx-activity = "1.11.0"
androidx-browser = "1.9.0"
androidx-core = "1.17.0"

# Networking
ktor = "3.3.2"

# Database
sqldelight = "2.2.1"

# Crypto
cryptography = "0.5.0"

# Logging
kermit = "2.0.8"

# Testing
robolectric = "4.13"
junit = "4.13.2"
```

### Adding a New Multiplatform Library

**Example: Adding kotlinx-serialization**

1. **Add version to `libs.versions.toml`:**

```toml
[versions]
kotlinx-serialization = "1.7.3"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" }
```

2. **Add plugin if needed (in project `build.gradle.kts`):**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)  // Add this
}
```

3. **Apply plugin in `composeApp/build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)  // Add this
}
```

4. **Add dependency:**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
```

### Adding an Android-Only Library

**Example: Adding ExoPlayer**

1. **Add to `libs.versions.toml`:**

```toml
[versions]
exoplayer = "2.19.1"

[libraries]
androidx-exoplayer = { module = "com.google.android.exoplayer:exoplayer", version.ref = "exoplayer" }
```

2. **Add to `androidMain`:**

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.exoplayer)
        }
    }
}
```

### Adding a Test Dependency

**Example: Adding MockK**

1. **Add to `libs.versions.toml`:**

```toml
[versions]
mockk = "1.13.8"

[libraries]
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
```

2. **Add to test dependencies:**

```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.mockk)
        }
    }
}
```

### Dependency Rules (MUST FOLLOW)

#### DO ✅

- **Always use version catalog** - Define in `libs.versions.toml`
- **Use semantic versioning** - Check for stable releases
- **Prefer multiplatform libraries** - Add to `commonMain` when possible
- **Check compatibility** - Ensure library supports your platforms
- **Use appropriate scope**:
  - `implementation` - Library is used in your code
  - `api` - Library is exposed in your public API
  - `compileOnly` - Needed at compile time only
- **Group related libraries** - Use same version reference
- **Keep alphabetical** - Easier to find and avoid duplicates
- **Update gradle wrapper** - `gradlew wrapper --gradle-version=8.14.3`

#### DON'T ❌

- **Don't hardcode versions** - Always use version catalog
- **Don't add duplicate dependencies** - Check existing libs first
- **Don't add platform-specific to common** - Will cause build errors
- **Don't use snapshot versions in production** - Use stable releases
- **Don't ignore deprecation warnings** - Migrate to new APIs
- **Don't add unnecessary dependencies** - Increases app size
- **Don't mix different versions** - Use same version for library family

### Syncing Dependencies

After adding dependencies:

```bash
# Sync Gradle
gradlew --refresh-dependencies

# Clean build
gradlew clean build

# Verify all platforms compile
gradlew compileKotlinAndroid
gradlew compileKotlinIosSimulatorArm64
gradlew compileKotlinDesktop
```

### Troubleshooting Dependencies

**Problem:** "Unresolved reference" after adding dependency
**Solution:**

1. Check dependency is in correct sourceSet (commonMain vs androidMain)
2. Run `gradlew --refresh-dependencies`
3. Invalidate caches in IDE

**Problem:** "Duplicate class" error
**Solution:** Check for duplicate dependencies with different artifacts

**Problem:** Version conflict
**Solution:** Use BOM (Bill of Materials) or force specific version:

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.example:library:1.0.0")
    }
}
```

**Problem:** Native library not found on iOS
**Solution:** Check if library is Kotlin Multiplatform compatible

### Checking for Updates

```bash
# Check for dependency updates
gradlew dependencyUpdates

# View dependency tree
gradlew :composeApp:dependencies

# Check specific configuration
gradlew :composeApp:dependencies --configuration commonMainCompileClasspath
```

Key dependencies (defined in `gradle/libs.versions.toml` and `build.gradle.kts`):

- Kotlin 2.2.20
- Compose Multiplatform 1.9.1
- Android minSdk: 27, targetSdk: 36
- AndroidX Browser (for Custom Tabs)
- kotlinx-serialization-json 1.9.0
- kotlinx-datetime 0.7.1
- kotlinx-io-core 0.8.0
- AndroidX Lifecycle (ViewModel, Runtime Compose)
- cryptography-kotlin 0.5.0 (core + optimal provider)
- Kermit 2.0.8 (logging)

## Code Style

### Naming Conventions

- **Packages**: lowercase with dots (e.g., `id.homebase.homebasekmppoc`)
- **Classes/Interfaces**: PascalCase (e.g., `YouAuthAuthorizeRequest`, `EccPublicKeyData`)
- **Functions/Methods**: camelCase (e.g., `launchCustomTabs()`, `buildAuthorizeUrl()`)
- **Variables/Properties**: camelCase (e.g., `selectedTabIndex`, `authCode`)
- **Constants**: UPPER_SNAKE_CASE

### Import Organization

1. AndroidX Compose imports
2. Other AndroidX imports
3. org.jetbrains imports
4. Generated resource imports
5. Use single imports, not wildcard imports

### Composable UI Guidelines

- Use `@Composable` annotation for UI functions
- Apply `MaterialTheme` wrapper for consistent theming
- Use `remember` for state that survives recomposition
- Chain modifiers with dot notation
- Prefer `fillMaxSize()`, `safeContentPadding()` for layout

## Testing

### Test Structure (STRICT - Must Follow)

The project follows a **strict multiplatform testing structure**. Tests MUST be organized as follows:

```
composeApp/src/
├── commonTest/                          # Shared tests (run on ALL platforms)
│   ├── kotlin/id/homebase/homebasekmppoc/
│   │   ├── {feature}/
│   │   │   └── {Feature}Test.kt        # Common test file (open class)
│   │   └── lib/image/
│   │       ├── TestImageLoader.kt      # Expect declarations for platform loaders
│   │       ├── ImageUtilsTest.kt       # Common image tests
│   │       └── ThumbnailGeneratorTest.kt
│   └── resources/
│       └── test-images/                 # Shared test resources (images, data)
│           ├── sample.jpg
│           ├── sample.png
│           └── ...
│
├── androidUnitTest/                     # Android-specific JVM tests
│   ├── kotlin/id/homebase/homebasekmppoc/
│   │   └── lib/image/
│   │       ├── TestImageLoader.android.kt       # Actual implementation
│   │       ├── ImageUtilsTestConfig.android.kt  # Robolectric wrapper
│   │       └── ThumbnailGeneratorTestConfig.android.kt
│   └── assets/
│       └── test-images -> symlink to commonTest/resources/test-images
│
├── iosTest/                             # iOS-specific tests
│   └── kotlin/id/homebase/homebasekmppoc/
│       └── lib/image/
│           └── TestImageLoader.ios.kt   # Actual implementation
│
└── desktopTest/                         # Desktop-specific tests
    └── kotlin/id/homebase/homebasekmppoc/
        └── lib/image/
            └── TestImageLoader.desktop.kt  # Actual implementation
```

### Writing Unit Tests - Step by Step

#### 1. Create Common Test (Required)

**File:** `composeApp/src/commonTest/kotlin/id/homebase/homebasekmppoc/{feature}/{Feature}Test.kt`

```kotlin
package id.homebase.homebasekmppoc.{feature}

import kotlin.test.*
import kotlinx.coroutines.test.runTest  // For async tests

/**
 * Common tests for {Feature} that run on all platforms
 * MUST be 'open class' to allow platform-specific extension
 */
open class FeatureTest {

    @Test
    fun basicTest_withValidInput_returnsExpectedResult() {
        // Arrange
        val input = "test"

        // Act
        val result = someFunction(input)

        // Assert
        assertNotNull(result)
        assertEquals("expected", result)
    }

    @Test
    fun asyncTest_withSuspendFunction_works() = runTest {
        // For suspend functions, wrap in runTest
        val result = someSuspendFunction()
        assertTrue(result)
    }
}
```

**Key Rules:**

- ✅ **MUST** be `open class` (allows Android Robolectric to extend)
- ✅ **MUST** use `kotlin.test.*` assertions
- ✅ **MUST** use `runTest { }` for suspend functions
- ✅ **MUST** use descriptive test names: `functionName_condition_expectedBehavior`

#### 2. Android Tests (If Using Android Framework APIs)

**File:** `composeApp/src/androidUnitTest/kotlin/id/homebase/homebasekmppoc/{feature}/FeatureTestConfig.android.kt`

```kotlin
package id.homebase.homebasekmppoc.{feature}

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android test wrapper using Robolectric
 * Required when tests use Android framework APIs (Bitmap, Context, etc.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class FeatureAndroidTest : FeatureTest()
```

**When to Use:**

- Tests use `BitmapFactory`, `Context`, `AssetManager`
- Tests need Android-specific behavior
- Android implementation differs from iOS/Desktop

#### 3. Platform-Specific Resource Loading (Expect/Actual Pattern)

**Common (Expect):** `composeApp/src/commonTest/kotlin/.../TestResourceLoader.kt`

```kotlin
package id.homebase.homebasekmppoc.{feature}

expect object TestResourceLoader {
    fun loadResource(filename: String): ByteArray
    fun resourceExists(filename: String): Boolean
}
```

**Android (Actual):** `composeApp/src/androidUnitTest/kotlin/.../TestResourceLoader.android.kt`

```kotlin
package id.homebase.homebasekmppoc.{feature}

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

actual object TestResourceLoader {
    actual fun loadResource(filename: String): ByteArray {
        val path = Path("composeApp/src/commonTest/resources/$filename")
        if (SystemFileSystem.exists(path)) {
            val buffer = Buffer()
            SystemFileSystem.source(path).use { source ->
                buffer.transferFrom(source)
            }
            return buffer.readByteArray()
        }
        throw IllegalArgumentException("Resource not found: $filename")
    }

    actual fun resourceExists(filename: String): Boolean {
        return SystemFileSystem.exists(
            Path("composeApp/src/commonTest/resources/$filename")
        )
    }
}
```

**iOS (Actual):** `composeApp/src/iosTest/kotlin/.../TestResourceLoader.ios.kt`

```kotlin
package id.homebase.homebasekmppoc.{feature}

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSData
// ... (use NSBundle to load resources)

@OptIn(ExperimentalForeignApi::class)
actual object TestResourceLoader {
    actual fun loadResource(filename: String): ByteArray {
        val bundle = NSBundle.mainBundle
        // ... implementation
    }
}
```

**Desktop (Actual):** Same pattern using Kotlin IO

### Adding Test Dependencies

#### 1. Add Version to `gradle/libs.versions.toml`

```toml
[versions]
# ...existing versions...
robolectric = "4.13"
junit = "4.13.2"
kotlinx-coroutines-test = "1.10.2"

[libraries]
# ...existing libraries...
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

#### 2. Add to `composeApp/build.gradle.kts`

```kotlin
kotlin {
    sourceSets {
        // Common test dependencies (ALL platforms)
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
        }

        // Android-specific test dependencies
        androidUnitTest.dependencies {
            implementation(libs.robolectric)  // For Android framework APIs
            implementation(libs.sqldelight.sqlite.driver)
        }

        // iOS-specific test dependencies
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        // Desktop-specific test dependencies
        val desktopTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
```

### Running Tests

```bash
# Run ALL tests (all platforms)
gradlew test

# Android tests only
gradlew testDebugUnitTest
gradlew testDebugUnitTest --tests "id.homebase.homebasekmppoc.{feature}.FeatureAndroidTest"

# Single Android test
gradlew testDebugUnitTest --tests "id.homebase.homebasekmppoc.{feature}.FeatureAndroidTest.specificTest"

# iOS tests
gradlew iosSimulatorArm64Test
gradlew iosArm64Test

# Desktop tests
gradlew desktopTest

# With detailed output
gradlew test --info

# View test reports
open composeApp/build/reports/tests/testDebugUnitTest/index.html
```

### Test Best Practices

#### DO ✅

- **Write tests in `commonTest` first** - Share tests across platforms
- **Use `open class`** - Allows platform-specific extension
- **Use descriptive names** - `functionName_condition_expectedResult`
- **Use `runTest { }`** - For suspend functions
- **Use Kotlin IO** - For file operations (`kotlinx.io.files.SystemFileSystem`)
- **Use expect/actual** - For platform-specific resource loading
- **Add Robolectric** - When testing Android framework APIs
- **Share test resources** - Put in `commonTest/resources/`
- **Use assertions properly**:
  ```kotlin
  assertEquals(expected, actual, "descriptive message")
  assertNotNull(value, "value should not be null")
  assertTrue(condition, "condition should be true")
  assertContentEquals(expectedArray, actualArray)
  ```

#### DON'T ❌

- **Don't use `class FeatureTest`** - Use `open class FeatureTest`
- **Don't use JUnit directly** - Use `kotlin.test` framework
- **Don't use Java File API** - Use Kotlin IO (`kotlinx.io.files`)
- **Don't duplicate test images** - Use symlinks or shared resources
- **Don't use `@Before/@After`** - Use `@BeforeTest/@AfterTest` (kotlin.test)
- **Don't hardcode paths** - Use platform-specific loaders
- **Don't skip platform tests** - Write once in `commonTest`, run everywhere

### Example: Complete Test Structure

**Common Test:**

```kotlin
package id.homebase.homebasekmppoc.lib.image

import kotlin.test.*
import kotlinx.coroutines.test.runTest

open class ImageUtilsTest {
    @Test
    fun getNaturalSize_withValidJpeg_returnsCorrectSize() {
        val imageData = loadTestImageOrSkip("sample.jpg")
        val size = ImageUtils.getNaturalSize(imageData)
        assertTrue(size.pixelWidth > 0)
        assertTrue(size.pixelHeight > 0)
    }

    @Test
    fun resizeImage_withAspectRatio_preservesAspectRatio() = runTest {
        val imageData = loadTestImageOrSkip("sample.png")
        val result = ImageUtils.resizePreserveAspect(
            imageData,
            maxWidth = 100,
            maxHeight = 100
        )
        assertNotNull(result)
        assertTrue(result.bytes.isNotEmpty())
    }
}
```

**Android Wrapper:**

```kotlin
package id.homebase.homebasekmppoc.lib.image

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ImageUtilsAndroidTest : ImageUtilsTest()
```

### Test Coverage Guidelines

Aim for:

- ✅ **80%+ code coverage** for business logic
- ✅ **Test happy paths** - Normal usage
- ✅ **Test edge cases** - Null, empty, boundary values
- ✅ **Test error handling** - Exceptions, invalid input
- ✅ **Test async operations** - Use `runTest { }`
- ✅ **Test platform-specific code** - In platform test folders

### Troubleshooting Tests

**Problem:** "Test image not found"
**Solution:** Add images to `commonTest/resources/test-images/`

**Problem:** "BitmapFactory not mocked"  
**Solution:** Add Robolectric dependency and wrapper class

**Problem:** "Unresolved reference in test"
**Solution:** Check dependency is in `commonTest.dependencies`

**Problem:** iOS tests can't find resources
**Solution:** Add resources to Xcode project or use NSBundle properly

**Problem:** Tests fail on CI but pass locally
**Solution:** Ensure test resources are committed to git

## Testing

### Test Framework

- Use `kotlin.test` framework for common tests
- Place shared test logic in `commonTest`
- Use descriptive test names that explain behavior
- Make test classes `open` for platform-specific extension

## Platform-Specific Notes

### Android

- Uses Custom Tabs (`androidx.browser:browser`) for in-app browsing
- Deeplinks handled in `MainActivity.onNewIntent()`
- Entry point: `MainActivity.kt`

### iOS

- Uses `ASWebAuthenticationSession` for secure in-app authentication
- Deeplinks configured via URL schemes in Info.plist
- Entry point: `MainViewController()` function
- Requires `AuthPresentationContextProvider` for modal presentation

## Important Context

- The backend API server (ASP.NET Core) runs on `https://localhost:5001` (see README)
- The app is a proof-of-concept for YouAuth authentication integration
- All cryptography is fully implemented using cryptography-kotlin in common code
- Do not use emojis unless explicitly requested
- Follow existing code patterns and architecture when adding new features

## Known Issues & Considerations

- iOS console may show harmless warnings about duplicate Objective-C classes from system frameworks
- CryptoKit provider on iOS doesn't support JWK format natively - we use RAW uncompressed EC point format instead
- All crypto operations are async (suspend functions) due to cryptography-kotlin API design
- Always compile when you're done making changes
- When possible, always prefer stable, currently maintained, well-documented and well-tested third-party libraries over custom implementations
- When possible, always prefer creating platorm shared code over creating platform-specific code
