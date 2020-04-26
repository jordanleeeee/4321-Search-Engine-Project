package spider;

import org.rocksdb.*;

import java.util.*;
/*
inverted index
word ID -> {pageID, Freq}
Forward index
Page-ID -> {keywords}
Link-based index
Parent-ID -> {ChildID}
*/
enum Type {WordID, PageID, ParentID}

public class InvertedIndex {
    private static InvertedIndex INSTANCE = new InvertedIndex();
    private RocksDB pageIDdb, parentIDdb, wordIdDb;

    static InvertedIndex getInstance() {
        return INSTANCE;
    }

    private InvertedIndex(){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            pageIDdb = RocksDB.open(options, "/Java/Spider/pageIDdb");
            parentIDdb = RocksDB.open(options, "/Java/Spider/parentIDdb");
            wordIdDb = RocksDB.open(options, "/Java/Spider/wordFreqdb");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    int getFreqOfWordInParticularPage(String word, int pageID) {
        try {
            int wordID = Indexer.getInstance().searchIdByWord(word);
            String record = new String(wordIdDb.get(String.valueOf(wordID).getBytes()));
            HashMap<Integer, Integer> recordDetails = Converter.readInvertedIndex(record);
            return recordDetails.get(pageID);
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    String[] getKeyWords(int pageID) {
        try {
            String words = new String(pageIDdb.get(String.valueOf(pageID).getBytes()));
            return Converter.readSeparateWords(words);
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * is not work... will make bugs
     * @param pageID page id
     */
    void clearRecord(int pageID) {
        String[] words = getKeyWords(pageID);
        for (String word : words) {
            try {
                if (word.equals("")) {
                    continue;
                }
                int wordId = Indexer.getInstance().searchIdByWord(word);
                byte[] record = wordIdDb.get(String.valueOf(wordId).getBytes());
                HashMap<Integer, Integer> map = Converter.readInvertedIndex(new String(record));
                map.remove(pageID);
                String newRecord = Converter.generateInvertedIndex(map);
                wordIdDb.put(String.valueOf(wordId).getBytes(), newRecord.getBytes());
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get all children page by page id
     * @param pageId page id
     * @return list containing all the children link
     */
    List<String> getAllChildPage(int pageId) {
        try {
            List<String> pages = new LinkedList<>();
            String listOfChild = new String(parentIDdb.get(String.valueOf(pageId).getBytes()));
            String[] childIDs = Converter.readSeparateWords(listOfChild);

            for (String childID : childIDs) {
                if (childID.equals("")) {
                    continue;
                }
                String l = Indexer.getInstance().searchURLById(Integer.parseInt(childID));
                if (l == null) { throw new IllegalStateException(); }
                pages.add(l);
            }
            return pages;
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get a string contain child page of a page by pageId
     * (only page that in the database will count), separate by \n
     * @param pageId page of
     * @return the string
     */
    String getChildPage(int pageId) {
        try {
            StringBuilder result = new StringBuilder();
            String listOfChild = new String(parentIDdb.get(String.valueOf(pageId).getBytes()));
            String[] childIDs = Converter.readSeparateWords(listOfChild);

            for (String childID : childIDs) {
                if (childID.equals("")) {
                    continue;
                }
                String l = PageProperty.getInstance().getUrl(Integer.parseInt(childID));
                if (l == null) {
                    continue;
                }
                result.append(l).append("\n");
            }
            return String.valueOf(result);
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void storeWordFreq(int pageID, Vector<String> keywords) {
        Map<String, Integer> wordFreqTable = new HashMap<>();
        for (String word : keywords) {
            Integer freq = wordFreqTable.get(word);
            if (freq == null) {
                wordFreqTable.put(word, 1);
            } else {
                wordFreqTable.replace(word, freq+1);
            }
        }
        for (Map.Entry<String, Integer> entry : wordFreqTable.entrySet()) {
            String word = entry.getKey();
            Integer freq = entry.getValue();
            Integer wordID = Indexer.getInstance().searchIdByWord(word);
            byte[] content = null;
            try {
                content = wordIdDb.get(String.valueOf(wordID).getBytes());
                if (content == null) {
                    content = (pageID + ":" + freq).getBytes();
                } else {
                    content = (new String(content) + " " + pageID + ":" + freq).getBytes();
                }
                wordIdDb.put(String.valueOf(wordID).getBytes(), content);
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    void store(int id, String url){
        try {
            WebInfoSeeker seeker = new WebInfoSeeker(url);
            Vector<String> keyWords = seeker.getKeywords();
            ///////////////////wordID -> {pageID freq}/////////////////////////
            //todo do not support update
            storeWordFreq(id, keyWords);
            //////////////////// Page-ID -> {keywords}////////////////////////
            LinkedHashSet<String> uniqueKeyWords = new LinkedHashSet<>(keyWords);
            keyWords.clear();
            keyWords.addAll(uniqueKeyWords);
            byte[] content = null;
            for (String keyWord : keyWords) {
                if (content == null) {
                    content = keyWord.getBytes();
                } else {
                    content = (new String(content) + " " + keyWord).getBytes();
                }
            }
            if (content == null) {             // if the web no not have any word (avoid crash the program)
                content = "".getBytes();
            }
            pageIDdb.put(Integer.toString(id).getBytes(), content);
            //////////////////parent ID -> {child ID}//////////////////////////
            List<String> child = seeker.getChildLinks();
            Set<String> childPage = new HashSet<>(child);

            content = null;
            for (String link : childPage) {
                if (link.equals(url)) {
                    continue;
                }
                int childId = Indexer.getInstance().searchIdByURL(link, true);
                if (content == null) {
                    content = Integer.toString(childId).getBytes();
                } else {
                    content = (new String(content) + " " + childId).getBytes();
                }
            }
            if (content == null) {             // if the web no not have any child (avoid crash the program)
                content = "".getBytes();
            }
            parentIDdb.put(Integer.toString(id).getBytes(), content);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private void printAll(Type situation){
        //Page-ID -> {keywords}
        if(situation == Type.PageID) {
            RocksIterator iter = pageIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("page ID: " + new String(iter.key()) + '\n' + new String(iter.value()) + "\n");
            }
        }

        if(situation == Type.ParentID) {
            RocksIterator iter = parentIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(iter.value()) + "\n");
            }
        }

        if (situation == Type.WordID) {
            RocksIterator iter = wordIdDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("wordID: " + new String(iter.key()) + '\n' +
                        "appear at: " + new String(iter.value()) + "\n");
            }
        }
    }

    public static void main(String[] args){
        InvertedIndex invertedIndex = getInstance();
        invertedIndex.printAll(Type.WordID);
    }
}
