package com.jungangtuth.nexusai.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jungangtuth.nexusai.data.SecureStore
import com.jungangtuth.nexusai.model.ChatMessage
import com.jungangtuth.nexusai.model.ProviderConfig
import com.jungangtuth.nexusai.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp() {
    val ctx = LocalContext.current
    val store = remember { SecureStore(ctx) }
    val api = remember { ApiClient() }
    var tab by remember { mutableStateOf(0) }
    var providers by remember { mutableStateOf(store.getProviders()) }
    var selectedId by remember { mutableStateOf(providers.firstOrNull { it.enabled }?.id ?: providers.first().id) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NEXUS AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141A22)))
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF141A22)) {
                NavigationBarItem(selected = tab==0, onClick={tab=0}, icon={}, label={Text("Chat")})
                NavigationBarItem(selected = tab==1, onClick={tab=1}, icon={}, label={Text("API Keys")})
                NavigationBarItem(selected = tab==2, onClick={tab=2}, icon={}, label={Text("Agent")})
                NavigationBarItem(selected = tab==3, onClick={tab=3}, icon={}, label={Text("Info")})
            }
        },
        containerColor = Color(0xFF0B0F14)
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ChatScreen(providers, selectedId, { selectedId = it }, messages, input, { input = it }, loading) {
                    val text = input.trim(); if (text.isEmpty() || loading) return@ChatScreen
                    val p = providers.find { it.id == selectedId } ?: return@ChatScreen
                    messages.add(ChatMessage("user", text, p.name)); input = ""; loading = true
                    scope.launch {
                        val reply = withContext(Dispatchers.IO) { api.sendMessage(p, messages.toList()) }
                        messages.add(ChatMessage("assistant", reply, p.name)); loading = false
                    }
                }
                1 -> SettingsScreen(providers, onSave = { providers = it; store.saveProviders(it); if (providers.none{p->p.id==selectedId && p.enabled}) selectedId = providers.first().id })
                2 -> AgentScreen()
                3 -> InfoScreen()
            }
        }
    }
}

@Composable
fun ChatScreen(providers: List<ProviderConfig>, selectedId: String, onSelect: (String)->Unit,
               messages: List<ChatMessage>, input: String, onInput:(String)->Unit, loading:Boolean, onSend:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            providers.forEach { p ->
                FilterChip(selected = selectedId==p.id, onClick={ onSelect(p.id) },
                    label={ Text(p.name) }, enabled = true)
            }
        }
        val current = providers.find{it.id==selectedId}
        if (current != null && (!current.enabled || current.apiKey.isBlank())) {
            Text("Provider belum aktif / API key kosong. Atur di tab API Keys.", color=Color(0xFFFFB74D), modifier=Modifier.padding(8.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { m ->
                val isUser = m.role=="user"
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if(isUser) Alignment.End else Alignment.Start) {
                    Surface(color = if(isUser) Color(0xFF243447) else Color(0xFF1C2430), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            if(!isUser && m.model!=null) Text(m.model, color=Color(0xFF7C4DFF), style=MaterialTheme.typography.labelSmall)
                            Text(m.content)
                        }
                    }
                }
            }
            if (loading) item { Text("NEXUS AI sedang menjawab...", color=Color(0xFF00B0FF), modifier=Modifier.padding(8.dp)) }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value=input, onValueChange=onInput, modifier=Modifier.weight(1f), placeholder={Text("Kirim pesan...")})
            Spacer(Modifier.width(8.dp))
            Button(onClick=onSend, enabled=!loading) { Text("Kirim") }
        }
    }
}

@Composable
fun SettingsScreen(providers: List<ProviderConfig>, onSave:(List<ProviderConfig>)->Unit) {
    var list by remember(providers) { mutableStateOf(providers) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Pengaturan Provider & API Key", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold) }
        item { Text("Kunci disimpan terenkripsi lokal (EncryptedSharedPreferences). Tidak ditampilkan utuh.", color=Color.Gray, style=MaterialTheme.typography.bodySmall) }
        items(list.size) { i ->
            val p = list[i]
            var keyInput by remember(p.id) { mutableStateOf("") }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141A22))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, modifier=Modifier.weight(1f), fontWeight=FontWeight.SemiBold)
                        Switch(checked=p.enabled, onCheckedChange={ v -> list = list.toMutableList().also{ it[i]=p.copy(enabled=v)} })
                    }
                    Text("Model: ${p.defaultModel}", color=Color.Gray, style=MaterialTheme.typography.bodySmall)
                    Text(if (p.apiKey.isBlank()) "Status: belum ada key" else "Status: key tersimpan (${p.apiKey.take(3)}•••)", color=if(p.apiKey.isBlank()) Color(0xFFFF8A80) else Color(0xFF80CBC4))
                    OutlinedTextField(value=keyInput, onValueChange={keyInput=it}, label={Text("API Key baru")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick={ if(keyInput.isNotBlank()){ list = list.toMutableList().also{ it[i]=p.copy(apiKey=keyInput.trim(), enabled=true)}; keyInput="" } }) { Text("Simpan") }
                        OutlinedButton(onClick={ list = list.toMutableList().also{ it[i]=p.copy(apiKey="", enabled=false)} }) { Text("Hapus") }
                    }
                }
            }
        }
        item { Button(onClick={ onSave(list) }, modifier=Modifier.fillMaxWidth()) { Text("Simpan Semua Pengaturan") } }
    }
}

@Composable
fun AgentScreen() {
    var backendUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Belum terhubung ke backend agen.") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Agent Mode (Target B) - MVP", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
        item { Text("NEXUS v2 menyiapkan UI klien untuk agen penuh (seperti opencode): baca/edit kode, jalankan perintah, dan alur multi-langkah. Eksekusi dilakukan di backend/workspace terisolasi, bukan di dalam APK.", color=Color.LightGray) }
        item { OutlinedTextField(value=backendUrl, onValueChange={backendUrl=it}, label={Text("URL Backend Agen (contoh: https://server/agent)")}, modifier=Modifier.fillMaxWidth(), singleLine=true) }
        item { Button(onClick={ status = if(backendUrl.isBlank()) "Masukkan URL backend." else "Konfigurasi tersimpan lokal (MVP). Koneksi & eksekusi alat memerlukan backend Target B yang kompatibel." }) { Text("Simpan / Cek Koneksi") } }
        item { Text(status, color=Color(0xFF00B0FF)) }
        item { Divider() }
        item { Text("Mitigasi & kontrol (direncanakan):", fontWeight=FontWeight.SemiBold) }
        item { Text("• Sandbox/workspace terisolasi untuk eksekusi.\n• Izin eksplisit & pratinjau diff sebelum aksi berisiko.\n• Batas perintah/sumber daya.\n• Kelola rahasia aman (tanpa log).\n• Wajib review/CI sebelum merge.", color=Color.Gray) }
        item { Text("Catatan: backend agen penuh dan unduhan APK otomatis ke Download dari hasil build belum terintegrasi penuh pada v2 ini.", color=Color(0xFFFFB74D)) }
    }
}

@Composable
fun InfoScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("NEXUS AI v2 (MVP)", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
        Text("Aplikasi multi-model: OpenAI, Gemini, Claude, DeepSeek, OpenRouter.")
        Text("Baru di v2: layar Agent Mode (Target B) sebagai fondasi integrasi backend agen (alat file/eksekusi) dengan kontrol & mitigasi.")
        Text("Fitur MVP: chat, ganti provider, simpan API key terenkripsi lokal, UI dark futuristik.", color=Color.Gray)
    }
}
