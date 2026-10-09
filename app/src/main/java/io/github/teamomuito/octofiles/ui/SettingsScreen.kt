package io.github.teamomuito.octofiles.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.teamomuito.octofiles.data.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.teamomuito.octofiles.BuildConfig
import io.github.teamomuito.octofiles.ui.theme.GlassCard
import io.github.teamomuito.octofiles.ui.theme.LiquidBackground

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val tidy by vm.tidy.collectAsStateWithLifecycle()
    val silent by vm.canTidySilently.collectAsStateWithLifecycle()
    val canNotify by vm.canNotify.collectAsStateWithLifecycle()
    val skipTrash by vm.skipTrash.collectAsStateWithLifecycle()
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    var rereadStarted by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LiquidBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back")
                }
                Text("settings", style = MaterialTheme.typography.headlineSmall)
            }

            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("tidy up temporary screenshots", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "qr codes, boarding passes and login codes go to the trash after a while. " +
                                "anything you mark as keep stays put.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = tidy.enabled, onCheckedChange = { on -> vm.updateTidy { it.copy(enabled = on) } })
                }
                if (tidy.enabled) {
                    Spacer(Modifier.height(14.dp))
                    Text("after", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        for ((days, label) in listOf(1 to "1 day", 3 to "3 days", 7 to "1 week", 14 to "2 weeks")) {
                            FilterChip(
                                selected = tidy.days == days,
                                onClick = { vm.updateTidy { it.copy(days = days) } },
                                label = { Text(label) },
                                shape = RoundedCornerShape(50),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    CheckRow("qr codes", tidy.qr) { on -> vm.updateTidy { it.copy(qr = on) } }
                    CheckRow("boarding passes", tidy.boarding) { on -> vm.updateTidy { it.copy(boarding = on) } }
                    CheckRow("login and verification codes", tidy.codes) { on -> vm.updateTidy { it.copy(codes = on) } }
                }
            }

            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("swipe deletes skip the trash", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (skipTrash) {
                                "on. photos you swipe away are gone right away and the space frees up immediately. no undo."
                            } else {
                                "off. swiped photos sit in your phone's trash for 30 days first, just in case. " +
                                    "the space frees up once the trash is emptied."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = skipTrash, onCheckedChange = vm::setSkipTrash)
                }
            }

            if (Build.VERSION.SDK_INT >= 31) {
                Section {
                    Text("tidy without asking", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (silent) {
                            "on. no more popups: old temporary screenshots go by themselves when you open the app, and swipe deletes go through in one tap."
                        } else {
                            "android normally asks before an app deletes photos. " +
                                "give potato \"media management\" access and it can do it without the popup, for screenshots and swipes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!silent) {
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(onClick = { openMediaManagementSettings(context) }) { Text("allow") }
                    }
                }
            }

            if (!canNotify && Build.VERSION.SDK_INT >= 33) {
                Section {
                    Text("reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "a little notification when there's stuff ready to tidy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("turn on") }
                }
            }

            Section {
                Text("read everything again", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (rereadStarted) "on it. this takes a bit if you have lots of screenshots."
                    else "handy if search seems off. your keep choices are remembered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    enabled = !rereadStarted,
                    onClick = {
                        rereadStarted = true
                        vm.readAgain()
                    },
                ) { Text("start over") }
            }

            Section {
                UpdateSection(
                    state = update,
                    current = BuildConfig.VERSION_NAME,
                    onCheck = {
                        scope.launch {
                            update = UpdateState.Checking
                            val release = withContext(Dispatchers.IO) { Updates.latest() }
                            update = when {
                                release == null -> UpdateState.Failed
                                Updates.isNewer(release.version, BuildConfig.VERSION_NAME) -> UpdateState.Available(release)
                                else -> UpdateState.Current
                            }
                        }
                    },
                    onOpen = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 32.dp, end = 32.dp),
            ) {
                Potato(boxSize = 96.dp)
                Spacer(Modifier.height(8.dp))
                Text("octo potato ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "your screenshots and the words in them stay on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Current : UpdateState
    data object Failed : UpdateState
    data class Available(val release: Updates.Release) : UpdateState
}

@Composable
private fun UpdateSection(state: UpdateState, current: String, onCheck: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Potato(boxSize = 56.dp, bob = state != UpdateState.Checking)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (state) {
                    UpdateState.Idle -> "updates"
                    UpdateState.Checking -> "checking…"
                    UpdateState.Current -> "you're up to date"
                    UpdateState.Failed -> "couldn't check"
                    is UpdateState.Available -> "version ${state.release.version} is out"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "you're on $current",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when (state) {
        UpdateState.Checking -> {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        UpdateState.Idle, UpdateState.Current, UpdateState.Failed -> {
            Spacer(Modifier.height(10.dp))
            Text(
                when (state) {
                    UpdateState.Idle -> "checks github for a newer release. only a request for the release list, nothing about this phone goes along."
                    UpdateState.Current -> "nothing newer on github right now."
                    else -> "no network, or no release has been published yet."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCheck) { Text(if (state == UpdateState.Idle) "check" else "check again") }
        }
        is UpdateState.Available -> {
            val release = state.release
            if (release.notes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    release.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { onOpen(release.apkUrl ?: release.page) }) {
                    Text(if (release.apkSize > 0) "download · ${formatBytes(context, release.apkSize)}" else "download")
                }
                TextButton(onClick = { onOpen(release.page) }) { Text("details") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "android will ask before it installs. your data stays put when the new version installs over the old one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
