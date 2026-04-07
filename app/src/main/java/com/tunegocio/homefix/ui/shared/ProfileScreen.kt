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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.theme.*

@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var user by remember { mutableStateOf<UserModel?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val u = doc.toObject(UserModel::class.java)
                user = u
                name = u?.name ?: ""
                phone = u?.phone ?: ""
                bio = u?.bio ?: ""
                isLoading = false
            }
    }

    fun saveProfile() {
        isSaving = true
        val updates = mutableMapOf<String, Any>(
            "name" to name.trim(),
            "phone" to phone.trim(),
            "whatsapp" to phone.trim()
        )
        if (user?.role == "technician") {
            updates["bio"] = bio.trim()
        }
        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                isSaving = false
                successMessage = "Perfil actualizado correctamente"
            }
            .addOnFailureListener {
                isSaving = false
                successMessage = "Error al guardar"
            }
    }

    fun logout() {
        auth.signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    // Diálogo de confirmación logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Cerrar sesión",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro que deseas cerrar sesión?")
            },
            confirmButton = {
                TextButton(
                    onClick = { logout() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val u = user ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Mi perfil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(45.dp),
                color = if (u.role == "technician")
                    TechnicianColor.copy(alpha = 0.15f)
                else
                    ClientColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = u.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (u.role == "technician") TechnicianColor else ClientColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge de rol
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (u.role == "technician")
                    TechnicianColor.copy(alpha = 0.1f)
                else
                    ClientColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (u.role == "technician") "🔧 Técnico" else "🙋 Cliente",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (u.role == "technician") TechnicianColor else ClientColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // Calificación si tiene
            if (u.rating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ ${"%.1f".format(u.rating)} calificación",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Campos editables
            Text(
                text = "Datos personales",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomefixTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre completo"
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Email — solo lectura
            OutlinedTextField(
                value = u.email,
                onValueChange = {},
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = CardBorder,
                    disabledLabelColor = TextSecondary,
                    disabledTextColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            HomefixTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Número de WhatsApp"
            )

            // Campos extra para técnico
            if (u.role == "technician") {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Información profesional",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Descripción profesional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text(
                            "Cuéntale a los clientes sobre tu experiencia...",
                            color = TextHint
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Especialidades (solo lectura por ahora)
                if (u.specialties.isNotEmpty()) {
                    Text(
                        text = "Especialidades",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        u.specialties.take(3).forEach { specialty ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TechnicianColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = specialty,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 5.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TechnicianColor
                                )
                            }
                        }
                    }
                }

                // Años de experiencia
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${u.yearsExp} años de experiencia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (successMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Success.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = "Guardar cambios",
                onClick = { saveProfile() },
                isLoading = isSaving
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón cerrar sesión
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cerrar sesión",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}