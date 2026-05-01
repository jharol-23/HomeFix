package com.tunegocio.homefix.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tunegocio.homefix.data.UserPreferences
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {

    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    val darkMode by prefs.darkMode.collectAsState(initial = false)
    val language by prefs.language.collectAsState(initial = "es")
    val notifSound by prefs.notifSound.collectAsState(initial = true)
    val notifVibration by prefs.notifVibration.collectAsState(initial = true)

    var showLanguageDialog by remember { mutableStateOf(false) }

    val languageLabel = when (language) {
        "en" -> "English"
        "pt" -> "Português"
        else -> "Español"
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Seleccionar idioma", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "es" to "🇵🇪  Español",
                        "en" to "🇺🇸  English",
                        "pt" to "🇧🇷  Português"
                    ).forEach { (code, label) ->
                        val isSelected = language == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Primary else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { prefs.setLanguage(code) }
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cerrar", color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuraciones",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sección Apariencia
            SettingsSectionTitle(title = "Apariencia")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconColor = if (darkMode) Color(0xFF90CAF9) else Warning,
                title = if (darkMode) "Modo oscuro" else "Modo claro",
                subtitle = if (darkMode) "Activado" else "Desactivado",
                checked = darkMode,
                onCheckedChange = { scope.launch { prefs.setDarkMode(it) } }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Idioma
            SettingsClickItem(
                icon = Icons.Default.Language,
                iconColor = Info,
                title = "Idioma",
                subtitle = languageLabel,
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección Notificaciones
            SettingsSectionTitle(title = "Notificaciones")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                iconColor = Secondary,
                title = "Sonido",
                subtitle = if (notifSound) "Las notificaciones emiten sonido" else "Sin sonido",
                checked = notifSound,
                onCheckedChange = { scope.launch { prefs.setNotifSound(it) } }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Default.Vibration,
                iconColor = TechnicianColor,
                title = "Vibración",
                subtitle = if (notifVibration) "El dispositivo vibra" else "Sin vibración",
                checked = notifVibration,
                onCheckedChange = { scope.launch { prefs.setNotifVibration(it) } }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(20.dp))
        }
    }
}