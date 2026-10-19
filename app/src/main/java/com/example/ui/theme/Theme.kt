package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeOption(
    val code: String,
    val title: String,
    val subtitle: String,
    val primarySwatch: Color,
    val containerSwatch: Color,
    val surfaceSwatch: Color,
    val isDarkPreview: Boolean = false
) {
    SAME_AS_DEVICE(
        code = "SAME_AS_DEVICE",
        title = "Same as Device",
        subtitle = "Ikuti Tema Sistem HP",
        primarySwatch = Color(0xFF1E40AF),
        containerSwatch = Color(0xFF1E293B),
        surfaceSwatch = Color(0xFFF8FAFC)
    ),
    LIGHT(
        code = "LIGHT",
        title = "Light",
        subtitle = "Mode Terang Bersih",
        primarySwatch = Color(0xFF2563EB),
        containerSwatch = Color(0xFFDBEAFE),
        surfaceSwatch = Color(0xFFFFFFFF)
    ),
    DARK(
        code = "DARK",
        title = "Dark",
        subtitle = "Mode Gelap Elegan",
        primarySwatch = Color(0xFF93C5FD),
        containerSwatch = Color(0xFF1E3A8A),
        surfaceSwatch = Color(0xFF0F172A),
        isDarkPreview = true
    ),
    MATERIAL_BLUE(
        code = "MATERIAL_BLUE",
        title = "Material Blue",
        subtitle = "Biru Laut Profesional",
        primarySwatch = Color(0xFF1D4ED8),
        containerSwatch = Color(0xFFDBEAFE),
        surfaceSwatch = Color(0xFFF0F7FF)
    ),
    MATERIAL_GREEN(
        code = "MATERIAL_GREEN",
        title = "Material Green",
        subtitle = "Hijau Zamrud Segar",
        primarySwatch = Color(0xFF15803D),
        containerSwatch = Color(0xFFDCFCE7),
        surfaceSwatch = Color(0xFFF0FDF4)
    ),
    MATERIAL_RED(
        code = "MATERIAL_RED",
        title = "Material Red",
        subtitle = "Merah Crimson Berani",
        primarySwatch = Color(0xFFB91C1C),
        containerSwatch = Color(0xFFFEE2E2),
        surfaceSwatch = Color(0xFFFEF2F2)
    ),
    MATERIAL_GREY(
        code = "MATERIAL_GREY",
        title = "Material Grey",
        subtitle = "Abu-abu Monokrom Modern",
        primarySwatch = Color(0xFF334155),
        containerSwatch = Color(0xFFE2E8F0),
        surfaceSwatch = Color(0xFFF8FAFC)
    ),
    MATERIAL_PINK(
        code = "MATERIAL_PINK",
        title = "Material Pink",
        subtitle = "Merah Muda Rose Ceria",
        primarySwatch = Color(0xFFBE185D),
        containerSwatch = Color(0xFFFCE7F3),
        surfaceSwatch = Color(0xFFFDF2F8)
    ),
    MATERIAL_YELLOW(
        code = "MATERIAL_YELLOW",
        title = "Material Yellow",
        subtitle = "Kuning Emas Hangat",
        primarySwatch = Color(0xFFB45309),
        containerSwatch = Color(0xFFFEF3C7),
        surfaceSwatch = Color(0xFFFFFBEB)
    ),
    MATERIAL_SAGE_GREEN(
        code = "MATERIAL_SAGE_GREEN",
        title = "Material Sage Green",
        subtitle = "Hijau Sage Natural",
        primarySwatch = Color(0xFF3F624D),
        containerSwatch = Color(0xFFD8E8DD),
        surfaceSwatch = Color(0xFFF4F8F5)
    ),
    MATERIAL_VIOLET(
        code = "MATERIAL_VIOLET",
        title = "Material Violet",
        subtitle = "Ungu Royal Mewah",
        primarySwatch = Color(0xFF6D28D9),
        containerSwatch = Color(0xFFEDE9FE),
        surfaceSwatch = Color(0xFFF5F3FF)
    );

    companion object {
        fun fromCode(code: String): AppThemeOption {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SAME_AS_DEVICE
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = OnPrimaryBlueDark,
    primaryContainer = PrimaryBlueContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SecondarySlateDark,
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = OnPrimaryBlueContainer,
    secondary = SecondarySlate,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight
)

private val MaterialBlueScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0284C7),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFFF4F8FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF6FF),
    outline = Color(0xFF93C5FD)
)

