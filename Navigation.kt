package com.example.rotinacriancas.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rotinacriancas.data.AuthFamilyRepository
import com.example.rotinacriancas.data.DiaSemana
import com.example.rotinacriancas.data.RotinaRepository

@Composable
fun AppNavigation(
    authRepo: AuthFamilyRepository = AuthFamilyRepository(),
    rotinaRepo: RotinaRepository = RotinaRepository()
) {
    val navController = rememberNavController()
    var logado by remember { mutableStateOf(authRepo.uidAtual != null) }

    if (!logado) {
        LoginScreen(authRepo) { logado = true }
        return
    }

    val familyId by authRepo.familyIdFlow().collectAsState(initial = "carregando")

    when (familyId) {
        "carregando" -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        null -> VincularFamiliaScreen(authRepo) { /* a tela reabre sozinha via flow */ }
        else -> {
            // Guardamos o nome da criança/dia selecionados em memória simples,
            // já que a navegação por argumentos de String cobre o essencial.
            var criancaSelecionadaId by remember { mutableStateOf<String?>(null) }
            var criancaSelecionadaNome by remember { mutableStateOf<String?>(null) }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(familyId = familyId!!, repo = rotinaRepo) { crianca ->
                        criancaSelecionadaId = crianca.id
                        criancaSelecionadaNome = crianca.nome
                        navController.navigate("semana")
                    }
                }
                composable("semana") {
                    val crianca = com.example.rotinacriancas.data.Crianca(
                        id = criancaSelecionadaId ?: "", nome = criancaSelecionadaNome ?: ""
                    )
                    WeekScreen(
                        crianca = crianca,
                        aoVoltar = { navController.popBackStack() },
                        aoAbrirDia = { dia -> navController.navigate("dia/${dia.name}") }
                    )
                }
                composable(
                    route = "dia/{diaNome}",
                    arguments = listOf(navArgument("diaNome") { type = NavType.StringType })
                ) { backStackEntry ->
                    val diaNome = backStackEntry.arguments?.getString("diaNome") ?: DiaSemana.SEGUNDA.name
                    val dia = DiaSemana.valueOf(diaNome)
                    val crianca = com.example.rotinacriancas.data.Crianca(
                        id = criancaSelecionadaId ?: "", nome = criancaSelecionadaNome ?: ""
                    )
                    DayScreen(
                        familyId = familyId!!,
                        crianca = crianca,
                        dia = dia,
                        repo = rotinaRepo,
                        aoVoltar = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
