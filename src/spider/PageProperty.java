package spider;

import org.rocksdb.*;

import java.util.*;

public class PageProperty {
    private final String PATH = "/Java/4321project/pagePropDB";
    private RocksDB db;
    private List<ColumnFamilyHandle> handles = new Vector<>();

    PageProperty() throws RocksDBException {
        try {
            db = RocksDB.open(PATH);
            db.createColumnFamily(new ColumnFamilyDescriptor("url".getBytes()));
            db.createColumnFamily(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
            db.createColumnFamily(new ColumnFamilyDescriptor("size".getBytes()));
            db.close();
        } catch (RocksDBException ignored){ }

        List<ColumnFamilyDescriptor> colFamily = new Vector<>();
        colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
        colFamily.add(new ColumnFamilyDescriptor("url".getBytes(), new ColumnFamilyOptions()));
        colFamily.add(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
        colFamily.add(new ColumnFamilyDescriptor("size".getBytes()));
        db = RocksDB.open(new DBOptions(), PATH, colFamily, handles);
    }

    public RocksDB getDb() {
        return db;
    }

    String getLastModificationTime(int id) {
        try {
            return new String(db.get(handles.get(2), String.valueOf(id).getBytes()));
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    private void delEntry(byte[] key) throws RocksDBException {
        for(ColumnFamilyHandle h: handles){
            db.delete(h, key);
        }
    }

    void clearDataBase() throws RocksDBException {
        RocksIterator iter = db.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            delEntry(iter.key());
        }
    }

    void store(int id, String url){
        //db.put(Integer.toString(id).getBytes(), "url".getBytes());
        WebInfoSeeker seeker = new WebInfoSeeker(url);
        try {
            db.put(handles.get(0), Integer.toString(id).getBytes(), seeker.getTitle().getBytes());
            db.put(handles.get(1), Integer.toString(id).getBytes(), url.getBytes());
            db.put(handles.get(2), Integer.toString(id).getBytes(), seeker.getLastModificationTime().getBytes());
            db.put(handles.get(3), Integer.toString(id).getBytes(), seeker.getPageSize().getBytes());
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

    public static void main(String[] args) throws RocksDBException {
        PageProperty fetcher = new PageProperty();
        //fetcher.clearDataBase();
        fetcher.printAll();
    }
}
