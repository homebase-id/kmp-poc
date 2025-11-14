# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project targeting Android and iOS, built with Compose Multiplatform. The application implements a YouAuth authentication flow using OAuth2-like browser-based authentication with deeplinks.

**Package:** `id.homebase.homebasekmppoc`

## Build Commands

### Android
```bash
./gradlew :composeApp:assembleDebug          # Build debug APK
./gradlew :composeApp:assembleRelease        # Build release APK
./gradlew build                               # Build entire project
```

### Testing
```bash
./gradlew test                                # Run all tests
./gradlew :composeApp:testDebugUnitTest      # Run Android unit tests
./gradlew test --tests "id.homebase.homebasekmppoc.ComposeAppCommonTest.example"  # Run specific test
```

### Other Commands
```bash
./gradlew lint                                # Lint code
./gradlew lintFix                             # Auto-fix lint issues
./gradlew clean                               # Clean build artifacts
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

The project uses Kotlin's `expect`/`actual` mechanism for platform-specific implementations:

```kotlin
// commonMain
expect fun getPlatform(): Platform
expect fun launchCustomTabs(url: String)
expect fun showMessage(title: String, message: String)

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

**Key Files:**
- `composeApp/src/commonMain/kotlin/id/homebase/homebasekmppoc/youauth/` - YouAuth implementation
- `composeApp/src/commonMain/kotlin/id/homebase/homebasekmppoc/DomainPage.kt` - Authentication UI

### UI Architecture

The app uses a tab-based navigation with three main screens:
- **Home**: Main landing page
- **Domain**: Authentication/domain entry
- **App**: Application features

Main UI entry point: `App.kt` with `PrimaryTabRow` for navigation.

### Cryptography Implementation

The project uses [cryptography-kotlin](https://github.com/whyoleg/cryptography-kotlin) v0.5.0 for cross-platform cryptographic operations.

**Current Status:**
All cryptography is implemented in common code using cryptography-kotlin:
- `Crc32c.kt` - CRC32C checksum calculation
- `SensitiveByteArray.kt` - Secure memory handling for sensitive data
- `UnixTimeUtc.kt` - Unix timestamp utilities
- `AesCbc.kt` - AES-CBC encryption/decryption
- `HashUtil.kt` - SHA-256 hashing and HKDF key derivation
- `EccKeyData.kt` - ECC key generation, ECDH key agreement, JWK/DER conversions
- `ByteArrayUtil.kt` - Byte array utilities and Base64 encoding
- `Base64UrlEncoder.kt` - Base64 URL-safe encoding/decoding

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

- Use `kotlin.test` framework for common tests
- Place shared test logic in `commonTest`
- Use descriptive test names that explain behavior

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
- always compile when you're done making changes