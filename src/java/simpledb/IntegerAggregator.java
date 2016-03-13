package simpledb;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op op;

    private HashMap<Field,Integer> aggregates;
    private HashMap<Field,Integer> count;




    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.op = what;

        this.aggregates = new HashMap<Field,Integer>();
        this.count = new HashMap<Field,Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {

        // get the group field from the tuple
        Field groupfield;
        if (this.gbfield != -1)
            groupfield = tup.getField(gbfield);
        else
            groupfield = null;

        // if groupfield isn't in the map, add it with initial values
        if (!aggregates.containsKey(groupfield))
        {
            // set initial value: large number for min, small for max, 0 for avg,sum,count
            int aggregatesValue = 0;
            if (this.op==Op.MAX)
                aggregatesValue = Integer.MIN_VALUE;
            else if (this.op==Op.MIN)
                aggregatesValue = Integer.MAX_VALUE;
            else if (this.op==Op.AVG || this.op==Op.SUM || this.op==Op.COUNT)
                aggregatesValue = 0;

            // add groupfield to aggregate with initial value (no tuple actually added yet)
            // set count to 0 (same reason as above)
            aggregates.put(groupfield,aggregatesValue);
            count.put(groupfield,0);
        }


        // get value out of the tuple
        int tupval = ((IntField) tup.getField(afield)).getValue();

        // get current value from aggregate
        int value = aggregates.get(groupfield);

        // take the max of current value and tuple value
        if (this.op==Op.MAX && tupval > value)
            value = tupval;
        // take min of current value and tuple value
        else if (this.op==Op.MIN && tupval < value)
            value = tupval;
        else if (this.op==Op.AVG || this.op==Op.SUM)
        {
            // add tupval to current value, update count
            count.put(groupfield,count.get(groupfield)+1);
            value += tupval;
        }
        // increment value (represents the count)
        else if (this.op==Op.COUNT)
            value += 1;

        // finally, add updated value to aggregate
        aggregates.put(groupfield,value);

    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {

        ArrayList<Tuple> tuples = new ArrayList<Tuple>();

        // create the tupledesc for the Tuple
        TupleDesc td;
        // grouping
        if (this.gbfield != -1)
            td = new TupleDesc(new Type[]{gbfieldtype,Type.INT_TYPE},new String[]{"groupVal","aggregateVal"});
        // no grouping
        else
            td = new TupleDesc(new Type[]{Type.INT_TYPE},new String[]{"aggregateVal"});

        // use for temp storage for each aggregate entry
        Tuple temp;

        for (HashMap.Entry<Field,Integer> entry : aggregates.entrySet())
        {
            // instantiate tuple with tupledesc
            temp = new Tuple(td);

            // need to complete average aggregate
            int finval;
            if (this.op==Op.AVG)
                finval = entry.getValue() / count.get(entry.getKey());
            else
                finval = entry.getValue();

            // set the Tuple fields
            if (gbfield != -1) {
                temp.setField(0, entry.getKey());
                temp.setField(1,new IntField(finval));
            }
            else
                temp.setField(0,new IntField(finval));

            // add tuple to list
            tuples.add(temp);
        }

        // can use TupleIterator using tupledesc and tuples list
        return new TupleIterator(td,tuples);
    }

}
