package com.example.smartparkingapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*

data class Zone(
    val name: String,
    val latLng: LatLng,
    val price: String,
    val info: String
)

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var selectedZone by remember { mutableStateOf<Zone?>(null) }
    var nearestZone by remember { mutableStateOf<Zone?>(null) }

    val zones = listOf(
        Zone("Zona 1", LatLng(45.5600, 18.6753), "Cijena: 1,50€/h", "Parkiralište u centru grada (Trg Ante Starčevića)"),
        Zone("Zona 2", LatLng(45.5608, 18.6785), "Cijena: 1,0€/h", "Parkiralište uz tržnicu u Gornjem gradu"),
        Zone("Zona 3", LatLng(45.5533, 18.6797), "Cijena: 0,50€/h", "Parkiralište kod autobusnog kolodvora Osijek")
    )


    Column {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = { mapView }) { view ->
                mapView.getMapAsync { map ->
                    googleMap = map
                    map.uiSettings.isZoomControlsEnabled = true

                    zones.forEach { zone ->
                        map.addMarker(
                            MarkerOptions()
                                .position(zone.latLng)
                                .title(zone.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }

                    map.setOnMarkerClickListener { marker ->
                        val zone = zones.find { it.latLng == marker.position }
                        zone?.let { selectedZone = it }
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
                        true
                    }

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                val userLatLng = LatLng(it.latitude, it.longitude)

                                map.addMarker(
                                    MarkerOptions()
                                        .position(userLatLng)
                                        .title("Vi ste ovdje")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                )

                                if (selectedZone == null) {
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                                }

                                nearestZone = zones.minByOrNull { zone ->
                                    FloatArray(1).also { result ->
                                        Location.distanceBetween(
                                            userLatLng.latitude, userLatLng.longitude,
                                            zone.latLng.latitude, zone.latLng.longitude, result
                                        )
                                    }[0]
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Nedostaje dozvola za lokaciju!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            // Animacija za odabranu zonu
            AnimatedVisibility(
                visible = selectedZone != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                selectedZone?.let { zone ->
                    ZoneCard(zone, highlight = false)
                }
            }

            // Animacija za najbližu zonu
            AnimatedVisibility(
                visible = nearestZone != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                nearestZone?.let { zone ->
                    ZoneCard(zone, highlight = true)
                }
            }
        }
    }
}

@Composable
fun ZoneCard(zone: Zone, highlight: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFFBBDEFB) else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalParking,
                contentDescription = "Parking",
                tint = if (highlight) Color(0xFF0D47A1) else Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (highlight) "Najbliža zona: ${zone.name}" else zone.name,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) Color(0xFF0D47A1) else Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(zone.info, color = if (highlight) Color(0xFF0D47A1) else Color(0xFF555555))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    zone.price,
                    color = if (highlight) Color(0xFF0D47A1) else Color(0xFF0D47A1),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}
