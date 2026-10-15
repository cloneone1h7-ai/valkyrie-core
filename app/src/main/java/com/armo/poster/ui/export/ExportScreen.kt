package com.armo.poster.ui.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armo.poster.R
import androidx.compose.material.icons.filled.BugReport
import com.armo.poster.domain.model.MatchData
import com.armo.poster.renderer.RenderResult
import com.armo.poster.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    matchData: MatchData,
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    val isRendering by viewModel.isRendering.collectAsState()
    val renderResult by viewModel.renderResult.collectAsState()
    val scale by viewModel.scale.collectAsState()

    LaunchedEffect(matchData, scale) {
        viewModel.renderPoster(matchData)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.export_title),
                        fontWeight = FontWeight.Bold,
                        color = VoltAccent
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = stringResource(R.string.logs_title),
                            tint = VoltAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resolution Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.setScale(2) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scale == 2) VoltAccent else Color.Transparent,
                        contentColor = if (scale == 2) BgDark else TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("2400×2400 (2x)", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.setScale(1) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scale == 1) VoltAccent else Color.Transparent,
                        contentColor = if (scale == 1) BgDark else TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("1200×1200 (1x)", fontWeight = FontWeight.Bold)
                }
            }

            // Status Banner
            when (val res = renderResult) {
                is RenderResult.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (res.fitLevel > 0) {
                                    "تطبیق خودکار پوستر (سطح ${res.fitLevel})"
                                } else {
                                    "ابعاد کاملاً متناسب و استاندارد (FITS)"
                                },
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (res.fontTimeout) {
                                Text(
                                    stringResource(R.string.font_warning),
                                    color = Color(0xFFFFB300),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                is RenderResult.TooLong -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "پوستر با حداکثر مقیاس تطبیق داده شد (سطح ${res.fitLevel})",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is RenderResult.Failed -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.status_failed, res.reason),
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onNavigateToLogs) {
                                Text(
                                    stringResource(R.string.view_logs),
                                    color = VoltAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                null -> {}
            }

            // Image Preview or Loading Progress
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isRendering) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = VoltAccent)
                        Text(
                            stringResource(R.string.rendering_progress),
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val previewBitmap = when (val res = renderResult) {
                        is RenderResult.Success -> res.bitmap
                        is RenderResult.TooLong -> res.bitmap
                        else -> null
                    }
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Poster Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            val hasBitmap = when (val res = renderResult) {
                is RenderResult.Success -> true
                is RenderResult.TooLong -> res.bitmap != null
                else -> false
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveToGallery(context, matchData) },
                    enabled = !isRendering && hasBitmap,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltAccent,
                        contentColor = BgDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.save_gallery), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.shareToX(context, matchData) },
                    enabled = !isRendering && hasBitmap,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = VoltAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.share_x), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
