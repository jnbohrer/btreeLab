package simpledb;

import java.io.IOException;

/**
 * Inserts tuples read from the child operator into the tableid specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private DbIterator child;
    private int tableid;
    // tupledesc of the tuple returned in fetchNext
    private TupleDesc td;

    // fetchNext should return null if done more than once
    private boolean finishedInserting;

    /**
     * Constructor.
     * 
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableid
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t,DbIterator child, int tableid)
            throws DbException {
        if (!child.getTupleDesc().equals(Database.getCatalog().getTupleDesc(tableid)))
            throw new DbException("tupledesc mismatch");

        this.tid = t;
        this.child = child;
        this.tableid = tableid;

        // final tupledesc requirement (returned in fetchNext)
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE},new String[]{"numRecordsInserted"});

        // init to false since we haven't run through fetchNext
        this.finishedInserting = false;
    }

    public TupleDesc getTupleDesc() {
        return this.td;
    }

    public void open() throws DbException, TransactionAbortedException {
        this.finishedInserting = false;
        this.child.open();
        super.open();
    }

    public void close() {
        this.child.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.close();
        this.open();
    }

    /**
     * Inserts tuples read from child into the tableid specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     * 
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // count
        int numInsertions = 0;

        // if we've called fetchNext already
        if (this.finishedInserting)
            return null;

        // loop through
        while (this.child.hasNext())
        {
            // get next tuple, insert into table, increment insertions count
            Tuple temp = this.child.next();
            try {
                Database.getBufferPool().insertTuple(this.tid,this.tableid,temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            numInsertions++;
        }

        // create return tuple
        Tuple res = new Tuple(this.td);
        res.setField(0,new IntField(numInsertions));
        // we've now called this function
        this.finishedInserting = true;

        return res;
    }

    @Override
    public DbIterator[] getChildren() {
        DbIterator[] children = {this.child};
        return children;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        children[0] = this.child;
    }
}
