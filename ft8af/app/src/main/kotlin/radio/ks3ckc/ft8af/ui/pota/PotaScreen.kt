package radio.ks3ckc.ft8af.ui.pota

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k1af.ft8af.GeneralVariables
import com.k1af.ft8af.MainViewModel
import com.k1af.ft8af.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import radio.ks3ckc.ft8af.pota.PotaAuth
import radio.ks3ckc.ft8af.pota.PotaClient
import radio.ks3ckc.ft8af.pota.PotaUploadException
import radio.ks3ckc.ft8af.pota.PotaSessionManager
import radio.ks3ckc.ft8af.pota.PotaSpotsRepository
import radio.ks3ckc.ft8af.pota.potaSpotFrequencyKhz
import radio.ks3ckc.ft8af.pota.model.PotaActivation
import radio.ks3ckc.ft8af.pota.model.PotaQso
import radio.ks3ckc.ft8af.pota.model.PotaSpot
import radio.ks3ckc.ft8af.ui.decode.QrzAvatar
import radio.ks3ckc.ft8af.theme.Accent
import radio.ks3ckc.ft8af.theme.AccentSoft
import radio.ks3ckc.ft8af.theme.BgApp
import radio.ks3ckc.ft8af.theme.BgSurface
import radio.ks3ckc.ft8af.theme.BgSurface2
import radio.ks3ckc.ft8af.theme.Border
import radio.ks3ckc.ft8af.theme.BorderStrong
import radio.ks3ckc.ft8af.theme.StatusConfirmed
import radio.ks3ckc.ft8af.theme.TextMuted
import radio.ks3ckc.ft8af.theme.TextPrimary
import radio.ks3ckc.ft8af.ui.components.CredentialFieldRole
import radio.ks3ckc.ft8af.ui.components.autofill

private enum class PotaSubTab(@StringRes val labelRes: Int) {
    ACTIVATE(R.string.pota_tab_activate),
    HUNT(R.string.pota_tab_hunt),
}

