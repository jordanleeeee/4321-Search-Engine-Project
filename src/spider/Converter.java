package spider;

import java.util.HashMap;
import java.util.Map;

public class Converter {
    // page id, freq

    /**
     * convert inverted index to hash map
     * "1:2 3:4 5:6"  to  1->2, 3->4, 5->6
     * @param recordList
     * @return
     */
    static HashMap<Integer, Integer> readInvertedIndex(String recordList) {
        String[] records = readSeparateWords(recordList);
        HashMap<Integer, Integer> record = new HashMap<>();
        for (String oneRecord : records) {
            String[] temp = oneRecord.split(":");
            record.put(Integer.valueOf(temp[0]), Integer.valueOf(temp[1]));
        }
        return record;
    }

    static String generateInvertedIndex(HashMap<Integer, Integer> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            result.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return String.valueOf(result);
    }

    /**
     * e.g. convert  "apple boy cat" -> ["apple", "boy", "cat"]
     * @param words a string contain words separate by a space
     * @return a array contain those words
     */
    static String[] readSeparateWords(String words) {
        return words.split(" ");
    }
}
