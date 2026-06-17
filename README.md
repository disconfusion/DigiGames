# DigiGames 🎮

Piattaforma interna di minigiochi multiplayer (battaglia navale, forza 4, impiccato, quiz)
con lobby, inviti diretti, real-time e leaderboard. Uso aziendale, accesso ristretto ai
domini `@digitaliasistemi.it` e `@ascesa.it`.

## Stack

| Layer      | Tecnologia                                   |
|------------|----------------------------------------------|
| Frontend   | SvelteKit (Svelte 5 runes) + adapter-static (SPA) |
| Backend    | Quarkus (Java 21) + Maven                    |
| Real-time  | `quarkus-websockets-next`                    |
| DB         | PostgreSQL (Dev Services in locale, Neon in prod) |
| Auth       | JWT RSA (smallrye-jwt) + bcrypt              |
| Deploy     | Fly.io (singola istanza, stato partita in-memory) |

## Sviluppo locale

Prerequisiti: Java 21, Maven, Node 22, Docker (per il Postgres Dev Services).

```bash
# Backend (Postgres avviato automaticamente via Docker) -> http://localhost:8085
cd backend && ./mvnw quarkus:dev

# Frontend (Vite) -> http://localhost:5173, chiama il backend su :8085
cd frontend && npm install && npm run dev
```

> Le porte 8080/8081 sono occupate dallo stack `pio/traefik`: il backend in dev usa **8085**
> (vedi `%dev.quarkus.http.port` in `application.properties`).

## Build di produzione (single deploy)

Il frontend statico viene servito da Quarkus. Build + copia in `META-INF/resources`:

```bash
cd frontend && npm run build
rm -rf ../backend/src/main/resources/META-INF/resources
mkdir -p ../backend/src/main/resources/META-INF/resources
cp -r build/* ../backend/src/main/resources/META-INF/resources/
cd ../backend && ./mvnw package   # un solo artefatto, un solo deploy su Fly
```

Variabili d'ambiente in prod (Fly secrets): `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Stato (roadmap)

- [x] **Step 1** — Auth (whitelist dominio + JWT + bcrypt), lobby/stanze, scheletro WebSocket real-time
- [x] **Step 2** — Impiccato (co-op, parola random server-side)
- [x] **Step 3** — Forza 4 (2 giocatori a turni, win detection)
- [x] **Step 4** — Quiz (multiplayer, banca domande IT, fasi QUESTION/REVEAL, punteggi)
- [x] **Step 5** — Battaglia navale (2 fasi, informazione nascosta via viste per-giocatore)
- [x] **Step 6** — Campo minato (co-op, flood-fill, mine nascoste lato server)
- [ ] Step 7 — Impiccato del giorno (parola unica giornaliera + classifica tentativi)
- [ ] Step 8 — Leaderboard globale + persistenza risultati su Postgres

Tutti i giochi sono coperti da unit test (logica pura) e test d'integrazione WebSocket.

## Architettura backend

```
backend/src/main/java/it/digitaliasistemi/minigames/
  domain/   AppUser (Panache)
  auth/     AuthResource (register/login), AuthService (JWT + bcrypt + whitelist)
  rooms/    Room (stato in-memory, campo `game`), RoomManager, RoomResource (REST lobby)
  ws/       RoomSocket (websockets-next: hello/chat + delega al GameEngine)
  game/     GameEngine (interfaccia), GameContext, GameEngines (registry CDI)
    hangman/ connect4/ minesweeper/ quiz/ battleship/   <- un bean GameEngine + logica pura per gioco
```

**Aggiungere un gioco** = creare un bean `@ApplicationScoped implements GameEngine` (auto-scoperto
dal registry) + un componente board nel frontend (`frontend/src/lib/games/`) registrato in
`registry.ts`. Nessuna modifica a `RoomSocket`. Logica di gioco = classi **pure** testabili;
il WebSocket è sottile sopra e il **server è sempre l'autorità** (valida ogni mossa).

Protocollo di gioco: snapshot completi `game:state` (l'ultimo è autoritativo) + `game:over`.
I giochi a informazione nascosta (battaglia navale) usano `GameContext.sendTo(email, ...)` per
viste per-giocatore.
