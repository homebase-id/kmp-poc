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
- **Authenticated requests**: Injects auth header to backend requests for secure access
- **Adaptive streaming**: Supports HLS manifests with byte-range segments (`#EXT-X-BYTERANGE`)
- **Streaming proxy**: Efficiently streams video segments without buffering in memory

### Architecture Overview

```
┌─────────────────┐
│ VideoPlayerTest │ ← Entry point (manages LocalVideoServer)
│      Page       │
└────────┬────────┘
         │ 1. Fetches manifest content from backend
         │ 2. Modifies URLs to proxy through local server
         │ 3. Registers content with auth token
         │ 4. Passes local manifest URL to player
         │    (e.g., http://127.0.0.1:PORT/content/video-1-manifest)
         ▼
┌─────────────────┐
│ LocalVideoServer│ ← Ktor HTTP server (127.0.0.1:random_port)
│  (Common code)  │
└────────┬────────┘
         │ HTTP Endpoints:
         │ • GET /content/{id} → Serves modified manifest
         │ • GET /proxy?url=...&manifestId=... → Proxies segments with auth header
         │
         │ Features:
         │ • Streams responses (no buffering)
         │ • Handles byte-range requests (206 Partial Content)
         │ • Auth token stored per-content registration
         │
         │ ◄────── HTTP GET /content/{id} (manifest request)
         │ ◄────── HTTP GET /proxy?url=... (segment requests)
         ▼
┌──────────────┬──────────────┐
│   Android    │     iOS      │
│  ExoPlayer   │  AVPlayer    │
│              │              │
│ Makes HTTP   │ Makes HTTP   │
│ GET requests │ GET requests │
│ to local     │ to local     │
│ server       │ server       │
└──────────────┴──────────────┘
```

**Note**: Native video players (ExoPlayer, AVPlayer) make standard HTTP GET requests to the LocalVideoServer running on `127.0.0.1`. The server acts as an authentication proxy, injecting the auth header into backend requests.

### Why LocalVideoServer is Needed

**Problem**: Native video players (AVPlayer on iOS, ExoPlayer on Android) cannot inject custom authentication headers into HLS segment requests:

- **iOS AVPlayer**: No API to add custom headers to segment requests
- **Android ExoPlayer**: Can add headers, but requires managing auth token lifecycle and manual injection
- **Backend requires authentication**: All video segment requests need auth header

**Solution**: LocalVideoServer acts as an authentication proxy:

1. **Manifest Modification**: Rewrites segment URLs to point to local proxy
   - Original: `https://backend.com/segment.ts`
   - Modified: `http://127.0.0.1:12345/proxy?url=https%3A%2F%2Fbackend.com%2Fsegment.ts&manifestId=video-1-manifest`

2. **Authenticated Proxying**: Intercepts segment requests and adds auth header
   - Player requests: `http://127.0.0.1:12345/proxy?url={encoded_backend_url}&manifestId={id}`
   - Server looks up auth token from content registration
   - Proxy forwards: `GET {backend_url}` with `some-auth: {clientAuthToken}`

3. **Transparent Streaming**: Streams response directly to player
   - No buffering (uses Ktor channels)
   - Preserves HTTP status (206 for byte ranges)
   - Forwards all headers (Content-Range, Content-Type, etc.)

### How It Works

#### 1. VideoPlayerTestPage (Test Harness)

```kotlin
// Create single LocalVideoServer that runs continuously
val videoServer = remember { LocalVideoServer() }

// Start server once on page load
LaunchedEffect(Unit) {
    videoServer.start()  // Starts on random port (e.g., http://127.0.0.1:44683)
}

// When video selected:
val currentAuthToken = (authState as? AuthState.Authenticated)?.clientAuthToken
val originalManifest = header.getVideoMetaData()  // Fetch from backend
val serverUrl = videoServer.getServerUrl()

val manifestId = "video-1-manifest"

// Modify segment URLs to proxy through local server with manifestId
val modifiedManifest = originalManifest.lines().joinToString("\n") { line ->
    if (line.startsWith("https://")) {
        val encodedUrl = line.encodeURLParameter()
        "$serverUrl/proxy?url=$encodedUrl&manifestId=$manifestId"
    } else line
}

// Register manifest with auth token
videoServer.registerContent(
    id = manifestId,
    data = modifiedManifest.encodeToByteArray(),
    contentType = "application/vnd.apple.mpegurl",
    authTokenHeaderName = "XXX",
    authToken = currentAuthToken
)

val localManifestUrl = videoServer.getContentUrl(manifestId)

// Pass to player
HlsVideoPlayer(manifestUrl = localManifestUrl, clientAuthToken = null, ...)
```

#### 2. LocalVideoServer (Common Code)

**Responsibilities**:
- Serve modified HLS manifests
- Proxy video segment requests with authentication
- Stream responses efficiently (no memory buffering)
- Handle HTTP byte-range requests (206 Partial Content)

**Key Implementation Details**:

