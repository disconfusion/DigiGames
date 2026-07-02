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

- [ ] Restyling animazioni dei pezzi di scacchi e dama
- [x] Esplosioni e buchi nell'acqua in Battaglia navale
- [x] Campo minato
- [x] Forza 4

# Game Loop / Game design

- [x] Alpha test: sistema di crediti (Token) con acquisto di "poteri" usabili in partita
- [ ] Alpha test: pack opening di carte a tema inside joke interni
- [ ] Accessori avatar comprabili con i crediti — invio doni fra utenti
- [x] Companion acquistabili dallo shop (animati a mano)
- [x] Leaderboard coi punti accumulati al posto della colonna vittorie (che passa in seconda posizione)
- [x] Alpha test: clan/casate (temporaneamente le 4 di Hogwarts), badge casata personalizzabile nella sezione personale
- [x] Impiccato: tentativo dell'intera parola in qualsiasi momento (giusto = vittoria, sbagliato = eliminazione diretta) — sblocca gli stalli in cui non si possono più chiamare lettere/vocali

## Altre fix

- [x] Modale "Ultime Fix" editabile dall'Admin
- [ ] Regole dell'Impiccato del giorno personalizzabili dall'Admin
- [ ] Callout con messaggi dall'Admin nella sezione "Impiccato del giorno" per giornate speciali / info utili

# Tecnico / Infra

- [ ] Efficientare storage e cancellazione dati dal DB: Neon offre solo ~0,5 GB, quindi strutturare il progetto attorno a questo limite (retention e purge dei dati storici)

# Bug Fixing

Segnalazioni utenti.

- [x] Se hosti una partita e poi esci o finisci di giocare, la stanza rimane aperta e non si può chiudere (@emanuele.taglia) — risolto: uscita esplicita ("Esci") libera lo slot anche a partita in corso e distrugge la stanza se vuota; l'host può chiuderla per tutti ("Chiudi stanza")
- [ ] Quiz: gestire la casistica di pareggio (@manuchao)
- [x] Impiccato: se una parola contiene più di due vocali non può essere completata (@manuchao) — risolto: ora si può tentare l'intera parola in qualsiasi momento
- [ ] Togliere le emoji dalla personalizzazione account, o sostituirle con nuove animazioni e sprite (@spacevampire)
