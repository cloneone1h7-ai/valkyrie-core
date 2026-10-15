package com.armo.poster.ui.editor

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.webkit.WebViewAssetLoader
import com.armo.poster.R
import com.armo.poster.domain.model.MatchData
import com.armo.poster.domain.model.TeamRef
import com.armo.poster.ui.theme.*
import com.armo.poster.util.AppLog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToExport: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val matchData by viewModel.matchData.collectAsState()
    val debouncedPayload by viewModel.debouncedPayload.collectAsState()
    val teams = viewModel.availableTeams

    var homeXgInput by remember { mutableStateOf(if (matchData.homeXg == 0.0) "" else matchData.homeXg.toString()) }
    var awayXgInput by remember { mutableStateOf(if (matchData.awayXg == 0.0) "" else matchData.awayXg.toString()) }

    var showHomeColorPicker by remember { mutableStateOf(false) }
    var showAwayColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }
    var showAccentColorPicker by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var previewExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(matchData.id) {
        homeXgInput = if (matchData.homeXg == 0.0) "" else matchData.homeXg.toString()
        awayXgInput = if (matchData.awayXg == 0.0) "" else matchData.awayXg.toString()
    }

    if (showHomeColorPicker) {
        ColorPickerDialog(
            teamName = matchData.home.name,
            initialColorHex = matchData.home.color,
            defaultColorHex = viewModel.getDefaultColorForTeam(matchData.home.name),
            onDismiss = { showHomeColorPicker = false },
            onColorSelected = { colorHex ->
                viewModel.updateHomeColor(colorHex)
                showHomeColorPicker = false
            }
        )
    }

    if (showAwayColorPicker) {
        ColorPickerDialog(
            teamName = matchData.away.name,
            initialColorHex = matchData.away.color,
            defaultColorHex = viewModel.getDefaultColorForTeam(matchData.away.name),
            onDismiss = { showAwayColorPicker = false },
            onColorSelected = { colorHex ->
                viewModel.updateAwayColor(colorHex)
                showAwayColorPicker = false
            }
        )
    }

    if (showBgColorPicker) {
        ColorPickerDialog(
            teamName = "پس‌زمینه پوستر",
            initialColorHex = matchData.bgColor,
            defaultColorHex = "#0B1A14",
            onDismiss = { showBgColorPicker = false },
            onColorSelected = { colorHex ->
                viewModel.updateBgColor(colorHex)
                showBgColorPicker = false
            }
        )
    }

    if (showAccentColorPicker) {
        ColorPickerDialog(
            teamName = "رنگ هایلایت و بولت‌ها (Volt)",
            initialColorHex = matchData.accentColor,
            defaultColorHex = "#D4FF00",
            onDismiss = { showAccentColorPicker = false },
            onColorSelected = { colorHex ->
                viewModel.updateAccentColor(colorHex)
                showAccentColorPicker = false
            }
        )
    }

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
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = stringResource(R.string.logs_title),
                            tint = VoltAccent
                        )
                    }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
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
            }
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Sticky Docked Mini Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, HairlineColor)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (previewExpanded) 340.dp else 210.dp)
                    ) {
                        MiniPreviewWebView(payloadJson = debouncedPayload)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .clickable { previewExpanded = !previewExpanded }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VoltAccent)
                            )
                            Text(
                                "پیش‌نمایش زنده پوستر (ثابت)",
                                fontSize = 11.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (previewExpanded) "کوچک‌نمایی" else "بزرگ‌نمایی",
                                fontSize = 11.sp,
                                color = VoltAccent
                            )
                            Icon(
                                imageVector = if (previewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = VoltAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. TabRow (محتوای مسابقه / استودیو و طراحی)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = BgDark,
                contentColor = VoltAccent,
                divider = { HorizontalDivider(color = HairlineColor) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "محتوای مسابقه",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = VoltAccent,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "استودیو و طراحی",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = VoltAccent,
                    unselectedContentColor = TextMuted
                )
            }

            if (selectedTabIndex == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TeamPickerDropdown(
                                    label = stringResource(R.string.home_team),
                                    selectedTeam = matchData.home,
                                    teams = teams,
                                    onSelect = { viewModel.updateHomeTeam(it) }
                                )
                            }
                            OutlinedButton(
                                onClick = { showHomeColorPicker = true },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
                                border = BorderStroke(1.dp, HairlineColor),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                val parsedColor = try {
                                    Color(AndroidColor.parseColor(matchData.home.color))
                                } catch (_: Exception) {
                                    VoltAccent
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(1.5.dp, TextWhite.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "تنظیم رنگ",
                                        tint = VoltAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

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
                                value = homeXgInput,
                                onValueChange = { newStr ->
                                    if (newStr.isEmpty() || newStr.matches(Regex("""^\d{0,2}(\.\d{0,2})?$"""))) {
                                        homeXgInput = newStr
                                        val parsed = newStr.toDoubleOrNull() ?: 0.0
                                        viewModel.updateHomeXg(parsed)
                                    }
                                },
                                label = { Text("Home xG") },
                                placeholder = { Text("0.0", color = TextMuted) },
                                leadingIcon = {
                                    val parsedColor = try {
                                        Color(AndroidColor.parseColor(matchData.home.color))
                                    } catch (_: Exception) {
                                        VoltAccent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(1.dp, TextWhite.copy(alpha = 0.5f), CircleShape)
                                            .clickable { showHomeColorPicker = true }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = VoltAccent,
                                    unfocusedBorderColor = HairlineColor
                                )
                            )
                        }

                        HorizontalDivider(color = HairlineColor)

                        // Away Team
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TeamPickerDropdown(
                                    label = stringResource(R.string.away_team),
                                    selectedTeam = matchData.away,
                                    teams = teams,
                                    onSelect = { viewModel.updateAwayTeam(it) }
                                )
                            }
                            OutlinedButton(
                                onClick = { showAwayColorPicker = true },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
                                border = BorderStroke(1.dp, HairlineColor),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                val parsedColor = try {
                                    Color(AndroidColor.parseColor(matchData.away.color))
                                } catch (_: Exception) {
                                    VoltAccent
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(1.5.dp, TextWhite.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "تنظیم رنگ",
                                        tint = VoltAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

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
                                value = awayXgInput,
                                onValueChange = { newStr ->
                                    if (newStr.isEmpty() || newStr.matches(Regex("""^\d{0,2}(\.\d{0,2})?$"""))) {
                                        awayXgInput = newStr
                                        val parsed = newStr.toDoubleOrNull() ?: 0.0
                                        viewModel.updateAwayXg(parsed)
                                    }
                                },
                                label = { Text("Away xG") },
                                placeholder = { Text("0.0", color = TextMuted) },
                                leadingIcon = {
                                    val parsedColor = try {
                                        Color(AndroidColor.parseColor(matchData.away.color))
                                    } catch (_: Exception) {
                                        VoltAccent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(1.dp, TextWhite.copy(alpha = 0.5f), CircleShape)
                                            .clickable { showAwayColorPicker = true }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                placeholder = { Text(stringResource(R.string.note_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                minLines = 1,
                                maxLines = 4,
                                trailingIcon = {
                                    TextButton(
                                        onClick = { newNoteText = if (newNoteText.isEmpty()) "" else "$newNoteText\n" },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("↵ خط بعد", color = VoltAccent, fontSize = 11.sp)
                                    }
                                },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTakeawayText,
                                onValueChange = { newTakeawayText = it },
                                placeholder = { Text(stringResource(R.string.takeaway_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                minLines = 1,
                                maxLines = 4,
                                trailingIcon = {
                                    TextButton(
                                        onClick = { newTakeawayText = if (newTakeawayText.isEmpty()) "" else "$newTakeawayText\n" },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("↵ خط بعد", color = VoltAccent, fontSize = 11.sp)
                                    }
                                },
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
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                StudioTabContent(
                    matchData = matchData,
                    viewModel = viewModel,
                    onRequestBgColorPicker = { showBgColorPicker = true },
                    onRequestAccentColorPicker = { showAccentColorPicker = true }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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

            HorizontalDivider(color = HairlineColor)

            Text("Add Player Card", color = TextMuted, fontSize = 13.sp)
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = note1,
                    onValueChange = { note1 = it },
                    label = { Text("Bullet 1") },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 4,
                    trailingIcon = {
                        TextButton(
                            onClick = { note1 = if (note1.isEmpty()) "" else "$note1\n" },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("↵ خط بعد", color = VoltAccent, fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                Text(
                    "متن اضافی درون همین بولت به خط بعد منتقل می‌شود بدون ایجاد بولت جدید",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = note2,
                    onValueChange = { note2 = it },
                    label = { Text("Bullet 2 (Optional)") },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 4,
                    trailingIcon = {
                        TextButton(
                            onClick = { note2 = if (note2.isEmpty()) "" else "$note2\n" },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("↵ خط بعد", color = VoltAccent, fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
            }

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
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = true

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                        cm?.let {
                            AppLog.d("MiniPreviewJS", "${it.message()} [${it.sourceId()}:${it.lineNumber()}]")
                        }
                        return true
                    }
                }

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

@Composable
fun ColorPickerDialog(
    teamName: String,
    initialColorHex: String,
    defaultColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialInt = remember(initialColorHex) {
        try {
            AndroidColor.parseColor(if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex")
        } catch (_: Exception) {
            AndroidColor.parseColor("#00E5FF")
        }
    }
    val initialHsv = remember(initialInt) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialInt, hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColorInt = remember(hue, saturation, value) {
        AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    }
    val currentHex = remember(currentColorInt) {
        String.format(Locale.US, "#%06X", 0xFFFFFF and currentColorInt)
    }

    var hexInput by remember { mutableStateOf(currentHex) }
    LaunchedEffect(currentHex) {
        hexInput = currentHex
    }

    val popularColors = remember {
        listOf(
            "#6CABDD", // Man City sky
            "#034694", // Chelsea royal
            "#132257", // Spurs navy
            "#EF0107", // Arsenal red
            "#DA291C", // Man United crimson
            "#7A003C", // Villa / West Ham claret
            "#FDB913", // Wolves gold
            "#F36C21", // Hull City amber
            "#00A651", // Forest green
            "#EBFF00", // Volt neon
            "#FFFFFF", // White
            "#111111"  // Black
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "تنظیم رنگ تیم و بار xG",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltAccent
                        )
                        Text(
                            "تیم: $teamName",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(currentColorInt))
                            .border(2.dp, TextWhite, CircleShape)
                    )
                }

                HorizontalDivider(color = HairlineColor)

                // 1. Rainbow Hue Spectrum Slider (Color Axis)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("محور طیف رنگ (Hue)", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("${hue.toInt()}°", fontSize = 12.sp, color = VoltAccent)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Cyan,
                                        Color.Blue,
                                        Color.Magenta,
                                        Color.Red
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                }

                // 2. Brightness / Value Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("روشنایی / تیرگی (Brightness)", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("${(value * 100).toInt()}%", fontSize = 12.sp, color = VoltAccent)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black,
                                        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1.0f)))
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                }

                // 3. Saturation Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("غلظت رنگ (Saturation)", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("${(saturation * 100).toInt()}%", fontSize = 12.sp, color = VoltAccent)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White,
                                        Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1.0f, value)))
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..1.0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                }

                // 4. Hex Input Field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val formatted = if (input.startsWith("#")) input else "#$input"
                        if (formatted.length == 7 && formatted.matches(Regex("""^#[0-9a-fA-F]{6}$"""))) {
                            try {
                                val parsed = AndroidColor.parseColor(formatted)
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsed, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("کد رنگ HEX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = VoltAccent,
                        unfocusedBorderColor = HairlineColor
                    )
                )

                // 5. Popular Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("رنگ‌های محبوب لیگ:", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularColors.forEach { presetHex ->
                            val c = try { Color(AndroidColor.parseColor(presetHex)) } catch (_: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        if (currentHex.equals(presetHex, ignoreCase = true)) 2.dp else 1.dp,
                                        if (currentHex.equals(presetHex, ignoreCase = true)) VoltAccent else HairlineColor,
                                        CircleShape
                                    )
                                    .clickable {
                                        try {
                                            val parsed = AndroidColor.parseColor(presetHex)
                                            val hsv = FloatArray(3)
                                            AndroidColor.colorToHSV(parsed, hsv)
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            value = hsv[2]
                                        } catch (_: Exception) {}
                                    }
                            )
                        }
                    }
                }

                // 6. Reset to default button
                OutlinedButton(
                    onClick = {
                        try {
                            val parsed = AndroidColor.parseColor(defaultColorHex)
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(parsed, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
                    border = BorderStroke(1.dp, HairlineColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = VoltAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بازگردانی به رنگ اصلی تیم ($defaultColorHex)", fontSize = 13.sp, color = TextWhite)
                }

                HorizontalDivider(color = HairlineColor)

                // 7. Actions (Confirm / Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground)
                    ) {
                        Text("انصراف", color = TextMuted)
                    }
                    Button(
                        onClick = { onColorSelected(currentHex) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltAccent, contentColor = BgDark)
                    ) {
                        Text("تایید رنگ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudioTabContent(
    matchData: MatchData,
    viewModel: EditorViewModel,
    onRequestBgColorPicker: () -> Unit,
    onRequestAccentColorPicker: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = VoltAccent, modifier = Modifier.size(20.dp))
                Text(
                    "استودیو طراحی و گرافیک پوستر",
                    color = VoltAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // 1. xG Layout Selection (Center-Out vs Side-by-Side vs Stacked)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "چیدمان نوارهای xG (امید گل):",
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "center_out" to "مرکز به بیرون (جذاب)",
                        "side_by_side" to "روبروی هم",
                        "stacked" to "زیر هم"
                    ).forEach { (layoutKey, label) ->
                        val isSelected = matchData.xgLayout == layoutKey
                        Button(
                            onClick = { viewModel.updateXgLayout(layoutKey) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) VoltAccent else PanelBackground,
                                contentColor = if (isSelected) BgDark else TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Text(
                    "در حالت «مرکز به بیرون»، نوارها از وسط به سمت دو تیم کشیده شده و نشان xG در مرکز قرار می‌گیرد.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = HairlineColor)

            // 2. Typography Sliders (Body font size, Name font size, Font Weight, Line Height)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سایز متن و توضیحات (Body Size):", color = TextWhite, fontSize = 13.sp)
                    Text("${matchData.bodyFontSize}px", color = VoltAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = matchData.bodyFontSize.toFloat(),
                    onValueChange = { viewModel.updateBodyFontSize(it.toInt()) },
                    valueRange = 15f..26f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = VoltAccent,
                        activeTrackColor = VoltAccent,
                        inactiveTrackColor = PanelBackground
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سایز نام بازیکنان (Player Name Size):", color = TextWhite, fontSize = 13.sp)
                    Text("${matchData.nameFontSize}px", color = VoltAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = matchData.nameFontSize.toFloat(),
                    onValueChange = { viewModel.updateNameFontSize(it.toInt()) },
                    valueRange = 22f..36f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = VoltAccent,
                        activeTrackColor = VoltAccent,
                        inactiveTrackColor = PanelBackground
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فاصله بین خطوط (Line Height):", color = TextWhite, fontSize = 13.sp)
                    Text(String.format(Locale.US, "%.2f", matchData.lineHeight), color = VoltAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = matchData.lineHeight.toFloat(),
                    onValueChange = { viewModel.updateLineHeight(Math.round(it * 100.0) / 100.0) },
                    valueRange = 1.15f..1.55f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = VoltAccent,
                        activeTrackColor = VoltAccent,
                        inactiveTrackColor = PanelBackground
                    )
                )

                // Font Weight selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ضخامت قلم متون (Font Weight):", color = TextWhite, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            400 to "معمولی (400)",
                            500 to "متوسط (500)",
                            600 to "نیمه‌پر (600)",
                            700 to "ضخیم (700)"
                        ).forEach { (w, label) ->
                            val isSelected = matchData.fontWeight == w
                            Button(
                                onClick = { viewModel.updateFontWeight(w) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) VoltAccent else PanelBackground,
                                    contentColor = if (isSelected) BgDark else TextWhite
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = HairlineColor)

            // 3. Theme Colors (Background, Accent, Body Text)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "رنگ پس‌زمینه پوستر (Background):",
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                // Presets + Custom picker button
                val bgPresets = listOf(
                    "#0B1A14" to "زمردی تیره",
                    "#0A0D14" to "شب",
                    "#050505" to "مشکی مطلق",
                    "#0A1128" to "سرمه‌ای",
                    "#1A0B0E" to "زرشکی تیره",
                    "#0F172A" to "دودی اسلیت"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bgPresets.forEach { (hex, _) ->
                        val isSelected = matchData.bgColor.equals(hex, ignoreCase = true)
                        val c = try { Color(AndroidColor.parseColor(hex)) } catch (_: Exception) { Color.Black }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) VoltAccent else HairlineColor,
                                    CircleShape
                                )
                                .clickable { viewModel.updateBgColor(hex) }
                        )
                    }
                    // Custom picker button
                    OutlinedButton(
                        onClick = onRequestBgColorPicker,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
                        border = BorderStroke(1.dp, VoltAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = VoltAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رنگ دلخواه", fontSize = 11.sp, color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "رنگ تأکید، بولت‌ها و هایلایت‌ها (Accent Volt):",
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                val accentPresets = listOf(
                    "#D4FF00" to "ولت نئون",
                    "#00F0FF" to "فیروزه‌ای نئون",
                    "#FFD700" to "طلایی کهربایی",
                    "#FF4B4B" to "مرجانی درخشان",
                    "#00FF9D" to "نعنایی روشن",
                    "#A855F7" to "بنفش نئون"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accentPresets.forEach { (hex, _) ->
                        val isSelected = matchData.accentColor.equals(hex, ignoreCase = true)
                        val c = try { Color(AndroidColor.parseColor(hex)) } catch (_: Exception) { VoltAccent }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else HairlineColor,
                                    CircleShape
                                )
                                .clickable { viewModel.updateAccentColor(hex) }
                        )
                    }
                    // Custom picker button
                    OutlinedButton(
                        onClick = onRequestAccentColorPicker,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
                        border = BorderStroke(1.dp, VoltAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = VoltAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رنگ دلخواه", fontSize = 11.sp, color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "رنگ متون و نوشته‌ها (Body Text Color):",
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                val textPresets = listOf(
                    "#E2E8F0" to "نقره‌ای (پیش‌فرض)",
                    "#FFFFFF" to "سفید خالص",
                    "#F5F5F0" to "عاجی گرم",
                    "#CBD5E1" to "طوسی روشن"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    textPresets.forEach { (hex, label) ->
                        val isSelected = matchData.bodyTextColor.equals(hex, ignoreCase = true)
                        Button(
                            onClick = { viewModel.updateBodyTextColor(hex) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) VoltAccent else PanelBackground,
                                contentColor = if (isSelected) BgDark else TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = HairlineColor)

            // 4. Canvas Space & Margins
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "فضای کادر پوستر مربعی (Square Canvas):",
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isExpanded = matchData.canvasSpace == "expanded"
                    Button(
                        onClick = { viewModel.updateCanvasSpace("expanded") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpanded) VoltAccent else PanelBackground,
                            contentColor = if (isExpanded) BgDark else TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "فضای باز و جادار (بهینه)",
                            fontSize = 12.sp,
                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Button(
                        onClick = { viewModel.updateCanvasSpace("standard") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpanded) VoltAccent else PanelBackground,
                            contentColor = if (!isExpanded) BgDark else TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "حاشیه استاندارد",
                            fontSize = 12.sp,
                            fontWeight = if (!isExpanded) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Text(
                    "حالت فضای باز حاشیه‌ها را بهینه کرده و ۱۰٪ تا ۱۵٪ سطح مفید بیشتری برای متن‌ها ایجاد می‌کند.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = HairlineColor)

            // 5. High Contrast & Readability Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "کنتراست و ووضوح بالا برای شبکه‌های اجتماعی",
                        color = TextWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        "متون روشن‌تر و پررنگ‌تر برای خوانایی کامل در فید توییتر و موبایل",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = matchData.highContrast,
                    onCheckedChange = { viewModel.updateHighContrast(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BgDark,
                        checkedTrackColor = VoltAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = PanelBackground
                    )
                )
            }
        }
    }
}
