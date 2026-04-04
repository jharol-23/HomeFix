package com.tunegocio.homefix.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun RegisterScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Campos comunes
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") } // "client" o "technician"

    // Campos solo técnico
    var dni by remember { mutableStateOf("") }
    var yearsExp by remember { mutableStateOf("") }
    var selectedSpecialties by remember { mutableStateOf(listOf<String>()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val specialties = listOf(
        "Electricidad", "Gasfitería", "Carpintería",
        "Pintura", "Albañilería", "Otros"
    )

    fun register() {
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            errorMessage = "Completa todos los campos"
            return
        }
        if (selectedRole.isEmpty()) {
            errorMessage = "Selecciona un rol"
            return
        }
        if (selectedRole == "technician" && dni.isBlank()) {
            errorMessage = "El DNI es obligatorio para técnicos"
            return
        }
        if (selectedRole == "technician" && selectedSpecialties.isEmpty()) {
            errorMessage = "Selecciona al menos una especialidad"
            return
        }

        isLoading = true
        errorMessage = ""

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val user = UserModel(
                    uid = uid,
                    name = name.trim(),
                    email = email.trim(),
                    role = selectedRole,
                    phone = phone.trim(),
                    dni = if (selectedRole == "technician") dni.trim() else "",
                    yearsExp = yearsExp.toIntOrNull() ?: 0,
                    specialties = selectedSpecialties,
                    whatsapp = phone.trim(),
                    createdAt = System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        isLoading = false
                        if (selectedRole == "technician") {
                            navController.navigate(Routes.HOME_TECHNICIAN) {
                                popUpTo(Routes.REGISTER) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.HOME_CLIENT) {
                                popUpTo(Routes.REGISTER) { inclusive = true }
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = when {
                    e.message?.contains("email") == true -> "Ese correo ya está registrado"
                    e.message?.contains("password") == true -> "La contraseña debe tener mínimo 6 caracteres"
                    else -> "Error al registrarse"
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Completa tus datos para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selección de rol
            Text(
                text = "¿Cómo quieres usar HomeFix?",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    title = "Cliente",
                    description = "Necesito un técnico",
                    emoji = "🙋",
                    isSelected = selectedRole == "client",
                    color = ClientColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "client" }
                )
                RoleCard(
                    title = "Técnico",
                    description = "Ofrezco servicios",
                    emoji = "🔧",
                    isSelected = selectedRole == "technician",
                    color = TechnicianColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "technician" }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Campos comunes
            HomefixTextField(value = name, onValueChange = { name = it }, label = "Nombre completo")
            Spacer(modifier = Modifier.height(12.dp))
            HomefixTextField(value = email, onValueChange = { email = it }, label = "Correo electrónico")
            Spacer(modifier = Modifier.height(12.dp))
            HomefixTextField(value = password, onValueChange = { password = it }, label = "Contraseña", isPassword = true)
            Spacer(modifier = Modifier.height(12.dp))
            HomefixTextField(value = phone, onValueChange = { phone = it }, label = "Número de teléfono")

            // Campos solo para técnico
            if (selectedRole == "technician") {
                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Datos del técnico",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                HomefixTextField(value = dni, onValueChange = { dni = it }, label = "DNI (8 dígitos)")
                Spacer(modifier = Modifier.height(12.dp))
                HomefixTextField(value = yearsExp, onValueChange = { yearsExp = it }, label = "Años de experiencia")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Especialidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Chips de especialidades
                specialties.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { specialty ->
                            val isSelected = selectedSpecialties.contains(specialty)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSpecialties = if (isSelected) {
                                        selectedSpecialties - specialty
                                    } else {
                                        selectedSpecialties + specialty
                                    }
                                },
                                label = { Text(specialty, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomefixButton(
                text = "Crear cuenta",
                onClick = { register() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(text = "¿Ya tienes cuenta? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Inicia sesión",
                    color = Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Routes.LOGIN) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    emoji: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else CardBackground
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, color)
        else
            androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = if (isSelected) color else TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}