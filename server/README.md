# Serveur de signaling de la formation

Implémente le protocole du module 6 :

- `POST /rooms/{room}/join` renvoie `{ token, participantId, iceServers }`.
- `GET /rooms/{room}?token=…` ouvre le WebSocket. Le serveur diffuse `join` aux autres participants quand une socket s'ouvre et `leave` quand elle se ferme, route les messages par `to` (sinon diffusion à la room), et renseigne toujours `from` lui-même.
- `GET /health` répond `ok`.

## Lancer en local (instance de secours, plan B réseau)

```bash
./gradlew :server:run            # écoute sur 0.0.0.0:8080
PORT=9000 ./gradlew :server:run  # autre port
```

Puis, dans `shared/src/commonMain/kotlin/com/advisely/call/SignalingConfig.kt` (à partir de `etape/07-cablage`), mettre l'adresse IP du poste formateur sur le réseau de la salle, par exemple `http://192.168.1.20:8080`. Ne pas utiliser `localhost` : sur un téléphone, `localhost` désigne le téléphone.

## TURN de secours

Le handshake renvoie toujours un STUN public. Pour ajouter un TURN (coturn ou service managé), avec ses identifiants :

```bash
TURN_URLS="turn:turn.example.com:3478?transport=udp" TURN_USERNAME=formation TURN_CREDENTIAL=secret ./gradlew :server:run
```

Sans `username` et `credential`, un serveur TURN refuse la connexion.
