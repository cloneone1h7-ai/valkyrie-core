package com.armo.poster.ui.editor

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.armo.poster.R
import com.armo.poster.domain.model.TeamRef
import com.armo.poster.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToExport: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val matchData by viewModel.matchData.collectAsState()
    val debouncedPayload by viewModel.debouncedPayload.collectAsState()
    val teams = viewModel.availableTeams

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.editor_title),
                        fontWeight = FontWeight.Bold,
                        color = VoltAccent
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.history_title),
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.saveCurrentMatch()
                        onNavigateToExport()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltAccent,
                        contentColor = BgDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(R.string.render_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = BgDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sticky Mini Live Preview
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    MiniPreviewWebView(payloadJson = debouncedPayload)
                }
            }

            // General & Gameweek
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.gameweek_label), color = VoltAccent, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = matchData.gwLabel,
                            onValueChange = { viewModel.updateGwLabel(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = VoltAccent,
                                unfocusedBorderColor = HairlineColor
                            )
                        )
                    }
                }
            }

            // Match Setup (Teams, Formations, xG)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Teams & Scoreline", color = VoltAccent, fontWeight = FontWeight.SemiBold)

                        // Home Team
                        TeamPickerDropdown(
                            label = stringResource(R.string.home_team),
                            selectedTeam = matchData.home,
                            teams = teams,
                            onSelect = { viewModel.updateHomeTeam(it) }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = matchData.homeFormation,
                                onValueChange = { viewModel.updateHomeFormation(it) },
                                label = { Text("Home Formation") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            OutlinedTextField(
                                value = matchData.homeXg.toString(),
                                onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateHomeXg(v) } },
                                label = { Text("Home xG") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                        }

                        Divider(color = HairlineColor)

                        // Away Team
                        TeamPickerDropdown(
                            label = stringResource(R.string.away_team),
                            selectedTeam = matchData.away,
                            teams = teams,
                            onSelect = { viewModel.updateAwayTeam(it) }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = matchData.awayFormation,
                                onValueChange = { viewModel.updateAwayFormation(it) },
                                label = { Text("Away Formation") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            OutlinedTextField(
                                value = matchData.awayXg.toString(),
                                onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateAwayXg(v) } },
                                label = { Text("Away xG") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                        }
                    }
                }
            }

            // Game Notes
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.game_notes_title), color = VoltAccent, fontWeight = FontWeight.SemiBold)
                        matchData.gameNotes.forEachIndexed { index, note ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• $note", color = TextBody, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = { viewModel.removeGameNote(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }

                        var newNoteText by remember { mutableStateOf("") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                placeholder = { Text(stringResource(R.string.note_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            IconButton(onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.addGameNote(newNoteText)
                                    newNoteText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = VoltAccent)
                            }
                        }
                    }
                }
            }

            // Home Players
            item {
                PlayerBlockSection(
                    title = stringResource(R.string.home_players_title),
                    blocks = matchData.homeBlocks,
                    onAddPlayer = { name, notes -> viewModel.addHomePlayer(name, notes) },
                    onRemovePlayer = { viewModel.removeHomePlayer(it) }
                )
            }

            // Away Players
            item {
                PlayerBlockSection(
                    title = stringResource(R.string.away_players_title),
                    blocks = matchData.awayBlocks,
                    onAddPlayer = { name, notes -> viewModel.addAwayPlayer(name, notes) },
                    onRemovePlayer = { viewModel.removeAwayPlayer(it) }
                )
            }

            // Biggest Takeaways
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.takeaways_title), color = VoltAccent, fontWeight = FontWeight.SemiBold)
                        matchData.takeaways.forEachIndexed { index, takeaway ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• $takeaway", color = TextBody, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = { viewModel.removeTakeaway(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }

                        var newTakeawayText by remember { mutableStateOf("") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newTakeawayText,
                                onValueChange = { newTakeawayText = it },
                                placeholder = { Text(stringResource(R.string.takeaway_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            IconButton(onClick = {
                                if (newTakeawayText.isNotBlank()) {
                                    viewModel.addTakeaway(newTakeawayText)
                                    newTakeawayText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = VoltAccent)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PlayerBlockSection(
    title: String,
    blocks: List<com.armo.poster.domain.model.PlayerBlock>,
    onAddPlayer: (String, List<String>) -> Unit,
    onRemovePlayer: (Int) -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    var note1 by remember { mutableStateOf("") }
    var note2 by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = VoltAccent, fontWeight = FontWeight.SemiBold)

            blocks.forEachIndexed { index, block ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PanelBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, HairlineColor, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(block.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            block.notes.forEach { n ->
                                Text("• $n", color = TextBody, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = { onRemovePlayer(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                        }
                    }
                }
            }

            Divider(color = HairlineColor)

            Text("Add Player Card", color = TextMuted, fontSize = 13.sp)
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
            )
            OutlinedTextField(
                value = note1,
                onValueChange = { note1 = it },
                label = { Text("Bullet 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
            )
            OutlinedTextField(
                value = note2,
                onValueChange = { note2 = it },
                label = { Text("Bullet 2 (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
            )

            Button(
                onClick = {
                    if (playerName.isNotBlank()) {
                        val notes = listOfNotNull(
                            note1.takeIf { it.isNotBlank() },
                            note2.takeIf { it.isNotBlank() }
                        )
                        onAddPlayer(playerName, notes)
                        playerName = ""
                        note1 = ""
                        note2 = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PanelBackground, contentColor = VoltAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Player Block")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPickerDropdown(
    label: String,
    selectedTeam: TeamRef,
    teams: List<TeamRef>,
    onSelect: (TeamRef) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTeam.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                val parsedColor = try {
                    Color(AndroidColor.parseColor(selectedTeam.color))
                } catch (_: Exception) {
                    VoltAccent
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = VoltAccent,
                unfocusedBorderColor = HairlineColor
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dotColor = try {
                                Color(AndroidColor.parseColor(team.color))
                            } catch (_: Exception) {
                                VoltAccent
                            }
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(dotColor))
                            Text(team.name, color = TextWhite)
                            Text("(${team.abbr})", color = TextMuted, fontSize = 12.sp)
                        }
                    },
                    onClick = {
                        onSelect(team)
                        expanded = false
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniPreviewWebView(payloadJson: String) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        isReady = true
                    }
                }

                loadUrl("https://appassets.androidplatform.net/assets/poster/poster.html")
                webViewRef = this
            }
        },
        update = { webView ->
            if (isReady && payloadJson.isNotBlank()) {
                val escaped = payloadJson.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                webView.evaluateJavascript("window.__armo && window.__armo.build('$escaped')", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
