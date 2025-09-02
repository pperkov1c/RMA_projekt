package com.example.smartparkingapp.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartparkingapp.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    price: Double,
    plate: String,
    zone: String,
    duration: Int,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var cardNumber by remember { mutableStateOf(TextFieldValue("")) }
    var expiryDate by remember { mutableStateOf(TextFieldValue("")) }
    var cvv by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rotation by animateFloatAsState(
        targetValue = if (cvv.isNotEmpty()) 180f else 0f,
        animationSpec = tween(600), label = ""
    )

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (rotation < 90f) Color(0xFF3949AB) else Color(0xFF1A237E),
        animationSpec = tween(600)
    )

    fun formatCardDigits(digitsOnly: String): String {
        return digitsOnly.chunked(4).joinToString(" ")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF3949AB)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Plaćanje parkiranja",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Iznos: %.2f €".format(price),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A237E)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .rotate(rotation)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    if (rotation < 90f) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (cardNumber.text.isEmpty()) "**** **** **** ****" else cardNumber.text,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        blurRadius = 6f
                                    )
                                )
                            )
                            Text(
                                text = "Datum: ${expiryDate.text.ifEmpty { "MM/YY" }}",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f))
                            )
                            Text(
                                text = "CVV ***",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }.take(16)
                        val formatted = formatCardDigits(digits)
                        val delta = formatted.length - newValue.text.length
                        val rawCursor = newValue.selection.start + delta
                        val newCursor = rawCursor.coerceIn(0, formatted.length)
                        cardNumber = TextFieldValue(text = formatted, selection = TextRange(newCursor))
                    },
                    label = { Text("Broj kartice") },
                    placeholder = { Text("1234 5678 9012 3456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Kartica") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { newValue ->
                            val digits = newValue.text.filter { it.isDigit() }.take(4)
                            val formatted = if (digits.length >= 3) {
                                digits.substring(0, 2) + "/" + digits.substring(2)
                            } else digits
                            val delta = formatted.length - newValue.text.length
                            val rawCursor = newValue.selection.start + delta
                            val newCursor = rawCursor.coerceIn(0, formatted.length)
                            expiryDate = TextFieldValue(text = formatted, selection = TextRange(newCursor))
                        },
                        label = { Text("Datum isteka (MM/YY)") },
                        placeholder = { Text("MM/YY") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if (it.length <= 3 && it.all { ch -> ch.isDigit() }) cvv = it
                        },
                        label = { Text("CVV") },
                        placeholder = { Text("***") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Crossfade(targetState = isLoading) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = {
                                val plainCard = cardNumber.text.replace(" ", "")
                                if (plainCard.length < 16) {
                                    Toast.makeText(context, "Unesite ispravan broj kartice.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!expiryDate.text.matches(Regex("""\d{2}/\d{2}"""))) {
                                    Toast.makeText(context, "Unesite ispravan datum isteka (MM/YY).", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (cvv.length < 3) {
                                    Toast.makeText(context, "Unesite ispravan CVV.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                scope.launch {
                                    delay(2000)
                                    currentUser?.let { user ->
                                        val startMillis = System.currentTimeMillis()
                                        val endMillis = startMillis + duration * 60 * 60 * 1000

                                        val parkingData = hashMapOf(
                                            "plate" to plate,
                                            "zone" to zone,
                                            "durationHours" to duration,
                                            "startTime" to startMillis,
                                            "endTime" to endMillis,
                                            "price" to price
                                        )

                                        val userDoc = firestore.collection("users").document(user.uid)

                                        // 1) Spremi u povijest
                                        userDoc.collection("parking_history")
                                            .add(parkingData)

                                        // 2) Spremi i u aktivna
                                        userDoc.collection("activeParkings")
                                            .add(parkingData)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                Toast.makeText(context, "Plaćanje uspješno!", Toast.LENGTH_SHORT).show()
                                                NotificationHelper.showInstantNotification(
                                                    context,
                                                    "Parkiranje započeto",
                                                    "Vaše parkiranje je aktivno do " +
                                                            java.text.SimpleDateFormat("HH:mm").format(java.util.Date(endMillis))
                                                )
                                                onPaymentSuccess()
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                Toast.makeText(context, "Greška pri spremanju parkiranja.", Toast.LENGTH_SHORT).show()
                                            }
                                    } ?: run {
                                        isLoading = false
                                        Toast.makeText(context, "Korisnik nije prijavljen.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer(alpha = pulseAlpha),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Plati", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Natrag", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
