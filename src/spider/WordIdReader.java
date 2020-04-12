package spider;

import java.util.HashMap;

public class WordIdReader {
    // page id, freq
    static HashMap<Integer, Integer> converter(String recordList) {
        String[] records = readSeparateWords(recordList);
        HashMap<Integer, Integer> record = new HashMap<>();
        for (String oneRecord : records) {
            String[] temp = oneRecord.split(":");
            record.put(Integer.valueOf(temp[0]), Integer.valueOf(temp[1]));
        }
        return record;
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
