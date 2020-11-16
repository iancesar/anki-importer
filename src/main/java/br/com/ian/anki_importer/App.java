package br.com.ian.anki_importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    private static final String ANKI_MEDIA_COLLECTION = "/home/ian/.local/share/Anki2/Ian/collection.media";

    private static final String ANKI_MEDIA_TO_IMPORT = "/home/ian/Documents/import-anki";

    private static final String ANKI_SENTENCES = "/home/ian/Documents/import-anki/sentences.txt";

    private static final String ANKI_SENTENCES_TO_IMPORT = "/home/ian/Documents/import-anki/sentences.csv";

    private App() {
    }

    public static void main(String[] args) throws IOException {
        createCsvFile(readSentences(), listMedia());
        copyMedia();
        LOGGER.info("Processo finalizado");
    }

    private static Map<String, String> readSentences() throws IOException {
        final List<String> allLines = Files.readAllLines(Path.of(ANKI_SENTENCES), StandardCharsets.UTF_8);
        Map<String, String> sentences = new LinkedHashMap<>();

        final Iterator<String> iterator = allLines.iterator();

        while (iterator.hasNext()) {
            String sentence = iterator.next();
            LOGGER.info("Sentence: " + sentence);

            String answer = iterator.next();
            LOGGER.info("Answer: " + answer);

            sentence = sentence.replaceAll("^[0-9]*.[.]", "").trim();

            sentences.put(sentence, answer);
        }

        return sentences;
    }

    private static Iterator<Path> listMedia() throws IOException {
        final Iterator<Path> media = Files.list(Path.of(ANKI_MEDIA_TO_IMPORT)).sorted().iterator();
        return fixMediaFileName(media);
    }

    private static Iterator<Path> fixMediaFileName(Iterator<Path> iterator) throws IOException {

        Pattern pattern = Pattern.compile("^[0-9][.]");
        iterator.forEachRemaining(path -> {
            Matcher matcher = pattern.matcher(path.toFile().getName());
            if (matcher.find()) {
                try {
                    Files.move(path, Path.of(path.getParent().toFile().getAbsolutePath(), "0" + path.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return Files.list(Path.of(ANKI_MEDIA_TO_IMPORT)).sorted().iterator();
    }

    private static void copyMedia() throws IOException {

        final Path from = Path.of(ANKI_MEDIA_TO_IMPORT);
        final Path to = Path.of(ANKI_MEDIA_COLLECTION);

        Files.list(from).filter(p -> p.toFile().getName().endsWith(".mp3")).forEach(p -> {
            try {
                Files.copy(p, Path.of(to.toFile().getAbsolutePath(), p.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.severe("Erro ao copiar as midias");
            }
        });
    }

    private static void createCsvFile(final Map<String, String> sentences, final Iterator<Path> listMedia) throws IOException {
        StringJoiner csv = new StringJoiner("\n");

        sentences.forEach((k, v) -> {
            csv.add(String.format("%s[sound:%s];%s", k, listMedia.next().toFile().getName(), v));
        });

        Files.writeString(Path.of(ANKI_SENTENCES_TO_IMPORT), csv.toString());
    }
}
