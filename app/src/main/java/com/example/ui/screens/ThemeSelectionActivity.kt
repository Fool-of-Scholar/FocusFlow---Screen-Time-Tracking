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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.ThemePreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThemeSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E2E)
                )
            ) {
                ThemeSelectionScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Library", "Custom", "Hot", "Gradient")
    val context = LocalContext.current

    var myImages by remember { mutableStateOf(ThemePreferences.getCustomImages(context)) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    val themesDir = java.io.File(context.filesDir, "themes")
                    if (!themesDir.exists()) themesDir.mkdirs()
                    val newFile = java.io.File(themesDir, "theme_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        java.io.FileOutputStream(newFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ThemePreferences.addCustomImage(context, newFile.absolutePath)
                    myImages = ThemePreferences.getCustomImages(context)
                    Toast.makeText(context, "Theme downloaded to My Library!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to import theme.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val colors = listOf(
        "#8AB4F8", "#4FC3F7", "#2962FF", "#3D5AFE", "#0D47A1",
        "#FFCA28", "#FFC107", "#FFA000", "#FF8F00", "#FF6F00",
        "#F48FB1", "#F06292", "#E91E63", "#D32F2F", "#B71C1C",
        "#B388FF", "#D500F9", "#651FFF", "#7C4DFF", "#6200EA",
        "#A5D6A7", "#81C784", "#00C853", "#00BFA5", "#00838F"
    )

    val gradients = listOf(
        listOf("#FF9A9E", "#FECFEF"),
        listOf("#A18CD1", "#FBC2EB"),
        listOf("#84FAB0", "#8FD3F4"),
        listOf("#E0C3FC", "#8EC5FC"),
        listOf("#F093FB", "#F5576C"),
        listOf("#4FACFE", "#00F2FE"),
        listOf("#43E97B", "#38F9D7"),
        listOf("#FA709A", "#FEE140"),
        listOf("#30CFD0", "#330867"),
        listOf("#FF0844", "#FFB199")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                    if (myImages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No custom images downloaded yet.\nAdd them from the Custom tab!", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(myImages) { uriString ->
                                AsyncImage(
                                    model = uriString,
                                    contentDescription = "Custom Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(0.6f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            ThemePreferences.setBackgroundColor(context, uriString)
                                            Toast.makeText(context, "Custom Image Applied!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                )
                            }
                        }
                    }
                }
                1 -> { // Custom
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Color",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(colors) { hex ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable {
                                            ThemePreferences.setBackgroundColor(context, hex)
                                            Toast.makeText(context, "Theme Applied!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                )
                            }
                        }

                        // Add Image Section
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Add image",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Add an image to set as your lock screen\nwallpaper",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Button(
                                onClick = {
                                    imagePickerLauncher.launch(arrayOf("image/*"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.width(200.dp).height(48.dp)
                            ) {
                                Text("Add", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                3 -> { // Gradient
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(gradients) { grad ->
                            val color1 = Color(android.graphics.Color.parseColor(grad[0]))
                            val color2 = Color(android.graphics.Color.parseColor(grad[1]))
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.6f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(listOf(color1, color2)))
                                    .clickable {
                                        // Save the first color for now or serialize the gradient
                                        // For simplicity, applying a gradient lock screen background is possible but requires a comma-separated format.
                                        ThemePreferences.setBackgroundColor(context, "${grad[0]},${grad[1]}")
                                        Toast.makeText(context, "Gradient Theme Applied!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                            )
                        }
                    }
                }
                else -> {
                    // Hot, Anime, Emoji, Live placeholders mapped to the 16 new images
                    val presetUrls = listOf(
                        "file:///android_asset/themes/adam-neumann-x3MMT1g7i-I-unsplash.jpg",
                        "file:///android_asset/themes/alexander-mass-xdv8rGTw7Y8-unsplash.jpg",
                        "file:///android_asset/themes/david-becker-AAFKJ_sAv8U-unsplash.jpg",
                        "file:///android_asset/themes/david-becker-gDrUcMGZdsg-unsplash.jpg",
                        "file:///android_asset/themes/deep-aayn3P9RHsI-unsplash.jpg",
                        "file:///android_asset/themes/dmytro-bayer-Q0tiP6MIaTY-unsplash.jpg",
                        "file:///android_asset/themes/jacqueline-munguia-1pAwJiCD60c-unsplash.jpg",
                        "file:///android_asset/themes/jean-carlo-emer-xnDTYtppjHs-unsplash.jpg",
                        "file:///android_asset/themes/jin-cl-gJdQ3FV3-Mw-unsplash.jpg",
                        "file:///android_asset/themes/jon-tyson-r27umXAelDc-unsplash.jpg",
                        "file:///android_asset/themes/kristaps-ungurs-VllG6IhEI78-unsplash.jpg",
                        "file:///android_asset/themes/lawrence-krowdeed-U1KwIi4_amQ-unsplash.jpg",
                        "file:///android_asset/themes/manas-rb-wDdW-hF9pgM-unsplash.jpg",
                        "file:///android_asset/themes/night-city-arts-AmKcEdjAu-E-unsplash.jpg",
                        "file:///android_asset/themes/samuel-berner-kcvEQb7GXZc-unsplash.jpg",
                        "file:///android_asset/themes/zhuo-cheng-you-Y-TzM28dDhU-unsplash.jpg"
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
                            
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.6f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2A2A3A))
                                    .clickable {
                                        ThemePreferences.setBackgroundColor(context, url)
                                        Toast.makeText(context, "Theme Applied!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Preset Theme",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
