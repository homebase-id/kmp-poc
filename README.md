This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```
  
- turn on adb reverse for HTTPS support in the Android emulator
```shell
adb root
adb reverse tcp:443 tcp:443
```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## YouAuth Authentication

This app implements a YouAuth authentication flow using OAuth2-like browser-based authentication with deeplinks.

### Features

- **Cross-platform authentication**: Works on both Android and iOS
- **In-app browser experience**: Uses Custom Tabs on Android and ASWebAuthenticationSession on iOS
- **Deeplink handling**: Handles `youauth://callback?code={authCode}` URLs
- **Backend integration**: Includes a simple ASP.NET Core API that serves the auth redirect

### How it works

1. User enters their Odin Identity domain
2. App launches in-app browser to `https://{domain}/api/v1/kmp/auth`
3. Backend serves HTML that redirects to `youauth://callback?code={randomCode}`
4. App receives the deeplink and processes the auth code

### Running the Backend

```bash
cd backend/OdinAuthApi
dotnet run
```

The API will be available at `https://localhost:5001`

### Testing

- Android: Uses Custom Tabs for seamless in-app browsing
- iOS: Uses ASWebAuthenticationSession for secure in-app authentication
- Both platforms handle deeplinks to complete the auth flow

### Configuration

- Deeplinks are configured for `youauth://` scheme
- Android manifest includes intent filters for deeplinks
- iOS Info.plist includes URL scheme configuration

## HLS Video Playback

This app includes cross-platform HLS video playback with authenticated backend requests.

### Features

- **Cross-platform HLS streaming**: Works on both Android and iOS
- **Authenticated requests**: Adds `DY0810` header to backend requests for secure access
- **Adaptive streaming**: Supports HLS manifests with byte-range segments
- **Proxy-based authentication**: Uses LocalVideoServer to proxy requests with auth headers

### How it works

1. **Manifest Loading**: App fetches HLS manifest from backend
2. **URL Modification**: Manifest URLs are rewritten to route through local proxy
3. **Authenticated Proxying**: LocalVideoServer forwards requests with `DY0810` header
4. **Video Playback**:
   - Android: ExoPlayer with custom DataSource for header injection
   - iOS: AVPlayerViewController with LocalVideoServer proxy

### Architecture

- **LocalVideoServer**: Ktor-based HTTP server running locally on device
- **Proxy Endpoint**: `/proxy?url={encoded_url}` forwards requests with auth headers
- **Header Forwarding**: All client headers (including Range for byte requests) are forwarded
- **Response Headers**: Backend response headers are copied back to client

### Example Manifest

```
#EXTM3U
#EXT-X-VERSION:4
#EXT-X-TARGETDURATION:7
#EXT-X-MEDIA-SEQUENCE:0
#EXT-X-KEY:METHOD=AES-128,URI="data:application/octet-stream;base64,WFLbzyKRzPyVp8LWbgu3fA==",IV=0x415efd95c27dadf95000e7cd867b6f59
#EXTINF:6.985233,
#EXT-X-BYTERANGE:19748464@0
https://frodo.dotyou.cloud/api/owner/v1/drive/files/payload?ss={encrypted_auth}
```

### Why LocalVideoServer Proxy?

AVPlayer on iOS doesn't provide APIs to add custom headers to HLS segment requests. The proxy approach:

- ✅ Enables authenticated requests across platforms
- ✅ Handles byte-range requests properly
- ✅ Maintains security (headers not exposed in URLs)
- ✅ Works with existing backend authentication

### Testing

- Android: ExoPlayer handles headers directly in DataSource
- iOS: LocalVideoServer proxies requests with authentication
- Both platforms support encrypted HLS streams with byte ranges

