package spider;

import java.io.File;
import java.io.FileNotFoundException;
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
            result.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return String.valueOf(result);
    }

    /**
     * e.g. convert  "apple boy cat"  to  ["apple", "boy", "cat"]
     * @param words a string contain words separate by a space
     * @return a array contain those words
     */
    public static String[] readSeparateWords(String words) {
        return words.split(" ");
    }

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

    public static String porterAlgorithm(String word) {
        //todo
        return word;
    }
}
