# Dependency Injection with Koin

This project uses [Koin](https://insert-koin.io/) for dependency injection. Koin is a lightweight DI framework designed for Kotlin and works seamlessly with Compose Multiplatform.

## Quick Start

### Injecting Dependencies in Composables

```kotlin
import org.koin.compose.koinInject

@Composable
fun MyScreen() {
    val myService: MyService = koinInject()
    // Use myService
}
```

### Injecting in ViewModels

```kotlin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyScreen() {
    val viewModel: MyViewModel = koinViewModel()
}
```

---

## Registering New Dependencies

### Step 1: Create Your Class

```kotlin
// In lib/ or appropriate package
class MyRepository(private val httpClient: HttpClient) {
    suspend fun fetchData(): List<Data> { ... }
}
```

### Step 2: Register in AppModule

Open `di/AppModule.kt` and add your dependency:

```kotlin
val appModule = module {
    // Existing dependencies
    singleOf(::YouAuthManager)
    
    // Add your new dependency
    singleOf(::MyRepository)
}
```

### Registration Types

| Type | Usage | Example |
|------|-------|---------|
| `singleOf(::Class)` | Singleton - one instance shared | Services, Managers, Repositories |
| `factoryOf(::Class)` | Factory - new instance each call | Use cases, Presenters |
| `single { }` | Singleton with custom init | Complex initialization |
| `factory { }` | Factory with custom init | Parameterized creation |

### With Dependencies

```kotlin
val appModule = module {
    // HttpClient is created first
    single { createHttpClient() }
    
    // MyRepository receives HttpClient via get()
    single { MyRepository(get()) }
    
    // Or use constructor injection
    singleOf(::MyRepository)  // Koin resolves HttpClient automatically
}
```

---

## Creating Feature Modules

For larger features, create separate modules:

### Step 1: Create Feature Module

```kotlin
// In di/FeatureModule.kt
val driveModule = module {
    singleOf(::DriveRepository)
    factoryOf(::FetchFilesUseCase)
}
```

### Step 2: Register in allModules

```kotlin
// In di/AppModule.kt
val allModules = listOf(
    appModule,
    driveModule
)
```

---

## Testing with Koin

### Mock Dependencies in Tests

```kotlin
@Test
fun testWithMock() = runTest {
    val mockRepo = MockRepository()
    
    startKoin {
        modules(module {
            single<MyRepository> { mockRepo }
        })
    }
    
    // Test code
    
    stopKoin()
}
```

---

## Best Practices

1. **Use constructor injection** - Prefer `singleOf(::Class)` over manual `single { Class(get()) }`
2. **Keep modules focused** - One module per feature area
3. **Avoid circular dependencies** - Services shouldn't depend on each other in a cycle
4. **Use interfaces** - Define interfaces for repositories/services for easier testing
5. **Lazy initialization** - Dependencies are created only when first requested

---

## Common Patterns

### Repository Pattern

```kotlin
// Interface in lib/
interface UserRepository {
    suspend fun getUser(id: String): User
}

// Implementation in lib/
class UserRepositoryImpl(private val api: ApiClient) : UserRepository {
    override suspend fun getUser(id: String) = api.fetchUser(id)
}

// Registration
val appModule = module {
    single<UserRepository> { UserRepositoryImpl(get()) }
}
```

### ViewModel with Repository

```kotlin
class HomeViewModel(
    private val userRepo: UserRepository
) : ViewModel() {
    // ViewModel logic
}

// Registration
val appModule = module {
    viewModelOf(::HomeViewModel)
}
```
