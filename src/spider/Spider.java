package spider;

import org.rocksdb.RocksDBException;

import java.net.URL;
import java.util.*;

public class Spider {
    private Indexer indexer;
    private InvertedIndex invertedIndex;
    private PageProperty pageProperty;
    private String url;

    Spider(String url){
        indexer = Indexer.getInstance();
        invertedIndex = InvertedIndex.getInstance();
        pageProperty = PageProperty.getInstance();
        this.url = url;
    }
    /**
     * do BFS
     * if a page is need to be fetch, fetch it and update pageProperty and indexer, and invertedIndex
     *  fetch until the required number of page
     * @param numOfPage required number of page to be fetch
     */
    private void BFS(int numOfPage){
        Set<String> discoveredPage = new HashSet<>();
        int pageFetched = 0;
        Queue<String> queue = new LinkedList<>();
        queue.add(url);
        while (!queue.isEmpty()) {
            String site = queue.remove();
            if (pageFetched >= numOfPage) {
                break;
            }
            if (discoveredPage.contains(site)) {
                continue;
            }
            discoveredPage.add(site);
            //todo update page property
            if(needFetched(site)) {
                pageFetched++;
                int id = indexer.searchIdByURL(site, true);
                System.out.println(id +" handling " + site);
                pageProperty.store(id, site);
                invertedIndex.store(id, site);
            }
            List<String> links = new WebInfoSeeker(site).getChildLinks();
            for (String link : links) {
                if (!queue.contains(link)) {
                    queue.add(link);
                }
            }
        }
    }

    /**
     * determine weather a page need to be fetched or not:
     * if the link is dead or need login access: no
     * if not the local system: yes
     * if in the local system:
     *      if previous update time is less then 1 day compare to now: no
     *      else: yes
     * @param url the url to the page
     * @return true if needed
     */
    private boolean needFetched(String url) {
        // System.out.println(url);

        //ignore page that need login to access, or the link is dead
        try{
            new URL(url).openStream();
        } catch (Exception e) {return false;}
        //if site is not in the system
        Integer pageID = indexer.searchIdByURL(url,false);
        if (pageID == -1) {
            return true;
        }
        if (pageProperty.getUrl(pageID) == null) {
            return true;
        }
        //if need update
        String lastModify = pageProperty.getLastModificationTime(pageID);
        long diff = new Date(Date.parse(new WebInfoSeeker(url).getLastModificationTime())).getTime()
                - new Date(Date.parse(lastModify)).getTime();
        return diff >= 86400000;  //1 day
    }

    public void printAll() {
        for(int id =1; id<100; id++) {
            String url = pageProperty.getUrl(id);
            if (url == null) {
                continue;
            }
            System.out.println(pageProperty.getTitle(id));
            System.out.println(url);
            System.out.print(pageProperty.getLastModificationTime(id));
            System.out.print(", ");
            System.out.println(pageProperty.getSize(id));
            System.out.println(invertedIndex.getKeyWords(id));
            System.out.println(invertedIndex.getChildPage(id));
            System.out.println("......................................................................");
        }
    }

    public static void main(String[] args) throws RocksDBException {
        Spider spider = new Spider("https://www.cse.ust.hk");
        spider.BFS(10);
        spider.printAll();
//        System.out.println("page property contain");
//        spider.pageProperty.printAll();
//        spider.invertedIndex.printAll(Type.PageID);
//        spider.invertedIndex.printAll(Type.ParentID);
//        spider.invertedIndex.printAll(Type.WordID);
//        spider.indexer.printAll(1);
////        spider.indexer.printAll(2);

    }
}