@Composable
fun PotaScreen(mainViewModel: MainViewModel) {
    var subTab by rememberSaveable { mutableStateOf(PotaSubTab.ACTIVATE) }
    // When non-null, a history entry was tapped: show its detail screen instead
    // of the tab UI. Plain remember (not saveable) — PotaActivation isn't
    // Parcelable and re-tapping after a config change is cheap.
    var selectedActivation by remember { mutableStateOf<PotaActivation?>(null) }

    // Hunter tab and an active activation both rely on fresh spots — start
    // polling whenever the POTA screen is mounted.
    DisposableEffect(Unit) {
        PotaSpotsRepository.start()
        onDispose { PotaSpotsRepository.stop() }
    }

    val detail = selectedActivation
    if (detail != null) {
        BackHandler { selectedActivation = null }
        ActivationDetailScreen(
            activation = detail,
            mainViewModel = mainViewModel,
            onBack = { selectedActivation = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        PotaTabHeader(subTab = subTab, onTabSelected = { subTab = it })
        Spacer(Modifier.height(10.dp))
        when (subTab) {
            PotaSubTab.ACTIVATE -> ActivateTab(onOpenActivation = { selectedActivation = it })
            PotaSubTab.HUNT -> HuntTab(mainViewModel)
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-tab chip header
// ---------------------------------------------------------------------------

@Composable
private fun PotaTabHeader(subTab: PotaSubTab, onTabSelected: (PotaSubTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (tab in PotaSubTab.entries) {
            val selected = tab == subTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) AccentSoft else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    color = if (selected) Accent else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Activate tab
// ---------------------------------------------------------------------------

@Composable
private fun ActivateTab(onOpenActivation: (PotaActivation) -> Unit) {
    val activation by PotaSessionManager.currentActivation.collectAsStateWithLifecycle()
    val contacts by PotaSessionManager.activationQsos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Past activations, surfaced inline below the Start Activation form so the
    // per-park ADIF export (reached by tapping a row) is one tap from this
    // landing tab instead of buried in a separate History tab. Reload whenever
    // an activation starts/ends so a just-finished session appears immediately.
    var history by remember { mutableStateOf<List<PotaActivation>>(emptyList()) }
    LaunchedEffect(activation?.id, activation?.endedAtMs) {
        history = withContext(Dispatchers.IO) { PotaSessionManager.history() }
    }
    val recent = remember(history) { historyForDisplay(history) }
    val parkRefs = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf(*it.toTypedArray()) },
        ),
    ) { mutableStateListOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Park picker sheet state
    var showPicker by remember { mutableStateOf(false) }
    var pickerTargetIndex by remember { mutableIntStateOf(-1) }

    // Refresh the activation's QSO counter periodically while one is running so
    // the on-screen tally reflects QSOs added by the TX/RX path. The first
    // refresh is immediate (no delay) so QSOs logged while on another tab are
    // visible as soon as the user switches back to the POTA tab.
    LaunchedEffect(activation?.id, refreshKey) {
        if (activation != null) {
            if (refreshKey > 0) kotlinx.coroutines.delay(3000)
            // refreshCounter() does blocking SQLite (reload + QSO list); keep it
            // off the main dispatcher so the immediate first poll (and every 3s
            // poll after) can't jank the UI / trigger an ANR.
            withContext(Dispatchers.IO) { PotaSessionManager.refreshCounter() }
            refreshKey++
        }
    }

    // Tick every 30 s so the per-row "5m ago" labels don't stay stuck at
    // whatever they said when the list was first rendered.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000)
        }
    }

    // Box host so ParkPickerSheet (a fillMaxSize scrim + sheet) overlays the tab
    // content. Without it the sheet is a trailing sibling of the fillMaxSize
    // Activate content in PotaScreen's Column, so it lays out into 0 leftover
    // height and the picker is invisible (tap does nothing). Mirrors how
    // FrequencyPickerSheet is hosted as a sibling overlay in FT8AFApp.
    Box(modifier = Modifier.fillMaxSize()) {
        if (activation == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "start-card") {
                    Card {
                        SectionTitle(stringResource(R.string.pota_start_activation))
                        Spacer(Modifier.height(8.dp))
                        parkRefs.forEachIndexed { index, ref ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = ref,
                                    onValueChange = { parkRefs[index] = it.uppercase().take(10) },
                                    label = { Text(stringResource(R.string.pota_park_reference_label)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            pickerTargetIndex = index
                                            showPicker = true
                                        }) {
                                            Icon(
                                                Icons.Filled.Search,
                                                contentDescription = stringResource(R.string.pota_search_parks),
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                    colors = textFieldColors(),
                                    modifier = Modifier.weight(1f),
                                )
                                if (parkRefs.size > 1) {
                                    IconButton(onClick = { parkRefs.removeAt(index) }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.pota_remove_park),
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                            if (index < parkRefs.lastIndex) Spacer(Modifier.height(4.dp))
                        }
                        if (parkRefs.size < 10) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { parkRefs.add("") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.pota_add_park), color = TextPrimary, fontSize = 13.sp)
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.pota_max_parks_reached),
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(120) },
                            label = { Text(stringResource(R.string.pota_notes_optional_label)) },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val started = PotaSessionManager.start(parkRefs, notes.ifBlank { null })
                                if (started == null) {
                                    Toast.makeText(context, context.getString(R.string.pota_enter_park_reference_first), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.pota_activating, started.parkRefsDisplay), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgApp),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.pota_start_activation), fontWeight = FontWeight.SemiBold) }
                    }
                }
                item(key = "recent-header") {
                    Text(
                        stringResource(R.string.pota_tab_history),
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                if (recent.isEmpty()) {
                    item(key = "recent-empty") {
                        Text(
                            stringResource(R.string.pota_no_past_activations),
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                } else {
                    items(recent, key = { it.id }) { row ->
                        HistoryRow(row = row, onClick = { onOpenActivation(row) })
                    }
                }
            }
        } else {
            // Frame the operator + plottable contacts; null when nothing has a grid yet.
            val mapData = remember(contacts) {
                buildActivationMapData(contacts, GeneralVariables.getMyMaidenhead4Grid())
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "activation-card") {
                    ActiveActivationCard(
                        activation = activation!!,
                        onStop = {
                            PotaSessionManager.end()
                            Toast.makeText(context, context.getString(R.string.pota_activation_ended), Toast.LENGTH_SHORT).show()
                        },
                        onSelfSpot = {
                            val myCall = GeneralVariables.myCallsign ?: ""
                            if (myCall.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.pota_set_callsign_first), Toast.LENGTH_SHORT).show()
                            } else {
                                val refs = activation!!.parkRefs
                                scope.launch {
                                    var successCount = 0
                                    for (ref in refs) {
                                        val ok = PotaClient.selfSpot(
                                            activator = myCall,
                                            spotter = myCall,
                                            frequencyKhz = potaSpotFrequencyKhz(GeneralVariables.band),
                                            mode = GeneralVariables.currentMode().displayName,
                                            reference = ref,
                                            comments = "CQ POTA via FT8AF",
                                        )
                                        if (ok) successCount++
                                    }
                                    val msg = if (successCount == refs.size) {
                                        if (refs.size == 1) context.getString(R.string.pota_self_spot_posted)
                                        else context.getString(R.string.pota_spotted_n_parks, successCount)
                                    } else {
                                        context.getString(R.string.pota_self_spot_failed)
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
                if (mapData != null) {
                    item(key = "activation-map") {
                        Spacer(Modifier.height(2.dp))
                        PotaActivationMap(
                            operatorLat = mapData.operatorLat,
                            operatorLon = mapData.operatorLon,
                            contacts = mapData.contacts,
                        )
                    }
                }
                if (contacts.isNotEmpty()) {
                    item(key = "contacts-header") {
                        Text(
                            stringResource(R.string.pota_qsos),
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(contacts, key = { it.id }) { qso ->
                        PotaContactRow(qso, nowMs)
                    }
                }
            }
        }

        ParkPickerSheet(
            visible = showPicker,
            onDismiss = { showPicker = false },
            onParkSelected = { ref ->
                if (pickerTargetIndex in parkRefs.indices) {
                    parkRefs[pickerTargetIndex] = ref
                }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ActiveActivationCard(
    activation: PotaActivation,
    onStop: () -> Unit,
    onSelfSpot: () -> Unit,
) {
    Card {
        val parks = activation.parkRefs
        if (parks.size <= 3) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.pota_status_active).uppercase(),
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                for (ref in parks) ParkPill(ref)
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.pota_status_active).uppercase(),
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                for (ref in parks.take(3)) ParkPill(ref)
                Text(
                    stringResource(R.string.pota_parks_more, parks.size - 3),
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatBlock(
                label = stringResource(R.string.pota_qsos),
                value = activation.qsoCount.toString(),
                hint = if (activation.qsoCount >= 10) stringResource(R.string.pota_valid_activation) else stringResource(R.string.pota_to_valid, 10 - activation.qsoCount),
                valueColor = if (activation.qsoCount >= 10) StatusConfirmed else Accent,
            )
            StatBlock(
                label = stringResource(R.string.pota_elapsed),
                value = formatElapsed(System.currentTimeMillis() - activation.startedAtMs),
                hint = stringResource(R.string.pota_since_start),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSelfSpot,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgApp),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.pota_self_spot), fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.pota_end_activation), color = TextPrimary) }
        }
        if (!activation.notes.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.pota_notes_quote, activation.notes),
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.pota_tx_cq_preview, GeneralVariables.myCallsign ?: "", GeneralVariables.getMyMaidenhead4Grid()),
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Contact row
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PotaContactRow(qso: PotaQso, nowMs: Long) {
    val context = LocalContext.current
    // Prefer the relative "ago" readout; the raw stored HHMMz string is a
    // fallback for imported/ADIF rows with no parsable qso_date so the column
    // never shows a blank cell.
    val qsoMs = remember(qso.qsoDate, qso.timeOn) { parseQsoUtcMs(qso.qsoDate, qso.timeOn) }
    val agoLabel = qsoMs?.let { qsoAgeLabel(context.resources, qsoTimeAgo(it, nowMs)) }
        ?: formatQsoTimeUtc(qso.timeOn)
    val utcLabel = formatQsoTimeUtc(qso.timeOn)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QrzAvatar(callsign = qso.callsign, size = 32.dp, fallbackText = qso.callsign.take(2))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    qso.callsign,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (qso.sigInfo != null) {
                    Spacer(Modifier.width(6.dp))
                    ParkPill(qso.sigInfo)
                }
            }
            Row {
                Text(
                    formatContactDetails(qso.mode, qso.band, qso.grid),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        val showUtc = { Toast.makeText(context, utcLabel, Toast.LENGTH_SHORT).show() }
        val showUtcLabel = stringResource(R.string.pota_qso_time_show_utc)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                // Long-press shows the UTC time (the ask in #783). A long-press-
                // only control is a trap for accessibility services, though: they
                // announce a plain "activate" action that did nothing. So a tap
                // does the same thing, both actions carry a localized label, and
                // the target meets the 48 dp minimum — the two 11 sp labels alone
                // are well under it (Copilot review on #787).
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .combinedClickable(
                    onClickLabel = showUtcLabel,
                    onLongClickLabel = showUtcLabel,
                    onClick = showUtc,
                    onLongClick = showUtc,
                ),
        ) {
            Text(
                agoLabel,
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "${qso.rstRcvd} dB",
                color = Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hunt tab
// ---------------------------------------------------------------------------

@Composable
private fun HuntTab(mainViewModel: MainViewModel) {
    val spotsMap by PotaSpotsRepository.spotsByCall.collectAsStateWithLifecycle()
    val spots = remember(spotsMap) { spotsMap.values.sortedBy { it.frequencyKhz } }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (spots.isEmpty()) stringResource(R.string.pota_no_spots) else stringResource(R.string.pota_active_spots, spots.size),
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(spots, key = { "${it.activator}-${it.reference}-${it.frequencyKhz}" }) { spot ->
                SpotRow(spot = spot, onClick = {
                    // Open the QSO sheet pre-filled with this activator's callsign. Tuning the
                    // radio is the operator's job — most rigs aren't CAT-controlled by FT8AF
                    // for frequency changes during a session.
                    mainViewModel.qsoSheetCallsign.postValue(spot.activator)
                    Toast.makeText(
                        context,
                        context.getString(R.string.pota_targeting, spot.activator, "%.1f".format(spot.frequencyKhz), spot.reference),
                        Toast.LENGTH_LONG,
                    ).show()
                })
            }
        }
    }
}

@Composable
private fun SpotRow(spot: PotaSpot, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spot.activator,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                ParkPill(spot.reference)
            }
            if (spot.parkName.isNotBlank()) {
                Text(
                    spot.parkName + if (spot.locationDesc.isNotBlank()) " · ${spot.locationDesc}" else "",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            if (spot.comments.isNotBlank()) {
                Text(stringResource(R.string.pota_notes_quote, spot.comments), color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "%.1f kHz".format(spot.frequencyKhz),
            color = Accent,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

/**
 * Past activations to list (newest first) below the Start Activation form.
 * Drops any still-active row so the in-progress activation is never shown as
 * "past", and sorts by start time so the ordering is deterministic regardless
 * of how the DAO returned the rows.
 */
internal fun historyForDisplay(all: List<PotaActivation>): List<PotaActivation> =
    all.filterNot { it.isActive }.sortedByDescending { it.startedAtMs }

/** No usable ID token (never logged in, or the stored refresh token was revoked). */
private class NotSignedInException : Exception("not signed in")

/**
 * Build the per-park ADIF for [activation] and upload each document to POTA's
 * authenticated endpoint. Returns the number of parks uploaded on success, or a
 * failure carrying the first error. A [NotSignedInException] specifically means
 * the caller should prompt for login (no token, or the refresh token is dead).
 */
private suspend fun uploadActivation(
    mainViewModel: MainViewModel,
    activation: PotaActivation,
): Result<Int> {
    val token = PotaAuth.idToken()
        ?: return Result.failure(NotSignedInException())
    val db = mainViewModel.databaseOpr?.db
        ?: return Result.failure(IllegalStateException("no database"))
    val docs = withContext(Dispatchers.IO) { PotaAdifExporter.buildActivationAdif(db, activation) }
    if (docs.isEmpty()) return Result.failure(IllegalStateException("no QSOs to upload"))
    var ok = 0
    var firstError: Throwable? = null
    for (doc in docs) {
        PotaClient.uploadAdif(token, doc.filename, doc.content)
            .onSuccess { ok++ }
            .onFailure { if (firstError == null) firstError = it }
    }
    return if (ok == docs.size) {
        Result.success(ok)
    } else {
        // Preserve the original throwable (e.g. PotaUploadException) so the caller
        // can classify it for a useful message rather than a raw HTTP dump.
        Result.failure(firstError ?: IllegalStateException("uploaded $ok of ${docs.size}"))
    }
}

/** How an upload failed, mapped to a user-facing message in [startUpload]. */
internal enum class UploadFailureKind { BUSY, SERVER, NETWORK, OTHER }

/**
 * Classify an upload failure so the UI can explain it.
 *
 *  - [BUSY]: a gateway status (502/503/504) — POTA's backend was transiently down.
 *    [PotaClient.uploadAdif] already retried with backoff, so by the time this
 *    surfaces it's still flapping; tell the user to try again shortly, *not* to
 *    go check their park ref.
 *  - [SERVER]: any other 5xx — POTA accepted the request but rejected the log,
 *    almost always a bad park ref or a callsign not registered to the account.
 *  - [NETWORK]: an [java.io.IOException] — a connectivity problem on our side.
 *  - [OTHER]: anything else, surfaced with its raw message.
 */
internal fun classifyUploadFailure(error: Throwable?): UploadFailureKind = when {
    error is PotaUploadException && error.httpCode in setOf(502, 503, 504) -> UploadFailureKind.BUSY
    error is PotaUploadException && error.httpCode in 500..599 -> UploadFailureKind.SERVER
    error is java.io.IOException -> UploadFailureKind.NETWORK
    else -> UploadFailureKind.OTHER
}

@Composable
private fun PotaLoginDialog(
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onUseSocial: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(PotaAuth.loggedInEmail().orEmpty()) }
    // Plain remember (not rememberSaveable) — the password must never be written to
    // SavedInstanceState, which can be persisted to disk on process death. Matches
    // the QRZ credentials dialog.
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        title = { Text(stringResource(R.string.pota_login_title), color = TextPrimary) },
        text = {
            Column {
                Text(
                    stringResource(R.string.pota_login_desc),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text(stringResource(R.string.pota_login_email)) },
                    singleLine = true,
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = textFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(CredentialFieldRole.EMAIL) {
                            email = it.trim()
                        },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.pota_login_password)) },
                    singleLine = true,
                    enabled = !submitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = textFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(CredentialFieldRole.PASSWORD) {
                            password = it
                        },
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(Border))
                    Text(
                        stringResource(R.string.pota_login_or),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(Border))
                }
                Spacer(Modifier.height(12.dp))
                // Federated accounts (Google/Facebook/Amazon) have no Cognito password,
                // so the email/password fields above can't authenticate them — this
                // opens the hosted-UI WebView flow instead.
                OutlinedButton(
                    onClick = onUseSocial,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.pota_login_social),
                        color = TextPrimary,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email, password) },
                enabled = !submitting && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgApp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BgApp, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.pota_login_button), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.pota_login_cancel), color = TextMuted)
            }
        },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HistoryRow(
    row: PotaActivation,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    for (ref in row.parkRefs) ParkPill(ref)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (row.isActive) stringResource(R.string.pota_status_active) else stringResource(R.string.pota_qso_count, row.qsoCount),
                    color = if (row.isActive) Accent else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDateRange(row), color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (!row.notes.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.pota_notes_quote, row.notes), color = TextMuted, fontSize = 11.sp)
        }
        if (!row.isActive) {
            TextButton(onClick = { PotaShareActivity.open(context, row) }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.pota_social_share), color = Accent, fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Activation detail screen (opened from a history row)
// ---------------------------------------------------------------------------

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ActivationDetailScreen(
    activation: PotaActivation,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contacts by remember(activation.id) { mutableStateOf<List<PotaQso>?>(null) }

    LaunchedEffect(activation.id) {
        contacts = withContext(Dispatchers.IO) { PotaSessionManager.getQsosForActivation(activation) }
    }

    // Tick every 30 s so the per-row "5m ago" labels stay fresh for a
    // currently-open historical activation (same reason as ActivateTab).
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val loaded = contacts
    // Fall back to the live grid only while the activation is active — before the
    // first QSO carries my_gridsquare the map would otherwise be hidden. For a
    // finished activation we keep null so a stale current grid can't misplace the
    // operator on a historical map.
    val mapData = remember(loaded, activation.isActive) {
        loaded?.let {
            buildActivationMapData(
                it,
                fallbackOperatorGrid = if (activation.isActive) GeneralVariables.getMyMaidenhead4Grid() else null,
            )
        }
    }

    // Direct upload-to-POTA state. A missing/revoked token surfaces as
    // NotSignedInException, which re-prompts for sign-in (SRP dialog, or the
    // hosted-UI WebView for federated accounts) before retrying the upload.
    var uploading by remember { mutableStateOf(false) }
    var loginPending by remember { mutableStateOf(false) }
    var loginSubmitting by remember { mutableStateOf(false) }
    var oauthPending by remember { mutableStateOf(false) }

    fun startUpload() {
        if (uploading) return
        uploading = true
        scope.launch {
            val res = uploadActivation(mainViewModel, activation)
            uploading = false
            res.onSuccess { n ->
                Toast.makeText(context, context.getString(R.string.pota_upload_success, n), Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                if (e is NotSignedInException) {
                    loginPending = true
                } else {
                    val msg = when (classifyUploadFailure(e)) {
                        UploadFailureKind.BUSY -> context.getString(R.string.pota_upload_busy)
                        UploadFailureKind.SERVER -> context.getString(R.string.pota_upload_server_error)
                        UploadFailureKind.NETWORK -> context.getString(R.string.pota_upload_network_error)
                        UploadFailureKind.OTHER -> context.getString(R.string.pota_upload_failed, e.message ?: "?")
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Header: back chevron + park pills + date + count.
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pota_back),
                    tint = TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (ref in activation.parkRefs) ParkPill(ref)
                }
                Text(formatDateRange(activation), color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                if (activation.isActive) stringResource(R.string.pota_status_active) else stringResource(R.string.pota_qso_count, activation.qsoCount),
                color = if (activation.isActive) Accent else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!activation.notes.isNullOrBlank()) {
                item(key = "notes") {
                    Text(stringResource(R.string.pota_notes_quote, activation.notes), color = TextMuted, fontSize = 11.sp)
                }
            }
            if (mapData != null) {
                item(key = "map") {
                    Text(
                        stringResource(R.string.pota_contacts_map),
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    PotaActivationMap(
                        operatorLat = mapData.operatorLat,
                        operatorLon = mapData.operatorLon,
                        contacts = mapData.contacts,
                    )
                }
            }
            item(key = "actions") {
                if (!activation.isActive) {
                    OutlinedButton(
                        onClick = { PotaShareActivity.open(context, activation) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pota_social_share), color = Accent)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (activation.qsoCount > 0) {
                    Button(
                        onClick = {
                            // uploadActivation re-prompts for sign-in via
                            // NotSignedInException when there's no usable token.
                            startUpload()
                        },
                        enabled = !uploading,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgApp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BgApp, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.pota_upload_to_pota), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            PotaAdifExporter.shareActivationAdif(context, mainViewModel, activation) { ok ->
                                if (!ok) Toast.makeText(context, context.getString(R.string.pota_export_failed), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pota_share_adif), color = TextPrimary, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pota.app/#/user/upload"))
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, context.getString(R.string.pota_no_browser_available), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pota_open_pota_app), color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
            val list = loaded
            if (!list.isNullOrEmpty()) {
                item(key = "contacts-header") {
                    Text(
                        stringResource(R.string.pota_qsos),
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                items(list, key = { it.id }) { qso ->
                    PotaContactRow(qso, nowMs)
                }
            }
        }
    }

    if (loginPending) {
        PotaLoginDialog(
            submitting = loginSubmitting,
            onDismiss = { if (!loginSubmitting) loginPending = false },
            onSubmit = { email, pass ->
                loginSubmitting = true
                scope.launch {
                    val r = PotaAuth.login(email, pass)
                    loginSubmitting = false
                    r.onSuccess {
                        loginPending = false
                        startUpload()
                    }.onFailure { e ->
                        Toast.makeText(context, context.getString(R.string.pota_login_failed, e.message ?: "?"), Toast.LENGTH_LONG).show()
                    }
                }
            },
            onUseSocial = {
                // Federated sign-in (Google/Facebook/Amazon) can't go through SRP —
                // hand off to the hosted-UI WebView, keeping the pending upload.
                if (!loginSubmitting) {
                    loginPending = false
                    oauthPending = true
                }
            },
        )
    }

    if (oauthPending) {
        PotaOAuthDialog(onClose = { success ->
            oauthPending = false
            if (success) startUpload()
        })
    }
}

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BorderStrong, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StatBlock(label: String, value: String, hint: String, valueColor: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = TextMuted, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(hint, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ParkPill(ref: String) {
    Text(
        ref,
        color = StatusConfirmed,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(BgSurface2, RoundedCornerShape(6.dp))
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = BgSurface2,
    unfocusedContainerColor = BgSurface2,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextMuted,
    focusedIndicatorColor = Accent,
    unfocusedIndicatorColor = Border,
    cursorColor = Accent,
)

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Second line of a contact row: mode · band · grid, with blank fields (and the
 * separators that would surround them) suppressed. All three are blank on a
 * bare imported row, which must render as an empty string rather than a row of
 * orphaned separators.
 */
internal fun formatContactDetails(mode: String, band: String, grid: String): String =
    listOfNotNull(
        mode.ifBlank { null },
        band.ifBlank { null },
        grid.ifBlank { null },
    ).joinToString(" · ")

/**
 * Widen a stored `time_on` to 6-digit HHMMSS using the same rule as
 * [PotaQsoWindow.ROW_STAMP] and DatabaseOpr's dedupe ORDER BY: empty is
 * midnight, odd-width values recover a dropped leading zero (`"815"` is 08:15)
 * before padding, even-width values just pad. Returns `null` for a
 * non-numeric value, which no padding rule can rescue, and for a value longer
 * than ADIF's six-digit maximum: padding would silently truncate `"14453099"`
 * to a plausible 14:45:30 (and the odd-width `"1445309"` to 01:44:53), so the
 * callers would show a confident but wrong time instead of their raw-value
 * fallback. (The SQL rule keeps its substr because a WHERE clause has no
 * "reject" branch; it only orders rows, it never displays them.)
 */
internal fun normalizeAdifTimeOn(timeOn: String): String? = when {
    timeOn.isEmpty() -> "000000"
    timeOn.length > 6 -> null
    timeOn.any { !it.isDigit() } -> null
    timeOn.length % 2 == 1 -> ("0$timeOn" + "000000").substring(0, 6)
    else -> (timeOn + "000000").substring(0, 6)
}

/**
 * UTC readout for the long-press tooltip on a contact row: HH:MMz. Normalizes
 * the variable-width `time_on` first ([normalizeAdifTimeOn]), so a dropped
 * leading zero reads the same here as it does everywhere else — `"815"` and
 * `"81530"` are both 08:15, not `"815"` and a nonsensical `"81:53z"`. Returns
 * the raw value rather than throwing when the row simply has no usable time
 * (empty / non-numeric / out-of-range clock fields, e.g. an ADIF import that
 * dropped `time_on`), which is what the caller shows when the row can't be
 * dated at all.
 */
internal fun formatQsoTimeUtc(timeOn: String): String {
    if (timeOn.isEmpty()) return timeOn
    val padded = normalizeAdifTimeOn(timeOn) ?: return timeOn
    val hh = padded.substring(0, 2)
    val mm = padded.substring(2, 4)
    val ss = padded.substring(4, 6)
    // Seconds are never shown, but they still decide whether the row has a real
    // clock reading: parseQsoUtcMs rejects "144560" (60 s), so rendering it as
    // a plausible "14:45z" here would have the two paths disagree about the
    // same row.
    if (hh.toInt() > 23 || mm.toInt() > 59 || ss.toInt() > 59) return timeOn
    return "$hh:${mm}z"
}

/**
 * Turn the stored `qso_date` (YYYYMMDD) + `time_on` into a GMT epoch millis
 * instant, or `null` if the row lacks a usable date. `time_on` is normalized to
 * HHMMSS using the same padding rule as [PotaQsoWindow.ROW_STAMP]: HHMM widens
 * to HHMM00, odd-width values recover the dropped leading zero first. Both
 * callers (the "ago" formatter and any future callers) treat the row as
 * un-datable and fall back to the raw UTC readout when this returns null.
 */
internal fun parseQsoUtcMs(qsoDate: String, timeOn: String): Long? {
    if (qsoDate.length != 8 || qsoDate.any { !it.isDigit() }) return null
    val padded = normalizeAdifTimeOn(timeOn) ?: return null
    return try {
        java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
            .apply {
                timeZone = java.util.TimeZone.getTimeZone("GMT")
                // SimpleDateFormat is lenient by default, which silently rolls
                // impossible values into a neighbouring instant instead of
                // rejecting them: "20260231" becomes 2026-03-03 and "256000"
                // becomes 01:00 the next day. Those rows would then show a
                // plausible-looking but wrong "ago" delta rather than falling
                // back to the raw UTC readout, so demand a real calendar
                // instant here.
                isLenient = false
            }
            .parse(qsoDate + padded)
            ?.time
    } catch (_: java.text.ParseException) {
        null
    }
}

/** Which relative-time bucket a contact's age falls in; see [qsoTimeAgo]. */
internal enum class QsoAgeUnit { JUST_NOW, SECONDS, MINUTES, HOURS, DAYS }

/** A contact's age: the bucket plus the count shown in it (0 for [QsoAgeUnit.JUST_NOW]). */
internal data class QsoAge(val unit: QsoAgeUnit, val count: Int)

/**
 * Bucket a contact's age for the "ago" readout: under 30 s is "just now", then
 * seconds, minutes, hours, days (no upper bucket — an old QSO keeps counting in
 * days). Clamps negative deltas to "just now" so clock skew (device time behind
 * rig time, or a bogus imported timestamp) can't produce "-2m ago". Buckets
 * flip on exact minute / hour / day boundaries so the display is consistent
 * between rows.
 *
 * Returns the bucket rather than text: the words come from string/plural
 * resources via [qsoAgeLabel], so the row is translated like the rest of the
 * screen instead of staying English in every `values-*` locale (Copilot
 * review on #787).
 */
internal fun qsoTimeAgo(qsoTimeMs: Long, nowMs: Long): QsoAge {
    val deltaSec = ((nowMs - qsoTimeMs) / 1000L).coerceAtLeast(0L)
    return when {
        deltaSec < 30L -> QsoAge(QsoAgeUnit.JUST_NOW, 0)
        deltaSec < 60L -> QsoAge(QsoAgeUnit.SECONDS, deltaSec.toInt())
        deltaSec < 3600L -> QsoAge(QsoAgeUnit.MINUTES, (deltaSec / 60L).toInt())
        deltaSec < 86_400L -> QsoAge(QsoAgeUnit.HOURS, (deltaSec / 3600L).toInt())
        else -> QsoAge(QsoAgeUnit.DAYS, (deltaSec / 86_400L).toInt())
    }
}

/**
 * Resolve a [QsoAge] in the operator's language: the "just now" string plus
 * one plural per unit, so translators own both the wording and the
 * singular/plural forms. A plain function over [android.content.res.Resources]
 * rather than a Composable so the resource mapping is unit-testable under
 * Robolectric; the row passes `LocalContext.current.resources`.
 */
internal fun qsoAgeLabel(res: android.content.res.Resources, age: QsoAge): String = when (age.unit) {
    QsoAgeUnit.JUST_NOW -> res.getString(R.string.pota_qso_age_just_now)
    QsoAgeUnit.SECONDS -> res.getQuantityString(R.plurals.pota_qso_age_seconds, age.count, age.count)
    QsoAgeUnit.MINUTES -> res.getQuantityString(R.plurals.pota_qso_age_minutes, age.count, age.count)
    QsoAgeUnit.HOURS -> res.getQuantityString(R.plurals.pota_qso_age_hours, age.count, age.count)
    QsoAgeUnit.DAYS -> res.getQuantityString(R.plurals.pota_qso_age_days, age.count, age.count)
}

private fun formatDateRange(row: PotaActivation): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
    val start = fmt.format(java.util.Date(row.startedAtMs))
    val end = row.endedAtMs?.let { fmt.format(java.util.Date(it)) } ?: "—"
    return "$start → $end"
}
