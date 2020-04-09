package spider;

import org.rocksdb.*;

import java.util.*;

public class PageProperty {
    private Set<String> fetchedPage = new HashSet<>();
    private RocksDB db;
    private List<ColumnFamilyDescriptor> colFamily = new Vector<>();
    private List<ColumnFamilyHandle> handles = new Vector<>();
    private int numOfPage;

    PageProperty(String url, int numOfPage) throws RocksDBException {
        try {
            db = RocksDB.open("/Java/4321project/db");
            db.createColumnFamily(new ColumnFamilyDescriptor("title".getBytes()));
            db.createColumnFamily(new ColumnFamilyDescriptor("url".getBytes()));
            db.createColumnFamily(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
            db.createColumnFamily(new ColumnFamilyDescriptor("size".getBytes()));
            db.close();
        }catch (RocksDBException ignored){ }


        colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
        colFamily.add(new ColumnFamilyDescriptor("title".getBytes(), new ColumnFamilyOptions()));
        colFamily.add(new ColumnFamilyDescriptor("url".getBytes(), new ColumnFamilyOptions()));
        colFamily.add(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
        colFamily.add(new ColumnFamilyDescriptor("size".getBytes()));
        db = RocksDB.open(new DBOptions(), "/Java/4321project/db", colFamily, handles);
        this.numOfPage = numOfPage;
        DFS(url);
    }

    private Set<String> getFetchedPage() {
        return fetchedPage;
    }

    private void store(int id, String url) throws RocksDBException {
        db.put(Integer.toString(id).getBytes(), "url".getBytes());
        WebInfoSeeker seeker = new WebInfoSeeker(url);
        db.put(handles.get(0), Integer.toString(id).getBytes(), seeker.fetchTitle().getBytes());
        db.put(handles.get(1), Integer.toString(id).getBytes(), url.getBytes());
        db.put(handles.get(2), Integer.toString(id).getBytes(), seeker.fetchLastModificationTime().getBytes());
        db.put(handles.get(3), Integer.toString(id).getBytes(), seeker.fetchSize().getBytes());
    }

    private void delEntry(int id) throws RocksDBException {
        db.remove(Integer.toString(id).getBytes());
    }

    private void printAll() throws RocksDBException {
        for (int i = 1; i < numOfPage+1; i++) {
//            System.out.print(new String(db.get(Integer.toString(i).getBytes())));
            System.out.print(i+": ");
            System.out.print(new String(db.get(handles.get(0), Integer.toString(i).getBytes())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(1), Integer.toString(i).getBytes())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(2), Integer.toString(i).getBytes())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(3), Integer.toString(i).getBytes())));
            System.out.println();
        }
    }

    private void DFS(String url) throws RocksDBException {
        Queue<String> queue = new LinkedList<>();
        queue.add(url);
        int i = 0;
        while (!queue.isEmpty()) {
            String site = queue.remove();
            if(fetchedPage.size() >= numOfPage){
                break;
            }
            if (fetchedPage.contains(site)) {
                continue;
            }
            System.out.println("site "+ i++);
            fetchedPage.add(site);
            store(i, site);
            System.out.println("handling " + site);
            List<String> links = WebInfoSeeker.getChildLinks(site);
            for (String link : links) {
                if (!fetchedPage.contains(link)) {
                    queue.add(link);
                }
            }
        }
    }

    public static void main(String[] args) throws RocksDBException {
        PageProperty fetcher = new PageProperty("https://www.cse.ust.hk",30);
        System.out.println("the database contain...");
        fetcher.printAll();
        //Set<String> fetchedPage = fetcher.getFetchedPage();
    }
}
