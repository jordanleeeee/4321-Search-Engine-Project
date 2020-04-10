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
    private Set<String> fetchedPage = new HashSet<>();
    private RocksDB pageIDdb, parentIDdb;

    InvertedIndex() throws RocksDBException {
        Options options = new Options();
        options.setCreateIfMissing(true);
        //todo
        pageIDdb = RocksDB.open(options, "/Java/4321project/pageIDdb");
        parentIDdb = RocksDB.open(options, "/Java/4321project/parentIDdb");
    }

    private void wordUpdate(String word, int PageID) {
        //todo
        //if word not in database, add it
    }

    public void store(int id, String url){
        try {
            // Page-ID -> {keywords}
            WebInfoSeeker seeker = new WebInfoSeeker(url);
            byte[] content = null;
            for (int i = 0; i < seeker.getKeywords().size(); i++) {
                //todo newly added function
                wordUpdate(seeker.getKeywords().get(i), id);
                //System.out.print(seeker.getKeywords().get(i) + " ");
                if (content == null) {
                    content = (seeker.getKeywords().get(i)).getBytes();
                } else {
                    content = (new String(content) + " " + seeker.getKeywords().get(i)).getBytes();
                }
            }
            assert content != null;
            pageIDdb.put(Integer.toString(id).getBytes(), content);


            List<String> child = WebInfoSeeker.getChildLinks(url);
            Set<String> childPage = new HashSet<>(child);

            content = null;
            System.out.print("Parent ID " + id + " " + url + "\n"); //url = parent
            for (String link : childPage) {
                if (link.equals(url)) {
                    continue;
                }
                //todo get ID with given url
                int childId = 1;
                if (content == null) {
                    content = Integer.toString(childId).getBytes();
                } else {
                    content = (new String(content) + " " + childId).getBytes();
                }
            }
            assert content != null;
            parentIDdb.put(Integer.toString(id).getBytes(), content);
            System.out.println();
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


    private void printAll(Type situation){
        //Page-ID -> {keywords}
        if(situation == Type.PageID) {
            RocksIterator iter = pageIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ID: " + new String(iter.key()) + '\n' + new String(iter.value()) + "\n");
            }
        }

        if(situation == Type.ParentID) {
            RocksIterator iter = parentIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(iter.value()) + "\n");
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
