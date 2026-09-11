# In arrivo: DigiGames 2.0

Il **prossimo aggiornamento** sarà il più grande di sempre. Cosa bolle in pentola:

- **Rivoluzione della UI** — l'interfaccia sarà ripensata da zero, nuova veste grafica per tutta l'app
- **Carte collezionabili** — un sistema di carte a tema, con gli inside joke interni, da aprire e collezionare
- **Nuovi poteri** — altre abilità da comprare e usare in partita

Restate sintonizzati: la **2.0** è vicina.

---

## Novità e fix di questa versione

**Nuovo gioco: Poker (Texas Hold'em)** — tavolo da 2 a 6 posti, in formato Sit & Go: si gioca mano dopo mano, con i bui che raddoppiano ogni 8 mani, finché resta un solo stack in piedi. Chi crea la stanza decide **quanti posti sono per le persone e quanti per gli avversari IA** (tre livelli di bravura: facile, normale, tosta) e quante fiches ricevere all'inizio — così si può giocare fra colleghi, da soli contro il computer o in tavoli misti. I posti umani sono anche il tetto della stanza: oltre quel numero non entra più nessuno, nemmeno con il codice della stanza. Due monete separate e da non confondere: le **fiches** sono la moneta del tavolo, i **Token** sono quelli del sito. Se l'host imposta un **buyin in Token** maggiore di zero, all'avvio ogni umano lo versa e il montepremi (buyin × giocatori umani) va a chi vince il tavolo; l'IA gioca fiches della casa e non versa nulla, quindi se il tavolo lo vince lei il montepremi resta al banco. Se la partita non arriva alla fine — stanza chiusa, tutti disconnessi o riavvio del server — il buyin torna a tutti: nessuno perde Token per una partita che il sistema non ha concluso. Le carte coperte le vede solo il proprietario, i side pot degli all-in sono calcolati per livelli di puntata, il turno scade dopo 30 secondi (si bussa se è gratis, altrimenti si passa) e la cronaca del tavolo mostra mano per mano chi ha vinto e con quale punto.

**Nuovo gioco: Battle City** — i carrarmatini pixelati del NES, in due modalità che si scelgono creando la stanza. **Co-op**: fino a due colleghi insieme difendono l'aquila e abbattono venti tank nemici a ondate (quattro in campo alla volta, quattro tipi diversi: normale, veloce, cannone rapido e corazzato che regge quattro colpi), con tutti e sei i bonus dell'originale — stella (cannone potenziato, dal terzo livello sfonda l'acciaio), granata (spazza i nemici in campo), casco (scudo), pala (base murata in acciaio), tank (vita extra) e orologio (nemici congelati). **Duello 1v1**: niente nemici, solo voi due sulla mappa a muri distruttibili, primo a tre colpi. I mattoni si rompono a mezzo tile come nell'originale, l'acqua ferma i tank ma non i proiettili, i cespugli nascondono chi ci passa sotto e sul ghiaccio si slitta. Sei mappe disegnate a mano, poi livelli generati sempre più difficili: completando un livello si passa al successivo. Si gioca con frecce o WASD e spazio per sparare; su telefono ci sono croce direzionale e pulsante di fuoco.

**Battle City: la mappa la disegnate voi** — c'è anche il Construction Mode dell'originale, ma a quattro mani: nella stanza, prima di giocare, ognuno disegna la propria metà del campo e vede in tempo reale quella dell'altro. Si scelgono mattoni, acciaio, acqua, cespugli e ghiaccio; ci sono bozza casuale, specchia e svuota, e il tasto "sono pronto" (che decade se riprendi a disegnare). La colonna centrale resta sempre libera come corridoio e le righe di comparsa dei nemici e la zona dell'aquila non si possono murare: il server rifiuta le mappe da cui non si raggiunge la base. La mappa finita si può salvare con un nome nella libreria condivisa e rigiocare quando volete.

**Battle City: carri ridisegnati e cannone che punta dove guardi** — quando lasci i tasti il carro non gira più di scatto verso l'alto: resta rivolto nell'ultima direzione, cannone compreso, così spari dove stavi andando. I carri hanno anche una grafica nuova, più vicina all'originale NES: cingoli con le tacche dei rulli, scafo squadrato, torretta in rilievo e cannone che esce dalla torretta.

**Nuovo gioco: Pong 1v1** — il primo gioco in tempo reale della piattaforma. Il server simula la partita a 60 tick al secondo ed è l'unica autorità: dal browser parte solo la posizione della racchetta (mouse, dito o tasti W/S e frecce). In partita trovi un HUD con FPS del browser, tick del server, velocità della palla e colpi dello scambio, più il colore del monitor scelto da te (ognuno vede il suo) e i punti partita configurabili alla creazione della stanza.

**Nuovi companion** — Scarabeo Rinoceronte (con il colore della corazza scelto da te: verde sgargiante di serie, ma prende qualunque tinta), Lo Special (il panino alla salsiccia del paninaro Fabio, col sugo che "coce da 200 anni"), Castoro di DBeaver, Lancer e l'Elmo di Master Chief.

**Altri quattordici companion** — insetti ricolorabili (Ape Operaia, Vespa, Calabrone, Farfalla), la Macchinetta Frog dell'ufficio con la sua tanica mai lavata, Procione, Gatto, quattro cani (Chihuahua, Akita, Pastore Tedesco, Dalmata), Capybara, Ratto e il Pipistrello, che dalla tua Area personale puoi trasformare in vampiro con tanto di nuvola di fumo: la forma scelta resta anche nell'header e nelle stanze. La motosega del Lancer ora si vede come si deve.

**Ogni companion si muove a modo suo** — nell'Area personale la traiettoria attorno all'avatar dipende da chi ti accompagna: api, vespe, calabroni e scarabei volano a scatti in zig-zag, la farfalla planata su un'orbita ellittica, il pipistrello e la gondola a picchiate avanti e indietro, gli altri sul giro classico. Gatto, Ratto e Procione sono stati ridisegnati con il doppio del dettaglio (occhi, baffi, code animate). Chi sceglie un colore per un companion ricolorabile lo vede applicato; senza scelta resta il colore naturale (l'ape è gialla, la farfalla rosa, lo scarabeo verde).

**Parole vere per l'Impiccato** — la Parola del Giorno non arriva più dall'elenco fisso del sito: viene pescata ogni giorno da un dizionario italiano online, con una difficoltà estratta a caso per la giornata e mostrata in un badge colorato (verde facile, ambra media, rosso difficile). Accanto al badge c'è un'iconcina che spiega da dove arriva la parola. Nell'Impiccato personalizzato puoi scegliere se usare le parole del sito o il dizionario, in che lingua (italiano, inglese, spagnolo, francese, tedesco, portoghese, romeno) e con quale difficoltà: la prima parola richiede qualche secondo di attesa (con schermata di caricamento) e le successive vengono preparate in background mentre giocate, così partono subito. Se il dizionario non risponde si continua con le parole del sito, senza bloccare la partita.

**Token sempre reattivi** — il saldo in alto si aggiorna all'istante quando guadagni o spendi Token: vittorie, acquisti nello shop e regali arrivano in tempo reale invece di aspettare il prossimo controllo.

**Inviti gestibili dalla notifica** — quando ricevi un invito puoi accettarlo o rifiutarlo direttamente dal messaggio che compare, senza passare dalla pagina Inviti.

**Parola del Giorno** — la sezione "Come si gioca" è stata rivista e ora le regole sono aggiornabili dall'admin

**Impiccato multiplayer** — turni più solidi
- Ordine dei turni stabile e coerente con i posti mostrati a schermo
- Chi entra a partita già iniziata viene inserito nel giro e gioca dal turno successivo
- Se un giocatore esce, il turno passa avanti da solo: niente più partite bloccate
- Con il cappello non sparisce più la corda del disegno

**Modali** — su mobile non restano più intrappolate: se il contenuto è troppo alto scorre all'interno, con il tasto chiudi sempre raggiungibile (si chiudono anche con Esc o toccando fuori)

**Parola del Giorno** — quando la parola è scelta dall'admin ora viene segnalato nella pagina, e un banner callout mostra messaggi e info per le giornate speciali

**Suggerisci feature/gioco** — pulsante in alto per proporre idee di nuove funzioni o giochi

**Accessori avatar** — comprabili nello shop con i Token e indossati sul volto del tuo avatar (corona, cilindro, cuffie, occhiali, occhiali da sole, baffi). Sono combinabili, uno per zona (testa, occhi, bocca), e si vedono nel profilo e nelle stanze di gioco.

**Regala a un collega** — dalla tua Area personale puoi regalare Token, poteri, companion o accessori a un altro utente. Paghi tu con i tuoi Token; se il collega possiede già quel companion o accessorio non è regalabile.

Buon divertimento!

---
_Aggiornato: 11 settembre 2026 — nuovo gioco Poker Texas Hold'em (tavoli misti umani/IA, buyin in Token con montepremi e rimborso se la partita non finisce). Battle City: fix del cannone che tornava sempre verso l'alto e carri ridisegnati in stile NES. Nuovi giochi Battle City (co-op, duello e costruzione della mappa a due, sei mappe più livelli generati) e Pong 1v1 (realtime 60 Hz, HUD e colore monitor), 19 nuovi companion (insetti ricolorabili, animali, Macchinetta Frog, pipistrello trasformabile in vampiro), parole dell'Impiccato dal dizionario online con difficoltà e lingua a scelta, saldo Token reattivo, inviti accettabili dalla notifica._