```kotlin
class LocalVideoServer {
    private val contentRegistry = mutableMapOf<String, ContentData>()
    private val httpClient = HttpClient()  // Reusable for proxying

    private data class ContentData(
        val data: SecureByteArray,
        val contentType: String,
        val authTokenHeaderName: String? = null,
        val authToken: String? = null
    )

    // Proxy endpoint: /proxy?url={encoded_url}&manifestId={id}
    get("/proxy") {
        val url = call.request.queryParameters["url"]
        val manifestId = call.request.queryParameters["manifestId"]

        // Look up auth token from content registration
        val authTokenHeaderName = manifestId?.let { contentRegistry[it]?.authTokenHeaderName }
        val authToken = manifestId?.let { contentRegistry[it]?.authToken }

        val response = httpClient.get(url) {
            // Forward all headers (including Range for byte-range requests)
            call.request.headers.forEach { key, values ->
                if (key.lowercase() != "host" && key.lowercase() != "accept-encoding") {
                    header(key, value)
                }
            }
            // Add authentication header from content data
            if (authTokenHeaderName != null && authToken != null) {
                header(authTokenHeaderName, authToken)
            }
        }

        // Preserve HTTP status (important for 206 Partial Content)
        call.response.status(response.status)

        // Forward response headers (Content-Range, Content-Type, etc.)
        response.headers.forEach { key, values ->
            if (key.lowercase() != "content-length" && key.lowercase() != "content-type") {
                call.response.headers.append(key, value)
            }
        }

        // Stream response (no buffering!)
        val responseChannel = response.bodyAsChannel()
        call.respond(object : OutgoingContent.ReadChannelContent() {
            override val contentType = ContentType.parse(contentTypeString)
            override fun readFrom() = responseChannel
        })
    }
}
```

**Why Streaming Instead of Buffering?**
- Video segments can be 17-19MB each
- Buffering causes GC pauses and playback stutters
- Streaming delivers bytes as they arrive from backend
- Reduces memory usage from ~20MB per segment to ~8KB buffer

#### 3. HLS Video Players (Platform-Specific)

**Android** (`HlsVideoPlayer.android.kt`):
```kotlin
@Composable
actual fun HlsVideoPlayer(manifestUrl: String, clientAuthToken: String?, modifier: Modifier) {
    val exoPlayer = remember(manifestUrl) {
        ExoPlayer.Builder(context).build().apply {
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(manifestUrl))
            setMediaSource(hlsMediaSource)
            prepare()
            playWhenReady = true
        }
    }
    // ExoPlayer fetches manifest and segments from LocalVideoServer
}
```

**iOS** (`HlsVideoPlayer.ios.kt`):
```kotlin
@Composable
actual fun HlsVideoPlayer(manifestUrl: String, clientAuthToken: String?, modifier: Modifier) {
    val playerViewController = remember(manifestUrl) {
        val url = NSURL.URLWithString(manifestUrl)
        val player = AVPlayer.playerWithURL(url)
        val playerViewController = AVPlayerViewController()
        playerViewController.player = player
        playerViewController
    }
    // AVPlayer fetches manifest and segments from LocalVideoServer
}
```

**Key Points**:
- Both players are now **identical in architecture**
- Neither player manages LocalVideoServer internally
- Both receive a local manifest URL (`http://127.0.0.1:xxxxx/content/manifest`)
- Auth token handling is centralized in LocalVideoServer

### Example Manifest Flow

**Original Manifest** (from backend):
```
#EXTM3U
#EXT-X-VERSION:4
#EXTINF:6.985,
#EXT-X-BYTERANGE:19748464@0
https://backend.com/api/v1/files/payload?ss={encrypted_params}
```

**Modified Manifest** (served by LocalVideoServer):
```
#EXTM3U
#EXT-X-VERSION:4
#EXTINF:6.985,
#EXT-X-BYTERANGE:19748464@0
http://127.0.0.1:44683/proxy?url=https%3A%2F%2Fbackend.com%2Fapi%2Fv1%2Ffiles%2Fpayload%3Fss%3D...&manifestId=video-1-manifest
```

### Byte-Range Request Handling

HLS manifests use `#EXT-X-BYTERANGE` to specify segments as byte ranges within a single file:

**Example**:
```
#EXT-X-BYTERANGE:19748464@0         ← bytes 0-19748463
#EXT-X-BYTERANGE:17039952@19748464  ← bytes 19748464-36788415
```

**Player Request**:
```
GET /proxy?url=...&manifestId=video-1-manifest HTTP/1.1
Range: bytes=19748464-36788415
```

**LocalVideoServer Forwards**:
```
GET /api/v1/files/payload?ss=... HTTP/1.1
Range: bytes=19748464-36788415
auth: {clientAuthToken}
```

**Backend Response**:
```
HTTP/1.1 206 Partial Content
Content-Range: bytes 19748464-36788415/123456789
Content-Length: 17039952
```

**LocalVideoServer Streams Back**:
```
HTTP/1.1 206 Partial Content
Content-Range: bytes 19748464-36788415/123456789
Content-Length: 17039952

[streaming bytes...]
```

### Benefits

✅ **Cross-platform**: Single authentication approach for both iOS and Android
✅ **Secure**: Auth tokens never exposed in URLs or manifests
✅ **Efficient**: Streaming eliminates memory buffering
✅ **Standards-compliant**: Properly handles HTTP 206 Partial Content
✅ **Maintainable**: LocalVideoServer is managed externally (test page)

### Testing

Both platforms now work identically:
- ✅ Android: ExoPlayer plays HLS streams via LocalVideoServer proxy
- ✅ iOS: AVPlayer plays HLS streams via LocalVideoServer proxy
- ✅ Byte-range requests handled correctly (206 responses)
- ✅ Authenticated requests with auth header
- ✅ Smooth playback without stuttering

