package indexer;

import util.Converter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * the posting list of inverted index has the following format
 * pageID1:frequency:position1,position2,... pageID2:frequency2:position1,position2....
 */
public class PostingListHandler {
    /**
     * pageID -> frequency
     */
    private HashMap<Integer, Integer> frequencyRecord = new HashMap<>();
    /**
     * pageID -> {position}
     */
    private HashMap<Integer, LinkedList<Integer>> positionsRecord = new HashMap<>();

    PostingListHandler(String invertedIndex) {
        String[] details = Converter.readSeparateWords(invertedIndex);
        for (String detail : details) {
            if (detail.equals("")) {
                continue;
            }
            String[] content = detail.split(":");
            int pageID = Integer.parseInt(content[0]);
            frequencyRecord.put(pageID, Integer.valueOf(content[1]));
            positionsRecord.put(pageID, new LinkedList<>());
            String[] listOfPosition = content[2].split(",");
            for (String pos : listOfPosition) {
                positionsRecord.get(pageID).addLast(Integer.valueOf(pos));
            }
        }
    }

    /**
     * get relation: pageID -> frequency
     * @return a hashMap
     */
    HashMap<Integer, Integer> getFrequencyRecord() {
        return frequencyRecord;
    }

    /**
     * get relation: pageID -> {position}
     * @return a hashMap
     */
    HashMap<Integer, LinkedList<Integer>> getPositionsRecord() {
        return positionsRecord;
    }

    /**
     * remove record of a particular page in the posting list
     * @param pageID page ID
     */
    void removeRecord(int pageID) {
        frequencyRecord.remove(pageID);
        positionsRecord.remove(pageID);
    }

    /**
     * add a word
     * @param pageID page ID
     * @param pos position
     * @return current term frequency
     */
    int addWord(int pageID, int pos) {
        if (frequencyRecord.containsKey(pageID)) {
            frequencyRecord.replace(pageID, frequencyRecord.get(pageID) + 1);
            positionsRecord.get(pageID).addLast(pos);
        } else {
            frequencyRecord.put(pageID, 1);
            positionsRecord.put(pageID, new LinkedList<>());
            positionsRecord.get(pageID).addLast(pos);
        }
        return positionsRecord.get(pageID).size();
    }
    public String toString(){
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : frequencyRecord.entrySet()) {
            int pageID = entry.getKey();
            int frequency = entry.getValue();
            result.append(pageID);
            result.append(":");
            result.append(frequency);
            result.append(":");
            for (int pos : positionsRecord.get(pageID)) {
                result.append(pos);
                result.append(",");
            }
            result.deleteCharAt(result.length()-1);
            result.append(" ");
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length()-1);
        }
        return String.valueOf(result);
    }

    public static void main(String[] args) {
//        InvertedIndexHandler reader = new InvertedIndexHandler(20, 5);
//        System.out.println(reader.toString());
//        reader.addWord(20, 6);
//        System.out.println(reader.toString());
//        reader.addWord(25, 6);
//        System.out.println(reader.toString());
//        reader.addWord(25, 20);
//        System.out.println(reader.toString());
//        InvertedIndexHandler temp = new InvertedIndexHandler("20:2:5,6 25:2:6,20");
//        System.out.println(temp.toString());
//        temp.removeRecord(22);
//        System.out.println(temp.toString());
        PostingListHandler one = new PostingListHandler("");
        one.addWord(23,20);
        one.addWord(23,25);
        one.addWord(25,23);
        System.out.println(one);
    }
}
