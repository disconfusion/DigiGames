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

> **Fonte di verità: [`backend/src/main/resources/roadmap.md`](backend/src/main/resources/roadmap.md)** (mostrata anche sul sito in `/roadmap`).
> Questa sezione ne è un mirror: aggiornare sempre il file, poi riallineare qui.

<!-- ROADMAP:START -->
### FIX UI/UX

- [x] Monitoraggio dei pezzi persi/conquistati (scacchi, dama, battaglia navale)
- [x] Restyling header
- [x] Toast quando arrivano notifiche
- [x] Scritta di game over e win con animazioni annesse
- [x] Visibili tutte le mosse dell'utente nella sezione "Impiccato del giorno"
- [x] Regole dell'Impiccato del giorno visibili nella view
- [x] Feedback al click dei pulsanti nella sezione Admin, uniformato alle altre interazioni
- [x] Card e view troppo compresse: più responsive per schermi grandi
- [x] View della roadmap con anteprima Markdown
- [x] Sezione "Partite in corso" nella home: rientrare nelle partite attive con altri utenti (ora restano sospese, si rientra solo col codice invito)

#### Nuove animazioni

- [ ] Restyling animazioni dei pezzi di scacchi e dama
- [x] Esplosioni e buchi nell'acqua in Battaglia navale
- [x] Campo minato
- [x] Forza 4

### Game Loop / Game design

- [x] Alpha test: sistema di crediti (Token) con acquisto di "poteri" usabili in partita
- [ ] Alpha test: pack opening di carte a tema inside joke interni
- [ ] Accessori avatar comprabili con i crediti — invio doni fra utenti
- [x] Companion acquistabili dallo shop (animati a mano)
- [x] Leaderboard coi punti accumulati al posto della colonna vittorie (che passa in seconda posizione)
- [x] Alpha test: clan/casate (temporaneamente le 4 di Hogwarts), badge casata personalizzabile nella sezione personale
- [x] Impiccato: tentativo dell'intera parola in qualsiasi momento (giusto = vittoria, sbagliato = eliminazione diretta) — sblocca gli stalli in cui non si possono più chiamare lettere/vocali

#### Altre fix

- [x] Modale "Ultime Fix" editabile dall'Admin
- [ ] Regole dell'Impiccato del giorno personalizzabili dall'Admin
- [ ] Callout con messaggi dall'Admin nella sezione "Impiccato del giorno" per giornate speciali / info utili

### Tecnico / Infra

- [ ] Efficientare storage e cancellazione dati dal DB: Neon offre solo ~0,5 GB, quindi strutturare il progetto attorno a questo limite (retention e purge dei dati storici)

### Bug Fixing

Segnalazioni utenti.

- [ ] Se hosti una partita e poi esci o finisci di giocare, la stanza rimane aperta e non si può chiudere (@emanuele.taglia)
- [ ] Quiz: gestire la casistica di pareggio (@manuchao)
- [x] Impiccato: se una parola contiene più di due vocali non può essere completata (@manuchao) — risolto: ora si può tentare l'intera parola in qualsiasi momento
- [ ] Togliere le emoji dalla personalizzazione account, o sostituirle con nuove animazioni e sprite (@spacevampire)
<!-- ROADMAP:END -->

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