private val MaterialGreenScheme = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = Color(0xFF0D9488),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    background = Color(0xFFF3FAF5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECFDF5),
    outline = Color(0xFF86EFAC)
)

private val MaterialRedScheme = lightColorScheme(
    primary = Color(0xFFB91C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = Color(0xFFE11D48),
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF881337),
    background = Color(0xFFFFF5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFEF2F2),
    outline = Color(0xFFFCA5A5)
)

private val MaterialGreyScheme = lightColorScheme(
    primary = Color(0xFF334155),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF475569),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF1E293B),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    outline = Color(0xFF94A3B8)
)

private val MaterialPinkScheme = lightColorScheme(
    primary = Color(0xFFBE185D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF831843),
    secondary = Color(0xFFDB2777),
    secondaryContainer = Color(0xFFFDF2F8),
    onSecondaryContainer = Color(0xFF500724),
    background = Color(0xFFFFF5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFDF2F8),
    outline = Color(0xFFF9A8D4)
)

private val MaterialYellowScheme = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFD97706),
    secondaryContainer = Color(0xFFFFFBEB),
    onSecondaryContainer = Color(0xFF78350F),
    background = Color(0xFFFFFDF4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFEF9C3),
    outline = Color(0xFFFCD34D)
)

private val MaterialSageGreenScheme = lightColorScheme(
    primary = Color(0xFF3F624D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8DD),
    onPrimaryContainer = Color(0xFF1E3528),
    secondary = Color(0xFF52796F),
    secondaryContainer = Color(0xFFE6F0EC),
    onSecondaryContainer = Color(0xFF223831),
    background = Color(0xFFF4F8F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF1EC),
    outline = Color(0xFFA3BFA8)
)

private val MaterialVioletScheme = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF7C3AED),
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF3B0764),
    background = Color(0xFFF8F5FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F3FF),
    outline = Color(0xFFC4B5FD)
)

fun getColorSchemeForThemeOption(
    themeOptionCode: String,
    systemInDarkTheme: Boolean
): ColorScheme {
    return when (AppThemeOption.fromCode(themeOptionCode)) {
        AppThemeOption.SAME_AS_DEVICE -> if (systemInDarkTheme) DarkColorScheme else LightColorScheme
        AppThemeOption.LIGHT -> LightColorScheme
        AppThemeOption.DARK -> DarkColorScheme
        AppThemeOption.MATERIAL_BLUE -> MaterialBlueScheme
        AppThemeOption.MATERIAL_GREEN -> MaterialGreenScheme
        AppThemeOption.MATERIAL_RED -> MaterialRedScheme
        AppThemeOption.MATERIAL_GREY -> MaterialGreyScheme
        AppThemeOption.MATERIAL_PINK -> MaterialPinkScheme
        AppThemeOption.MATERIAL_YELLOW -> MaterialYellowScheme
        AppThemeOption.MATERIAL_SAGE_GREEN -> MaterialSageGreenScheme
        AppThemeOption.MATERIAL_VIOLET -> MaterialVioletScheme
    }
}

@Composable
fun MyApplicationTheme(
    themeOptionCode: String = AppThemeOption.SAME_AS_DEVICE.code,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getColorSchemeForThemeOption(
        themeOptionCode = themeOptionCode,
        systemInDarkTheme = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
