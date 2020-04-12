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

    String getKeyWords(int pageID) {
        try {
            return new String(pageIDdb.get(String.valueOf(pageID).getBytes()));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    String getChildPage(int parentID) {
        try {
            StringBuilder result = new StringBuilder();
            String listOfChild = new String(parentIDdb.get(String.valueOf(parentID).getBytes()));
            String[] childIDs = WordIdReader.readSeparateWords(listOfChild);
            for (String childID : childIDs) {
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

    private InvertedIndex(){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            pageIDdb = RocksDB.open(options, "/Java/Spider/x");
            parentIDdb = RocksDB.open(options, "/Java/Spider/y");
            wordIdDb = RocksDB.open(options, "/Java/Spider/z");
        } catch (RocksDBException e) {
            e.printStackTrace();
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
            // System.out.println(word +": "+freq);
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
            storeWordFreq(id, seeker.getKeywords());
            //////////////////// Page-ID -> {keywords}////////////////////////
            LinkedHashSet<String> uniqueKeyWords = new LinkedHashSet<>(keyWords);
            keyWords.clear();
            keyWords.addAll(uniqueKeyWords);
            byte[] content = null;
            for (String keyWord : keyWords) {
                //System.out.print(seeker.getKeywords().get(i) + " ");
                if (content == null) {
                    content = keyWord.getBytes();
                } else {
                    content = (new String(content) + " " + keyWord).getBytes();
                }
            }
            assert content != null;
            pageIDdb.put(Integer.toString(id).getBytes(), content);
            //////////////////parent ID -> {child ID}//////////////////////////

            List<String> child = seeker.getChildLinks();
            Set<String> childPage = new HashSet<>(child);

            content = null;
            for (String link : childPage) {
                if (link.equals(url)) {
                    continue;
                }
                //todo get ID with given url
                int childId = Indexer.getInstance().searchIdByURL(link, true);
                if (content == null) {
                    content = Integer.toString(childId).getBytes();
                } else {
                    content = (new String(content) + " " + childId).getBytes();
                }
            }
            assert content != null;
            parentIDdb.put(Integer.toString(id).getBytes(), content);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    void printAll(Type situation){
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

    public static void main(String[] args) throws RocksDBException {
        InvertedIndex invertedIndex = new InvertedIndex();
        // 1 for word ID -> {pageID, Freq}, 2 for Page-ID -> {keywords}, 3 for Parent-ID ->{ChildID}
        invertedIndex.store(0, "https://www.cse.ust.hk");
        invertedIndex.printAll(Type.PageID);
        invertedIndex.printAll(Type.ParentID);
    }
}
