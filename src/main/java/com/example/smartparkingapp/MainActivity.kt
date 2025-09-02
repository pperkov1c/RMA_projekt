package com.example.smartparkingapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartparkingapp.screens.*
import com.google.firebase.auth.FirebaseAuth
class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()
        createNotificationChannel()

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate("register")
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                    )
                }


                composable("home") {
                    HomeScreen(
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onAddVehicle = { navController.navigate("addVehicle") },
                        onViewZones = { navController.navigate("zones") },
                        onStartParking = { navController.navigate("startParking") },
                        onViewHistory = { navController.navigate("history") },
                        onActiveParking = { navController.navigate("activeParking") }
                    )
                }

                composable("addVehicle") {
                    AddVehicleScreen(onBack = { navController.navigateUp() })
                }

                composable("startParking") {
                    StartParkingScreen(navController = navController)
                }

                composable("activeParking") {
                    ActiveParkingScreen(
                        onExtendParking = { navController.navigate("extendParking") },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable("history") {
                    HistoryScreen(onBack = { navController.navigateUp() })
                }

                composable("extendParking") {
                    ExtendParkingScreen(onBack = { navController.navigateUp() })
                }

                composable("zones") {
                    MapScreen()
                }

                // Payment screen sa svim potrebnim parametrima
                composable(
                    "payment/{plate}/{zone}/{duration}/{price}",
                    arguments = listOf(
                        navArgument("plate") { type = NavType.StringType },
                        navArgument("zone") { type = NavType.StringType },
                        navArgument("duration") { type = NavType.IntType },
                        navArgument("price") { type = NavType.FloatType }
                    )
                ) { backStackEntry ->
                    val plate = backStackEntry.arguments?.getString("plate") ?: ""
                    val zone = backStackEntry.arguments?.getString("zone")?.replace("%20", " ") ?: ""
                    val duration = backStackEntry.arguments?.getInt("duration") ?: 1
                    val price = backStackEntry.arguments?.getFloat("price")?.toDouble() ?: 0.0

                    PaymentScreen(
                        plate = plate,
                        zone = zone,
                        duration = duration,
                        price = price,
                        onBack = { navController.navigateUp() },
                        onPaymentSuccess = {
                            navController.navigate("home") {
                                popUpTo("payment/{plate}/{zone}/{duration}/{price}") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "parking_channel",
                "Obavijesti o parkiranju",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanal za obavijesti o isteku parkiranja"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}
