package retriever;

import indexer.InvertedIndex;
import indexer.PageProperty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageRank {

    private static final PageRank INSTANCE = new PageRank();
    InvertedIndex invertedIndex = InvertedIndex.getInstance();
    PageProperty pageProperty = PageProperty.getInstance();
    RocksDB pageRankDb;

    public static PageRank getInstance() {
        return INSTANCE;
    }

    private PageRank(){
        try {
            pageRankDb = RocksDB.open(new Options().setCreateIfMissing(true), "pageRankDB");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public double getPageRank(int pageID) {
        try {
            return Double.parseDouble(new String(pageRankDb.get(String.valueOf(pageID).getBytes())));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void preCalculatePageRank(int max_iter, double dampingFactor){
        HashMap<Integer, Double> pastPageRankResult = new HashMap<>();
        HashMap<Integer, Double> currentPageRankResult = new HashMap<>();
        List<Integer> allPages = pageProperty.getAllPageID();
        for (Integer pageID: allPages) {
            pastPageRankResult.put(pageID, 1.0);
            currentPageRankResult.put(pageID, 0.0);
        }

        for(int i = 0; i < max_iter; i++) {
            for (Integer pageID : allPages) {
                String[] parentIDs = PreProcessor.getInstance().getParentIDs(pageID);

                if (parentIDs != null) {
                    for (String parentID : parentIDs) {
                        if (parentID.equals("")) {
                            continue;
                        }
                        if (invertedIndex.getChildIDs(Integer.parseInt(parentID)) != null) {
                            Double partialScore = pastPageRankResult.get(Integer.parseInt(parentID)) / invertedIndex.getChildIDs(Integer.parseInt(parentID)).length;
                            currentPageRankResult.put(pageID, currentPageRankResult.get(pageID)+partialScore);
                        }
                    }
                    double RankPage = (1.0 - dampingFactor) + (dampingFactor * currentPageRankResult.get(pageID));
                    currentPageRankResult.put(pageID, RankPage);

                }

            }
            // perform the normalization at the final iteration
            if (i == max_iter-1) {
                for (Map.Entry<Integer, Double> entry: currentPageRankResult.entrySet()) {
                    try {
                        System.out.println(entry.getKey() +": "+entry.getValue());
                        pageRankDb.put(String.valueOf(entry.getKey()).getBytes(), String.valueOf(entry.getValue()).getBytes());
                    } catch (RocksDBException e) {
                        e.printStackTrace();
                    }
                }
            }
            //reset the PageRank values if not final iteration
            else {
                System.out.println(currentPageRankResult);
                double diff = 0.0;
                for (int pageId : allPages) {
                    diff += Math.abs(pastPageRankResult.get(pageId) - currentPageRankResult.get(pageId));
                }
                System.out.println("in " + i + " iteration, diff = " + diff);
                for (Integer pageID : allPages) {
                    pastPageRankResult.put(pageID, currentPageRankResult.get(pageID));
                }
                for (Integer pageID : allPages) {
                    currentPageRankResult.put(pageID, 0.0);
                }
            }
        }
    }

    public static void main(String[] args){
        PageRank pageRank = PageRank.getInstance();
        //don't run it again as the db is already here
//        pageRank.preCalculatePageRank(30, 0.85);
        Integer[] count = new Integer[60];
        for (int i = 0; i < 60; i++) {
            count[i] = 0;
        }
        for (int pageID : PageProperty.getInstance().getAllPageID()) {
            System.out.println(pageRank.getPageRank(pageID));
            int c = (int) pageRank.getPageRank(pageID);
            count[c] += 1;
        }
        System.out.println(Arrays.toString(count));
    }

}
