package tree;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Original at https://github.com/linli2016/BPlusTree
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
	 * @throws IOException
	 */
	public void insert(TKey key, TValue value) throws IOException {
		// CHANGE FOR STORING ON FILE
		nextFreeDatafileByteOffset = StorageCache.getInstance().newData((Data)value, nextFreeDatafileByteOffset);

		// CHANGE FOR STORING ON FILE
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);
		leaf.insertKey(key, value);

		if (leaf.isOverflow()) {
			BTreeNode<TKey> n = leaf.dealOverflow();
			if (n != null)
				this.root = n;
		}
		StorageCache.getInstance().cacheNode(leaf.getStorageDataPage(), leaf);//i changed that

		// CHANGE FOR STORING ON FILE
		StorageCache.getInstance().flush();
	}

	/**
	 * Search a key value on the tree and return its associated value.
	 * @throws IOException
	 */
	public TValue search(TKey key) throws IOException {
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);

		int index = leaf.search(key);
		return (index == -1) ? null : leaf.getValue(index);
	}

	/**
	 * Search a range of int key values on the tree and prints their associated values.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void searchRange(TKey keyLow, TKey keyHigh) throws IOException{
		if((int)keyLow > (int)keyHigh) {
			System.out.println("Minimum key must be greater than maximum key");
			return;
		}
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(keyLow);

		int index = leaf.search(keyLow);
		System.out.println("(Keys found, Associated values):");
		if(index != -1) {	//if min key is found

			while(leaf != null && (int)leaf.getKey(index) <= (int)keyHigh) {	//cross leaf nodes
				System.out.print("("+leaf.getKey(index)+", "+leaf.getValue(index)+")"+" ");
				if(leaf.getKey(index) == keyHigh)		//we don't have to search another node,
					//thus saving disk accesses
					break;
				index++;
				if(index < leaf.keyCount) {	//i haven't reached end of node yet
					continue;
				}else {		//I reached end of node, now i must move to rightSibling
					leaf = (BTreeLeafNode<TKey, TValue>) leaf.getRightSibling();
					index = 0;
				}
			}
		}else {		//keyLow not found
			//cross every leaf node to check if there are keys within range beginning from the first key of leaf
			index = 0;
			while(leaf != null && (int)leaf.getKey(index) <= (int)keyHigh) {
				if((int)leaf.getKey(index) >= (int)keyLow && (int)leaf.getKey(index) <= (int)keyHigh) {
					System.out.print("("+leaf.getKey(index)+", "+leaf.getValue(index)+")"+" ");
				}
				if(leaf.getKey(index) == keyHigh)		//we don't have to search another node,
					//thus saving disk accesses
					break;
				index++;
				if(index < leaf.keyCount) {	//i haven't reached end of node yet
					continue;
				}else {		//I reached end of node, now i must move to rightSibling
					leaf = (BTreeLeafNode<TKey, TValue>) leaf.getRightSibling();
					index = 0;
				}
			}
		}
		System.out.println();
	}



	/**
	 * Delete a key and its associated value from the tree.
	 * @throws IOException
	 */
	public void delete(TKey key) throws IOException {
		BTreeLeafNode<TKey, TValue> leaf = this.findLeafNodeShouldContainKey(key);

		if (leaf.delete(key) && leaf.isUnderflow()) {
			BTreeNode<TKey> n = leaf.dealUnderflow();
			if (n != null)
				this.root = n;
		}
		// CHANGE FOR STORING ON FILE
		StorageCache.getInstance().flush();
	}

	/**
	 * Search the leaf node which should contain the specified key
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private BTreeLeafNode<TKey, TValue> findLeafNodeShouldContainKey(TKey key) throws IOException {
		BTreeNode<TKey> node = this.root;

		while (node.getNodeType() == TreeNodeType.InnerNode) {
			node = ((BTreeInnerNode<TKey>)node).getChild( node.search(key) );
		}

		return (BTreeLeafNode<TKey, TValue>)node;
	}
}
