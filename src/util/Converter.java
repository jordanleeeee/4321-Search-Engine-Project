package util;

import java.io.*;
import java.util.*;

public class Converter {

    /**
     * e.g. convert  "apple boy cat"  to  ["apple", "boy", "cat"]
     * @param words a string contain words separate by a space
     * @return a array contain those words
     */
    public static String[] readSeparateWords(String words) {
        return words.split(" ");
    }

    /**
     * load all url in the file into a list
     * @param path path to the txt file
     * @return a queue contain all url
     */
    public static Queue<String> readRemainingQueue(String path) {
        Queue<String> queue = new LinkedList<>();
        File file = new File(path);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()){
                queue.add(sc.nextLine());
            }
        } catch (FileNotFoundException e) { e.printStackTrace(); }
        return queue;
    }

    /**
     * convert content to list of stem word and ignore stop word
     * can use as converting title or query
     * @param content content
     * @return list of stem word
     */
    public static List<String> phraseString(String content) {
        String[] titleWords = readSeparateWords(content);
        List<String> stemTitleWord = new ArrayList<>();
        for (String word : titleWords) {
            if (Word.isMeaningfulWord(word)) {
                stemTitleWord.add(Word.porterAlgorithm(word));
            }
        }
        return stemTitleWord;
    }
}
