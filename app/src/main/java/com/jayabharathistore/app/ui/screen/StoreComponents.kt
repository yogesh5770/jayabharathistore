package com.jayabharathistore.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.ui.theme.*

@Composable
fun UserList(
    users: List<com.jayabharathistore.app.data.model.User>,
    onDeleteUser: (String) -> Unit = {}
) {
    var userToDelete by remember { mutableStateOf<com.jayabharathistore.app.data.model.User?>(null) }
    
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Neutral300
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No users registered yet", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(PrimaryPurpleSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name.ifBlank { "Anonymous" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (user.phoneNumber.isNotBlank()) {
                                Text(
                                    text = "+91 ${user.phoneNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        // Delete button
                        IconButton(
                            onClick = { userToDelete = user }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete User",
                                tint = ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove ${user.name.ifBlank { user.email }} from the system? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    product: Product? = null,
    onDismiss: () -> Unit,
    onProductAdded: (Product, android.net.Uri?) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var description by remember(product) { mutableStateOf(product?.description ?: "") }
    var price by remember(product) { mutableStateOf(product?.price?.toString() ?: "") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var category by remember(product) { mutableStateOf(product?.category ?: "") }
    var stockQuantity by remember(product) { mutableStateOf(product?.stockQuantity?.toString() ?: "10") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { 
            imageUri = it 
            android.widget.Toast.makeText(context, "Image Selected!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Add New Product" else "Edit Product",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image Picker Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Neutral100)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (imageUri != null) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(imageUri),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else if (product?.imageUrl?.isNotBlank() == true) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(product.imageUrl),
                                contentDescription = "Current Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (imageUri == null && product?.imageUrl?.isBlank() != false) Icons.Default.Add else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUri == null && product?.imageUrl?.isBlank() != false) "Select Image" else "Change Image")
                        }
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange
                    )
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) price = it },
                    label = { Text("Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange
                    )
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange
                    )
                )
                
                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d+$"))) stockQuantity = it },
                    label = { Text("Stock Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalProduct = (product ?: Product()).copy(
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        category = category,
                        stockQuantity = stockQuantity.toIntOrNull() ?: 10,
                        inStock = if (product == null) true else product.inStock
                    )
                    onProductAdded(finalProduct, imageUri)
                },
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null && category.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(if (product == null) "Add Product" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeleteProductDialog(
    productName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Product?", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete '$productName'? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
