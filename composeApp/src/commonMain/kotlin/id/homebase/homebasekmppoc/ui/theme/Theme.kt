package id.homebase.homebasekmppoc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Light color scheme using LightColors */
private val LightColorScheme =
        lightColorScheme(
                primary = LightColors.Primary,
                onPrimary = LightColors.OnPrimary,
                primaryContainer = LightColors.PrimaryContainer,
                onPrimaryContainer = LightColors.OnPrimaryContainer,
                secondary = LightColors.Secondary,
                onSecondary = LightColors.OnSecondary,
                secondaryContainer = LightColors.SecondaryContainer,
                onSecondaryContainer = LightColors.OnSecondaryContainer,
                tertiary = LightColors.Tertiary,
                onTertiary = LightColors.OnTertiary,
                tertiaryContainer = LightColors.TertiaryContainer,
                onTertiaryContainer = LightColors.OnTertiaryContainer,
                background = LightColors.Background,
                onBackground = LightColors.OnBackground,
                surface = LightColors.Surface,
                onSurface = LightColors.OnSurface,
                surfaceVariant = LightColors.SurfaceVariant,
                onSurfaceVariant = LightColors.OnSurfaceVariant,
                error = LightColors.Error,
                onError = LightColors.OnError,
                errorContainer = LightColors.ErrorContainer,
                onErrorContainer = LightColors.OnErrorContainer,
                outline = LightColors.Outline,
                outlineVariant = LightColors.OutlineVariant
        )

/** Dark color scheme using DarkColors */
private val DarkColorScheme =
        darkColorScheme(
                primary = DarkColors.Primary,
                onPrimary = DarkColors.OnPrimary,
                primaryContainer = DarkColors.PrimaryContainer,
                onPrimaryContainer = DarkColors.OnPrimaryContainer,
                secondary = DarkColors.Secondary,
                onSecondary = DarkColors.OnSecondary,
                secondaryContainer = DarkColors.SecondaryContainer,
                onSecondaryContainer = DarkColors.OnSecondaryContainer,
                tertiary = DarkColors.Tertiary,
                onTertiary = DarkColors.OnTertiary,
                tertiaryContainer = DarkColors.TertiaryContainer,
                onTertiaryContainer = DarkColors.OnTertiaryContainer,
                background = DarkColors.Background,
                onBackground = DarkColors.OnBackground,
                surface = DarkColors.Surface,
                onSurface = DarkColors.OnSurface,
                surfaceVariant = DarkColors.SurfaceVariant,
                onSurfaceVariant = DarkColors.OnSurfaceVariant,
                error = DarkColors.Error,
                onError = DarkColors.OnError,
                errorContainer = DarkColors.ErrorContainer,
                onErrorContainer = DarkColors.OnErrorContainer,
                outline = DarkColors.Outline,
                outlineVariant = DarkColors.OutlineVariant
        )

/**
 * Homebase app theme with dark/light mode support.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param content The content to display with this theme.
 */
@Composable
fun HomebaseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = HomebaseTypography, content = content)
}
