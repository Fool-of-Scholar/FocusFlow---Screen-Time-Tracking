package com.example.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.utils.ThemePreferences
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimationSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E2E)
                )
            ) {
                AnimationSelectionScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationSelectionScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Library", "Custom", "Hot", "Anime", "Emoji", "Live", "Gradient")
    val context = LocalContext.current

    var myAnims by remember { mutableStateOf(ThemePreferences.getCustomAnimations(context)) }

    val animPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    val animsDir = java.io.File(context.filesDir, "animations")
                    if (!animsDir.exists()) animsDir.mkdirs()
                    // Determine extension based on mime type, default to gif
                    val mimeType = context.contentResolver.getType(it)
                    val ext = if (mimeType?.contains("video") == true) "mp4" else "gif"
                    val newFile = java.io.File(animsDir, "anim_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        java.io.FileOutputStream(newFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ThemePreferences.addCustomAnimation(context, newFile.absolutePath)
                    myAnims = ThemePreferences.getCustomAnimations(context)
                    Toast.makeText(context, "Animation downloaded to My Library!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to import animation.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animation Theme", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Download feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF121212),
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFFFF5722),
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) Color.White else Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> { // My Library
                    if (myAnims.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No custom animations downloaded yet.\nAdd them from the Custom tab!", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(myAnims) { uriString ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uriString)
                                        .decoderFactory(ImageDecoderDecoder.Factory()) // Support GIFs
                                        .build(),
                                    contentDescription = "Custom Animation",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(0.6f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            ThemePreferences.setBackgroundColor(context, uriString)
                                            Toast.makeText(context, "Custom Animation Applied!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                )
                            }
                        }
                    }
                }
                1 -> { // Custom
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add Animation",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Add a GIF or short video to set as your\nlock screen wallpaper",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = {
                                animPickerLauncher.launch(arrayOf("image/gif", "video/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(200.dp).height(48.dp)
                        ) {
                            Text("Add", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    // Hot, Anime, Emoji, Live placeholders
                    val scope = rememberCoroutineScope()
                    val presetUrls = listOf(
                        "https://media.giphy.com/media/3o7aD2saal6gCGWYA8/giphy.gif",
                        "https://media.giphy.com/media/l41YkxvU8c7J7Bba0/giphy.gif",
                        "https://media.giphy.com/media/26AHONQ79FdWZhAI0/giphy.gif",
                        "https://media.giphy.com/media/xT1XGzYf1w2B4P1F4s/giphy.gif",
                        "https://media.giphy.com/media/3o6Zt481isNVuQI1l6/giphy.gif"
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(presetUrls.size) { index ->
                            val url = presetUrls[index]
                            var isDownloading by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.6f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2A2A3A))
                                    .clickable(enabled = !isDownloading) {
                                        isDownloading = true
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val animsDir = java.io.File(context.filesDir, "animations")
                                                if (!animsDir.exists()) animsDir.mkdirs()
                                                val newFile = java.io.File(animsDir, "preset_${System.currentTimeMillis()}.gif")
                                                java.net.URL(url).openStream().use { input ->
                                                    java.io.FileOutputStream(newFile).use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    ThemePreferences.addCustomAnimation(context, newFile.absolutePath)
                                                    myAnims = ThemePreferences.getCustomAnimations(context)
                                                    Toast.makeText(context, "Preset Animation downloaded to My Library!", Toast.LENGTH_SHORT).show()
                                                    isDownloading = false
                                                }
                                            } catch (e: Exception) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to download.", Toast.LENGTH_SHORT).show()
                                                    isDownloading = false
                                                }
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(url)
                                        .decoderFactory(ImageDecoderDecoder.Factory()) // Support GIFs
                                        .build(),
                                    contentDescription = "Preset Animation",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                if (isDownloading) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000)), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF5722))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Download", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
