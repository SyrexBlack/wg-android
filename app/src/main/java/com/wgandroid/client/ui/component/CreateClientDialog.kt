package com.wgandroid.client.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wgandroid.client.R

@Composable
fun CreateClientDialog(
    onCreateClient: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clientName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.wrapContentSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_client),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { 
                        clientName = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.client_name)) },
                    placeholder = { Text(stringResource(R.string.enter_client_name)) },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.error_empty_name)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Arrangement.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Button(
                        onClick = {
                            if (clientName.trim().isEmpty()) {
                                isError = true
                            } else {
                                onCreateClient(clientName.trim())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.create))
                    }
                }
            }
        }
    }
} 