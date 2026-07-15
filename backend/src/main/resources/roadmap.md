# FIX UI/UX

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

## Nuove animazioni

- [x] Restyling animazioni dei pezzi di scacchi e dama — pezzo che scivola dall'origine alla destinazione, dissolvenza dei pezzi catturati, pop di promozione (dama); diff client-side degli snapshot, rispetta prefers-reduced-motion
- [x] Esplosioni e buchi nell'acqua in Battaglia navale
- [x] Campo minato
- [x] Forza 4

# Game Loop / Game design

- [x] Alpha test: sistema di crediti (Token) con acquisto di "poteri" usabili in partita
- [ ] Alpha test: pack opening di carte a tema inside joke interni
- [x] Accessori avatar comprabili con i crediti — invio doni fra utenti
- [x] Companion acquistabili dallo shop (animati a mano)
- [x] Leaderboard coi punti accumulati al posto della colonna vittorie (che passa in seconda posizione)
- [x] Alpha test: clan/casate (temporaneamente le 4 di Hogwarts), badge casata personalizzabile nella sezione personale
- [x] Impiccato: tentativo dell'intera parola nel proprio turno (giusto = vittoria, sbagliato = eliminazione diretta) — sblocca gli stalli in cui non si possono più chiamare lettere/vocali

## Altre fix

- [x] Modale "Ultime Fix" editabile dall'Admin — ricompare sia quando l'Admin aggiorna il testo sia dopo ogni deploy (marker `revisione:build-id`), così gli utenti la rivedono al primo accesso post-deploy
- [x] Regole dell'Impiccato del giorno personalizzabili dall'Admin
- [x] Callout con messaggi dall'Admin nella sezione "Impiccato del giorno" per giornate speciali / info utili

# Tecnico / Infra

- [ ] Efficientare storage e cancellazione dati dal DB: Neon offre solo ~0,5 GB, quindi strutturare il progetto attorno a questo limite (retention e purge dei dati storici)

# Bug Fixing

Segnalazioni utenti.

- [x] Se hosti una partita e poi esci o finisci di giocare, la stanza rimane aperta e non si può chiudere (@emanuele.taglia) — risolto: uscita esplicita ("Esci") libera lo slot anche a partita in corso e distrugge la stanza se vuota; l'host può chiuderla per tutti ("Chiudi stanza"); inoltre le stanze abbandonate (host che chiude la scheda, o partita finita non chiusa) vengono ora rimosse automaticamente da un reaper dopo un breve periodo senza connessioni
- [x] Quiz: gestire la casistica di pareggio (@manuchao) — risolto: tutti i giocatori col punteggio massimo sono vincitori; se più di uno → DRAW (token pareggio), medaglie a pari merito nella classifica
- [x] Impiccato: se una parola contiene più di due vocali non può essere completata (@manuchao) — risolto: si può tentare l'intera parola nel proprio turno (sblocca lo stallo del limite vocali)
- [x] Togliere le emoji dalla personalizzazione account, o sostituirle con nuove animazioni e sprite (@spacevampire) — risolto: frecce selettore avatar e messaggi ✓/⚠ ora usano sprite pixel-art (`arrow_left`/`arrow_right`/`warning`/`check`) invece dei glifi Unicode
- [x] Impiccato: si poteva indovinare la parola durante il turno avversario (@spacevampire, @petrillimatteo) — risolto: il tentativo dell'intera parola è ora consentito solo nel proprio turno
- [x] Impiccato: usciva due volte di fila la stessa parola (@manuchao) — risolto: la nuova parola è sempre diversa dalla precedente nella stessa stanza
- [x] Stanze: alcuni utenti vedevano meno giocatori del reale, o lista/turno sballati dopo un refresh o entrando a metà partita (@spacevampire, @petrillimatteo) — risolto: al join/refresh il server invia lo snapshot completo dei membri della stanza
- [x] Indicatore di turno: mostra il nome visualizzato (displayName) invece dell'username in Impiccato e Campo minato (@manuchao, @petrillimatteo)
- [x] Tris: nuova modalità "sparizione" (@spacevampire) — ogni giocatore tiene al massimo 3 segni, il 4° piazzamento fa sparire il più vecchio dello stesso simbolo; niente pareggi
- [x] Companion: orbite più varie attorno all'avatar — velocità, verso, raggio e pulse diversi per companion (@spacevampire)
- [x] Modale non chiudibile da mobile quando troppo alta: straborda e non scrolla (@spacevampire) — risolto: componente Modale condiviso con altezza max 90dvh, corpo scrollabile, header/azioni sempre visibili, chiusura via Esc/backdrop e safe-area mobile
- [x] Impiccato: turni sballati / stallo dopo refresh, uscita o ingresso a metà partita (@spacevampire, @petrillimatteo) — risolto: roster dei turni ordinato (ordine d'ingresso), chi entra a partita in corso viene aggiunto in coda, chi esce viene tolto e il turno avanza da solo; il roster viaggia nello snapshot game:state
- [x] Impiccato: con il cappello spariva la corda dal disegno — risolto: la tesa del cappello non sovrascrive più la corda
- [x] Impiccato del giorno: se la parola è scelta dall'admin ora è segnalato nella pagina (@admin)
- [x] Sezione "Suggerisci feature/gioco" per proporre nuove funzioni o giochi (@admin) — inviabile da qualsiasi utente, visibile ed esportabile dall'Admin
