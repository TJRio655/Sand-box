package com.lab.assistant.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lab.assistant.data.ModelDownloader
import com.lab.assistant.data.ModelsCatalog
import com.lab.assistant.data.Prefs
import kotlinx.coroutines.launch

class VMFactory(private val ctx: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>): T = ChatViewModel(ctx) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val vm: ChatViewModel = viewModel(factory = VMFactory(ctx))
    var tab by remember { mutableIntStateOf(0) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chats by vm.chats.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            Column(Modifier.padding(12.dp)) {
                Text("History (local only, swipe to manage in chat)")
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(chats, key = { it.id }) { c ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.SpaceBetween) {
                                TextButton(onClick = { vm.currentId.value = c.id; vm.loadMessages(c.id); scope.launch { drawer.close() } }) {
                                    Text(c.title.take(24))
                                }
                                IconButton(onClick = { vm.deleteChat(c.id) }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.deleteAll() }) { Text("Delete all history") }
            }
        }
    ) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(if (tab == 0) "Lab Assistant" else if (tab == 1) "Models" else "Defensive Labs") },
                navigationIcon = { TextButton(onClick = { scope.launch { drawer.open() } }) { Text("☰") } },
                actions = { IconButton(onClick = { vm.newChat() }) { Icon(Icons.Default.Add, null) } }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Chat") }, icon = { Text("💬") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Models") }, icon = { Text("📦") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Labs") }, icon = { Text("🛡") })
                }
            }
        ) { pv ->
            Column(Modifier.padding(pv).fillMaxSize()) {
                when (tab) {
                    0 -> ChatScreen(vm)
                    1 -> ModelsScreen()
                    else -> LabsScreen()
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val id by vm.currentId.collectAsState()
    val msgs by vm.messages.collectAsState()
    val stream by vm.streaming.collectAsState()
    val busy by vm.busy.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(id) { if (id == null) vm.newChat() else vm.loadMessages(id!!) }

    LazyColumn(Modifier.weight(1f).padding(8.dp), reverseLayout = false) {
        items(msgs, key = { it.uid }) { m ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text((if (m.role == "user") "You" else "Assistant") + " • long-press to delete", style = MaterialThemeCustom.label())
                    Text(m.text)
                    TextButton(onClick = { id?.let { vm.deleteMsg(it, m.uid) } }) { Text("Delete") }
                }
            }
        }
        if (stream.isNotEmpty()) { item { Card(Modifier.fillMaxWidth().padding(4.dp)) { Text(stream, Modifier.padding(10.dp)) } } }
        if (busy && stream.isEmpty()) { item { Text("Thinking on-device…", Modifier.padding(8.dp)) } }
    }
    Row(Modifier.fillMaxWidth().padding(8.dp)) {
        OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask defensive / lab question…") }, maxLines = 4)
        IconButton(onClick = { if (input.isNotBlank() && !busy) { vm.send(input.trim()); input = "" } }) {
            Icon(Icons.Default.Send, null)
        }
    }
    Text("Private: no analytics, history stays on device. Delete anytime.", Modifier.padding(horizontal = 12.dp))
}

@Composable
fun ModelsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var selPath by remember { mutableStateOf("") }
    var gpu by remember { mutableStateOf(true) }
    var temp by remember { mutableFloatStateOf(0.8f) }
    var maxTok by remember { mutableIntStateOf(1024) }
    LaunchedEffect(Unit) {
        selPath = kotlinx.coroutines.flow.first(Prefs.modelPath(ctx))
        gpu = kotlinx.coroutines.flow.first(Prefs.gpu(ctx))
        temp = kotlinx.coroutines.flow.first(Prefs.temp(ctx))
        maxTok = kotlinx.coroutines.flow.first(Prefs.maxTok(ctx))
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(ModelsCatalog.models) { m ->
            val downloaded = remember(selPath) { ModelDownloader.isDownloaded(ctx, m) }
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.name); Text(m.desc); Text("${m.sizeMb} MB • ${m.fileName}")
                    Row {
                        if (!downloaded) Button(onClick = { ModelDownloader.enqueue(ctx, m) }, Modifier.padding(end = 8.dp)) { Text("Download") }
                        else {
                            Button(onClick = { scope.launch { Prefs.setModelPath(ctx, ModelDownloader.fileFor(ctx, m).absolutePath) ; selPath = ModelDownloader.fileFor(ctx, m).absolutePath } }) { Text("Select") }
                            TextButton(onClick = { ModelDownloader.delete(ctx, m) }) { Text("Delete") }
                        }
                    }
                    if (selPath.endsWith(m.fileName)) Text("✓ Active")
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("GPU acceleration"); Switch(gpu, { gpu = it; scope.launch { Prefs.setGpu(ctx, it) } }) }
            Text("Temperature: $temp"); Slider(temp, { temp = it; scope.launch { Prefs.setTemp(ctx, it) } }, valueRange = 0f..1.5f)
            Text("Max tokens: $maxTok"); Slider(maxTok.toFloat(), { maxTok = it.toInt(); scope.launch { Prefs.setMaxTok(ctx, it.toInt()) } }, valueRange = 256f..4096f)
            Text("Tip: int8 .task + GPU = fastest on phone. Close background apps. 6GB+ RAM recommended for 3B models.")
        }
    }
}

@Composable
fun LabsScreen() {
    val items = listOf(
        "Isolated Lab Only" to "Use emulator / spare device, airplane mode + local Wi-Fi lab. Never test systems you don't own. Get written authorization.",
        "Defensive Notes" to "Document configs, patches, firewall rules, detection queries. This app stores notes locally.",
        "Vuln Triage (defensive)" to "Inventory → scan own lab with permission → prioritize patching → verify fix. No exploit automation.",
        "Blue Team Drills" to "Practice log review, backup restore, phishing report flow, incident checklist.",
        "PhD Research Log" to "Hypothesis, method, lab setup, results, mitigations. Export via copy-paste, no cloud."
    )
    LazyColumn(Modifier.padding(12.dp)) {
        items(items) { (t, d) -> Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Column(Modifier.padding(12.dp)) { Text(t); Spacer(Modifier.height(4.dp)); Text(d) } } }
    }
}

object MaterialThemeCustom { @Composable fun label() = androidx.compose.material3.MaterialTheme.typography.labelSmall }
