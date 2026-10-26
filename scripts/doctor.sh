#!/usr/bin/env bash
# Vérification du poste avant la formation (kit J-7). Doit se terminer par OK.
# En cas d'échec, envoyez toute la sortie de ce script au formateur avant la session.
set -u
cd "$(dirname "$0")/.."

ok()   { printf '  \033[32m✔\033[0m %s\n' "$1"; }
ko()   { printf '  \033[31m✘\033[0m %s\n' "$1"; FAILED=1; }
info() { printf '  · %s\n' "$1"; }
FAILED=0

echo "1. Outils"
if java_version=$(java -version 2>&1 | head -1); then
  major=$(echo "$java_version" | sed -E 's/.*version "([0-9]+).*/\1/')
  if [ "${major:-0}" -ge 17 ] 2>/dev/null; then ok "JDK $major ($java_version)"; else ko "JDK 17 requis, trouvé : $java_version"; fi
else
  ko "java introuvable : installer un JDK 17 (Temurin)"
fi

if command -v git >/dev/null; then ok "$(git --version)"; else ko "git introuvable"; fi

if [ -n "${ANDROID_HOME:-}" ] || [ -f local.properties ]; then
  ok "SDK Android trouvé (${ANDROID_HOME:-local.properties})"
elif [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"; ok "SDK Android trouvé ($ANDROID_HOME)"
else
  ko "SDK Android introuvable : ouvrir le projet une fois dans Android Studio, ou définir ANDROID_HOME"
fi

IS_MAC=0
if [ "$(uname)" = "Darwin" ]; then
  IS_MAC=1
  if xcode=$(xcodebuild -version 2>/dev/null | head -1); then
    case "$xcode" in
      "Xcode 26.4"*) ok "$xcode" ;;
      "Xcode 27"*)   ko "$xcode : non validé par Kotlin 2.4, installer Xcode 26.4.x (developer.apple.com/download/all)" ;;
      *)             ko "$xcode : la formation demande Xcode 26.4.x" ;;
    esac
    if xcode-select -p >/dev/null 2>&1; then ok "outils en ligne de commande : $(xcode-select -p)"; else ko "outils en ligne de commande Xcode absents (xcode-select --install)"; fi
  else
    ko "Xcode introuvable ou licence non acceptée (lancer Xcode une fois)"
  fi
else
  info "Pas de Mac : la partie iOS se fera en binôme (c'est prévu)."
fi

echo "2. Gradle wrapper"
if ./gradlew --version >/dev/null 2>&1; then ok "$(./gradlew --version | grep '^Gradle')"; else ko "le wrapper Gradle ne démarre pas"; fi

echo "3. Compilation réelle (commun, iOS, Android) : plusieurs minutes au premier lancement"
TASKS=":shared:compileCommonMainKotlinMetadata :androidApp:assembleDebug"
[ "$IS_MAC" = 1 ] && TASKS="$TASKS :shared:compileKotlinIosSimulatorArm64"
if ./gradlew $TASKS; then ok "build Gradle"; else ko "build Gradle en échec (voir ci-dessus)"; fi

if [ "$IS_MAC" = 1 ]; then
  echo "4. SDK WebRTC iOS (package SPM)"
  if xcodebuild -project iosApp/iosApp.xcodeproj -resolvePackageDependencies >/dev/null 2>&1; then
    ok "package WebRTC résolu"
  else
    ko "résolution du package WebRTC en échec : ouvrir iosApp/iosApp.xcodeproj dans Xcode et attendre la fin de la résolution"
  fi
fi

echo
if [ "$FAILED" = 0 ]; then echo "OK"; else echo "ÉCHEC : envoyez cette sortie au formateur."; exit 1; fi
