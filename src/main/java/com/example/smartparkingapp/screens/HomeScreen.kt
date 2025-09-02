package com.example.smartparkingapp.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.smartparkingapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAddVehicle: () -> Unit,
    onViewZones: () -> Unit,
    onStartParking: () -> Unit,
    onActiveParking: () -> Unit,
    onViewHistory: () -> Unit,
) {
    // 🔹 Dinamična pozadina
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // Mijenjamo nijansu plave
    val startColor = Color(0xFF2196F3)
    val endColor = Color(0xFF64B5F6)
    val animatedTopColor = lerp(startColor, endColor, colorShift)
    val animatedBottomColor = lerp(Color(0xFFBBDEFB), Color(0xFFE3F2FD), colorShift)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(" Smart parKING", fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Odjava")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(animatedTopColor, animatedBottomColor)
                    )
                )
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Animacija plutanja auta
            val carOffsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )

            Image(
                painter = painterResource(id = R.drawable.car),
                contentDescription = "Auto",
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = carOffsetY.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dobrodošli natrag!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grid layout s animiranim karticama
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedHomeCard("Dodaj vozilo", Icons.Filled.Add, 0, onAddVehicle, Modifier.weight(1f))
                    AnimatedHomeCard("Započni parkiranje", Icons.Filled.LocalParking, 1, onStartParking, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedHomeCard("Aktivno parkiranje", Icons.Filled.CarRental, 2, onActiveParking, Modifier.weight(1f))
                    AnimatedHomeCard("Povijest", Icons.Filled.History, 3, onViewHistory, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedHomeCard("Karta zona", Icons.Filled.Map, 4, onViewZones, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AnimatedHomeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.8f, label = "")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, label = "")

    LaunchedEffect(Unit) {
        delay(index * 100L) // efekt "jedna po jedna"
        visible = true
    }

    Card(
        modifier = modifier
            .height(120.dp)
            .scale(scale)
            .alpha(alpha)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF2196F3), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
