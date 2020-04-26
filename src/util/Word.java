package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class Word {
    private static Porter porter = new Porter();
    private static HashSet<String> stopWordsList = new HashSet<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader("stopwords.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopWordsList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String porterAlgorithm(String word) {
        return porter.stripAffixes(word);
    }

    private static boolean isStopWord(String word) {
        return stopWordsList.contains(word);
    }

    public static boolean isMeaningfulWord(String word) {
        return !isStopWord(word) && word.length() >= 2
                && word.matches("^[a-zA-Z]*$");
    }
}
