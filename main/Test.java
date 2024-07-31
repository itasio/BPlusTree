package main;
import java.io.IOException;
import java.io.RandomAccessFile;


import tree.BTree;
import tree.Data;
import tree.StorageCache;
public class Test {
    /*
     * important notes for this assignment
     * Page size = 256 bytes
     * Two files are created
     * plh201_node.bin(1) and plh201_data.bin(2)
     * In file (1) is stored one page for every node
     * 	Every page has the following format: Identifier(0->leaf/1->inner), parent, left-sibling, right-sibling, keyCount, value(leaf)/child(inner), key consequently
     * 	if node is inner last element stored in each page is child
     * 	All elements stored as integers(4 bytes)
     * 	29 keys are stored in every node, because
     * 	Inner: 5*4 + 4m+4(m+1) = 256 => m = 29
     * 	Leaf: 5*4 + 4m+4m = 256 => m = 29.5 = 29
     *In file (2) is stored one page for the data of every node
     *	every set of data has 32 bytes size, so there are 256/32 = 8 set of data in every page
     */
    static final int numKeys = 100000;
    static final int minKey = 1;
    static final int maxKey = 1000001;
    static final int numOfSearches = 20;
    static final int rangeMin = 10;
    static final int rangeMax = 1000;
    static final int minVal = 1;
    static final int maxVal = 10001;
    public static void main(String[] args) throws IOException {
        RandomAccessFile nodes = new RandomAccessFile(StorageCache.getNodeStorageFilename(), "rw");
        RandomAccessFile datas= new RandomAccessFile(StorageCache.getDataStorageFilename(), "rw");
        nodes.setLength(0);
        datas.setLength(0);
        int [] keys = intArrayGenerator(minKey, maxKey, numKeys+numOfSearches);
        int [] searchKeys = intArrayGenerator(minKey, maxKey, numOfSearches);

        BTree<Integer, Data> tree = new BTree<Integer, Data>();

		/*tree.insert(Integer.valueOf(30), new Data(1,2,3,4,17, 18, 19, 20));
		tree.insert(Integer.valueOf(20), new Data(5,6,7,821, 22, 23, 24, 29));
		tree.insert(Integer.valueOf(10), new Data(9,10,11,12, 25, 26, 27, 28));
		tree.insert(Integer.valueOf(40), new Data(13,14,15,16, 30, 31, 32, 33));
		tree.delete(80);
		System.out.println(tree.search(10));
		System.out.println(tree.search(20));
		tree.searchRange(10, 30);
		tree.searchRange(10, 10);
		tree.delete(20);
		tree.delete(20);
		tree.delete(30);
		tree.delete(10);
		tree.delete(80);
		tree.searchRange(20, 20);
		tree.searchRange(10, 30);
		System.out.println(tree.search(10));
		System.out.println(tree.search(20));
		*/


        //insert 10^5 +20 random keys with 8 random values
        //count disk accesses for the last 20 insertions
        int i = 0;
        while(i < numKeys) {
            int[] dataForEachKey = intArrayGenerator(minVal, maxVal, Data.getSize());
            tree.insert(Integer.valueOf(keys[i]), new Data(dataForEachKey));
            i++;
        }
        MultiCounter.resetCounter(1);		//reset counter of insertion

        for(; i < keys.length; i++) {
            int[] dataForEachKey = intArrayGenerator(minVal, maxVal, Data.getSize());
            tree.insert(Integer.valueOf(keys[i]), new Data(dataForEachKey));			//MultiCounter 1
        }
        System.out.println("Mean number of disk accesses for insertion in B+ tree: "+(float)MultiCounter.getCount(1) / numOfSearches);

        //make 20 key searches, 20 deletions, 20 range queries(range = 10), 20 range queries(range = 1000)
        System.out.println("(Key searched, Page(Found) / null(Not Found)):");
        for(i = 0; i < numOfSearches; i++) {
            System.out.println(searchKeys[i]+", "+tree.search(searchKeys[i]));								//MultiCounter 4
        }
        System.out.println("Mean number of disk accesses for key search in B+ tree: "+(float)MultiCounter.getCount(4) / numOfSearches);

        System.out.println("(Keys found, Associated values):");
        for(i = 0; i < numOfSearches; i++) {
            System.out.println("Search range "+searchKeys[i]+" - "+(searchKeys[i]+rangeMin));
            tree.searchRange(searchKeys[i], searchKeys[i]+rangeMin);					//MultiCounter 5
        }
        System.out.println("Mean number of comparisons in range searching(10) in B+ tree: "+(float)MultiCounter.getCount(5) / numOfSearches);

        //reset MultiCounter for rangeSearch
        MultiCounter.resetCounter(5);

        System.out.println("(Keys found, Associated values):");
        for(i = 0; i < numOfSearches; i++) {
            System.out.println("Search range "+searchKeys[i]+"- "+(searchKeys[i]+rangeMax));
            tree.searchRange(searchKeys[i], searchKeys[i]+rangeMax);					//MultiCounter 5
        }
        System.out.println("Mean number of comparisons in range searching(1000) in B+ tree: "+(float)MultiCounter.getCount(5) / numOfSearches);

        for(i = 0; i < numOfSearches; i++) {
            tree.delete(searchKeys[i]);													//MultiCounter 6
        }
        System.out.println("Mean number of disk accesses for deletion in B+ tree: "+(float)MultiCounter.getCount(6) / numOfSearches);
    }


    /**
     * Creates an array with distinct numbers between startInt (inclusive)  and endInt (exclusive)
     * @param startInt the lower limit of the numbers created
     * @param endInt the upper limit of the numbers created
     * @param numOfElements the number of numbers created i.e. the size of the array
     * @return int []
     */
    public static int[] intArrayGenerator(int startInt, int endInt, int numOfElements) {
        java.util.Random randomGenerator = new java.util.Random();
        return randomGenerator.ints(startInt, endInt).distinct().limit(numOfElements).toArray();
    }



}
