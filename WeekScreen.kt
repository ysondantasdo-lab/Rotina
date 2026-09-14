package com.example.rotinacriancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rotinacriancas.data.Crianca
import com.example.rotinacriancas.data.DiaSemana

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    crianca: Crianca,
    aoVoltar: () -> Unit,
    aoAbrirDia: (DiaSemana) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(crianca.nome) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(DiaSemana.entries) { dia ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(
                        dia.label,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aoAbrirDia(dia) }
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}
