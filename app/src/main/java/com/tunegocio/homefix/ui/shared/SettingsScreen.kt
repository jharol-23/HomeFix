package com.tunegocio.homefix.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    // Estado del modo oscuro (por ahora solo visual, se puede expandir)
    var modoOscuro by remember { mutableStateOf(false) }
    var vibracion by remember { mutableStateOf(true) }
    var sonido by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sección apariencia
        SettingsSectionTitle(titulo = "Apariencia")

        SettingsToggleItem(
            icono = Icons.Default.DarkMode,
            titulo = "Modo oscuro",
            descripcion = "Cambia el tema de la app",
            valor = modoOscuro,
            onCambio = { modoOscuro = it }
        )

        // Sección notificaciones
        SettingsSectionTitle(titulo = "Notificaciones")

        SettingsToggleItem(
            icono = Icons.Default.Vibration,
            titulo = "Vibración",
            descripcion = "Vibra al recibir alertas importantes",
            valor = vibracion,
            onCambio = { vibracion = it }
        )

        SettingsToggleItem(
            icono = Icons.Default.VolumeUp,
            titulo = "Sonido",
            descripcion = "Sonido al recibir notificaciones",
            valor = sonido,
            onCambio = { sonido = it }
        )

        // Sección idioma
        SettingsSectionTitle(titulo = "Idioma")

        SettingsClickItem(
            icono = Icons.Default.Language,
            titulo = "Idioma de la app",
            descripcion = "Español (Perú)",
            onClick = { /* Por implementar */ }
        )

        // Sección cuenta
        SettingsSectionTitle(titulo = "Cuenta")

        SettingsClickItem(
            icono = Icons.Default.Lock,
            titulo = "Privacidad",
            descripcion = "Términos y condiciones",
            onClick = { /* Por implementar */ }
        )

        // Botón cerrar sesión
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = {
                    // Cierra sesión y regresa al login
                    auth.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Título de sección
@Composable
fun SettingsSectionTitle(titulo: String) {
    Text(
        text = titulo,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Primary,
        fontWeight = FontWeight.SemiBold
    )
}

// Item con toggle
@Composable
fun SettingsToggleItem(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    valor: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = valor,
                onCheckedChange = onCambio,
                colors = SwitchDefaults.colors(checkedTrackColor = Primary)
            )
        }
    }
}

// Item clickeable
@Composable
fun SettingsClickItem(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}