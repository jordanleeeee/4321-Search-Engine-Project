package indexer;

import org.rocksdb.*;
import util.Converter;
import spider.WebInfoSeeker;
import util.Word;

import java.math.BigDecimal;
import java.util.*;

/*
inverted index
word ID -> {pageID, fred, {occurrence}}
Forward index
Page-ID -> {ChildID}, {keywords}, {title words}, maxTF
*/

public class InvertedIndex {
    enum Type {Content, Child, Title, WordID, MaxTf}

    private static InvertedIndex INSTANCE = new InvertedIndex();
    private RocksDB pageDetailDb, wordIdDb;
    private List<ColumnFamilyHandle> handles = new Vector<>();

    public static InvertedIndex getInstance() {
        return INSTANCE;
    }

    private InvertedIndex(){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            wordIdDb = RocksDB.open(options, "/Java/Spider/wordFreqdb");
            List<ColumnFamilyDescriptor> colFamily = new Vector<>();
            colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
            colFamily.add(new ColumnFamilyDescriptor("bodyWords".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("titleWords".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("maxTF".getBytes()));
            DBOptions options2 = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            pageDetailDb = RocksDB.open(options2, "/Java/Spider/pageDetailDB", colFamily, handles);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    /**
     * get all page that contain a particular keyword
     * @param wordID wordID of that particular word
     * @return set of pageID
     */
    public Set<Integer> getRelatedPage(int wordID) {
        HashMap<Integer, Integer> map = getPostingList(wordID);
        assert map != null;
        return map.keySet();
    }

    public LinkedList<Integer> getWordPositionsInPage(int wordID, int pageID) {
        try {
            byte[] record = wordIdDb.get(String.valueOf(wordID).getBytes());
            HashMap<Integer, LinkedList<Integer>> pos = new PostingListHandler(new String(record)).getPositionsRecord();
            return pos.get(pageID);
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * the stem word from a title of a page
     * @param pageID page ID of a page
     * @return a hashSet of those words
     */
    public Set<String> getTitleWords(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(2), String.valueOf(pageID).getBytes());
            String[] words = Converter.readSeparateWords(new String(content));
            return new HashSet<>(Arrays.asList(words));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get df(j), the program will crash if no such word appeared in the record
     * @param word a specific stemmed word
     * @return df(j)
     */
    private int getDocumentFrequency(String word) {
        try {
            int wordID = Indexer.getInstance().searchIDByWord(word, false);
            String record = new String(wordIdDb.get(String.valueOf(wordID).getBytes()));
            return Converter.readSeparateWords(record).length;
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * get tf(i, j), the program will crash if the pageID is invalID or no such word in that page
     * @param word a specific stemmed word
     * @param pageID page ID
     * @return tf(i, j)
     */
    public int getFreqOfWordInParticularPage(String word, int pageID) {
        int wordID = Indexer.getInstance().searchIDByWord(word, false);
        HashMap<Integer, Integer> recordDetails = getPostingList(wordID);
        assert recordDetails != null;
        return recordDetails.get(pageID);
    }

    /**
     * get idf(j), the program will crash if no such word appeared in the record
     * @param word a specific stemmed word
     * @return idf(j)
     */
    private double getIdf(String word) {
        double N = PageProperty.getInstance().getNumOfPageFetched();
        double df = getDocumentFrequency(word);
        return Math.log(N/df)/Math.log(2);
    }

    /**
     * get the term frequency of the most frequent term in document j
     * @return max Tf(j)
     */
    private int getMaxTf(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(3), String.valueOf(pageID).getBytes());
            return Integer.parseInt(new String(content));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * get term weight w(i,j)
     * the program will crash if the pageID is invalid or no such word in that page
     * @param word word i
     * @param pageID page j
     * @return w(i,j)
     */
    public double getTermWeight(String word, int pageID) {
        return ((double)getFreqOfWordInParticularPage(word, pageID) / getMaxTf(pageID)) * getIdf(word);
    }

    /**
     * get all unique keyword of a page
     * @param pageID pageID
     * @return an array of string contain those word
     */
    public String[] getKeyWords(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(1), String.valueOf(pageID).getBytes());
            return Converter.readSeparateWords(new String(content));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<Integer, Integer> getPostingList(int wordID) {
        try {
            byte[] record = wordIdDb.get(String.valueOf(wordID).getBytes());
            return new PostingListHandler(new String(record)).getFrequencyRecord();
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * clear record in inverted index which related to a page
     * @param pageID page ID
     */
    public void clearRecord(int pageID) {
        String[] words = getKeyWords(pageID);
        for (String word : words) {
            try {
                if (word.equals("")) {
                    continue;
                }
                int wordID = Indexer.getInstance().searchIDByWord(word,false);
                byte[] postingList = wordIdDb.get(String.valueOf(wordID).getBytes());
                PostingListHandler newRecord = new PostingListHandler(new String(postingList));
                newRecord.removeRecord(pageID);
                wordIdDb.put(String.valueOf(wordID).getBytes(), newRecord.toString().getBytes());
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get all children page by page ID
     * @param pageID page ID
     * @return list containing all the children link
     */
    public List<String> getAllChildPage(int pageID) {
        try {
            List<String> pages = new LinkedList<>();
            String listOfChild = new String(pageDetailDb.get(handles.get(0), String.valueOf(pageID).getBytes()));
            String[] childIDs = Converter.readSeparateWords(listOfChild);

            for (String childID : childIDs) {
                if (childID.equals("")) {
                    continue;
                }
                String l = Indexer.getInstance().searchURLByID(Integer.parseInt(childID));
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
     * get a string contain child page of a page by pageID
     * (only page that in the database will count), separate by \n
     * @param pageID page of
     * @return the string
     */
    public String getChildPages(int pageID) {
        try {
            StringBuilder result = new StringBuilder();
            String listOfChild = new String(pageDetailDb.get(handles.get(0), String.valueOf(pageID).getBytes()));
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
        int maxFreq = 0;
        for (int i = 0; i < keywords.size(); i++) {
            Integer wordID = Indexer.getInstance().searchIDByWord(keywords.get(i), true);
            byte[] content;
            try {
                content = wordIdDb.get(String.valueOf(wordID).getBytes());
                PostingListHandler temp;
                if (content == null) {
                    temp = new PostingListHandler("");
                } else {
                    temp = new PostingListHandler(new String(content));
                }
                int tf = temp.addWord(pageID, i);
                content = temp.toString().getBytes();
                wordIdDb.put(String.valueOf(wordID).getBytes(), content);
                if (tf > maxFreq) {
                    maxFreq = tf;
                }
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
        try {
            pageDetailDb.put(handles.get(3), String.valueOf(pageID).getBytes(), String.valueOf(maxFreq).getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void store(int pageID, String url){
        try {
            WebInfoSeeker seeker = new WebInfoSeeker(url);
            //separate title word and content word
            Vector<String> keyWords = seeker.getKeywords();
            List<String> titleWord = Word.phraseString(PageProperty.getInstance().getTitle(pageID));
            for (String s : titleWord) {
                try {
                    if (s.equals(keyWords.get(0))) {
                        keyWords.remove(0);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
            }
            ///////////////////wordID -> {pageID freq}, pageID ->{tf max}/////////////////////////
            storeWordFreq(pageID, keyWords);
            //////////////////// Page-ID -> {title Words}/////////////////////
            LinkedHashSet<String> uniqueKeyWords0 = new LinkedHashSet<>(titleWord);
            titleWord.clear();
            titleWord.addAll(uniqueKeyWords0);
            byte[] content = null;
            for (String word : titleWord) {
                if (content == null) {
                    content = word.getBytes();
                } else {
                    content = (new String(content) + " " + word).getBytes();
                }
            }
            if (content == null) {
                content = "".getBytes();
            }
            pageDetailDb.put(handles.get(2), Integer.toString(pageID).getBytes(), content);
            //////////////////// Page-ID -> {keywords}////////////////////////
            LinkedHashSet<String> uniqueKeyWords = new LinkedHashSet<>(keyWords);
            keyWords.clear();
            keyWords.addAll(uniqueKeyWords);
            content = null;
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
            pageDetailDb.put(handles.get(1), Integer.toString(pageID).getBytes(), content);
            //////////////////pageID -> {child ID}//////////////////////////
            List<String> child = seeker.getChildLinks();
            Set<String> childPage = new HashSet<>(child);

            content = null;
            for (String link : childPage) {
                if (link.equals(url)) {
                    continue;
                }
                int childID = Indexer.getInstance().searchIDByURL(link, true);
                if (content == null) {
                    content = Integer.toString(childID).getBytes();
                } else {
                    content = (new String(content) + " " + childID).getBytes();
                }
            }
            if (content == null) {             // if the web no not have any child (avoid crash the program)
                content = "".getBytes();
            }
            pageDetailDb.put(handles.get(0), Integer.toString(pageID).getBytes(), content);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private void printAll(Type situation) throws RocksDBException {
        //Page-ID -> {keywords}
        if(situation == Type.Content) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("page ID: " + new String(iter.key()) + '\n'
                        + new String(pageDetailDb.get(handles.get(1), iter.key())) + "\n");
            }
        }

        if(situation == Type.Child) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(pageDetailDb.get(handles.get(0), iter.key())) + "\n");
            }
        }

        if(situation == Type.Title) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(pageDetailDb.get(handles.get(2), iter.key())) + "\n");
            }
        }

        if(situation == Type.MaxTf) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("pageID: " + new String(iter.key()) + '\n' +
                        "MaxTf: " + new String(pageDetailDb.get(handles.get(3), iter.key())) + "\n");
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

    public static void main(String[] args) throws RocksDBException{
        InvertedIndex invertedIndex = getInstance();
        invertedIndex.clearRecord(7802);
//        System.out.println(PageProperty.getInstance().getNumOfPageFetched());
//        System.out.println(invertedIndex.getFreqOfWordInParticularPage("rainbow", 17));
//        System.out.println(invertedIndex.getDocumentFrequency("rainbow"));
//        System.out.println(invertedIndex.getIdf("rainbow"));
//        System.out.println(invertedIndex.getMaxTf(17));
//        System.out.println(invertedIndex.getTermWeight("rainbow",17));
//        System.out.println(invertedIndex.getRelatedPage(95));
//          invertedIndex.printAll(Type.WordID);
    }
}
