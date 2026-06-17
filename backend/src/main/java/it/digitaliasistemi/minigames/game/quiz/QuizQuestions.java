package it.digitaliasistemi.minigames.game.quiz;

import java.util.List;

/** Banca domande per il gioco Quiz. Mix di cultura generale e programmazione web. */
public class QuizQuestions {

    public record Question(String text, String category, List<String> options, int correctIndex) {}

    public static final List<Question> ALL = List.of(

        // --- Cultura generale ---
        new Question(
            "Qual e' la capitale della Francia?",
            "Cultura generale",
            List.of("Berlino", "Madrid", "Parigi", "Roma"),
            2
        ),
        new Question(
            "Quanti pianeti compongono il Sistema Solare?",
            "Cultura generale",
            List.of("7", "8", "9", "10"),
            1
        ),
        new Question(
            "Chi ha dipinto la Gioconda?",
            "Cultura generale",
            List.of("Michelangelo", "Raffaello", "Leonardo da Vinci", "Caravaggio"),
            2
        ),
        new Question(
            "In quale anno e' iniziata la Prima Guerra Mondiale?",
            "Cultura generale",
            List.of("1910", "1912", "1914", "1916"),
            2
        ),
        new Question(
            "Qual e' il fiume piu' lungo del mondo?",
            "Cultura generale",
            List.of("Nilo", "Rio delle Amazzoni", "Mississippi", "Yangzi"),
            0
        ),
        new Question(
            "Qual e' il simbolo chimico dell'oro?",
            "Cultura generale",
            List.of("Go", "Or", "Au", "Ag"),
            2
        ),
        new Question(
            "Quante ossa ha il corpo umano adulto?",
            "Cultura generale",
            List.of("186", "206", "226", "256"),
            1
        ),
        new Question(
            "Chi ha scritto la Divina Commedia?",
            "Cultura generale",
            List.of("Francesco Petrarca", "Giovanni Boccaccio", "Dante Alighieri", "Torquato Tasso"),
            2
        ),
        new Question(
            "Qual e' la montagna piu' alta della Terra?",
            "Cultura generale",
            List.of("K2", "Kangchenjunga", "Monte Bianco", "Everest"),
            3
        ),

        // --- Programmazione web ---
        new Question(
            "Cosa significa HTML?",
            "Programmazione web",
            List.of("Hyper Text Markup Language", "High Text Machine Language", "Hyper Transfer Markup Link", "Hyper Text Modern Layout"),
            0
        ),
        new Question(
            "Quale proprieta' CSS si usa per cambiare il colore del testo?",
            "Programmazione web",
            List.of("font-color", "text-color", "color", "foreground"),
            2
        ),
        new Question(
            "Quale metodo HTTP si usa di solito per inviare dati a un server?",
            "Programmazione web",
            List.of("GET", "POST", "PUT", "DELETE"),
            1
        ),
        new Question(
            "Cosa restituisce typeof null in JavaScript?",
            "Programmazione web",
            List.of("\"null\"", "\"undefined\"", "\"object\"", "\"boolean\""),
            2
        ),
        new Question(
            "Quale codice di stato HTTP indica che una risorsa non e' stata trovata?",
            "Programmazione web",
            List.of("200", "301", "403", "404"),
            3
        ),
        new Question(
            "Cosa si intende per CSS Flexbox?",
            "Programmazione web",
            List.of("Un database per immagini", "Un modello di layout unidimensionale", "Un linguaggio di template", "Un protocollo di rete"),
            1
        ),
        new Question(
            "Quale attributo HTML associa un'etichetta a un campo di input?",
            "Programmazione web",
            List.of("name", "id", "for", "rel"),
            2
        ),
        new Question(
            "Quale di questi e' un esempio di selettore CSS di classe?",
            "Programmazione web",
            List.of("#titolo", ".titolo", "titolo", "*titolo"),
            1
        ),
        new Question(
            "Quale protocollo usa WebSocket per l'handshake iniziale?",
            "Programmazione web",
            List.of("FTP", "SMTP", "HTTP", "SSH"),
            2
        )
    );
}
