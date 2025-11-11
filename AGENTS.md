# Agent Guidelines for HomebaseKmpPoc

## Build/Test Commands
- **Build project**: `./gradlew build`
- **Run all tests**: `./gradlew test`
- **Run single test**: `./gradlew test --tests "id.homebase.homebasekmppoc.ComposeAppCommonTest.example"`
- **Lint code**: `./gradlew lint`
- **Auto-fix lint issues**: `./gradlew lintFix`

## Code Style Guidelines

### Naming Conventions
- **Packages**: lowercase with dots (e.g., `id.homebase.homebasekmppoc`)
- **Classes/Interfaces**: PascalCase (e.g., `Greeting`, `Platform`, `AndroidPlatform`)
- **Functions/Methods**: camelCase (e.g., `greet()`, `getPlatform()`)
- **Variables/Properties**: camelCase (e.g., `showContent`, `greeting`)
- **Constants**: UPPER_SNAKE_CASE (not used in examples)

### Imports
- Group androidx.compose imports first
- Then other androidx imports
- Then org.jetbrains imports
- Finally generated resource imports
- Use single imports, not wildcard imports

### Kotlin Multiplatform Patterns
- Use `expect`/`actual` for platform-specific implementations
- Common code goes in `commonMain`
- Platform-specific code in `{platform}Main` (e.g., `androidMain`, `iosMain`)

### Compose UI Patterns
- Use `@Composable` annotation for UI functions
- Use `@Preview` for composable previews
- Apply `MaterialTheme` wrapper for consistent theming
- Use `remember` for state that survives recomposition
- Chain modifiers with dot notation

### Error Handling
- Use try/catch blocks for expected exceptions
- Prefer Result/Sealed classes for complex error states
- Log errors appropriately for debugging

### Type Safety
- Use explicit types for public APIs and complex expressions
- Use type inference for obvious cases
- Prefer immutable collections (`List` over `MutableList`) when possible
- Use nullable types (`?`) only when necessary

### Testing
- Use `kotlin.test` framework for common tests
- Place tests in `commonTest` for shared logic
- Use descriptive test names that explain the behavior being tested</content>
<parameter name="filePath">/Users/seb/code/odin/kmp/odin-kmp/AGENTS.md