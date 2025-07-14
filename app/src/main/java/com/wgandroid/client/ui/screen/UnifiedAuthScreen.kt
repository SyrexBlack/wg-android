package com.wgandroid.client.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wgandroid.client.ui.theme.GlassColors
import com.wgandroid.client.ui.theme.GlassModifiers
import com.wgandroid.client.ui.viewmodel.UnifiedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAuthScreen(
    viewModel: UnifiedViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Анимации
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val animatedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_animation"
    )
    
    // Мониторим успешную авторизацию
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Плавающие элементы для красоты
        FloatingElements(animatedFloat)
        
        // Основной контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Логотип и заголовок
            AuthHeader(animatedFloat)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Форма авторизации
            AuthForm(
                serverUrl = uiState.serverUrl,
                password = uiState.password,
                isLoading = uiState.isLoading,
                error = uiState.authError,
                passwordVisible = passwordVisible,
                passwordFocusRequester = passwordFocusRequester,
                onServerUrlChange = viewModel::updateServerUrl,
                onPasswordChange = viewModel::updatePassword,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                onAuthenticate = {
                    focusManager.clearFocus()
                    viewModel.authenticate()
                },
                onClearError = viewModel::clearError
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Информация внизу
            AuthFooter()
        }
    }
}

@Composable
fun FloatingElements(animatedFloat: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(6) { index ->
            val offset = ((index + 1) * 60).dp
            val size = (80 + index * 20).dp
            val alpha = 0.1f + index * 0.02f
            
            Box(
                modifier = Modifier
                    .offset(
                        x = offset + (animatedFloat * 20).dp,
                        y = offset + (animatedFloat * 30).dp
                    )
                    .size(size)
                    .alpha(alpha)
                    .blur(20.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlassColors.accentBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
fun AuthHeader(animatedFloat: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(0.95f + animatedFloat * 0.05f)
    ) {
        // Иконка WireGuard
        Box(
            modifier = Modifier
                .size(120.dp)
                .then(GlassModifiers.glassCard(cornerRadius = 60, alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "WireGuard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 42.sp
        )
        
        Text(
            text = "Подключитесь к вашему серверу",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthForm(
    serverUrl: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    passwordVisible: Boolean,
    passwordFocusRequester: FocusRequester,
    onServerUrlChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onAuthenticate: () -> Unit,
    onClearError: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(GlassModifiers.glassCard(cornerRadius = 24, alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // URL сервера
            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = { 
                    Text(
                        "URL сервера", 
                        color = Color.White.copy(alpha = 0.8f)
                    ) 
                },
                placeholder = { 
                    Text(
                        "https://your-wg-server.com", 
                        color = Color.White.copy(alpha = 0.5f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = GlassColors.accentBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = GlassColors.accentBlue
                )
            )
            
            // Пароль
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { 
                    Text(
                        "Пароль", 
                        color = Color.White.copy(alpha = 0.8f)
                    ) 
                },
                placeholder = { 
                    Text(
                        "Введите пароль", 
                        color = Color.White.copy(alpha = 0.5f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff 
                                         else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть пароль" 
                                               else "Показать пароль",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None 
                                     else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onAuthenticate() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = GlassColors.accentBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = GlassColors.accentBlue
                )
            )
            
            // Ошибка
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                error?.let { errorMessage ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Кнопка подключения
            Button(
                onClick = onAuthenticate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = serverUrl.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassColors.accentBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Подключение...")
                } else {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Подключиться",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AuthFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(0.7f)
    ) {
        Text(
            text = "🔐 Безопасное VPN подключение",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Версия 1.0 • Сделано с ❤️",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
} 