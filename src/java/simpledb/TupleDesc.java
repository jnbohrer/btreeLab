package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {


        private static final long serialVersionUID = 1L;


        /**
         * The type of the field
         * */
        public final Type fieldType;
        
        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    private List<TDItem> fieldList;

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        return fieldList.iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        fieldList = new ArrayList<TDItem>();
        for (int i=0; i < typeAr.length; i++)
        {
        	fieldList.add(new TDItem(typeAr[i],fieldAr[i]));
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        fieldList = new ArrayList<TDItem>();
        for (int i=0; i < typeAr.length; i++)
        {
            fieldList.add(new TDItem(typeAr[i],null));
        }
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return fieldList.size();
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (i < 0 || i > this.numFields()-1)
            throw new NoSuchElementException("Index out of bounds");
        else
            return fieldList.get(i).fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if (i < 0 || i > this.numFields()-1)
            throw new NoSuchElementException("Index out of bounds");
        else
            return fieldList.get(i).fieldType;
    }

    /**
     * Find the index of the field with a given name.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        for (int i=0; i < fieldList.size(); i++)
        {
            if (fieldList.get(i).fieldName != null && fieldList.get(i).fieldName.equals(name))
                return i;
        }

        throw new NoSuchElementException("No field with given name found");
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        int size = 0;

        // loop through fields
        for (int i=0; i < this.numFields(); i++)
        {
            // int is 4 bytes
            if (this.getFieldType(i) == Type.INT_TYPE)
                size += 4;
            // each char in string is 1 byte
            else
                size+= Type.STRING_LEN;
        }
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        int nsize = td1.numFields() + td2.numFields(); // new td size

        // TupleDesc constructor takes arrays of Type and String
        Type[] ntypes = new Type[nsize];
        String[] nfields = new String[nsize];

        // fill the arrays
        for (int i=0; i < nsize; i++)
        {
            // add the first fields
            if (i < td1.numFields())
            {
                ntypes[i] = td1.getFieldType(i);
                nfields[i] = td1.getFieldName(i);
            }
            // add the second fields
            else
            {
                ntypes[i] = td2.getFieldType(i-td1.numFields());
                nfields[i] = td2.getFieldName(i-td1.numFields());
            }
        }

        return new TupleDesc(ntypes,nfields);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they are the same size and if the n-th
     * type in this TupleDesc is equal to the n-th type in td.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
        // check type
        if (o instanceof TupleDesc)
        {
            // cast
            TupleDesc td = (TupleDesc)o;
            // check size
            if (this.numFields() == td.numFields() && this.getSize() == td.getSize())
            {
                for (int i=0; i < this.numFields(); i++)
                {
                    // if any n-th type is not equal, they aren't equal
                    if (!this.getFieldType(i).equals(td.getFieldType(i)))
                        return false;
                }

                // passed all equality checks
                return true;
            }
            else
                return false;
        }
        // not even the same type
        else
            return false;
    }

    public int hashCode() {
        // string form of the td should work
        return this.toString().hashCode();
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * 
     * @return String describing this descriptor.
     */
    public String toString() {
        String res = "";

        // add every field to the result string
        for (int i=0; i < this.fieldList.size(); i++)
        {
            TDItem item = this.fieldList.get(i);
            res = res + item.fieldType + "(" + item.fieldName + "), ";
        }

        // remove the last comma added from the loop
        res = res.substring(0,res.length()-1);

        return res;
    }
}
