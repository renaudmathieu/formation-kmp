#!/usr/bin/env bash
# Maintenance des checkpoints (côté formateur). Les branches etape/* forment une chaîne linéaire
# au-dessus de main ; le bonus part de etape/11-connexion. Pas de merge : une correction se fait
# sur la branche du module concerné, puis `restack` la reporte dans toutes les branches suivantes.
#
#   ./scripts/checkpoints.sh check            vérifie la chaîne
#   ./scripts/checkpoints.sh restack          rebase chaque checkpoint sur le précédent
#   ./scripts/checkpoints.sh tag v2026.11     pose les tags immuables v2026.11/socle, v2026.11/05-structure…
#   ./scripts/checkpoints.sh push             pousse branches (force-with-lease) et tags
set -euo pipefail
cd "$(dirname "$0")/.."

CHAIN=(main etape/05-structure etape/07-cablage etape/09-etat etape/10-media-local
       etape/11-connexion etape/12-scale etape/13-cicd)
BONUS=etape/bonus-datachannel
BONUS_BASE=etape/11-connexion

ok() { printf '  \033[32m✔\033[0m %s\n' "$1"; }
ko() { printf '  \033[31m✘\033[0m %s\n' "$1"; exit 1; }

# Paires (précédent suivant) : la chaîne, puis le bonus sur son module de départ
pairs() {
  for ((i = 1; i < ${#CHAIN[@]}; i++)); do echo "${CHAIN[i-1]} ${CHAIN[i]}"; done
  echo "$BONUS_BASE $BONUS"
}

ensure_local() {   # crée les branches locales absentes à partir de origin
  for b in "${CHAIN[@]}" "$BONUS"; do
    git show-ref --verify -q "refs/heads/$b" || git branch -q "$b" "origin/$b"
  done
}

check() {
  pairs | while read -r prev next; do
    git merge-base --is-ancestor "$prev" "$next" || ko "$next ne contient pas $prev : lancer restack"
    [ -z "$(git rev-list --merges "$prev..$next")" ] || ko "$next contient un merge : la chaîne doit rester linéaire"
    ok "$prev → $next ($(git rev-list --count "$prev..$next") commit(s))"
  done
}

restack() {
  [ -z "$(git status --porcelain)" ] || ko "arbre de travail non propre"
  # Base de chaque branche AVANT de déplacer quoi que ce soit : le point où elle a quitté la précédente.
  # --fork-point retrouve l'ancien sommet dans le reflog, même si la précédente a été amendée.
  local bases=()
  while read -r prev next; do
    bases+=("$(git merge-base --fork-point "$prev" "$next" || git merge-base "$prev" "$next")")
  done < <(pairs)
  local i=0 start; start=$(git branch --show-current)
  while read -r prev next; do
    git rebase -q --onto "$prev" "${bases[i]}" "$next" \
      || ko "conflit sur $next : le résoudre, git rebase --continue, puis relancer restack"
    ok "$next rebasée sur $prev"
    i=$((i + 1))
  done < <(pairs)
  git checkout -q "$start"
}

tag() {
  local version=${1:?usage : checkpoints.sh tag <version>, par exemple v2026.11}
  git tag -a "$version/socle" -m "Socle de la formation ($version)" main
  for b in "${CHAIN[@]:1}" "$BONUS"; do
    git tag -a "$version/${b#etape/}" -m "Checkpoint $b ($version)" "$b"
  done
  ok "tags $version/* posés"
}

push() {
  git push --force-with-lease origin "${CHAIN[@]}" "$BONUS"
  git push origin --tags
}

ensure_local
case "${1:-}" in
  check)   check ;;
  restack) restack && check ;;
  tag)     check && tag "${2:-}" ;;
  push)    check && push ;;
  *)       sed -n '2,10p' "$0"; exit 1 ;;
esac
