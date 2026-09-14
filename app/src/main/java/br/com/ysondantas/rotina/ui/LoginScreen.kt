package br.com.ysondantas.rotina.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ysondantas.rotina.data.AuthFamilyRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(repo: AuthFamilyRepository, aoLogar: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var modoCadastro by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (modoCadastro) "Criar conta" else "Entrar",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = senha, onValueChange = { senha = it },
            label = { Text("Senha") }, modifier = Modifier.fillMaxWidth()
        )
        erro?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                escopo.launch {
                    try {
                        if (modoCadastro) repo.cadastrar(email, senha) else repo.entrar(email, senha)
                        aoLogar()
                    } catch (e: Exception) {
                        erro = e.localizedMessage ?: "Erro ao entrar"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (modoCadastro) "Cadastrar" else "Entrar")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { modoCadastro = !modoCadastro },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (modoCadastro) "Já tenho conta" else "Criar uma conta nova")
        }
    }
}

/**
 * Depois do login: se o usuário ainda não pertence a uma família,
 * ele precisa criar uma (e mandar o código pro cônjuge) ou digitar
 * o código que o cônjuge já criou.
 */
@Composable
fun VincularFamiliaScreen(repo: AuthFamilyRepository, aoVincular: () -> Unit) {
    var codigoGerado by remember { mutableStateOf<String?>(null) }
    var codigoDigitado by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vincular família", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        if (codigoGerado == null) {
            Button(
                onClick = {
                    escopo.launch {
                        try {
                            codigoGerado = repo.criarFamilia()
                        } catch (e: Exception) {
                            erro = e.localizedMessage
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Criar família nova") }
        } else {
            Text("Código da família: $codigoGerado", style = MaterialTheme.typography.titleLarge)
            Text("Compartilhe esse código com o outro pai/mãe.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = aoVincular, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar")
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("— ou —")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = codigoDigitado, onValueChange = { codigoDigitado = it },
            label = { Text("Código recebido do cônjuge") }, modifier = Modifier.fillMaxWidth()
        )
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                escopo.launch {
                    try {
                        repo.entrarNaFamilia(codigoDigitado)
                        aoVincular()
                    } catch (e: Exception) {
                        erro = e.localizedMessage
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Entrar com código") }
    }
}
