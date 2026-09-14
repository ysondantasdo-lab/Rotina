package com.example.rotinacriancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rotinacriancas.data.Crianca
import com.example.rotinacriancas.data.RotinaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    familyId: String,
    repo: RotinaRepository,
    aoAbrirCrianca: (Crianca) -> Unit
) {
    val criancas by repo.criancasFlow(familyId).collectAsState(initial = emptyList())
    var mostrarDialogo by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rotina das Crianças") }) },
        floatingActionButton = {
            if (criancas.isNotEmpty()) {
                FloatingActionButton(onClick = { mostrarDialogo = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar criança")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (criancas.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Nenhuma criança cadastrada", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { mostrarDialogo = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar criança")
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(criancas) { crianca ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                crianca.nome,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth()
                                    .clickable { aoAbrirCrianca(crianca) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        AdicionarCriancaDialog(
            aoConfirmar = { nome ->
                mostrarDialogo = false
                if (nome.isNotBlank()) {
                    escopo.launch { repo.adicionarCrianca(familyId, nome) }
                }
            },
            aoCancelar = { mostrarDialogo = false }
        )
    }
}

@Composable
private fun AdicionarCriancaDialog(aoConfirmar: (String) -> Unit, aoCancelar: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Nome da criança") },
        text = {
            OutlinedTextField(value = nome, onValueChange = { nome = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { aoConfirmar(nome) }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        }
    )
}
