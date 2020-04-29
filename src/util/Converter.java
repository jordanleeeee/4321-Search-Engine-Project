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

}
