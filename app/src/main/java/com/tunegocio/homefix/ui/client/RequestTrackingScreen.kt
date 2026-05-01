package com.tunegocio.homefix.ui.client

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
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.tunegocio.homefix.data.NotificationsRepository



@Composable
fun RequestTrackingScreen(
    navController: NavController,
    requestId: String
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current


    var request by remember { mutableStateOf<RequestModel?>(null) }


    // Lista de técnicos interesados con sus datos
    var tecnicosInteresados by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var eligiendoTecnicoId by remember { mutableStateOf("") } // ID del técnico que se está procesando

    val notificationsRepo = remember { NotificationsRepository() }


    var technician by remember { mutableStateOf<UserModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Escuchar cambios en tiempo real
    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                request = snapshot?.toObject(RequestModel::class.java)
                // Cargar datos de cada técnico interesado
                val interesados = snapshot?.get("interestedTechnicians") as? List<String> ?: emptyList()
                if (interesados.isNotEmpty()) {
                    val listaTemp = mutableListOf<UserModel>()
                    var pendientes = interesados.size
                    interesados.forEach { techId ->
                        db.collection("users").document(techId).get()
                            .addOnSuccessListener { doc ->
                                doc.toObject(UserModel::class.java)?.let { listaTemp.add(it.copy(uid = techId)) }
                                pendientes--
                                if (pendientes == 0) tecnicosInteresados = listaTemp.toList()
                            }
                            .addOnFailureListener {
                                pendientes--
                                if (pendientes == 0) tecnicosInteresados = listaTemp.toList()
                            }
                    }
                } else {
                    tecnicosInteresados = emptyList()
                }

                // Cargar técnico si ya fue asignado
                val techId = snapshot?.getString("technicianId") ?: ""
                if (techId.isNotEmpty()) {
                    db.collection("users").document(techId).get()
                        .addOnSuccessListener { doc ->
                            technician = doc.toObject(UserModel::class.java)
                        }
                }
            }
    }

    fun cancelRequest() {
        db.collection("requests").document(requestId)
            .update(
                mapOf(
                    "status" to "cancelada",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                navController.navigate(Routes.HOME_CLIENT) {
                    popUpTo(Routes.HOME_CLIENT) { inclusive = true }
                }
            }
    }

    fun openWhatsApp(phone: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = "Hola, soy el cliente de HomeFix. ¿Cómo va el servicio?"
        val uri = Uri.parse("https://wa.me/$fullNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) { }
    }
    fun elegirTecnico(tecnicoElegidoId: String) {
        eligiendoTecnicoId = tecnicoElegidoId
        db.collection("requests").document(requestId)
            .update(
                mapOf(
                    "status" to "aceptada",
                    "technicianId" to tecnicoElegidoId,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                // Notificar al técnico elegido
                notificationsRepo.crearNotificacion(
                    userId = tecnicoElegidoId,
                    titulo = "¡Te eligieron!",
                    cuerpo = "El cliente eligió tu propuesta para ${request?.serviceType ?: ""}. Prepárate para ir.",
                    tipo = "tecnico_elegido",
                    requestId = requestId
                )
                // Notificar a los técnicos NO elegidos
                tecnicosInteresados
                    .filter { it.uid != tecnicoElegidoId }
                    .forEach { tecnico ->
                        notificationsRepo.crearNotificacion(
                            userId = tecnico.uid,
                            titulo = "Solicitud asignada a otro",
                            cuerpo = "El cliente eligió a otro técnico para esta solicitud.",
                            tipo = "tecnico_rechazado",
                            requestId = requestId
                        )
                    }
                eligiendoTecnicoId = ""
            }
            .addOnFailureListener {
                eligiendoTecnicoId = ""
            }
    }
    // Diálogo de cancelación
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text("Cancelar solicitud", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Estás seguro que deseas cancelar esta solicitud?")
            },
            confirmButton = {
                TextButton(
                    onClick = { cancelRequest() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("Sí, cancelar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, mantener")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Mi solicitud",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Botón cancelar solo si está pendiente
                if (req.status == "pendiente") {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Error)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso de estados
            StatusProgressBar(status = req.status)

            Spacer(modifier = Modifier.height(20.dp))
            // Sección técnicos interesados — solo cuando está pendiente y hay interesados
            if (req.status == "pendiente" && tecnicosInteresados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Técnicos interesados (${tecnicosInteresados.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                tecnicosInteresados.forEach { tecnico ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Avatar con inicial
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = TechnicianColor.copy(alpha = 0.15f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = tecnico.name.firstOrNull()?.toString()?.uppercase() ?: "T",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TechnicianColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tecnico.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (tecnico.rating > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Warning,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${"%.1f".format(tecnico.rating)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        if (tecnico.specialties.isNotEmpty()) {
                                            Text(
                                                text = tecnico.specialties.take(2).joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Botón Elegir
                            Button(
                                onClick = { elegirTecnico(tecnico.uid) },
                                enabled = eligiendoTecnicoId.isEmpty(),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Success)
                            ) {
                                if (eligiendoTecnicoId == tecnico.uid) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "✓ Elegir este técnico",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Detalle de la solicitud
            Text(
                text = "Detalle",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 5.dp
                                ),
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
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 5.dp
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Error
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = req.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    if (req.address.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = req.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    // Foto si tiene
                    if (req.imageUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = req.imageUrls.first(),
                            contentDescription = "Foto del problema",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Técnico asignado
            if (technician != null && req.status != "pendiente") {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Técnico asignado",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = TechnicianColor.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = technician!!.name
                                                .firstOrNull()?.toString()
                                                ?.uppercase() ?: "T",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TechnicianColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = technician!!.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (technician!!.rating > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Warning,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${"%.1f".format(technician!!.rating)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    if (technician!!.specialties.isNotEmpty()) {
                                        Text(
                                            text = technician!!.specialties.take(2).joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Botón WhatsApp al técnico
                        if (technician!!.whatsapp.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { openWhatsApp(technician!!.whatsapp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen
                                )
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Contactar por WhatsApp",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Mensaje si está completada
            if (req.status == "completada") {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Success.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Servicio completado",
                            style = MaterialTheme.typography.titleMedium,
                            color = Success,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "¿Cómo fue tu experiencia?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                navController.navigate(Routes.rating(requestId))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Calificar servicio",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusProgressBar(status: String) {
    val steps = listOf(
        "pendiente" to "Pendiente",
        "en_revision" to "En revisión",
        "aceptada" to "Aceptada",
        "en_camino" to "En camino",
        "completada" to "Completada"
    )

    val currentIndex = steps.indexOfFirst { it.first == status }
        .let { if (it == -1) 0 else it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estado del servicio",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, (_, label) ->
                val isDone = index < currentIndex
                val isCurrent = index == currentIndex
                val isPending = index > currentIndex

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Línea vertical + círculo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Círculo del paso
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isDone -> Success
                                isCurrent -> Primary
                                else -> SurfaceVariant
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDone) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        // Línea conectora
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(28.dp)
                                    .background(
                                        color = if (isDone) Success else CardBorder,
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Texto del paso
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isDone -> Success
                                isCurrent -> Primary
                                else -> TextSecondary
                            },
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isCurrent) {
                            Text(
                                text = getStatusDescription(status),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (index < steps.size - 1) {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

fun getStatusDescription(status: String): String {
    return when (status) {
        "pendiente" -> "Esperando que un técnico acepte..."
        "en_revision" -> "Un técnico está evaluando tu solicitud"
        "aceptada" -> "¡Un técnico aceptó tu solicitud!"
        "en_camino" -> "El técnico está en camino a tu ubicación"
        "completada" -> "El servicio fue completado exitosamente"
        "cancelada" -> "La solicitud fue cancelada"
        else -> ""
    }
}