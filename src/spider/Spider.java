package spider;

import org.rocksdb.RocksDBException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
    void BFS(int numOfPage){
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
            StoringType type = fetchCase(site);
            if ( type != StoringType.ignore) {
                pageFetched++;
                int id = indexer.searchIdByURL(site, true);
                System.out.println(id +" handling " + site);
                if (type == StoringType.updateOld) {
                    invertedIndex.clearRecord(id);
                }
                pageProperty.store(id, site);
                invertedIndex.store(id, site);
                indexer.storeTitle(pageProperty.getTitle(id));
            }
            List<String> links = new WebInfoSeeker(site).getChildLinks();
            for (String link : links) {
                if (!queue.contains(link)) {
                    queue.add(link);
                }
            }
        }
    }
    enum StoringType{ignore, addNew, updateOld}
    /**
     * determine weather a page need to be fetched or not:
     * if the link is dead or need login access: no
     * if the link go to a non html page: no
     * if not in the local system: yes
     * if in the local system:
     *      if previous update time is less then 1 day compare to now: no
     *      else: yes
     * @param url the url to the page
     * @return true if needed
     */
    private StoringType fetchCase(String url) {
        // System.out.println(url);
        //ignore page that need login to access, or the link is dead
        try{
            new URL(url).openStream();
        } catch (Exception e) {return StoringType.ignore;}
        //ignore page that is a not html page
        try {
            String connectionType = new URL(url).openConnection().getHeaderField("Content-Type");
            if (!connectionType.contains("html")) {
                return StoringType.ignore;
            }
        } catch (IOException e) { e.printStackTrace(); }
        //if site is not in the system
        Integer pageID = indexer.searchIdByURL(url,false);
        if (pageID == -1) {
            return StoringType.addNew;
        }
        if (pageProperty.getUrl(pageID) == null) {
            return StoringType.addNew;
        }
        //if need update
        String lastModify = pageProperty.getLastModificationTime(pageID);
        long diff = new Date(Date.parse(new WebInfoSeeker(url).getLastModificationTime())).getTime()
                - new Date(Date.parse(lastModify)).getTime();
        return (diff >= 86400000)? StoringType.updateOld: StoringType.ignore;
    }

    /**
     * show details of all fetched page in a txt file
     * @param outputPath the path to the .txt file
     */
    void printAll(String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            int maxId = pageProperty.getMaxId();
            for (int id = 1; id < maxId; id++) {
                String url = pageProperty.getUrl(id);
                if (url == null) {
                    continue;
                }
                writer.println(pageProperty.getTitle(id));
                writer.println(url);
                writer.print(pageProperty.getLastModificationTime(id));
                writer.print(", ");
                writer.println(pageProperty.getSize(id));
                String[] words = invertedIndex.getKeyWords(id);
                for (String word : words) {
                    writer.print(word + " ");
                    writer.print(InvertedIndex.getInstance().getFreqOfWordInParticularPage(word, id) + "; ");
                }
                writer.println();
                writer.println(invertedIndex.getChildPage(id));
                writer.println("......................................................................");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RocksDBException {
        Spider spider = new Spider("https://www.cse.ust.hk");
        spider.BFS(30);
        System.out.println("page property contain");
        spider.pageProperty.printAll();
//        spider.indexer.printAll(IndexType.WordID);
//        spider.invertedIndex.printAll(Type.WordID);
//        spider.invertedIndex.printAll(Type.PageID);
        spider.printAll("spider_result.txt");
//        spider.invertedIndex.printAll(Type.ParentID);
    }
}
