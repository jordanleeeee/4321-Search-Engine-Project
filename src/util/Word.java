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

    /**
     * convert a word to stem using porter algorithm
     * @param word a word
     * @return stem word
     */
    public static String porterAlgorithm(String word) {
        return porter.stripAffixes(word);
    }

    private static boolean isStopWord(String word) {
        return stopWordsList.contains(word);
    }

    /**
     * check if a word is meaningful
     * a word is meaningful is it is not a stop word and length >= 2 and only contain alphabet
     * @param word a word
     * @return true if is meaningful
     */
    public static boolean isMeaningfulWord(String word) {
        return !isStopWord(word) && word.length() >= 2
                && word.matches("^[a-zA-Z]*$");
    }
}
