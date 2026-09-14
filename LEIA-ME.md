# Rotina das Crianças — guia de configuração

## Estrutura de arquivos entregues

```
app/src/main/java/br/com/ysondantas/rotina
├── MainActivity.kt
├── data/
│   ├── Modelos.kt              (Crianca, Evento, DiaSemana)
│   └── RotinaRepository.kt     (crianças, dias, eventos em tempo real)
└── ui/
    ├── theme/Theme.kt          (fontes grandes, alto contraste)
    ├── HomeScreen.kt           (Tela inicial)
    ├── WeekScreen.kt           (Tela Semana)
    ├── DayScreen.kt            (Tela Dia)
    └── Navigation.kt           (liga as telas)
```

Copie a pasta `app/src/main/java/com/example/rotinacriancas` para dentro do
seu projeto Android Studio (ajuste o nome do pacote se usar outro).

## 1. Criar o projeto no Firebase Console

1. Acesse console.firebase.google.com e crie um projeto novo.
2. Adicione um app Android informando o **applicationId** do seu projeto
   (ex: `com.example.rotinacriancas`).
3. Baixe o `google-services.json` e coloque em `app/`.
4. Em **Firestore Database**, crie o banco (modo produção).

> O fluxo atual não usa senha nem tela de login. Configure a autenticação
> anônima no Firebase caso as regras do Firestore exijam um usuário autenticado.

## 2. Dependências (Gradle)

**`build.gradle.kts` (nível do projeto)** — plugin do Google Services:
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

**`build.gradle.kts` (módulo `app`)**:
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
```

## 3. Regras de segurança do Firestore

No console, em **Firestore Database → Regras**, use algo assim (restringe
o acesso apenas a quem já está na lista de membros da família):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }
    match /families/{familyId} {
      allow read, update: if request.auth != null;
      allow create: if request.auth != null;
      match /criancas/{criancaId}/{document=**} {
        allow read, write: if request.auth != null
          && request.auth.uid in get(/databases/$(database)/documents/families/$(familyId)).data.membros;
      }
    }
  }
}
```

## 4. Estrutura dos dados no Firestore

```
families/{familyId}
    membros: [uid1, uid2]
    criancas/{criancaId}
        nome: "Maria"
        dias/{segunda|terca|...}
            eventos/{eventoId}
                horario: "07:30"
                descricao: "Escola"

users/{uid}
    familyId: "123456"
```

## Como funciona o vínculo entre os dois pais

1. O primeiro pai cria a conta, faz login e clica em **"Criar família nova"**
   → o app gera um código de 6 dígitos.
2. Ele passa esse código para o cônjuge (WhatsApp, por exemplo).
3. O segundo pai cria a própria conta, faz login e digita o código em
   **"Código recebido do cônjuge"**.
4. A partir daí os dois apontam para o mesmo `familyId` no Firestore, e
   como todas as telas usam `addSnapshotListener` (tempo real), qualquer
   alteração feita por um aparece automaticamente no aparelho do outro.
   A persistência offline do Firestore já vem ativada por padrão.

## O que falta para ficar 100% pronto

- Testar em dois dispositivos/emuladores reais com contas diferentes.
- Ajustar `applicationId` e nome do pacote conforme seu projeto atual.
- Opcional: tela de "esqueci a senha" (Firebase Auth já suporta,
  `sendPasswordResetEmail`).
