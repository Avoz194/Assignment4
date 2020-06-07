import java.util.*;

/**
 * Cuckoo hash table implementation of hash tables.
 *
 */
public class CuckooHashing {
    private static final int DEFAULT_TABLE_SIZE = 101;
    
    private final HashMethods hashFunctions;
    private final int numHashFunctions;
    
    private String[] array; // The array of elements
    private int currentSize; // The number of occupied cells
    private ArrayList<String> stash; //List of items that couldn't find a place
    private Stack cucLogs ; //Stack to log changes in the array during Insert process for Undo purposes

    /**
     * Construct the hash table.
     */
    public CuckooHashing(HashMethods hf) {
        this(hf, DEFAULT_TABLE_SIZE);
    }

    /**
     * Construct the hash table.
     *
     * @param hf   the hash family
     * @param size the approximate initial size.
     */
    public CuckooHashing(HashMethods hf, int size) {
        allocateArray(nextPrime(size));
        stash = new ArrayList<String>();
        makeEmpty();
        hashFunctions = hf;
        numHashFunctions = hf.getNumberOfFunctions();
        this.cucLogs=new Stack();
    }
    
    /**
     * Insert into the hash table. If the item is
     * already present, return false.
     *
     * @param x the item to insert.
     */
    public boolean insert(String x) {
    	if(this.capacity()==this.size()){
    		return false;
    	}
        if (find(x))
            return false;

        return insertHelper1(x);
    }
    
    private boolean insertHelper1(String x) {
        LinkedList<CuckooLog> changes = new LinkedList<>(); //LinkedList to contain all changes in the array during one Insert process
        while (true) {
            int pos = -1;
            int kick_pos = -1;

            //This is NOT part of a real cuckoo hash implementation
            //but is necessary to avoid randomization so we can test your work
            ArrayList<ArrayList<String>> cycle_tester = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.capacity();i++){
                cycle_tester.add(i, new ArrayList<String>());
            }
            boolean cycle=false;
            changes.addFirst(new CuckooLog(x,kick_pos));
            int MAXTRIES  = this.size();
            for (int count = 0; count <= MAXTRIES; count++) {
                for (int i = 0; i < numHashFunctions; i++) {
                    pos = myhash(x, i);
                    if(isCycle(cycle_tester,x,pos))
                    {
                        cycle=true;
                        break;
                    }
                    cycle_tester.get(pos).add(x);
                    if (array[pos] == null) {
                        array[pos] = x;
                        currentSize++;
                        cucLogs.push(changes); //push finished List of changes to the stack
                        return true;
                    }

                }
                if(cycle)
                    break;
                if(pos==kick_pos || kick_pos==-1)
                    kick_pos= myhash(x, 0);
                else
                    kick_pos=pos;
                // none of the spots are available, kick out item in kick_pos
                String tmp = array[kick_pos];
                array[kick_pos] = x;
                x = tmp;
                changes.addFirst(new CuckooLog(x,kick_pos)); //add x to the log linkedlist with his position in the array before the change. Last change to be first in list
            }
            //insertion got into a cycle use overflow list
            cucLogs.push(changes); //push finished List of changes to the stack
            this.stash.add(x);
                return true;
        }
    }
    private boolean isCycle(ArrayList<ArrayList<String>> cycle_tester,String x,int i) {
    	return cycle_tester.get(i).contains(x);
    }

    /**
     * Undo previous effective operations
     *
     */
	public void undo() {
        // TODO: implement your code here

        if(!cucLogs.empty()){
            LinkedList<CuckooLog> changes = (LinkedList<CuckooLog>)cucLogs.pop();
            //handle first change  - remove from position in array and place in its position before the action
            CuckooLog lastChange = changes.removeFirst();
            remove(lastChange.getValue(),true);
            //move each value in the linkedList to it's place in the array before the change (from last change to first)
            if(lastChange.getPreIndex()!=-1)
                array[lastChange.getPreIndex()] = lastChange.getValue();
            while(!changes.isEmpty()){
                lastChange=changes.removeFirst();
                if(lastChange.getPreIndex()!=-1)
                    array[lastChange.getPreIndex()] = lastChange.getValue();
            }
        }
	}

    /**
     * @param x the item
     * @param i index of hash function in hash family
     * @return hash value of x using hash function(i) mod table size
     */
    private int myhash(String x, int i) {
        long hashVal = hashFunctions.hash(x, i);

        hashVal %= array.length;
        if (hashVal < 0)
            hashVal += array.length;

        return (int) hashVal;
    }
    
    /**
     * Finds an item in the hash table.
     *
     * @param x the item to search for.
     * @return True iff item is in the table.
     */
    public boolean find(String x) {
        return findPos(x) != -1;
    }
    
    /**
     * Method that searches all hash function places.
     *
     * @param x the item to search for.
     * @return the position where the search terminates or capacity+1 if item is in overflow list, or -1 if not found.
     */
    private int findPos(String x) {
        for (int i = 0; i < numHashFunctions; i++) {
            int pos = (int) myhash(x, i);
            if (array[pos] != null && array[pos].equals(x))
                return pos;
        }
        for(String s:stash) {
        	if(s.equals(x)){
        		return this.capacity()+1;
        	}		
        }

        return -1;
    }

    /**
     * Gets the size of the table.
     *
     * @return number of items in the hash table.
     */
    public int size() {
        return currentSize;
    }

    /**
     * Gets the length (potential capacity) of the table.
     *
     * @return length of the internal array in the hash table.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * Remove from the hash table.
     *
     * @param x the item to remove.
     * @return true if item was found and removed
     */

    public boolean remove(String x){return remove(x,false);}

    /**
     * Remove from the hash table, make sure to clear the log stack for Undo purposes.
     *
     * @param x the item to remove.
     * @param isUndo to mark in case the remove function was trigered by Undo process
     * @return true if item was found and removed
     */
    private boolean remove(String x, boolean isUndo) {
        int pos = findPos(x);
        if(pos==-1)
        	return false;
        if (pos<this.capacity()) {
            array[pos] = null;
			currentSize--;
        } else {
        	this.stash.remove(x);
        }
        if(!isUndo)
            cucLogs.clear();
        return true;
    }

    /**
     * Make the hash table logically empty.
     */
    public void makeEmpty() {
    	currentSize = 0;
        for (int i = 0; i < array.length; i++)
            array[i] = null;
        this.stash.clear();
    }
    
    public String toString() {
    	String ans = "";
    	for (int i = 0; i < capacity(); i++) {
            if (array[i] != null)
                ans = ans.concat("Index: "+ i + " ,String: " +array[i]+"\n");
        }
    	int i=0;
        for(String s:stash) {
        	ans = ans + "Overflow["+ i + "] ,String: " +s+"\n";
        	i++;
        }
        return ans;
    }
    
    /**
     * Method to allocate array.
     */
    private void allocateArray(int arraySize) {
        array = new String [arraySize];
    }

    /**
     * Method to find a prime number at least as large as n.
     */
    protected static int nextPrime(int n) {
        if (n % 2 == 0)
            n++;

        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    /**
     * Method to test if a number is prime.
     */
    private static boolean isPrime(int n) {
        if (n == 2 || n == 3)
            return true;

        if (n == 1 || n % 2 == 0)
            return false;

        for (int i = 3; i * i <= n; i += 2)
            if (n % i == 0)
                return false;

        return true;
    }
	
}