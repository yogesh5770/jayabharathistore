package com.jayabharathistore.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jayabharathistore.app.ui.theme.ErrorRed
import com.jayabharathistore.app.ui.theme.TextSecondary
import com.jayabharathistore.app.ui.theme.WarningYellow

@Composable
fun ShopClosedBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, ErrorRed)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Shop is Closed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
                Text(
                    text = "We are currently not accepting new orders. Please check back later!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DeliveryBusyBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningYellow.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, WarningYellow)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Moped,
                contentDescription = null,
                tint = WarningYellow
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Delivery in Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningYellow
                )
                Text(
                    text = "Our delivery partner is busy. We will accept orders shortly!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
