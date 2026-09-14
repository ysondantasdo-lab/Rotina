package br.com.ysondantas.rotina.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ysondantas.rotina.data.Crianca
import br.com.ysondantas.rotina.data.DiaSemana
import br.com.ysondantas.rotina.data.Evento
import br.com.ysondantas.rotina.data.RotinaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    familyId: String, // Mantido apenas para não quebrar a chamada na Navigation
    crianca: Crianca,
    dia: DiaSemana,
    repo: RotinaRepository,
    aoVoltar: () -> Unit
) {
    // Corrigido para chamar o método correto do novo repositório simplificado
    val eventos by repo.escutarEventosDoDia(crianca.id, dia.chave).collectAsState(initial = emptyList())
    var eventoParaDeletar by remember { mutableStateOf<Evento?>(null) }
    var mostrarNovoDialogo by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${crianca.nome} · ${dia.label}") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rotina do dia", style = MaterialTheme.typography.titleLarge)
                FilledIconButton(onClick = { mostrarNovoDialogo = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar evento")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (eventos.isEmpty()) {
                Text("Nenhum evento cadastrado ainda", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(eventos, key = { it.id }) { evento ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { eventoParaDeletar = evento }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(evento.horario, style = MaterialTheme.typography.titleLarge)
                                    Text(evento.descricao, style = MaterialTheme.typography.bodyLarge)
                                }
                                Icon(Icons.Default.Delete, contentDescription = "Toque para excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarNovoDialogo) {
        EventoDialog(
            titulo = "Novo evento",
            horarioInicial = "",
            descricaoInicial = "",
            aoConfirmar = { horario, descricao ->
                mostrarNovoDialogo = false
                escopo.launch { repo.adicionarEvento(crianca.id, dia.chave, horario, descricao) }
            },
            aoCancelar = { mostrarNovoDialogo = false }
        )
    }

    eventoParaDeletar?.let { evento ->
        AlertDialog(
            onDismissRequest = { eventoParaDeletar = null },
            title = { Text("Excluir Evento") },
            text = { Text("Deseja mesmo apagar o evento '${evento.descricao}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idDoc = evento.id
                        eventoParaDeletar = null
                        escopo.launch { repo.excluirEvento(crianca.id, dia.chave, idDoc) }
                    }
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { eventoParaDeletar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EventoDialog(
    titulo: String,
    horarioInicial: String,
    descricaoInicial: String,
    aoConfirmar: (horario: String, descricao: String) -> Unit,
    aoCancelar: () -> Unit
) {
    var horario by remember { mutableStateOf(horarioInicial) }
    var descricao by remember { mutableStateOf(descricaoInicial) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = horario,
                    onValueChange = { horario = it },
                    label = { Text("Horário (ex: 07:30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Evento (ex: Escola)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { aoConfirmar(horario, descricao) }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        }
    )
}
