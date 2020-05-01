package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    /**
     * convert content to list of stem word and ignore stop word
     * can use as converting title or query
     * @param content content
     * @return list of stem word
     */
    public static List<String> phraseString(String content) {
        String[] titleWords = Converter.readSeparateWords(content);
        List<String> stemTitleWord = new ArrayList<>();
        for (String word : titleWords) {
            if (isMeaningfulWord(word)) {
                stemTitleWord.add(porterAlgorithm(word));
            }
        }
        return stemTitleWord;
    }

    public static void main(String[] args) {
        System.out.println(phraseString("News letters"));
    }
}
