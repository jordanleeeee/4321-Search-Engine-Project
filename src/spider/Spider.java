package spider;

import indexer.Indexer;
import indexer.InvertedIndex;
import indexer.PageProperty;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Spider {
    private Indexer indexer;
    private InvertedIndex invertedIndex;
    private PageProperty pageProperty;
    private Queue<String> queue;

    /**
     * fetch one link (use for beginning)
     * @param url the link
     */
    Spider(String url){
        indexer = Indexer.getInstance();
        invertedIndex = InvertedIndex.getInstance();
        pageProperty = PageProperty.getInstance();
        queue = new LinkedList<>();
        queue.add(url);
    }

    /**
     * fetch many link (use for subsequence fetch), is much quicker
     * @param queue the queue contain all the links
     */
    Spider(Queue<String> queue) {
        indexer = Indexer.getInstance();
        invertedIndex = InvertedIndex.getInstance();
        pageProperty = PageProperty.getInstance();
        this.queue = queue;
    }
    /**
     * do BFS
     * fetch n more page to the system, weather a page need to be fetch into the system
     * will be determine automatically.
     * n must not be too large, otherwise continuous access the same web server
     * too much in a short time will make the web server treat you as hacker
     * and block access from you temporarily.
     * @param numOfPage n (required number of page to be fetch)
     */
    void BFS(int numOfPage){
        Set<String> discoveredPage = new HashSet<>();
        int pageFetched = 0;
        while (!queue.isEmpty()) {
            String site = queue.remove();
            if (pageFetched >= numOfPage) {
                break;
            }
            if (discoveredPage.contains(site)) {
                continue;
            }
            PageType type = fetchCase(site);
            discoveredPage.add(site);
            if (type == PageType.ignore) {
                continue;
            }
            if ( type != PageType.bypass) {
                pageFetched++;
                int pageID = indexer.searchIdByURL(site, true);
                System.out.println(pageID +" handling " + site);
                if (type == PageType.updateOld) {
                    invertedIndex.clearRecord(pageID);
                }
                pageProperty.store(pageID, site);
                invertedIndex.store(pageID, site);
                indexer.storeTitle(pageProperty.getTitle(pageID));
            }
            int id = indexer.searchIdByURL(site, false);
            List<String> links = (type==PageType.bypass)?
                    invertedIndex.getAllChildPage(id): new WebInfoSeeker(site).getChildLinks();
            //List<String> links = new WebInfoSeeker(site).getChildLinks();
            for (String link : links) {
                if (!queue.contains(link)) {
                    queue.add(link);
                }
            }
        }
        try (PrintWriter writer = new PrintWriter("remainingQueue.txt")) {
            for (String link : queue) {
                writer.println(link);
            }
        } catch (FileNotFoundException e) { e.printStackTrace(); }
    }

    /**
     * type of pages:
     * ignore: should ignore by the spider
     * addNew: is unseen by the spider
     * updateOld: is seen by the spider, but the page need update
     * bypass: is seen by the spider, and the page not need update
     */
    private enum PageType {ignore, addNew, updateOld, bypass}

    /**
     * determine the type of page
     *      * if not cse website: ignore
     * if the link is dead or need login access: ignore
     * if the link go to a non html page: ignore
     * if not in the local system: addNew
     * if in the local system:
     *      if last modification date of the page is later than (more than 1 day) the recorded in the index: updateOld
     *      else: bypass
     * @param url the url to the page
     * @return case
     */
    private PageType fetchCase(String url) {
        //System.out.println("checking " + url);
        //todo should uncomment this line
        //ignore page that is not cse web page
        if (!url.contains("cse.ust.hk")) {
            return PageType.ignore;
        }
        WebInfoSeeker seeker = new WebInfoSeeker(url);
        //ignore page that need login to access, or the link is dead
        if (!seeker.canAccess()) {
            return PageType.ignore;
        }
        //ignore page that is a not html page
        if (!seeker.isHtmlPage()) {
            return PageType.ignore;
        }
        //if site is not in the system
        Integer pageID = indexer.searchIdByURL(url,false);
        if (pageID == -1) {
            return PageType.addNew;
        }
        if (pageProperty.getUrl(pageID) == null) {
            return PageType.addNew;
        }
        //if need update
        String lastModify = pageProperty.getLastModificationTime(pageID);
        long diff = new Date(Date.parse(new WebInfoSeeker(url).getLastModificationTime())).getTime()
                        - new Date(Date.parse(lastModify)).getTime();
        // 86400000 is 1 day in nanosecond
        return (diff > 86400000)? PageType.updateOld: PageType.bypass;
    }

    /**
     * show details of all fetched page in a txt file
     * @param outputPath the path to the .txt file
     */
    void printAll(String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            List<Integer> maxId = pageProperty.getAllPageId();
            for (int id : maxId) {
                String url = pageProperty.getUrl(id);
                writer.println(id);
                writer.println(pageProperty.getTitle(id));
                writer.println(url);
                writer.print(pageProperty.getLastModificationTime(id));
                writer.print(", ");
                writer.println(pageProperty.getSize(id));
                String[] words = invertedIndex.getKeyWords(id);

                for (String word : words) {
                    if (word.equals("")) {
                        continue;
                    }
                    writer.print(word + " ");
                    writer.print(InvertedIndex.getInstance().getFreqOfWordInParticularPage(word, id) + "; ");
                }

                writer.println();
                //writer.println(invertedIndex.getChildPages(id));
                writer.println("......................................................................");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) throws RocksDBException {
//        Spider spider = new Spider("https://www.cse.ust.hk");
//       // spider.BFS(30);
//        System.out.println("page property contain");
//        spider.pageProperty.printAll();
//        spider.indexer.printAll(IndexType.WordID);
//        spider.invertedIndex.printAll(Type.WordID);
//        spider.invertedIndex.printAll(Type.PageID);
//        spider.printAll("spider_result.txt");
//        spider.invertedIndex.printAll(Type.ParentID);
//    }
}
