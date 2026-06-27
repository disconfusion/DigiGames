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
- ⚠️ Il **push su branch `1.0`** fa partire la **build Docker completa** (FE `npm run build` + BE `mvn package`): un errore di compilazione (Java o Svelte) **rompe il deploy**. Rileggere bene il codice nuovo prima di pushare.

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

## Meta-sistemi (Token, shop, companion, casate, icone)
- **Token**: valuta interna su `AppUser.tokens` (`TokenService`); guadagnata giocando (vittoria 10 · pareggio 5 · Impiccato del giorno 50) e spesa nello shop.
- **Shop** (`shop/`): poteri di gioco (`PowerCatalog` statico · inventario `OwnedPower` · prezzi override admin `PowerPrice`) e **companion** cosmetici (`CompanionCatalog` · possesso/equip `OwnedCompanion`). La Battaglia navale usa i poteri (cyberdeck/HUD).
- **Casate/clan** (`house/`): 4 case stile Hogwarts (`HouseCatalog` · scelta in `UserHouse`); classifica casate in `/leaderboard` (somma punti membri).
- **Icone**: niente emoji nella UI → icone **pixel-art** animate in `frontend/src/lib/icons` (`<Icon name="..." />`; sprite in `sprites.ts`, colori dai token del tema, rispetta `prefers-reduced-motion`). Companion/casate/poteri usano lo stesso motore (id = nome sprite). **Usare `<Icon>`, non emoji.**
- **Modale "Ultime Fix/Novità"**: entità `Announcement` (DB, editabile da Admin in `/admin`), mostrata in home dopo il login finché l'utente non ha visto l'ultima `revision`.
- Identità extra esposte su `/api/me` e `/api/users`: `companion` e `house` (così header/stanze/classifica mostrano gli stemmi).

## Persistenza & schema
- **In-memory** (perso al restart/cold-start): stanze e stato partita (`room.game`).
- **Persistite** (PanacheEntity): `AppUser`, `MatchResult`, `DailyAttempt`, `DailyWordState`, `Invitation`, `BugReport`, `Roadmap`, `Announcement`, `PowerPrice`, `OwnedPower`, `OwnedCompanion`, `UserHouse`. ⚠️ Le identità "extra" (companion/casata) stanno in **tabelle separate** (`OwnedCompanion`, `UserHouse`) apposta per evitare l'ALTER ADD su `app_user`.
- Schema: dev `drop-and-create` (+ `import-dev.sql`); prod `update`. ⚠️ `update` **crea** tabelle mancanti in modo affidabile ma **l'ALTER ADD di colonne è inaffidabile**: per tabelle disallineate fare DROP + restart (i CREATE le rigenerano).

## Auth / ruoli
- JWT: `subject`=`upn`=**username** (minuscolo, chiave d'identità: leaderboard/JWT/players). `groups`=role.
- `@Authenticated` / `@RolesAllowed("admin")`. `AuthResponse {token,username,displayName,role}`; la `Session` FE memorizza `role`.
- **Admin**: role `admin`, account seedato all'avvio (`DataInitializer`): username `admin`, password `gianlucaGM`.
- Presenza online: heartbeat in-memory `POST /api/presence/ping` (TTL 60s), chiamato dal polling 15s in `+layout.svelte`.

## Convenzioni
- ⚠️ `backend/src/main/resources/privateKey.pem` è nel repo → tenere il repo **privato**.

⚠️ **Roadmap = `backend/src/main/resources/roadmap.md` (fonte di verità).** È mostrata nel sito alla pagina `/roadmap`: `DataInitializer` riallinea la tabella `roadmap` al file a ogni avvio (anche cold-start Render), e `/api/roadmap` la serve al frontend. **Ogni volta che si marca un punto come fatto (`[ ]` → `[x]`) o si modifica la roadmap, aggiornare SEMPRE `roadmap.md` nel repo**, così il default del sito resta sincronizzato col codice.
