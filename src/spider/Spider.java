package spider;

import org.rocksdb.RocksDBException;

import java.net.URL;
import java.util.*;

public class Spider {
    private Indexer indexer ;
    private InvertedIndex invertedIndex;
    private PageProperty pageProperty;
    private String url;
    private int numOfPage;

    private Spider(String url, int numOfPage) throws RocksDBException {
        indexer = new Indexer();
        invertedIndex = new InvertedIndex();
        pageProperty = new PageProperty();
        this.url = url;
        this.numOfPage = numOfPage;
    }

    /**
     * do BFS
     * if a page is need to be fetch, fetch it and update pageProperty and indexer, and invertedIndex
     * fetch until the required number of page
     */
    private void fetch() {
        Set<String> fetchedPage = new HashSet<>();
        int pageFetched = 0;
        Queue<String> queue = new LinkedList<>();
        queue.add(url);
        while (!queue.isEmpty()) {
            String site = queue.remove();
            if (needFetched(site)) {
                if (pageFetched >= numOfPage) {
                    break;
                }
                if (fetchedPage.contains(site)) {
                    continue;
                }
                //ignore page that need login
                try{
                    var x = new URL(site).openStream();
                } catch (Exception e) { continue; }
                pageFetched++;
                System.out.println(pageFetched +" handling " + site);
                fetchedPage.add(site);
                //todo update
                pageProperty.store(pageFetched, site);
                //todo update indexer
                /* sth */
                //todo update inverted index
                /* sth */
                List<String> links = WebInfoSeeker.getChildLinks(site);
                for (String link : links) {
                    if (!fetchedPage.contains(link)) {
                        queue.add(link);
                    }
                }
            }
        }
    }

    /**
     * determine weather a page need to be fetched or not:
     * if not the local system: yes
     * if in the local system:
     *      if previous update time is less then 1 day before: no
     *      else: yes
     * @param url the url to the page
     * @return trun if needed
     */
    private boolean needFetched(String url) {
        // todo if site is not in the inverted Index, return false
        if (true) {
            return true;
        }
        // todo get page id from url
        int pageID = 0;
        String lastModify = pageProperty.getLastModificationTime(pageID);
        long diff = new Date().getTime() - new Date(Date.parse(lastModify)).getTime();
        return diff >= 86400000;  //1 day
    }

    private void indexing() {
        //todo
    }

    public static void main(String[] args) throws RocksDBException {
        Spider spider = new Spider("https://www.cse.ust.hk", 30);
        spider.fetch();
        System.out.println("page property contain");
        spider.pageProperty.printAll();
    }
}
