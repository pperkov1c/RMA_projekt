package com.example.smartparkingapp.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartparkingapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddVehicleScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var plate by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // 🔹 Animacija pozadine
    val infiniteTransition = rememberInfiniteTransition()
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val startColor = Color(0xFF1976D2)
    val endColor = Color(0xFF90CAF9)
    val animatedTopColor = lerp(startColor, endColor, colorShift)
    val animatedBottomColor = lerp(endColor, startColor, colorShift)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(animatedTopColor, animatedBottomColor)
                )
            )
    ) {
        // Naslov i ikona
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.car),
                contentDescription = null,
                modifier = Modifier.size(80.dp).alpha(0.9f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Dodaj vozilo",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Forma u kartici (usklađena s prethodnim ekranom)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Registracija (npr. ZG123AB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        message = ""
                        if (plate.isEmpty()) {
                            message = "Unesite registraciju."
                            return@Button
                        }
                        if (!plate.matches(Regex("^[A-ZČĆŽŠĐ]{1,2}\\d{3,4}[A-ZČĆŽŠĐ]{1,2}$"))) {
                            message = "Neispravan format registracije."
                            return@Button
                        }

                        currentUser?.let { user ->
                            isLoading = true
                            val vehicleData = hashMapOf("plate" to plate)
                            firestore.collection("users")
                                .document(user.uid)
                                .collection("vehicles")
                                .add(vehicleData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, "Vozilo dodano!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    message = "Greška: ${e.localizedMessage}"
                                }
                        } ?: run {
                            message = "Korisnik nije prijavljen."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Dodaj vozilo", fontSize = 16.sp)
                    }
                }

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(message, color = Color.Red, fontSize = 14.sp)
                }
            }
        }
    }
}
