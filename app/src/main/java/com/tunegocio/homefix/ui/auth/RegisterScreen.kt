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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.components.isValidEmail
import com.tunegocio.homefix.ui.theme.*

import com.tunegocio.homefix.ui.components.PasswordRequirements
import com.tunegocio.homefix.ui.components.validatePassword

@Composable
fun RegisterScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Campos comunes
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }

    // Campos solo técnico
    var dni by remember { mutableStateOf("") }
    var yearsExp by remember { mutableStateOf("") }
    var selectedSpecialties by remember { mutableStateOf(listOf<String>()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Errores por campo
    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var dniError by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }
    var specialtyError by remember { mutableStateOf("") }

    val specialties = listOf(
        "Electricidad", "Gasfitería", "Carpintería",
        "Pintura", "Albañilería", "Otros"
    )

    fun register() {
        // Limpiar errores anteriores
        nameError = ""
        emailError = ""
        passwordError = ""
        phoneError = ""
        dniError = ""
        roleError = ""
        specialtyError = ""
        errorMessage = ""

        var hasError = false

        // Validar nombre
        if (name.isBlank()) {
            nameError = "El nombre es obligatorio"
            hasError = true
        } else if (name.trim().length < 3) {
            nameError = "Mínimo 3 caracteres"
            hasError = true
        }

        // Validar email
        if (email.isBlank()) {
            emailError = "El correo es obligatorio"
            hasError = true
        } else if (!isValidEmail(email)) {
            emailError = "Ingresa un correo válido (ej: correo@gmail.com)"
            hasError = true
        }

        // Validar contraseña
        if (password.isBlank()) {
            passwordError = "La contraseña es obligatoria"
            hasError = true
        } else {
            val validation = validatePassword(password)
            if (!validation.hasMinLength) {
                passwordError = "Mínimo 8 caracteres"
                hasError = true
            } else if (!validation.hasUppercase) {
                passwordError = "Debe tener al menos una mayúscula"
                hasError = true
            } else if (!validation.hasNumber) {
                passwordError = "Debe tener al menos un número"
                hasError = true
            }
        }

        // Validar confirmación de contraseña
        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Confirma tu contraseña"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPasswordError = "Las contraseñas no coinciden"
            hasError = true
        }

        /* Validar contraseña
        if (password.isBlank()) {
            passwordError = "La contraseña es obligatoria"
            hasError = true
        } else if (password.length < 6) {
            passwordError = "Mínimo 6 caracteres"
            hasError = true
        }*/

        // Validar teléfono
        if (phone.isBlank()) {
            phoneError = "El teléfono es obligatorio"
            hasError = true
        } else if (phone.length < 9) {
            phoneError = "Ingresa un número válido de 9 dígitos"
            hasError = true
        }

        // Validar rol
        if (selectedRole.isEmpty()) {
            roleError = "Selecciona cómo quieres usar HomeFix"
            hasError = true
        }

        // Validaciones solo para técnico
        if (selectedRole == "technician") {
            if (dni.isBlank()) {
                dniError = "El DNI es obligatorio"
                hasError = true
            } else if (dni.length != 8) {
                dniError = "El DNI debe tener exactamente 8 dígitos"
                hasError = true
            }
            if (selectedSpecialties.isEmpty()) {
                specialtyError = "Selecciona al menos una especialidad"
                hasError = true
            }
        }

        if (hasError) return

        isLoading = true

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
                when {
                    e.message?.contains("email") == true ->
                        emailError = "Ese correo ya está registrado"
                    e.message?.contains("password") == true ->
                        passwordError = "La contraseña debe tener mínimo 6 caracteres"
                    else -> errorMessage = "Error al registrarse, intenta de nuevo"
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
                    onClick = {
                        selectedRole = "client"
                        roleError = ""
                    }
                )
                RoleCard(
                    title = "Técnico",
                    description = "Ofrezco servicios",
                    emoji = "🔧",
                    isSelected = selectedRole == "technician",
                    color = TechnicianColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedRole = "technician"
                        roleError = ""
                    }
                )
            }
            // Error de rol
            if (roleError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = roleError,
                    color = Error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre
            HomefixTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = ""
                },
                label = "Nombre completo",
                isError = nameError.isNotEmpty(),
                errorMessage = nameError
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Email
            HomefixTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (it.isNotBlank()) emailError = ""
                },
                label = "Correo electrónico",
                isError = emailError.isNotEmpty(),
                errorMessage = emailError,
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(12.dp))

            /* Contraseña
            HomefixTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (it.isNotBlank()) passwordError = ""
                },
                label = "Contraseña",
                isPassword = true,
                isError = passwordError.isNotEmpty(),
                errorMessage = passwordError
            )
            Spacer(modifier = Modifier.height(12.dp))*/

            // Contraseña
            HomefixTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (it.isNotBlank()) passwordError = ""
                    confirmPasswordError = "" // limpia también confirmación al cambiar
                },
                label = "Contraseña",
                isPassword = true,
                isError = passwordError.isNotEmpty(),
                errorMessage = passwordError
            )
            // Requisitos en tiempo real
            PasswordRequirements(password = password)

            Spacer(modifier = Modifier.height(12.dp))

            // Confirmar contraseña
            HomefixTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (it.isNotBlank()) confirmPasswordError = ""
                },
                label = "Confirmar contraseña",
                isPassword = true,
                isError = confirmPasswordError.isNotEmpty(),
                errorMessage = confirmPasswordError
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Teléfono
            HomefixTextField(
                value = phone,
                onValueChange = {
                    if (it.all { char -> char.isDigit() } && it.length <= 9) {
                        phone = it
                        if (it.isNotBlank()) phoneError = ""
                    }
                },
                label = "Número de teléfono",
                isError = phoneError.isNotEmpty(),
                errorMessage = phoneError,
                keyboardType = KeyboardType.Number
            )

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

                // DNI
                HomefixTextField(
                    value = dni,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 8) {
                            dni = it
                            if (it.isNotBlank()) dniError = ""
                        }
                    },
                    label = "DNI (8 dígitos)",
                    isError = dniError.isNotEmpty(),
                    errorMessage = dniError,
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Años de experiencia
                HomefixTextField(
                    value = yearsExp,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                            yearsExp = it
                        }
                    },
                    label = "Años de experiencia",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Especialidades
                Text(
                    text = "Especialidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                                    specialtyError = ""
                                },
                                label = {
                                    Text(
                                        specialty,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Error de especialidad
                if (specialtyError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = specialtyError,
                        color = Error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                    )
                }
            }

            // Error general
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
                Text(
                    text = "¿Ya tienes cuenta? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Inicia sesión",
                    color = Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.LOGIN)
                    }
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
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) color else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}