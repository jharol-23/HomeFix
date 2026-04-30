package com.tunegocio.homefix.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tunegocio.homefix.data.model.RequestModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.components.HomefixButton
import com.tunegocio.homefix.ui.components.HomefixTextField
import com.tunegocio.homefix.ui.theme.*
import java.io.File
import java.util.UUID

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.tunegocio.homefix.data.CloudinaryUploader

import androidx.compose.material.icons.filled.PhotoCamera

import com.tunegocio.homefix.data.ALL_SPECIALTIES

@SuppressLint("MissingPermission")
@Composable
fun NewRequestScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    val scope = rememberCoroutineScope()


    var serviceType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var locationLoaded by remember { mutableStateOf(false) }

    var reference by remember { mutableStateOf("") }
    var clientDistrict by remember { mutableStateOf("") }

    /*val serviceTypes = listOf(
        "Electricidad", "Gasfitería", "Carpintería",
        "Pintura", "Albañilería", "Otros"
    )*/

    // Dentro del composable usa directamente:
    val serviceTypes = ALL_SPECIALTIES


    // Archivo temporal para la foto
    val photoFile = remember {
        File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg")
    }
    val photoUriForCamera = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri = photoUriForCamera
    }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { photoUri = it }
    }

    // Launcher para permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(photoUriForCamera)
    }

    // Launcher para permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
            fusedLocation.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    lat = it.latitude
                    lng = it.longitude
                    //SOLO ME MUESTRA 4 DECIMALES, DESPUES DEL PUNTO, LO MEJOR ES A 6

                    address = "Lat: ${"%.6f".format(lat)}, Lng: ${"%.6f".format(lng)}"
                    locationLoaded = true
                }
            }
        }
    }

    // Obtener ubicación automáticamente al abrir AJUSTAR ESTO
    LaunchedEffect(Unit) {

        // Cargar distrito del cliente
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                clientDistrict = doc.getString("district") ?: ""
            }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    fun publishRequest() {
        if (serviceType.isEmpty()) { errorMessage = "Selecciona el tipo de servicio"; return }
        if (description.length < 20) { errorMessage = "La descripción debe tener al menos 20 caracteres"; return }

        isLoading = true
        errorMessage = ""

        fun saveRequest(imageUrl: String) {
            val requestId = UUID.randomUUID().toString()
            val request = RequestModel(
                requestId = requestId,
                clientId = uid,
                serviceType = serviceType,
                description = description,
                imageUrls = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
                lat = lat,
                lng = lng,
                address = address,

                district = clientDistrict,    // ← nuevo
                reference = reference,         // ← nuevo

                status = "pendiente",
                isUrgent = isUrgent,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.collection("requests").document(requestId).set(request)
                .addOnSuccessListener {
                    isLoading = false
                    navController.navigate(Routes.HOME_CLIENT) {
                        popUpTo(Routes.NEW_REQUEST) { inclusive = true }
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = "Error al publicar la solicitud"
                }
        }

        if (photoUri != null) {
            scope.launch {
                val result = CloudinaryUploader.uploadImage(
                    context = context,
                    uri = photoUri!!,
                    folder = "homefix/requests"
                )
                result.fold(
                    onSuccess = { url -> saveRequest(url) },
                    onFailure = { saveRequest("") }
                )
            }
        } else {
            saveRequest("")
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
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nueva solicitud",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tipo de servicio
            Text(
                text = "Tipo de servicio",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            serviceTypes.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { type ->
                        FilterChip(
                            selected = serviceType == type,
                            onClick = { serviceType = type },
                            label = {
                                Text(
                                    type,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Descripción
            Text(
                text = "Describe el problema",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        "Ej: El tomacorriente de la cocina no funciona, hay un corto...",
                        color = TextHint
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = CardBorder
                )
            )
            Text(
                text = "${description.length}/500",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Foto
            Text(
                text = "Foto del problema (opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (photoUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto del problema",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { photoUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Quitar foto",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón cámara
                    OutlinedButton(
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        //Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cámara")
                    }
                    // Botón galería
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería")
                    }
                }
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
                colors = CardDefaults.cardColors(
                    containerColor = if (locationLoaded)
                        Success.copy(alpha = 0.08f)
                    else SurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (locationLoaded) Success else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locationLoaded) address else "Obteniendo ubicación...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (locationLoaded) TextPrimary else TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HomefixTextField(
                value = address,
                onValueChange = { address = it },
                label = "O escribe la dirección manualmente"
            )


            //NUEVO TEXTBOX PARA AGREGAR REFERENCIA DE DIRECCION:

            Spacer(modifier = Modifier.height(8.dp))
            HomefixTextField(
                value = reference,
                onValueChange = { reference = it },
                label = "Referencia (opcional)",
                singleLine = false
            )
            Text(
                text = "Ej: Frente al parque, casa de rejas azules",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )



            Spacer(modifier = Modifier.height(16.dp))

            // Toggle urgente
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUrgent)
                        Error.copy(alpha = 0.08f)
                    else SurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Urgente",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUrgent) Error else TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Prioridad alta para los técnicos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Error
                        )
                    )
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

            HomefixButton(
                text = "Publicar solicitud",
                onClick = { publishRequest() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}