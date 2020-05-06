package retriever;

import indexer.InvertedIndex;
import indexer.PageProperty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;


public class PreProcessor {
    private static PreProcessor instance = new PreProcessor();
    private PageProperty pageProperty = PageProperty.getInstance();
    private InvertedIndex invertedIndex = InvertedIndex.getInstance();
    /**
     * store doc length of each page : pageID -> document length
     */
    private RocksDB docLengthDB;

    static PreProcessor getInstance() {
        return instance;
    }

    double getDocLength(int pageID) {
        try {
            return Double.parseDouble(new String(docLengthDB.get(String.valueOf(pageID).getBytes())));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private PreProcessor() {
        try {
            docLengthDB = RocksDB.open(new Options().setCreateIfMissing(true), "/Java/Spider/docLengthDB");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    /**
     * pre-compute all doc length
     */
    private void preComputeDocumentLength() {
        try {
            for (int pageID : pageProperty.getAllPageID()) {
                System.out.println("calculate doc length of " + pageID);

                String[] keyWords = invertedIndex.getKeyWords(pageID);
                double documentLength = 0;
                for (String KeyWord : keyWords) {
                    if (KeyWord.equals("")) {
                        continue;
                    }
                    double termWeight = invertedIndex.getTermWeight(KeyWord, pageID);
                    documentLength += Math.pow(termWeight, 2);
                }
                documentLength = Math.sqrt(documentLength);
                System.out.println("save to db");
                docLengthDB.put(String.valueOf(pageID).getBytes(), String.valueOf(documentLength).getBytes());
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PreProcessor preProcessor = new PreProcessor();
        preProcessor.preComputeDocumentLength();
    }
}
