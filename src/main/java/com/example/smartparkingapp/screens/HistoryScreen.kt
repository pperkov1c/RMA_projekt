package com.example.smartparkingapp.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var parkingHistory by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    var selectedPlate by remember { mutableStateOf("Svi") }
    var selectedZone by remember { mutableStateOf("Sve") }
    var plateOptions by remember { mutableStateOf(listOf("Svi")) }
    var zoneOptions by remember { mutableStateOf(listOf("Sve")) }
    var sortDescending by remember { mutableStateOf(true) }

    // Animirana pozadina
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

    // Dohvat podataka
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            val snapshot = firestore.collection("users")
                .document(user.uid)
                .collection("parking_history")
                .get()
                .await()

            val data = snapshot.documents.mapNotNull { it.data }
            parkingHistory = data

            plateOptions = listOf("Svi") + data.mapNotNull { it["plate"] as? String }.distinct()
            zoneOptions = listOf("Sve") + data.mapNotNull { it["zone"] as? String }.distinct()
        }
    }

    val filteredHistory = parkingHistory
        .filter {
            (selectedPlate == "Svi" || it["plate"] == selectedPlate) &&
                    (selectedZone == "Sve" || it["zone"] == selectedZone)
        }
        .sortedBy { (it["startTime"] as? Timestamp)?.toDate() }
        .let { if (sortDescending) it.reversed() else it }

    val totalSpent = filteredHistory.sumOf { it["price"] as? Double ?: 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Povijest parkiranja", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1976D2), Color(0xFF42A5F5))
                    )
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(animatedTopColor, animatedBottomColor)))
                .padding(padding)
                .padding(16.dp)
        ) {
            // Filteri
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterCard(
                    label = "Registracija",
                    options = plateOptions,
                    selected = selectedPlate,
                    onSelected = { selectedPlate = it },
                    modifier = Modifier.weight(1f)
                )
                FilterCard(
                    label = "Zona",
                    options = zoneOptions,
                    selected = selectedZone,
                    onSelected = { selectedZone = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        selectedPlate = "Svi"
                        selectedZone = "Sve"
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF1976D2))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ukupno i sortiranje
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ukupno: ${formatPrice(totalSpent)}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { sortDescending = !sortDescending }) {
                        Text(if (sortDescending) "Najnovije" else "Najstarije", color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista
            if (filteredHistory.isEmpty()) {
                Text("Nema parkiranja za odabrane filtere.", color = Color.White, fontSize = 16.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredHistory) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCard(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) { // Svaki filter ima svoj Box
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Text(selected, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun HistoryCard(item: Map<String, Any>) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val timestamp = item["startTime"] as? Timestamp
    val formattedDate = timestamp?.toDate()?.let { dateFormat.format(it) } ?: "Nepoznat datum"
    val plate = item["plate"] as? String ?: "Nepoznato"
    val zone = item["zone"] as? String ?: "Nepoznata zona"
    val duration = (item["durationHours"] as? Long)?.toInt() ?: 0
    val price = item["price"] as? Double ?: 0.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🚗 $plate", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("📍 $zone", fontSize = 14.sp, color = Color.DarkGray)
            Text("🕒 $formattedDate", fontSize = 14.sp, color = Color.DarkGray)
            Text("⏳ $duration h", fontSize = 14.sp, color = Color.DarkGray)
            Text("💰 ${formatPrice(price)}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 15.sp)
        }
    }
}

fun formatPrice(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("hr", "HR"))
    return format.format(value)
}
