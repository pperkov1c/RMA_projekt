package com.example.smartparkingapp.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartparkingapp.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendParkingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var activeDocId by remember { mutableStateOf<String?>(null) }
    var additionalDuration by remember { mutableStateOf(1) }
    var currentDuration by remember { mutableStateOf(0) }
    var currentPrice by remember { mutableStateOf(0.0) }
    var activeEndTime by remember { mutableStateOf<LocalTime?>(null) }
    var zoneName by remember { mutableStateOf<String?>(null) }

    val pricePerZone = mapOf(
        "Zona 1 - Centar" to 1.5,
        "Zona 2 - Tržnica" to 1.0,
        "Zona 3 - Stanica" to 0.5
    )

    val startLimit = LocalTime.of(7, 0)
    val endLimit = LocalTime.of(18, 0)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    // Dohvat zadnjeg parkiranja
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("parking_history")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    doc?.let {
                        activeDocId = it.id
                        currentDuration = (it["durationHours"] as? Long ?: 0L).toInt()
                        currentPrice = (it["price"] as? Number)?.toDouble() ?: 0.0
                        zoneName = it["zone"] as? String

                        val startTime = (it["startTime"] as? Timestamp)?.toDate()?.toInstant()
                        if (startTime != null) {
                            val startLocal = startTime.atZone(ZoneId.systemDefault()).toLocalTime()
                            activeEndTime = startLocal.plusHours(currentDuration.toLong())
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Greška pri dohvaćanju podataka", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val pricePerHour = zoneName?.let { pricePerZone[it] ?: 1.0 } ?: 1.0
    val now = LocalTime.now()

    // Provjera vremena
    val isAfterEnd = now.isAfter(endLimit)
    val isParkingAllowedNow = now.isAfter(startLimit.minusSeconds(1)) && now.isBefore(endLimit.plusSeconds(1))

    // Od kojeg vremena kreće produženje
    val baseTime: LocalTime? = when {
        isAfterEnd -> null // poslije 18h nema produženja
        activeEndTime != null && activeEndTime!!.isAfter(now) -> activeEndTime // još traje
        else -> now // isteklo, ali prije 18h
    }

    // Maksimalni dodatni sati do 18h
    val maxAdditionalHours = baseTime?.let {
        Duration.between(it, endLimit).toHours().toInt().coerceAtLeast(0)
    } ?: 0

    // Novo trajanje, cijena i završetak
    val newDuration = currentDuration + additionalDuration
    val newPrice = currentPrice + additionalDuration * pricePerHour
    val newEndTime = baseTime?.plusHours(additionalDuration.toLong())

    // Animacije
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = ""
    )
    val animatedTopColor = lerp(Color(0xFF1976D2), Color(0xFF42A5F5), colorShift)
    val animatedBottomColor = lerp(Color(0xFF90CAF9), Color(0xFFE3F2FD), colorShift)

    val iconOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = ""
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produži parkiranje", fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Natrag", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onBack()
                    }) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Odjava", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(Color(0xFF1976D2), Color(0xFF42A5F5)))
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showPaymentDialog = true },
                    enabled = activeDocId != null &&
                            isParkingAllowedNow &&
                            baseTime != null &&
                            additionalDuration > 0 &&
                            additionalDuration <= maxAdditionalHours,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text("Produži parkiranje", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (!isParkingAllowedNow) {
                    Text(
                        "Produženje je moguće od 7-18h",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(animatedTopColor, animatedBottomColor)))
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.car),
                    contentDescription = "Auto",
                    modifier = Modifier
                        .size(70.dp)
                        .offset(y = iconOffsetY.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            InfoCard(title = "Trenutno trajanje: ", value = "$currentDuration h")
            InfoCard(title = "Trenutna cijena: ", value = String.format("%.2f €", currentPrice))
            zoneName?.let { InfoCard(title = "Zona parkiranja: ", value = it) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Dodaj trajanje (sati)", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { if (additionalDuration > 1) additionalDuration-- },
                            enabled = additionalDuration > 1,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.LightGray
                            )
                        ) { Text("-") }
                        Text("$additionalDuration h", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { if (additionalDuration < maxAdditionalHours) additionalDuration++ },
                            enabled = additionalDuration < maxAdditionalHours,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.LightGray
                            )
                        ) { Text("+") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nova cijena: ${String.format("%.2f €", newPrice)}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    newEndTime?.let {
                        Text(
                            "Novo vrijeme završetka: ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = if (maxAdditionalHours > 0) additionalDuration.toFloat() / maxAdditionalHours else 1f
                    val progressColor = if (additionalDuration >= maxAdditionalHours) Color.Red else Color(0xFF1976D2)
                    LinearProgressIndicator(
                        progress = progress,
                        color = progressColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Maksimalno dodavanje: $maxAdditionalHours h", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }

    // ---- PLATNI DIALOG ----
    if (showPaymentDialog) {
        val scope = rememberCoroutineScope()
        val transition = rememberInfiniteTransition(label = "")
        val rotation by transition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )

        AlertDialog(
            onDismissRequest = { if (!isProcessingPayment) showPaymentDialog = false },
            confirmButton = {},
            title = null,
            text = {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = "Plaćanje",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer(rotationZ = rotation)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Potvrda plaćanja",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Dodaješ:", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            "+$additionalDuration h",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Cijena produženja:", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            String.format("%.2f €", newPrice - currentPrice),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { showPaymentDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessingPayment
                            ) { Text("Odustani") }
                            Button(
                                onClick = {
                                    isProcessingPayment = true
                                    scope.launch {
                                        delay(2000)
                                        currentUser?.let { user ->
                                            activeDocId?.let { docId ->
                                                firestore.collection("users")
                                                    .document(user.uid)
                                                    .collection("parking_history")
                                                    .document(docId)
                                                    .update(
                                                        mapOf(
                                                            "durationHours" to newDuration.toLong(),
                                                            "price" to newPrice.toDouble(),
                                                            "endTime" to Timestamp.now().toDate()
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        isProcessingPayment = false
                                                        Toast.makeText(
                                                            context,
                                                            "Parkiranje produženo!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        showPaymentDialog = false
                                                        onBack()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isProcessingPayment = false
                                                        Toast.makeText(
                                                            context,
                                                            "Greška: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                            }
                                        }
                                    }
                                },
                                enabled = !isProcessingPayment,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Text("Plati")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 14.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
