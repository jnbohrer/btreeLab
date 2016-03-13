package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {


    private File hfile;
    private TupleDesc td;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.hfile = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return hfile;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // use hash code for the absolute file path
        return hfile.getAbsolutePath().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        int pageSize = BufferPool.getPageSize();
        // javadocs indicate IllegalArgumentException should be thrown for file not found
        try {
            RandomAccessFile raf = new RandomAccessFile(hfile, "r");
            // get the byte offset with page number and page size
            int offset = pageSize * pid.pageNumber();
            // move to correct page
            raf.seek(offset);

            // container for page data, size of a page
            byte[] pageData = new byte[pageSize];

            // read the data, and close the access file
            raf.read(pageData,0,pageSize);
            raf.close();

            return new HeapPage((HeapPageId)pid,pageData);

        } catch (FileNotFoundException e)
        {
            throw new IllegalArgumentException("File not found");
        } catch (IOException e) // seek requires IOException handling
        {
            throw new IllegalArgumentException("Page not found");
        }
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {

        RandomAccessFile raf = new RandomAccessFile(this.hfile,"rw");

        // move raf to correct position to write
        PageId pid = page.getId();
        int offset = BufferPool.getPageSize() * pid.pageNumber();
        raf.seek(offset);

        // write to the page, close raf
        byte[] pageData = page.getPageData();
        raf.write(pageData);
        raf.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // length of file/length of page gives # of pages
        // use ceiling as the decimal runs onto a new page
        return (int) Math.ceil(hfile.length()/BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {

        // look through current pages
        for (int i=0; i < this.numPages(); i++)
        {
            // create page id for each page
            PageId pid = new HeapPageId(this.getId(),i);
            // pull heap page for each page using getPage method
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid,pid,Permissions.READ_WRITE);

            // if the page has empty slots, we've found a valid spot to add the tuple
            if (page.getNumEmptySlots() > 0) {
                // use HeapPage insertTuple to finish the job
                page.insertTuple(t);
                // return the page as list
                return new ArrayList<Page>(Arrays.asList(page));
            }
        }

        // create new id, using numPages for pageno
        HeapPageId hid = new HeapPageId(this.getId(),this.numPages());
        // create new page from id with blank data
        HeapPage page = new HeapPage(hid,HeapPage.createEmptyPageData());
        // insert tuple into page
        page.insertTuple(t);


        // new RAF to write data to this heapfile
        RandomAccessFile raf  = new RandomAccessFile(this.hfile,"rw");
        // move the file access to the correct position
        int offset = BufferPool.getPageSize() * hid.pageNumber();
        raf.seek(offset);
        // get data from the page
        byte[] pageData = page.getPageData();
        // write data, close page
        raf.write(pageData);
        raf.close();

        return new ArrayList<Page>(Arrays.asList(page));
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {

        // get id of tuple page
        PageId pid = t.getRecordId().getPageId();

        // get tuple's page from db using getPage
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid,pid,Permissions.READ_WRITE);

        // make sure page is within file range
        if (pid.pageNumber() >= this.numPages())
            throw new DbException("Tuple not part of file");

        // use HeapPage deleteTuple to remove tuple from page
        page.deleteTuple(t);

        // return the affected page
        return new ArrayList<Page>(Arrays.asList(page));
    }

    // see DbFile.java for javadocs
    // return an iterator over all the tuples stored in this DbFile
    public DbFileIterator iterator(TransactionId tid) {
        List<Tuple> tupleList = new ArrayList<Tuple>();
        Iterator<Tuple> iterator;

        // loop through all pages in file
        for (int i=0; i < this.numPages(); i++)
        {
            // create page id using heapfile id and page number
            HeapPageId hid = new HeapPageId(this.getId(),i);
            HeapPage page = null;

            // create page using BufferPool getPage method
            try {
                page = (HeapPage) Database.getBufferPool().getPage(tid,hid,Permissions.READ_ONLY);
            } catch (TransactionAbortedException e) {
                e.printStackTrace();
            } catch (DbException e) {
                e.printStackTrace();
            }

            // create iterator
            iterator = page.iterator();
            // get list of tuples that can be passed to the HeapFileIterator
            while (iterator.hasNext())
                tupleList.add(iterator.next());
        }
        return new HeapFileIterator(tid,tupleList);
    }

    public class HeapFileIterator implements DbFileIterator {

        Iterator<Tuple> iterator;
        List<Tuple> tuples;
        TransactionId tid;

        public HeapFileIterator(TransactionId id,List<Tuple> l)
        {
            this.tid = id;
            this.tuples = l;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            this.iterator = this.tuples.iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (iterator!= null)
                return iterator.hasNext();
            return false;
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (iterator!=null)
                return iterator.next();
            throw new NoSuchElementException("No iterator");
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            close();
            open();
        }

        @Override
        public void close() {
            iterator = null;
        }

    }

}

