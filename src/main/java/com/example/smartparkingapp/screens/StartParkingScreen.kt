package com.example.smartparkingapp.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartparkingapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun StartParkingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var selectedPlate by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf("Zona 1 - Centar") }
    var duration by remember { mutableStateOf(1) }
    var vehicles by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pricePerZone = mapOf(
        "Zona 1 - Centar" to 1.5,
        "Zona 2 - Tržnica" to 1.0,
        "Zona 3 - Stanica" to 0.5
    )

    val pricePerHour = pricePerZone[selectedZone] ?: 1.0
    val totalPrice = duration * pricePerHour

    val currentTime = LocalTime.now()
    val startLimit = LocalTime.of(7, 0)
    val endLimit = LocalTime.of(18, 0)
    val isParkingAllowedNow = currentTime.isAfter(startLimit.minusSeconds(1)) &&
            currentTime.isBefore(endLimit)

    // Izračun kraja parkiranja
    val endTime = remember(duration, currentTime) {
        val rawEnd = currentTime.plusHours(duration.toLong())
        if (rawEnd.isAfter(endLimit)) endLimit else rawEnd
    }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Dohvati vozila
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("vehicles")
                .get()
                .addOnSuccessListener { snapshot ->
                    vehicles = snapshot.documents.mapNotNull { it.getString("plate") }
                    if (vehicles.isNotEmpty()) selectedPlate = vehicles.first()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Greška pri dohvaćanju vozila", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Animacije
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val animatedTopColor = lerp(Color(0xFF1976D2), Color(0xFF42A5F5), colorShift)
    val animatedBottomColor = lerp(Color(0xFF90CAF9), Color(0xFFE3F2FD), colorShift)

    val carOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Započni parkiranje", fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Odjava", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5))
                    )
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 36.dp), // povećan vertikalni padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonEnabled = isParkingAllowedNow && vehicles.isNotEmpty()
                Button(
                    onClick = {
                        val safeZone = Uri.encode(selectedZone)
                        navController.navigate("payment/$selectedPlate/$safeZone/$duration/$totalPrice")
                    },
                    enabled = buttonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .alpha(if (buttonEnabled) 1f else 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text("Započni parkiranje", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp)) // dodan razmak između gumba i poruka

                if (!isParkingAllowedNow) {
                    Text(
                        "Radno vrijeme od 7-18h",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                errorMessage?.let {
                    Text(
                        it,
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
            // Auto ikona
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
                        .offset(y = carOffsetY.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (vehicles.isNotEmpty()) {
                SelectionCard("Odaberi vozilo", selectedPlate, vehicles) { selectedPlate = it }
                SelectionCard("Odaberi zonu", selectedZone, pricePerZone.keys.toList()) { selectedZone = it }
                DurationSelector(
                    duration = duration,
                    endTime = endTime.format(timeFormatter),
                    onMinus = { if (duration > 1) duration-- },
                    onPlus = {
                        val potentialEnd = currentTime.plusHours((duration + 1).toLong())
                        if (potentialEnd.isAfter(endLimit)) {
                            errorMessage = "Parkiranje se ne može započeti poslije 18h"
                        } else {
                            duration++
                            errorMessage = null
                        }
                    },
                    enabled = isParkingAllowedNow
                )
                PriceCard(pricePerHour, totalPrice)
            } else {
                Text(
                    "Nemate dodanih vozila.",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SelectionCard(title: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {  // Omotavanje kartice i dropdowna
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = TextStyle(fontSize = 14.sp, color = Color.Gray))
                    Text(selected, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium))
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Više opcija", tint = Color.Gray)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth() // Širina kao kartica
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun DurationSelector(duration: Int, endTime: String, onMinus: () -> Unit, onPlus: () -> Unit, enabled: Boolean = true) {
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
            Text("Trajanje (sati)", fontSize = 14.sp, color = Color.Gray)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onMinus,
                    enabled = enabled && duration > 1,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.LightGray
                    )
                ) { Text("-") }

                Text("$duration h", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Button(
                    onClick = onPlus,
                    enabled = enabled,
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
            Text(
                "Parkiranje završava u $endTime",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun PriceCard(pricePerHour: Double, totalPrice: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cijena po satu: ", fontWeight = FontWeight.Medium)
                Text(String.format("%.2f €", pricePerHour))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ukupno: ", fontWeight = FontWeight.Medium)
                Text(String.format("%.2f €", totalPrice))
            }
        }
    }
}
