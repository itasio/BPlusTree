package tree;
import java.io.IOException;
import main.MultiCounter;

/**
 * Original at <a href="https://github.com/linli2016/BPlusTree">...</a>
 * A B+ tree
 * Since the structures and behaviors between internal node and external node are different,
 * so there are two different classes for each kind of node.
 * @param <TKey> the data type of the key
 * @param <TValue> the data type of the value
 */
public class BTree<TKey extends Comparable<TKey>, TValue> {
	private BTreeNode<TKey> root;


	private int nextFreeDatafileByteOffset = 0; // for this assignment, we only create new, empty files. We keep here the next free byteoffset in our file

	public BTree() throws IOException {
		// this.root = new BTreeLeafNode<TKey, TValue>();
		// CHANGE FOR STORING ON FILE
		this.root = StorageCache.getInstance().newLeafNode();
		StorageCache.getInstance().flush();
	}

	/**
	 * Insert a new key and its associated value into the B+ tree.
	 *
	 */
	public void insert(TKey key, TValue value) throws IOException {		//Multicounter 1
		//in every insertion Multicounters 2, 3 are increased through retrieveNode(), flush() respectively
		//after each insertion Multicounter 1 = disk accesses, 2,3 are reseted
		// CHANGE FOR STORING ON FILE
		nextFreeDatafileByteOffset = StorageCache.getInstance().newData((Data)value, nextFreeDatafileByteOffset);

		// CHANGE FOR STORING ON FILE
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);	//Multicounter 2 (retrieveNode())
		leaf.insertKey(key, value);

		if (leaf.isOverflow()) {
			BTreeNode<TKey> n = leaf.dealOverflow();		//Multicounter 2 (retrieveNode())
			if (n != null)
				this.root = n;
		}
		StorageCache.getInstance().cacheNode(leaf.getStorageDataPage(), leaf);//I changed that

		// CHANGE FOR STORING ON FILE
		StorageCache.getInstance().flush();	//MultiCounter 3

		MultiCounter.increaseCounter(1, MultiCounter.getCount(2));
		MultiCounter.increaseCounter(1, MultiCounter.getCount(3));
		MultiCounter.resetCounter(2);
		MultiCounter.resetCounter(3);


	}

	/**
	 * Search a key value on the tree and return its associated value.
	 *
	 */
	public TValue search(TKey key) throws IOException {			//Multicounter 4
		//in every search Multicounter 2 is increased through retrieveNode()
		//after each search Multicounter 4 = disk accesses, 2 is reseted
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);	//Multicounter 2 (retrieveNode())

		int index = leaf.search(key);
		MultiCounter.increaseCounter(4, MultiCounter.getCount(2));
		MultiCounter.resetCounter(2);
		return (index == -1) ? null : leaf.getValue(index);
	}

	/**
	 * Search a range of int key values on the tree and prints their associated values.
	 *
	 */

	public void searchRange(TKey keyLow, TKey keyHigh) throws IOException{		//MultiCounter 5
		//in every searchRange Multicounter 2 is increased through retrieveNode()
		//after each search Multicounter 5 = disk accesses, 2 is reseted

//		if((int)keyLow > (int)keyHigh) {
		if(keyLow.compareTo(keyHigh) > 0){
			System.out.println("Minimum key must be greater than maximum key");
			return;
		}
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(keyLow);	//MultiCounter 2

		int index = leaf.search(keyLow);
		//System.out.println("(Keys found, Associated values):");
		if(index != -1) {	//if min key is found

//			while(leaf != null && (int)leaf.getKey(index) <= (int)keyHigh) {	//cross leaf nodes
			while(leaf != null && leaf.getKey(index).compareTo(keyHigh) <= 0) {	//cross leaf nodes
				System.out.print("("+leaf.getKey(index)+", "+leaf.getValue(index)+")"+" ");
				if(leaf.getKey(index) == keyHigh)		//we don't have to search another node,
					//thus saving disk accesses
					break;
				index++;
				if(index >= leaf.keyCount) {	//I reached end of node, now I must move to rightSibling
					leaf = (BTreeLeafNode<TKey, TValue>) leaf.getRightSibling();		//MultiCounter 2
					index = 0;
				}
			}
		}else {		//keyLow not found
			//cross every leaf node to check if there are keys within range beginning from the first key of leaf
			index = 0;
//			while(leaf != null && (int)leaf.getKey(index) <= (int)keyHigh) {
//				if((int)leaf.getKey(index) >= (int)keyLow && (int)leaf.getKey(index) <= (int)keyHigh) {

			while(leaf != null && leaf.getKey(index).compareTo(keyHigh) <= 0) {
				if(leaf.getKey(index).compareTo(keyLow) >= 0 && leaf.getKey(index).compareTo(keyHigh) <= 0) {
					System.out.print("("+leaf.getKey(index)+", "+leaf.getValue(index)+")"+" ");
				}
				if(leaf.getKey(index) == keyHigh)		//we don't have to search another node,
					//thus saving disk accesses
					break;
				index++;
				if(index >= leaf.keyCount) {	//I reached end of node, now I must move to rightSibling
					leaf = (BTreeLeafNode<TKey, TValue>) leaf.getRightSibling();		//MultiCounter 2
					index = 0;
				}
			}
		}
		MultiCounter.increaseCounter(5, MultiCounter.getCount(2));
		MultiCounter.resetCounter(2);
		System.out.println();
	}



	/**
	 * Delete a key and its associated value from the tree.
	 *
	 */
	public void delete(TKey key) throws IOException {		//Multicounter 6
		//in every deletion Multicounter 2 is increased through retrieveNode() being called from other methods
		//after each search Multicounter 6 = disk accesses, 2 is reseted
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);	//Multicounter 2

		if (leaf.delete(key) && leaf.isUnderflow()) {
			BTreeNode<TKey> n = leaf.dealUnderflow();		//Multicounter 2
			if (n != null)
				this.root = n;
		}
		// CHANGE FOR STORING ON FILE
		StorageCache.getInstance().flush();		//Multicounter 2
		MultiCounter.increaseCounter(6, MultiCounter.getCount(2));
		MultiCounter.resetCounter(2);
	}

	/**
	 * Search the leaf node which should contain the specified key
	 *
	 */
	private BTreeLeafNode<TKey, TValue> findLeafNodeShouldContainKey(TKey key) throws IOException {	//Multicounter 2
		BTreeNode<TKey> node = this.root;

		while (node.getNodeType() == TreeNodeType.InnerNode) {
			node = ((BTreeInnerNode<TKey>)node).getChild( node.search(key) );
		}

		return (BTreeLeafNode<TKey, TValue>)node;
	}
}
