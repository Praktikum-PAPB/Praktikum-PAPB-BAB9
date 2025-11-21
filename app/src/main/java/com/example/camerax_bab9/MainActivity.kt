package com.example.camerax_bab9

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camerax_bab9.ui.theme.Cameraxbab9Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cameraxbab9Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State untuk fitur CameraX
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // State UI & Konfigurasi
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) }

    // Izin Kamera
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    // Re-bind kamera saat konfigurasi berubah (Switch Camera)
    LaunchedEffect(lensFacing, previewView) {
        val view = previewView ?: return@LaunchedEffect

        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build()
        // Gunakan selector sesuai state lensFacing
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview.setSurfaceProvider(view.surfaceProvider)

        // Unbind dulu sebelum bind ulang
        provider.unbindAll()

        try {
            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // Rotasi target agar foto tidak terbalik
                .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0)
                .build()

            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, ic)
            imageCapture = ic

            // Reset flash ke mati saat ganti kamera
            isTorchOn = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasPermission) {
            // 1. Preview Kamera
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }.also { previewView = it }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Overlay UI (Flash & Switch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tombol Flash
                IconButton(
                    onClick = {
                        // Cek apakah kamera punya flash sebelum mengaktifkan
                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            isTorchOn = !isTorchOn
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        } else {
                            Toast.makeText(context, "Flash tidak tersedia", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        // GANTI ERROR DISINI: Gunakan Star (Nyala) dan Close (Mati)
                        imageVector = if (isTorchOn) Icons.Default.Star else Icons.Default.Close,
                        contentDescription = "Toggle Flash",
                        tint = Color.White
                    )
                }

                // Tombol Switch Camera
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    // GANTI ERROR DISINI: Gunakan Refresh
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }

            // 3. Bagian Bawah: Tombol Foto & Thumbnail
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        imageCapture?.let { ic ->
                            takePhoto(context, ic) { uri ->
                                lastCapturedUri = uri
                                Toast.makeText(context, "Foto tersimpan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Ambil Foto")
                }

                lastCapturedUri?.let { uri ->
                    ThumbnailView(uri = uri, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }

        } else {
            Text("Izin kamera diperlukan.", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ThumbnailView(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Last Photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
        )
    }
}

// --- FUNGSI LOGIKA CAMERAX ---

fun outputOptions(ctx: Context): ImageCapture.OutputFileOptions {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
    }
    return ImageCapture.OutputFileOptions.Builder(
        ctx.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
}

fun takePhoto(ctx: Context, ic: ImageCapture, onSaved: (Uri) -> Unit) {
    val outputOptions = outputOptions(ctx)

    ic.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: return

                // --- PERBAIKAN UTAMA DISINI ---
                // Kita paksa Galeri untuk men-scan file baru ini agar langsung muncul
                // Sesuai Troubleshooting Modul X.8
                try {
                    val path = getRealPathFromURI(ctx, savedUri) ?: savedUri.path
                    if (path != null) {
                        MediaScannerConnection.scanFile(
                            ctx,
                            arrayOf(path),
                            arrayOf("image/jpeg"),
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // -------------------------------

                onSaved(savedUri)
            }

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(ctx, "Gagal: ${exc.message}", Toast.LENGTH_SHORT).show()
                exc.printStackTrace()
            }
        }
    )
}

fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
    var cursor: android.database.Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = context.contentResolver.query(contentUri, proj, null, null, null)
        val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        cursor?.getString(column_index ?: 0)
    } catch (e: Exception) {
        // Fallback jika gagal mendapatkan path asli
        contentUri.path
    } finally {
        cursor?.close()
    }
}