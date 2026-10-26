# Formation Kotlin Multiplatform : l'application d'appel vidéo

Dépôt du fil rouge de la formation « Maîtriser Kotlin Multiplatform (KMP) en 3 jours » (Advisely) : une application de visioconférence WebRTC pour Android et iOS, construite module après module à partir d'un seul code Kotlin.

## Avant la formation (kit J-7)

1. Installer Android Studio (pas IntelliJ IDEA : il n'ouvre pas encore un projet en AGP 9.3), un JDK 17 et, sur Mac, Xcode 26.4.x (pas Xcode 27).
2. Cloner ce dépôt, puis lancer :

   ```bash
   ./scripts/doctor.sh
   ```

   Le script vérifie les outils, compile le code commun, iOS et Android, et résout le SDK WebRTC iOS. Il doit se terminer par `OK`.
3. Sur Mac, ouvrir une fois `iosApp/iosApp.xcodeproj` dans Xcode pour laisser résoudre le package `WebRTC`, puis renseigner votre équipe (Apple ID) dans *Signing & Capabilities* pour déployer sur un iPhone.

## Branches

`main` est le socle : c'est là que commence la formation. Chaque branche `etape/…` est l'état de référence à la fin d'un module. Ce sont des **références en lecture seule** : travaillez sur votre propre branche, et repartez d'un checkpoint si vous êtes bloqué.

| Branche | Fin du module | Contenu |
| --- | --- | --- |
| `main` | socle | Structure `shared` / `androidApp` / `iosApp`, Compose, Ktor, Koin, SDK WebRTC iOS, bouchon `CallController` |
| `etape/05-structure` | 5 (fin du jour 1) | Sérialisation (2), framework iOS et SKIE (3), `CallEvent`, `CallRepository`, faux signaling et test Turbine (4) |
| `etape/07-cablage` | 7 | Client Ktor et `SignalingClient` (6), webrtc-kmp, caméra, permissions et Koin (7), types `CallState` et `CallIntent` |
| `etape/09-etat` | 9 (fin du jour 2) | Écran d'appel Compose (8), MVI : `CallViewModel`, réducteur, effets (9) |
| `etape/10-media-local` | 10 | Capture caméra et micro, `VideoRenderer` natif : « je me vois » |
| `etape/11-connexion` | 11 | `CallEngine` : offer, answer, candidats ICE, premier appel réel à deux |
| `etape/12-scale` | 12 | XCFramework, caches Gradle, `CallSession` et moteur maillé pour la démo à trois |
| `etape/13-cicd` | 13 | Workflow GitHub Actions du module, Fastlane |
| `etape/bonus-datachannel` | bonus | Chat texte par data channel |

Pour voir ce qu'apporte un module : `git diff etape/09-etat etape/10-media-local`.

Pour récupérer un fichier d'un checkpoint sur votre branche : `git checkout etape/07-cablage -- chemin/du/fichier`.

Chaque session fige aussi les checkpoints dans des tags immuables, `<version>/socle`, `<version>/05-structure`, …, `<version>/bonus-datachannel` (par exemple `v2026.10/09-etat`) : c'est l'état exact distribué à cette session, même si les branches sont corrigées ensuite.

### Maintenance des checkpoints (formateur)

Les branches `etape/*` forment une chaîne linéaire au-dessus de `main`, sans merge ; le bonus part de `etape/11-connexion`. Une correction se commite sur la branche du module concerné, puis :

```bash
./scripts/checkpoints.sh restack     # reporte la correction dans toutes les branches suivantes
./scripts/checkpoints.sh push        # force-with-lease sur les branches, plus les tags
./scripts/checkpoints.sh tag v2026.11  # avant une session : fige l'état distribué
```

`./scripts/checkpoints.sh check` vérifie que la chaîne est intacte.

## Lancer l'application

- **Android** : ouvrir le projet dans Android Studio et lancer la configuration `androidApp`.
- **iOS** : ouvrir `iosApp/iosApp.xcodeproj` dans Xcode et lancer sur un iPhone ou un simulateur. Le simulateur n'a pas de caméra : le jalon « je me vois » (module 10) demande un iPhone physique.
- **Serveur de signaling** (modules 6 et 11, côté formateur) : voir [server/README.md](server/README.md).

## Fichiers d'atelier

Le dossier `ateliers/` contient les fichiers de départ des ateliers des modules 1 (`AtelierBytecode.kt`) et 4 (`AtelierCoroutines.kt`). Ils ne sont pas compilés tant qu'on ne les a pas copiés dans `shared`.

## Versions

Toutes les versions sont dans `gradle/libs.versions.toml` : Kotlin 2.4.20, AGP 9.3.1, Gradle 9.7.0, Compose Multiplatform 1.12.2, Ktor 3.4, Koin 4.2.2, SKIE 0.10.15, webrtc-kmp 0.125.11, SDK WebRTC iOS 125.6422.07, Xcode 26.4.

## Vérification continue

Chaque branche est compilée par GitHub Actions (`.github/workflows/verify.yml`) : build Android, tests communs, tests du serveur, compilation Kotlin/Native et build de l'app iOS sur simulateur.
