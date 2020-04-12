package spider;

import org.rocksdb.*;

import java.util.*;
import com.google.common.collect.HashBiMap;

enum IndexType {PageURLID, WordID, TitleID, ParentID}
public class Indexer {
    private static Indexer INSTANCE = new Indexer();
    private Set<String> fetchedPage = new HashSet<>();
    private RocksDB pageURLIDdb, wordIDdb, titleIDdb, parentIDdb;
    private Integer wordCount = 0, titleCount = 0, URLCount = 0;
    HashBiMap<Integer, String> pageIndexer = HashBiMap.create();
    HashBiMap<Integer, String> wordIndexer = HashBiMap.create();
    HashBiMap<Integer, String> titleIndexer = HashBiMap.create();

    public static Indexer getInstance() {
        return INSTANCE;
    }

    private Indexer(){
        Options options = new Options();
        options.setCreateIfMissing(true);
        //todo
        try {
            pageURLIDdb = RocksDB.open(options, "/Java/Spider/pageURLIDdb");
            wordIDdb = RocksDB.open(options, "/Java/Spider/wordIDdb");
            titleIDdb = RocksDB.open(options, "/Java/Spider/titleIDdb");
            //  parentIDdb = RocksDB.open(options, "/Java/Spider/parentIDdb");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private void addPage(String url) {
        //todo
        //if page not in database, add it
        try {
            pageURLIDdb.put(Integer.toString(URLCount).getBytes(), url.getBytes());
            updatePageBiMap(URLCount, url);
            URLCount += 1;
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add page");
        }
    }

    private void addPageBiMap(int pageId, String url){
        RocksIterator iter = pageURLIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            pageIndexer.put(pageId, url);
        }
    }

    private void updatePageBiMap(int pageId, String url){
        pageIndexer.put(pageId, url);
    }

    Integer searchIdByURL(String url, boolean addIfMissing) {
        if (!(pageIndexer.containsValue(url))){
            if (addIfMissing) {
                addPage(url);
                return URLCount;
            }
            else {
                return -1;
            }
        }
        else return pageIndexer.inverse().get(url);
    }

    private void addTitle(String title) {
        //todo
        //if title not in database, add it
        try {
            if(searchIdByTitle(title) == titleCount) {
                titleIDdb.put(Integer.toString(titleCount).getBytes(), title.getBytes());
                updateTitleBiMap(titleCount, title);
                titleCount += 1;
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add title");
        }
    }

    private void addTitleBiMap(int titleID, String title){
        RocksIterator iter = titleIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            titleIndexer.put(titleID, title);
        }
    }

    private void updateTitleBiMap(int titleId, String title){
        titleIndexer.put(titleId, title);
    }

    Integer searchIdByTitle(String title) {
        if (!(titleIndexer.containsValue(title)))
            return titleCount;
        else return titleIndexer.inverse().get(title);
    }

    String searchTitleById(int titleId) {
        if (!(pageIndexer.containsKey(titleId)))
            return null;
        else return (String) titleIndexer.get(titleId);
    }

    private void addWord(String word) {
        //todo
        //if word not in database, add it
        try {
            wordIDdb.put(Integer.toString(wordCount).getBytes(), word.getBytes());
            updateWordBiMap(wordCount, word);
            wordCount += 1;
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add word");
        }
    }

    private void updateWordBiMap(int wordId, String word){
        wordIndexer.put(wordId, word);
    }

    Integer searchIdByWord(String word) {
        if (!(wordIndexer.containsValue(word))){
            addWord(word);
            return wordCount;
        }
        else return wordIndexer.inverse().get(word);
    }

    String searchWordById(int wordId) {
        if (!(wordIndexer.containsKey(wordId)))
            return null;
        else return (String) wordIndexer.get(wordId);
    }

    public void store(int id, String url){
        spider.WebInfoSeeker seeker = new spider.WebInfoSeeker(url);
        try {
            pageURLIDdb.put(Integer.toString(id).getBytes(), url.getBytes());
            titleIDdb.put(Integer.toString(id).getBytes(), seeker.getTitle().getBytes());
        } catch (RocksDBException e) {
            System.out.println("Indexer store error");
            e.printStackTrace();
        }
    }


    private void printAll(IndexType situation) {
        if (situation == IndexType.PageURLID) {
            RocksIterator iter = pageURLIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ID: " + new String(iter.key()) + '\n' + new String(iter.value()) + "\n");
            }
        }

        if (situation == IndexType.WordID) {
            RocksIterator iter = wordIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("Word ID: " + new String(iter.key()) + '\n' +
                        "Word: " + new String(iter.value()) + "\n");
            }
        }

        if (situation == IndexType.TitleID) {
            RocksIterator iter = titleIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("Title ID: " + new String(iter.key()) + '\n' +
                        "Title: " + new String(iter.value()) + "\n");
            }
        }

        if (situation == IndexType.ParentID) {
            RocksIterator iter = parentIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(iter.value()) + "\n");
            }
        }
    }

    public static void main(String[] args) throws RocksDBException {
        Indexer indexer = new Indexer();
        indexer.store(0, "https://www.cse.ust.hk");
        indexer.printAll(IndexType.PageURLID);
        indexer.printAll(IndexType.WordID);
        indexer.printAll(IndexType.TitleID);
        indexer.printAll(IndexType.ParentID);
    }
}