package simpledb;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op op;

    private HashMap<Field,Integer> aggregates;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;

        this.aggregates = new HashMap<Field,Integer>();

        if (what==Op.COUNT)
            this.op = what;
        else
            throw new IllegalArgumentException("String Aggregator only supports COUNT operator");
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {

        // get the group field
        Field groupfield;
        if (gbfield != -1)
            groupfield = tup.getField(gbfield);
        else
            groupfield = null;

        // if the group isn't in aggregate, add with initial value
        if (!aggregates.containsKey(groupfield))
            aggregates.put(groupfield,0);

        // increment count
        aggregates.put(groupfield,aggregates.get(groupfield)+1);
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {

        ArrayList<Tuple> tuples = new ArrayList<Tuple>();

        // create tupledesc
        TupleDesc td;
        if (this.gbfield != -1)
            td = new TupleDesc(new Type[]{gbfieldtype,Type.INT_TYPE},new String[]{"groupVal","aggregateVal"});
        else
            td = new TupleDesc(new Type[]{Type.INT_TYPE},new String[]{"aggregateVal"});

        // temp tuple for storage
        Tuple temp;
        for (HashMap.Entry<Field,Integer> entry : aggregates.entrySet())
        {
            temp = new Tuple(td);

            // handle grouping
            if (this.gbfield != -1)
            {
                temp.setField(0,entry.getKey());
                temp.setField(1,new IntField(entry.getValue()));
            }
            else
                temp.setField(0,new IntField(entry.getValue()));

            // add tuple to list
            tuples.add(temp);
        }

        // return TupleIterator as DbIterator
        return new TupleIterator(td,tuples);
    }

}
