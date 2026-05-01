package com.tunegocio.homefix.ui.technician

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.theme.*

@Composable
fun RequestDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val technicianId = auth.currentUser?.uid ?: ""

    var request by remember { mutableStateOf<RequestModel?>(null) }
    var client by remember { mutableStateOf<UserModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Repositorio para crear notificaciones
    val notificationsRepo = remember { com.tunegocio.homefix.data.NotificationsRepository() }


    LaunchedEffect(requestId) {
        // Cargar solicitud
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                request = doc.toObject(RequestModel::class.java)
                isLoading = false
                // Cargar datos del cliente
                request?.clientId?.let { clientId ->
                    db.collection("users").document(clientId).get()
                        .addOnSuccessListener { clientDoc ->
                            client = clientDoc.toObject(UserModel::class.java)
                        }
                }
            }
    }

    fun acceptRequest() {
        actionLoading = true
        // Agrega el técnico a la lista de interesados sin cambiar el status
        db.collection("requests").document(requestId)
            .update(
                mapOf(
                    "interestedTechnicians" to com.google.firebase.firestore.FieldValue.arrayUnion(technicianId),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                // Notifica al cliente que hay un técnico interesado
                request?.clientId?.let { clientId ->
                    notificationsRepo.crearNotificacion(
                        userId = clientId,
                        titulo = "¡Un técnico está interesado!",
                        cuerpo = "Alguien quiere atender tu solicitud de ${request?.serviceType}. Revísala para elegir.",
                        tipo = "tecnico_aceptado",
                        requestId = requestId
                    )
                }
                actionLoading = false
                // Se queda en pantalla mostrando confirmación
                request = request?.copy(
                    interestedTechnicians = (request?.interestedTechnicians ?: emptyList()) + technicianId
                )
            }
            .addOnFailureListener {
                actionLoading = false
                errorMessage = "Error al registrar interés"
            }
    }

    fun rejectRequest() {
        navController.popBackStack()
    }

    fun markInProgress() {
        actionLoading = true
        db.collection("requests").document(requestId)
            .update(
                mapOf(
                    "status" to "en_camino",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                actionLoading = false
                request = request?.copy(status = "en_camino")
                // Notificar al cliente que el técnico está en camino
                request?.clientId?.let { clientId ->
                    notificationsRepo.crearNotificacion(
                        userId = clientId,
                        titulo = "¡Tu técnico está en camino!",
                        cuerpo = "El técnico ya salió hacia tu ubicación para atender tu solicitud de ${request?.serviceType}.",
                        tipo = "en_camino",
                        requestId = requestId
                    )
                }
            }
    }

    fun markCompleted() {
        actionLoading = true
        db.collection("requests").document(requestId)
            .update(
                mapOf(
                    "status" to "completada",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                // Notificar al cliente que el servicio fue completado
                request?.clientId?.let { clientId ->
                    notificationsRepo.crearNotificacion(
                        userId = clientId,
                        titulo = "Servicio completado",
                        cuerpo = "Tu solicitud de ${request?.serviceType} fue marcada como completada. ¡No olvides calificar al técnico!",
                        tipo = "completado",
                        requestId = requestId
                    )
                }
                actionLoading = false
                navController.navigate(Routes.HOME_TECHNICIAN) {
                    popUpTo(Routes.HOME_TECHNICIAN) { inclusive = true }
                }
            }
    }

    fun openWhatsApp(phone: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = "Hola, vi tu solicitud en HomeFix y me gustaría ayudarte."
        val uri = Uri.parse("https://wa.me/$fullNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            errorMessage = "WhatsApp no está instalado"
        }
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

    val req = request ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detalle de solicitud",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tipo y urgencia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = req.serviceType,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (req.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⚡ Urgente",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Foto del problema
            if (req.imageUrls.isNotEmpty()) {
                Text(
                    text = "Foto del problema",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = req.imageUrls.first(),
                    contentDescription = "Foto del problema",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Descripción
            Text(
                text = "Descripción",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
            ) {
                Text(
                    text = req.description,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ubicación
            Text(
                text = "Ubicación",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = req.address.ifEmpty { "Ubicación no especificada" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Datos del cliente
            client?.let { c ->
                Text(
                    text = "Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = ClientColor.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = c.name.firstOrNull()?.toString() ?: "C",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ClientColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (c.rating > 0) {
                                    Text(
                                        text = "⭐ ${"%.1f".format(c.rating)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        // Botón WhatsApp
                        if (c.phone.isNotEmpty()) {
                            Button(
                                onClick = { openWhatsApp(c.phone) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "WhatsApp",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de acción según estado
            when (req.status) {
                "pendiente", "en_revision" -> {
                    HomefixButton(
                        text = "👍 Me interesa",
                        onClick = { acceptRequest() },
                        isLoading = actionLoading,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { rejectRequest() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ){
                        Text("✕ No me interesa", style = MaterialTheme.typography.labelLarge)
                    }
                }
                "aceptada" -> {
                    HomefixButton(
                        text = "🚗 Estoy en camino",
                        onClick = { markInProgress() },
                        isLoading = actionLoading,
                        color = Info
                    )
                }
                "en_camino" -> {
                    HomefixButton(
                        text = "✓ Marcar como completado",
                        onClick = { markCompleted() },
                        isLoading = actionLoading,
                        color = Success
                    )
                }
                "completada" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Success.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✓ Servicio completado",
                                style = MaterialTheme.typography.titleMedium,
                                color = Success,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}