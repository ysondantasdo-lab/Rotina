package br.com.ysondantas.rotina.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    familyId: String,
    crianca: Crianca,
    dia: DiaSemana,
    repo: RotinaRepository,
    aoVoltar: () -> Unit
) {
    val eventos by repo.escutarEventosDoDia(crianca.id, dia.chave).collectAsState(initial = emptyList())
    var eventoParaDeletar by remember { mutableStateOf<Evento?>(null) }
    var eventoParaEditar by remember { mutableStateOf<Evento?>(null) }
    var mostrarNovoDialogo by remember { mutableStateOf(false) }

    // Guarda os dados pendentes de salvar enquanto perguntamos o escopo (todos os dias x só esse dia)
    var pendenteEdicao by remember { mutableStateOf<PendenteEdicao?>(null) }

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
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f).clickable { eventoParaEditar = evento }) {
                                    Text(evento.horario, style = MaterialTheme.typography.titleLarge)
                                    Text(evento.descricao, style = MaterialTheme.typography.bodyLarge)
                                    if (evento.diasRepeticao.size > 1) {
                                        Text(
                                            "Repete: " + evento.diasRepeticao.joinToString(", ") { chave ->
                                                DiaSemana.entries.first { it.chave == chave }.label
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { eventoParaEditar = evento }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar evento")
                                    }
                                    IconButton(onClick = { eventoParaDeletar = evento }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir evento", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo: novo evento (com seleção de dias de repetição)
    if (mostrarNovoDialogo) {
        EventoDialog(
            titulo = "Novo evento",
            horarioInicial = "",
            descricaoInicial = "",
            diaAtual = dia,
            diasSelecionadosIniciais = setOf(dia.chave),
            aoConfirmar = { horario, descricao, diasSelecionados ->
                mostrarNovoDialogo = false
                escopo.launch {
                    repo.adicionarEvento(
                        criancaId = crianca.id,
                        dias = diasSelecionados.toList(),
                        horario = horario,
                        descricao = descricao
                    )
                }
            },
            aoCancelar = { mostrarNovoDialogo = false }
        )
    }

    // Diálogo: editar evento existente
    eventoParaEditar?.let { evento ->
        EventoDialog(
            titulo = "Editar evento",
            horarioInicial = evento.horario,
            descricaoInicial = evento.descricao,
            diaAtual = dia,
            diasSelecionadosIniciais = if (evento.diasRepeticao.isNotEmpty()) evento.diasRepeticao.toSet() else setOf(dia.chave),
            aoConfirmar = { horario, descricao, diasSelecionados ->
                eventoParaEditar = null
                val eraGrupo = evento.diasRepeticao.size > 1
                if (eraGrupo) {
                    // Pertencia a um grupo: pergunta o escopo da alteração
                    pendenteEdicao = PendenteEdicao(evento, horario, descricao, diasSelecionados)
                } else {
                    // Evento de um dia só: aplica direto (se marcou mais dias, vira grupo automaticamente)
                    escopo.launch {
                        repo.atualizarEventoGrupo(
                            criancaId = crianca.id,
                            diaAtual = dia.chave,
                            evento = evento,
                            novoHorario = horario,
                            novaDescricao = descricao,
                            novosDias = diasSelecionados.toList()
                        )
                    }
                }
            },
            aoCancelar = { eventoParaEditar = null }
        )
    }

    // Pergunta: aplicar a alteração para todos os dias do grupo ou só para este dia?
    pendenteEdicao?.let { pendente ->
        AlertDialog(
            onDismissRequest = { pendenteEdicao = null },
            title = { Text("Aplicar alteração") },
            text = { Text("Esse evento se repete em outros dias. Deseja alterar em todos os dias ou só em ${dia.label}?") },
            confirmButton = {
                TextButton(onClick = {
                    val p = pendenteEdicao
                    pendenteEdicao = null
                    if (p != null) {
                        escopo.launch {
                            repo.atualizarEventoGrupo(
                                criancaId = crianca.id,
                                diaAtual = dia.chave,
                                evento = p.evento,
                                novoHorario = p.horario,
                                novaDescricao = p.descricao,
                                novosDias = p.diasSelecionados.toList()
                            )
                        }
                    }
                }) { Text("Todos os dias") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val p = pendenteEdicao
                    pendenteEdicao = null
                    if (p != null) {
                        escopo.launch {
                            repo.atualizarEventoApenasEsseDia(
                                criancaId = crianca.id,
                                diaAtual = dia.chave,
                                evento = p.evento,
                                novoHorario = p.horario,
                                novaDescricao = p.descricao
                            )
                        }
                    }
                }) { Text("Só ${dia.label}") }
            }
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

private data class PendenteEdicao(
    val evento: Evento,
    val horario: String,
    val descricao: String,
    val diasSelecionados: Set<String>
)

@Composable
private fun EventoDialog(
    titulo: String,
    horarioInicial: String,
    descricaoInicial: String,
    diaAtual: DiaSemana,
    diasSelecionadosIniciais: Set<String>,
    aoConfirmar: (horario: String, descricao: String, diasSelecionados: Set<String>) -> Unit,
    aoCancelar: () -> Unit
) {
    var horario by remember { mutableStateOf(horarioInicial) }
    var descricao by remember { mutableStateOf(descricaoInicial) }
    var diasSelecionados by remember { mutableStateOf(diasSelecionadosIniciais) }

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
                Spacer(Modifier.height(16.dp))
                Text("Repetir em:", style = MaterialTheme.typography.titleSmall)
                DiaSemana.entries.forEach { dia ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                diasSelecionados = if (dia.chave in diasSelecionados) {
                                    diasSelecionados - dia.chave
                                } else {
                                    diasSelecionados + dia.chave
                                }
                            }
                    ) {
                        Checkbox(
                            checked = dia.chave in diasSelecionados,
                            onCheckedChange = { marcado ->
                                diasSelecionados = if (marcado) diasSelecionados + dia.chave else diasSelecionados - dia.chave
                            }
                        )
                        Text(dia.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Garante que pelo menos o dia atual fique marcado (não deixa "esvaziar" o evento)
                    val dias = if (diasSelecionados.isEmpty()) setOf(diaAtual.chave) else diasSelecionados
                    aoConfirmar(horario, descricao, dias)
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        }
    )
}