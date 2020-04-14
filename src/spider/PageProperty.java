package spider;

import org.rocksdb.*;

import java.util.*;

public class PageProperty {
    private static PageProperty INSTANCE = new PageProperty();
    private RocksDB db;
    private List<ColumnFamilyHandle> handles = new Vector<>();

    static PageProperty getInstance() {
        return INSTANCE;
    }

    /**
     * open db file
     */
    private PageProperty(){
        String PATH = "/Java/Spider/pagePropDB";
        try {
            List<ColumnFamilyDescriptor> colFamily = new Vector<>();
            colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
            colFamily.add(new ColumnFamilyDescriptor("url".getBytes(), new ColumnFamilyOptions()));
            colFamily.add(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("size".getBytes()));
            DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            db = RocksDB.open(options, PATH, colFamily, handles);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    String getTitle(int pageId) {
        try {
            byte[] url = db.get(handles.get(0), String.valueOf(pageId).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    String getUrl(int pageId) {
        try {
            byte[] url = db.get(handles.get(1), String.valueOf(pageId).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    String getSize(int pageId) {
        try {
            byte[] url = db.get(handles.get(3), String.valueOf(pageId).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    String getLastModificationTime(int pageId) {
        try {
            return new String(db.get(handles.get(2), String.valueOf(pageId).getBytes()));
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    /**
     * store page info into database
     * @param pageId id of the page
     * @param url link of the page
     */
    void store(int pageId, String url){
        //db.put(Integer.toString(pageId).getBytes(), "url".getBytes());
        WebInfoSeeker seeker = new WebInfoSeeker(url);
        try {
            db.put(handles.get(0), Integer.toString(pageId).getBytes(), seeker.getTitle().getBytes());
            db.put(handles.get(1), Integer.toString(pageId).getBytes(), url.getBytes());
            db.put(handles.get(2), Integer.toString(pageId).getBytes(), seeker.getLastModificationTime().getBytes());
            db.put(handles.get(3), Integer.toString(pageId).getBytes(), seeker.getPageSize().getBytes());
        } catch (RocksDBException e) {
            System.out.println("this should not happened");
            e.printStackTrace();
        }
    }

    void printAll() throws RocksDBException {
        RocksIterator iterator = db.newIterator();
        for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            System.out.print(new String(iterator.key())+": ");
            System.out.print(new String(db.get(handles.get(0), iterator.key())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(1), iterator.key())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(2), iterator.key())));
            System.out.print("\t");
            System.out.print(new String(db.get(handles.get(3), iterator.key())));
            System.out.println();
        }
    }

    int getMaxId() {
        int maxId = 0;
        RocksIterator iterator = db.newIterator();
        for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            int id = Integer.parseInt(new String(iterator.key()));
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId;
    }

//        private void delEntry(byte[] key) throws RocksDBException {
//        for(ColumnFamilyHandle h: handles){
//            db.delete(h, key);
//        }
//    }
//
//    private void clearDataBase() throws RocksDBException {
//        RocksIterator iter = db.newIterator();
//        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
//            delEntry(iter.key());
//        }
//    }
//
    public static void main(String[] args) throws RocksDBException {
        PageProperty fetcher = getInstance();
        //fetcher.clearDataBase();
        fetcher.printAll();
    }
}
