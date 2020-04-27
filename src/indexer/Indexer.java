package indexer;

import org.rocksdb.*;

import com.google.common.collect.HashBiMap;

public class Indexer {
    enum IndexType {PageURLID, WordID, TitleID, ParentID}

    private static Indexer INSTANCE = new Indexer();
    private RocksDB pageURLIDdb, wordIDdb, titleIDdb, parentIDdb;
    private Integer wordCount = 0, titleCount = 0, URLCount = 0;
    private HashBiMap<Integer, String> pageIndexer = HashBiMap.create();
    private HashBiMap<Integer, String> wordIndexer = HashBiMap.create();
    private HashBiMap<Integer, String> titleIndexer = HashBiMap.create();

    public static Indexer getInstance() {
        return INSTANCE;
    }

    /**
     * open all the database and place the record in hashBiMap and update the counters
     */
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
        addTitleBiMap();
        addPageBiMap();
        addWordBiMap();
    }

    ///////Page////////

    private void addPageBiMap(){
        RocksIterator iter = pageURLIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            updatePageBiMap(Integer.parseInt(new String(iter.key())), new String(iter.value()));
            URLCount++;
        }
    }

    private void updatePageBiMap(int pageId, String url){
        pageIndexer.put(pageId, url);
    }

    private void addPage(String url) {
        try {
            URLCount += 1;
            pageURLIDdb.put(Integer.toString(URLCount).getBytes(), url.getBytes());
            updatePageBiMap(URLCount, url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add page");
        }
    }

    /**
     * true:
     *     get id from url, if no such id (a newly appeared link), make one for it
     * false:
     *      get id from url, if no such id, return -1
     * @param url link
     * @param addIfMissing boolean
     * @return page id, -1 if no such page
     */
    public Integer searchIdByURL(String url, boolean addIfMissing) {
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

    String searchURLById(int pageID) {
        return pageIndexer.getOrDefault(pageID, null);
    }

      ///////Title////////
     public void storeTitle(String title) {
        try {
            titleCount += 1;
            titleIDdb.put(Integer.toString(titleCount).getBytes(), title.getBytes());
            updateTitleBiMap(titleCount, title);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add title");
        }
    }

    private void addTitleBiMap(){
        RocksIterator iter = titleIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            updateTitleBiMap(Integer.parseInt(new String(iter.key())), new String((iter.value())));
            titleCount++;
        }
    }

    private void updateTitleBiMap(int titleId, String title){
        if (titleIndexer.containsValue(title)) {
            titleCount--;
        } else {
            titleIndexer.put(titleId, title);
        }
    }

    Integer searchIdByTitle(String title) {
        return titleIndexer.inverse().get(title);
    }

    String searchTitleById(int titleId) {
        return titleIndexer.get(titleId);
    }

    ///////Word////////
    private void addWord(String word) {
        try {
            wordCount += 1;
            wordIDdb.put(Integer.toString(wordCount).getBytes(), word.getBytes());
            updateWordBiMap(wordCount, word);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add word");
        }
    }

    private void addWordBiMap(){
        RocksIterator iter = wordIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            updateWordBiMap(Integer.parseInt(new String(iter.key())), new String((iter.value())));
            wordCount++;
        }
    }

    private void updateWordBiMap(int wordId, String word){
        wordIndexer.put(wordId, word);
    }

    public Integer searchIdByWord(String word) {
        if (!(wordIndexer.containsValue(word))) {
            addWord(word);
            return wordCount;
        } else return wordIndexer.inverse().get(word);
    }

    public String searchWordById(int wordId) {
        return wordIndexer.getOrDefault(wordId, null);
    }

    //////////others///////////////////
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

    public static void main(String[] args){
        Indexer indexer = getInstance();
        indexer.printAll(IndexType.WordID);
//        indexer.printAll(IndexType.ParentID);
    }
}