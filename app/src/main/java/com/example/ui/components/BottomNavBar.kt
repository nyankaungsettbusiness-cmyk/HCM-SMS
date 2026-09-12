package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HcmBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = currentRoute == ScreenRoute.Dashboard.route,
            onClick = { onNavigate(ScreenRoute.Dashboard.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = currentRoute == ScreenRoute.Students.route,
            onClick = { onNavigate(ScreenRoute.Students.route) },
            icon = { Icon(Icons.Default.People, contentDescription = "Students") },
            label = { Text("Students", fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = currentRoute == ScreenRoute.Teachers.route,
            onClick = { onNavigate(ScreenRoute.Teachers.route) },
            icon = { Icon(Icons.Default.Badge, contentDescription = "Teachers") },
            label = { Text("Teachers", fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = currentRoute == ScreenRoute.Attendance.route,
            onClick = { onNavigate(ScreenRoute.Attendance.route) },
            icon = { Icon(Icons.Default.HowToReg, contentDescription = "Attendance") },
            label = { Text("Attendance", fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = currentRoute == ScreenRoute.Reports.route,
            onClick = { onNavigate(ScreenRoute.Reports.route) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Report Cards") },
            label = { Text("Report Cards", fontWeight = FontWeight.SemiBold) }
        )
    }
}

