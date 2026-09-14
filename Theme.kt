package com.example.rotinacriancas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

private val AzulPrincipal = Color(0xFF1565C0)
private val AzulClaro = Color(0xFFE3F2FD)
private val Texto = Color(0xFF102030)

private val cores = lightColorScheme(
    primary = AzulPrincipal,
    secondary = AzulPrincipal,
    background = Color.White,
    surface = AzulClaro,
    onPrimary = Color.White,
    onBackground = Texto,
    onSurface = Texto
)

// Fontes bem maiores que o padrão do Material, para fácil leitura.
private val tipografiaGrande = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun RotinaCriancasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = cores,
        typography = tipografiaGrande,
        content = content
    )
}
