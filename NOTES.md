# Notes de l'atelier

Chacun complète ce fichier sur sa propre branche. Les réponses ci-dessous sont celles de la branche de référence.

## Module 6, étape 4 : `send()` avant `incoming()`

`send()` ne fait rien et ne dit rien : `session` est encore `null` tant que personne ne collecte `incoming()`, et l'appel `session?.send(…)` est simplement ignoré.

Solution retenue dans le fil rouge : on n'envoie qu'en réaction à un message reçu sur la socket (module 11), donc toujours avec une session ouverte. Autres options valables : faire échouer `send()` explicitement (`checkNotNull(session)`), mettre les messages en file d'attente jusqu'à l'ouverture, ou exposer l'état de connexion en `StateFlow<Boolean>` (bonus du module 6).

## Module 7, étape 3 : `expect/actual` ou interface plus DI ?

`CameraController` (expect/actual) et `CameraCapture` (interface) font la même chose, et leurs deux `actual` sont identiques : webrtc-kmp offre déjà une API commune. Ici, l'interface gagne, parce qu'elle se remplace par un faux en test. `expect/actual` reste le bon outil pour `PermissionController`, où les deux plateformes n'ont vraiment rien en commun.
