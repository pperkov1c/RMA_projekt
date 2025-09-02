package com.example.smartparkingapp.screens

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ActiveParkingScreen(
    onExtendParking: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var activeParking by remember { mutableStateOf<Map<String, Any>?>(null) }
    var remainingTimeText by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(1f) }

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

    val startColor = Color(0xFF4A90E2)
    val endColor = Color(0xFF50E3C2)
    val animatedTopColor = lerp(startColor, endColor, colorShift)
    val animatedBottomColor = lerp(endColor, startColor, colorShift)

    // Dohvat zadnjeg parkiranja
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .collection("parking_history")
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val document = snapshot.documents.firstOrNull()
                    document?.data?.let { data ->

                        // 🔹 Robustno dohvaćanje startTime
                        val start: Timestamp? = when (val s = data["startTime"]) {
                            is Timestamp -> s
                            is java.util.Date -> Timestamp(s)
                            is Long -> Timestamp(s / 1000, 0) // u milisekundama
                            else -> null
                        }

                        val duration = (data["durationHours"] as? Long ?: 0L).toInt()
                        val endTimeMillis = start?.toDate()?.time?.plus(duration * 60 * 60 * 1000)
                        val now = System.currentTimeMillis()

                        Log.d("ActiveParking", "startTime je $start, duration je $duration, endTimeMillis=$endTimeMillis, now=$now")

                        if (endTimeMillis != null && now < endTimeMillis) {
                            activeParking = data

                            object : CountDownTimer(endTimeMillis - now, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val h = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                                    val m = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                                    val s = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                                    remainingTimeText = String.format("%02d:%02d:%02d", h, m, s)

                                    val total = duration * 60 * 60 * 1000L
                                    progress = millisUntilFinished.toFloat() / total.toFloat()
                                }

                                override fun onFinish() {
                                    remainingTimeText = "Parkiranje je isteklo."
                                    progress = 0f
                                    activeParking = null
                                }
                            }.start()
                        } else {
                            // Ako je parkiranje isteklo
                            activeParking = null
                        }
                    } ?: run {
                        activeParking = null
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Greška pri dohvaćanju podataka", Toast.LENGTH_SHORT).show()
                    Log.e("ActiveParking", "Greška: ", it)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(animatedTopColor, animatedBottomColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Header
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = "Parking",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Aktivno parkiranje",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (activeParking != null) {
                val plate = activeParking!!["plate"] as? String ?: ""
                val zone = activeParking!!["zone"] as? String ?: ""
                val duration = (activeParking!!["durationHours"] as? Long ?: 0L).toInt()
                val price = activeParking!!["price"] as? Double ?: 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Registracija vozila", fontSize = 14.sp, color = Color.Gray)
                        Text(plate, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        AssistChip(
                            onClick = {},
                            label = { Text("Zona $zone") },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4A90E2).copy(alpha = 0.2f),
                                labelColor = Color(0xFF4A90E2)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("Trajanje: $duration h", fontSize = 16.sp)
                        Text("Cijena: %.2f €".format(price), fontSize = 16.sp)

                        Spacer(Modifier.height(24.dp))

                        // 🔹 Timer s progress barom
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color(0xFF4A90E2),
                            trackColor = Color.LightGray.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Preostalo vrijeme:",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Text(
                            remainingTimeText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A90E2)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onExtendParking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                        ) {
                            Text("Produži parkiranje", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Natrag", fontSize = 16.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Trenutno nemate aktivno parkiranje.", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Natrag", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
