package simpledb;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private DbIterator child;
    private TupleDesc td;

    private boolean finishedDeleting;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, DbIterator child) {
        this.tid = t;
        this.child = child;

        // final tupledesc requirement (returned in fetchNext)
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE},new String[]{"numRecordsDeleted"});
        this.finishedDeleting = false;
    }

    public TupleDesc getTupleDesc() {
        return this.td;
    }

    public void open() throws DbException, TransactionAbortedException {
        super.open();
        this.child.open();
        this.finishedDeleting = false;
    }

    public void close() {
        super.close();
        this.child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.close();
        this.open();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * 
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // count
        int deletions = 0;

        // if called more than once, return null
        if (this.finishedDeleting)
            return null;

        // run through tuples
        while (this.child.hasNext())
        {
            // get nex tuple, delete the tuple from db, increment deletions
            Tuple temp = this.child.next();
            try {
                Database.getBufferPool().deleteTuple(this.tid,temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            deletions++;
        }

        // create return tuple
        Tuple res = new Tuple(this.td);
        res.setField(0,new IntField(deletions));
        // function has been called
        this.finishedDeleting = true;

        return res;
    }

    @Override
    public DbIterator[] getChildren() {
        return new DbIterator[]{this.child};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        children[0] = this.child;
    }

}
