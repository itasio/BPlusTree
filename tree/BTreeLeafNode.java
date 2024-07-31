package tree;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class BTreeLeafNode<TKey extends Comparable<TKey>, TValue> extends BTreeNode<TKey> {
	protected final static int LEAFORDER = 29;
	// private Object[] values;
	// CHANGE FOR STORING ON FILE
	private Object[] values; // integers pointing to byte offset in data file ///////na ginei Integer[] ??
	protected static int identifier = 0;

	public BTreeLeafNode() {
		this.keys = new Object[LEAFORDER];//
		this.values =  new Object[LEAFORDER];//						//cast se Integer[]???
	}

	@SuppressWarnings("unchecked")
	public TValue getValue(int index) {
		return (TValue)this.values[index];
	}

	public void setValue(int index, TValue value) {
		this.values[index] = (Object)value;
		setDirty(); // we changed a value, so this node is dirty and must be flushed to disk
	}

	@Override
	public TreeNodeType getNodeType() {
		return TreeNodeType.LeafNode;
	}

	@Override
	public int search(TKey key) {
		for (int i = 0; i < this.getKeyCount(); ++i) {
			int cmp = this.getKey(i).compareTo(key);
			if (cmp == 0) {
				return i;
			}
			else if (cmp > 0) {
				return -1;
			}
		}

		return -1;
	}


	/* The codes below are used to support insertion operation */

	public void insertKey(TKey key, TValue value) {
		int index = 0;
		while (index < this.getKeyCount() && this.getKey(index).compareTo(key) < 0)
			++index;
		this.insertAt(index, key, value);
	}

	private void insertAt(int index, TKey key, TValue value) {
		// move space for the new key
		for (int i = this.getKeyCount() - 1; i >= index; --i) {
			this.setKey(i + 1, this.getKey(i));
			this.setValue(i + 1, this.getValue(i));
		}

		// insert new key and value
		this.setKey(index, key);
		this.setValue(index, value);
		// setDirty() will be called in setKey/setValue
		++this.keyCount;
	}


	/**
	 * When splits a leaf node, the middle key is kept on new node and be pushed to parent node.
	 */
	@Override
	protected BTreeNode<TKey> split() {
		int midIndex = this.getKeyCount() / 2;

		BTreeLeafNode<TKey, TValue> newRNode = StorageCache.getInstance().newLeafNode();
		for (int i = midIndex; i < this.getKeyCount(); ++i) {
			newRNode.setKey(i - midIndex, this.getKey(i));
			newRNode.setValue(i - midIndex, this.getValue(i));
			this.setKey(i, null);
			this.setValue(i, null);
		}
		newRNode.keyCount = this.getKeyCount() - midIndex;
		this.keyCount = midIndex;
		setDirty();// just to make sure

		return newRNode;
	}

	@Override
	protected BTreeNode<TKey> pushUpKey(TKey key, BTreeNode<TKey> leftChild, BTreeNode<TKey> rightNode) {
		throw new UnsupportedOperationException();
	}




	/* The codes below are used to support deletion operation */

	public boolean delete(TKey key) {
		int index = this.search(key);
		if (index == -1)
			return false;

		this.deleteAt(index);
		return true;
	}

	private void deleteAt(int index) {
		int i = index;
		for (i = index; i < this.getKeyCount() - 1; ++i) {
			this.setKey(i, this.getKey(i + 1));
			this.setValue(i, this.getValue(i + 1));
		}
		this.setKey(i, null);
		this.setValue(i, null);
		--this.keyCount;

		// setDirty will be called through setKey/setValue
	}

	@Override
	protected void processChildrenTransfer(BTreeNode<TKey> borrower, BTreeNode<TKey> lender, int borrowIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected BTreeNode<TKey> processChildrenFusion(BTreeNode<TKey> leftChild, BTreeNode<TKey> rightChild) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Notice that the key sunk from parent is be abandoned.
	 * @throws IOException
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void fusionWithSibling(TKey sinkKey, BTreeNode<TKey> rightSibling) throws IOException {	//Multicounter 2
		BTreeLeafNode<TKey, TValue> siblingLeaf = (BTreeLeafNode<TKey, TValue>)rightSibling;

		int j = this.getKeyCount();
		for (int i = 0; i < siblingLeaf.getKeyCount(); ++i) {
			this.setKey(j + i, siblingLeaf.getKey(i));
			this.setValue(j + i, siblingLeaf.getValue(i));
		}
		this.keyCount += siblingLeaf.getKeyCount();

		this.setRightSibling(siblingLeaf.getRightSibling());		//Multicounter 2
		if (siblingLeaf.rightSibling != null)
			siblingLeaf.getRightSibling().setLeftSibling(this);		//Multicounter 2
	}

	@Override
	@SuppressWarnings("unchecked")
	protected TKey transferFromSibling(TKey sinkKey, BTreeNode<TKey> sibling, int borrowIndex) {
		BTreeLeafNode<TKey, TValue> siblingNode = (BTreeLeafNode<TKey, TValue>)sibling;

		this.insertKey(siblingNode.getKey(borrowIndex), siblingNode.getValue(borrowIndex));
		siblingNode.deleteAt(borrowIndex);
		// setDirty will be called through setKey/setValue in deleteAt
		return borrowIndex == 0 ? sibling.getKey(0) : this.getKey(0);
	}

	protected byte[] toByteArray() throws IOException {
		// very similar to BTreeInnerNode. Instead of pointers to children (offset to our data pages in our node file), we have pointers
		// to data (byte offset to our data file)
		ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
		DataOutputStream out = new DataOutputStream(bos);
		out.writeInt(BTreeLeafNode.identifier);	//0 for leaf node
		out.writeInt((this.parentNode == null) ? -1 : this.parentNode);	//-1 if node has no parent
		out.writeInt((this.leftSibling == null) ? -1 : this.leftSibling);
		out.writeInt((this.rightSibling == null) ? -1 : this.rightSibling);
		out.writeInt(this.keyCount);
		for(int i = 0; i < this.keyCount; i++) {
			if(this.values[i] instanceof Integer)	//value is Integer when key was already in the tree
				out.writeInt((int)this.values[i]);
			else {
				Data dt = (Data)this.values[i];		//value is Data when we insert key in tree
				out.writeInt(dt.getStorageByteOffset());
			}
			//out.writeInt(dt.gatDataValues()[i]);
			//out.writeInt((int)this.values[i]);		//write values
			out.writeInt((int)this.keys[i]);		//write keys
		}
		out.close();

		byte []byteArray = bos.toByteArray();		//maybe< pagesize
		byte[] byteArray2return = new byte[PageSize];//////////////////anapoda
		System.arraycopy(byteArray, 0, byteArray2return, 0, byteArray.length);
		bos.close();

		return byteArray2return;
		//byteArray [] = identifier, parent, leftSib, rightSib, keysCount,  value, key consequently(last element key)
	}
	protected BTreeLeafNode<TKey, TValue> fromByteArray(byte[] byteArray, int dataPageOffset) {
		// this takes a byte array of fixed size, and transforms it to a BTreeLeafNode
		// it takes the format we store our node (as specified in toByteArray()) and constructs the BTreeLeafNode
		// We need as parameter the dataPageOffset in order to set it
		this.setStorageDataPage(dataPageOffset);
		ByteConverter bc = new ByteConverter();
		int[]intArray = bc.byteArrayToIntArray(byteArray);
		//now created int [] from byte []| int[] = {identifier, Parent, LeftSib, RightSib, keysCount,value, key consequently}
		this.parentNode = intArray[1];
		this.leftSibling = intArray[2];
		this.rightSibling = intArray[3];
		this.keyCount = intArray[4];
		int j = 5;
		for(int i=0; i<this.keyCount; i++) {

			this.values[i] = intArray[j];
			this.keys[i] =  intArray[j+1];
			j +=2;
		}
		return this;
	}
}
