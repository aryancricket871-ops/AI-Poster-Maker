package com.example.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.ads.UnityBannerAd
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.DocumentScanner
import java.io.ByteArrayOutputStream
import android.graphics.ImageDecoder
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasEditorScreen(
    templateImageUrl: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var textLayers by remember { mutableStateOf(listOf<TextLayerData>()) }
    var imageLayers by remember { mutableStateOf(listOf<ImageLayerData>()) }
    
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var showAiAnalyzeDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageLayers = imageLayers + ImageLayerData(it)
        }
    }

    val graphicsLayer = rememberGraphicsLayer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Poster Editor", fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                saveBitmapToGallery(context, bitmap)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Poster")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { showAddTextDialog = true }) {
                            Icon(Icons.Default.TextFields, contentDescription = "Add Text")
                        }
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image")
                        }
                        IconButton(onClick = { showAiGeneratorDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate")
                        }
                        IconButton(onClick = { showAiAnalyzeDialog = true }) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Analyze Photo")
                        }
                    }
                )
                if (context is Activity) {
                    UnityBannerAd(activity = context)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            // The Canvas Area
            Box(
                modifier = Modifier
                    .aspectRatio(1f) // Keeping it square for poster
                    .fillMaxSize()
                    .background(Color.White)
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                    .clipToBounds()
            ) {
                // Background Template
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(templateImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Render Image Layers
                imageLayers.forEach { layer ->
                    DraggableImageLayer(layer = layer)
                }

                // Render Text Layers
                textLayers.forEach { layer ->
                    DraggableTextLayer(layer = layer)
                }
            }
        }

        if (showAddTextDialog) {
            AddTextDialog(
                onDismiss = { showAddTextDialog = false },
                onAddText = { text, size, color ->
                    textLayers = textLayers + TextLayerData(text, size, color)
                    showAddTextDialog = false
                }
            )
        }

        if (showAiGeneratorDialog) {
            AiImageGeneratorDialog(
                viewModel = viewModel,
                onDismiss = { showAiGeneratorDialog = false },
                onImageGenerated = { bitmap ->
                    imageLayers = imageLayers + ImageLayerData(bitmap = bitmap)
                    showAiGeneratorDialog = false
                }
            )
        }

        if (showAiAnalyzeDialog) {
            AiAnalyzeDialog(
                viewModel = viewModel,
                onDismiss = { showAiAnalyzeDialog = false }
            )
        }
    }
}

fun Modifier.clipToBounds(): Modifier = this.clip(RoundedCornerShape(0.dp))

@Composable
fun DraggableTextLayer(layer: TextLayerData) {
    var offset by remember { mutableStateOf(layer.offset) }
    var scale by remember { mutableStateOf(1f) }

    Text(
        text = layer.text,
        fontSize = (layer.size * scale).sp,
        color = layer.color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offset += pan
                    scale *= zoom
                }
            }
    )
}

@Composable
fun DraggableImageLayer(layer: ImageLayerData) {
    var offset by remember { mutableStateOf(layer.offset) }
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offset += pan
                    scale *= zoom
                }
            }
    ) {
        if (layer.uri != null) {
            AsyncImage(
                model = layer.uri,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        } else if (layer.bitmap != null) {
            Image(
                painter = rememberAsyncImagePainter(model = layer.bitmap),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

data class TextLayerData(
    val text: String,
    val size: Float = 24f,
    val color: Color = Color.Black,
    var offset: Offset = Offset(100f, 100f)
)

data class ImageLayerData(
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    var offset: Offset = Offset(100f, 100f)
)

@Composable
fun AddTextDialog(onDismiss: () -> Unit, onAddText: (String, Float, Color) -> Unit) {
    var text by remember { mutableStateOf("") }
    var size by remember { mutableStateOf(24f) }
    var selectedColor by remember { mutableStateOf(Color.Black) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text Layer") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your Text") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Font Size: ${size.toInt()}")
                Slider(value = size, onValueChange = { size = it }, valueRange = 12f..72f)
                
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    val colors = listOf(Color.Black, Color.White, Color.Red, Color.Blue, Color.Yellow)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(color, RoundedCornerShape(15.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(15.dp))
                                .clickable { selectedColor = color }
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, contentDescription = "", tint = if(color==Color.White) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center).size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onAddText(text, size, selectedColor) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiImageGeneratorDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onImageGenerated: (Bitmap) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("1K") }
    var isPro by remember { mutableStateOf(false) }
    val isGenerating by viewModel.aiGenerating.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Image with AI") },
        text = {
            Column {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Image Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Model: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = !isPro, onClick = { isPro = false }, label = { Text("Flash") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = isPro, onClick = { isPro = true }, label = { Text("Pro") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("1K", "2K", "4K").forEach { s ->
                        FilterChip(selected = quality == s, onClick = { quality = s }, label = { Text(s) })
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        coroutineScope.launch {
                            val bitmap = viewModel.generateAiImage(prompt, quality, isPro)
                            if (bitmap != null) {
                                onImageGenerated(bitmap)
                            } else {
                                Toast.makeText(context, "Failed to generate image.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isGenerating && prompt.isNotBlank()
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("Cancel") }
        }
    )
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "Poster_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AIPosterMaker")
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AiAnalyzeDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var analysisResult by remember { mutableStateOf("") }
    val isGenerating by viewModel.aiGenerating.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analyze Image with Gemini") },
        text = {
            Column {
                if (selectedUri == null) {
                    Button(onClick = { imagePicker.launch("image/*") }) {
                        Text("Select Photo to Analyze")
                    }
                } else {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected",
                        modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { selectedUri = null }) {
                        Text("Clear")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (analysisResult.isNotEmpty()) {
                    Text(analysisResult)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUri?.let { uri ->
                        coroutineScope.launch {
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                                    ImageDecoder.decodeBitmap(source)
                                } else {
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                }
                                val out = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                                val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
                                analysisResult = viewModel.analyzeImage(base64, "Analyze the aesthetic and technical quality of this image for a poster background. Provide suggestions.")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to analyze: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = selectedUri != null && !isGenerating
            ) {
                Text("Analyze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("Close") }
        }
    )
}
