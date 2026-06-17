# DigiGames — guida sviluppo

Web app aziendale interna di minigiochi multiplayer (svago tra colleghi, non un prodotto).

## Stack
- **Frontend**: SvelteKit + Svelte 5 (runes), `adapter-static` SPA (`ssr=false`, `prerender=false`). Dir `frontend/`.
- **Backend**: Quarkus (Java 21, Maven), `quarkus-websockets-next`, Panache/Hibernate. Dir `backend/`.
- **DB**: PostgreSQL — locale via Dev Services (Docker), prod su Neon.
- **Auth**: JWT RSA (smallrye-jwt) + bcrypt.

## Dev / build
- Backend dev: porta **8085** (8080/8081 occupate da stack `pio/*`). Frontend dev: 5173. CORS già configurato per 5173.
- `cd backend && ./mvnw quarkus:dev` · `cd frontend && npm run dev`.
- In prod il build statico del frontend è servito dallo stesso origin Quarkus (single deploy: Render + Neon). Render dorme dopo 15 min → cold start.
- ⚠️ Su questa macchina Windows **non c'è JDK né Node**: build/test si fanno altrove.

## Architettura giochi (il pattern ricorrente)
Ogni gioco = bean `@ApplicationScoped implements GameEngine` in `backend/.../game/<slug>/`, **auto-scoperto** dal registry `GameEngines` (nessuna registrazione manuale lato BE).
- `GameEngine`: `slug()`, `maxPlayers()`, `onMessage(ctx,type,payload)`, opz. `onJoin(ctx)`.
- Logica = classe **State pura** (no framework, testabile con JUnit puro) in `room.game`.
- `RoomSocket` è generico: gestisce `hello`/`chat`, delega il resto all'engine via `GameContext` (`replyToSender`/`broadcast`/`sendTo`). **Aggiungere un gioco non lo tocca.**
- Protocollo: snapshot completi `game:state` (ultimo autoritativo) + `game:over`; info nascosta via `ctx.sendTo`. **Server sempre autorità** (valida ogni mossa).
- Config pre-partita generica via `Room.options` (JsonNode), letta dall'engine in `game:start`. Scelta dal componente riutilizzabile `frontend/src/lib/games/GameOptions.svelte`.
- Fine partita: `leaderboard.record(player, slug, "WIN"|"LOSE"|"DRAW")`.

Frontend: board in `frontend/src/lib/games/<X>Board.svelte`, registrato in `registry.ts` (`BOARDS`), props `BoardProps {send, event, me}`; reagisce agli eventi `game:state`/`game:over`. Lista giochi condivisa in `lib/games/catalog.ts`.

### Aggiungere un gioco
1. BE: `State` puro + `Engine` (slug/maxPlayers/onMessage/onJoin) + test dello State.
2. FE: `<Gioco>Board.svelte` → riga in `registry.ts` e in `catalog.ts`.
3. Label in `leaderboard/+page.svelte` e `profile/+page.svelte`.
4. Se ha opzioni: gestirle in `GameOptions.svelte` + parsing in `game:start`.

## Persistenza & schema
- **In-memory** (perso al restart/cold-start): stanze e stato partita (`room.game`).
- **Persistite** (PanacheEntity): `AppUser`, `MatchResult`, `DailyAttempt`, `DailyWordState`, `Invitation`, `BugReport`.
- Schema: dev `drop-and-create` (+ `import-dev.sql`); prod `update`. ⚠️ `update` **crea** tabelle mancanti in modo affidabile ma **l'ALTER ADD di colonne è inaffidabile**: per tabelle disallineate fare DROP + restart (i CREATE le rigenerano).

## Auth / ruoli
- JWT: `subject`=`upn`=**username** (minuscolo, chiave d'identità: leaderboard/JWT/players). `groups`=role.
- `@Authenticated` / `@RolesAllowed("admin")`. `AuthResponse {token,username,displayName,role}`; la `Session` FE memorizza `role`.
- **Admin**: role `admin`, account seedato all'avvio (`DataInitializer`): username `admin`, password `gianlucaGM`.
- Registrazione ristretta ai domini `@digitaliasistemi.it` / `@ascesa.it` (login no). Lo **username non si cambia**, il **displayName sì**.
- Presenza online: heartbeat in-memory `POST /api/presence/ping` (TTL 60s), chiamato dal polling 15s in `+layout.svelte`.

## Convenzioni
- UI e commenti in **italiano**.
- ⚠️ `backend/src/main/resources/privateKey.pem` è nel repo → tenere il repo **privato**.

## Stato / roadmap
Giochi: forza4, impiccato (+ parola del giorno), quiz, battaglia navale, campo minato, tris, dama, scacchi. Mancano: **briscola, scopa**. Vedi anche la memoria di sessione per il backlog dettagliato.
