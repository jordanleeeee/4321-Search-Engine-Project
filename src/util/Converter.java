package util;

import java.io.*;
import java.util.*;

public class Converter {

    /**
     * convert inverted index to hash map
     * convert "1:2 3:4 5:6"  to  1->2, 3->4, 5->6
     * @param recordList string representation of inverted index
     * @return hashMap where key is pageID, value is frequency
     */
    public static HashMap<Integer, Integer> readInvertedIndex(String recordList) {
        String[] records = readSeparateWords(recordList);
        HashMap<Integer, Integer> record = new HashMap<>();
        for (String oneRecord : records) {
            if (oneRecord.equals("")) {
                continue;
            }
            String[] temp = oneRecord.split(":");
            record.put(Integer.valueOf(temp[0]), Integer.valueOf(temp[1]));
        }
        return record;
    }
    /**
     * convert hash map to inverted index
     * convert 1->2, 3->4, 5->6  to  "1:2 3:4 5:6"
     * @param map the hash map represent the inverted index
     * @return inverted index
     */
    public static String generateInvertedIndex(HashMap<Integer, Integer> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            result.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        String detail = String.valueOf(result);
        if (detail.equals("")) {
            return detail;
        }
        return detail.substring(0, detail.length()-1);
    }

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
