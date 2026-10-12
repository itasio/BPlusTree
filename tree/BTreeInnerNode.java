package tree;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class BTreeInnerNode<TKey extends Comparable<TKey>> extends BTreeNode<TKey> {
	protected final static int INNERORDER = 29;
	// protected Object[] children;
	// CHANGE FOR STORING ON FILE
	protected Integer[] children;
	protected static int identifier = 1;

	public BTreeInnerNode() {
		this.keys = new Object[INNERORDER];
		//this.children = new Object[INNERORDER + 2];
		// CHANGE FOR STORING ON FILE
		this.children = new Integer[INNERORDER + 1];
	}

	@SuppressWarnings("unchecked")
	public BTreeNode<TKey> getChild(int index) throws IOException {	//Multicounter 2
//		return (BTreeNode<TKey>)this.children[index];
		// CHANGE FOR STORING ON FILE
		return (BTreeNode<TKey>)StorageCache.getInstance().retrieveNode(this.children[index]);
	}

	public void setChild(int index, BTreeNode<TKey> child) {
//		this.children[index] = child;
		// CHANGE FOR STORING ON FILE
		this.children[index] = null;
		if (child != null) {
			child.setParent(this);
			this.children[index] = child.getStorageDataPage();
		}
		setDirty();
	}

	@Override
	public TreeNodeType getNodeType() {
		return TreeNodeType.InnerNode;
	}

	@Override
	public int search(TKey key) {
		int index;
		for (index = 0; index < this.getKeyCount(); ++index) {
			int cmp = this.getKey(index).compareTo(key);
			if (cmp == 0) {
				return index + 1;
			}
			else if (cmp > 0) {
				return index;
			}
		}

		return index;
	}


	/* The codes below are used to support insertion operation */

	private void insertAt(int index, TKey key, BTreeNode<TKey> leftChild, BTreeNode<TKey> rightChild) throws IOException {
		// move space for the new key
		for (int i = this.getKeyCount() + 1; i > index; --i) {
			this.setChild(i, this.getChild(i - 1));
		}
		for (int i = this.getKeyCount(); i > index; --i) {
			this.setKey(i, this.getKey(i - 1));
		}

		// insert the new key
		this.setKey(index, key);
		this.setChild(index, leftChild);
		this.setChild(index + 1, rightChild);
		this.keyCount += 1;

	}

	/**
	 * When splits an internal node, the middle key is kicked out and be pushed to parent node.
	 *
	 */
	@Override
	protected BTreeNode<TKey> split() throws IOException {
		int midIndex = this.getKeyCount() / 2;

		BTreeInnerNode<TKey> newRNode = StorageCache.getInstance().newInnerNode();///
		for (int i = midIndex + 1; i < this.getKeyCount(); ++i) {
			newRNode.setKey(i - midIndex - 1, this.getKey(i));
			this.setKey(i, null);
		}
		for (int i = midIndex + 1; i <= this.getKeyCount(); ++i) {
			newRNode.setChild(i - midIndex - 1, this.getChild(i));
			newRNode.getChild(i - midIndex - 1).setParent(newRNode);
			this.setChild(i, null);
		}
		this.setKey(midIndex, null);
		newRNode.keyCount = this.getKeyCount() - midIndex - 1;
		this.keyCount = midIndex;
		setDirty();
		return newRNode;
	}

	@Override
	protected BTreeNode<TKey> pushUpKey(TKey key, BTreeNode<TKey> leftChild, BTreeNode<TKey> rightNode) throws IOException {
		// find the target position of the new key
		int index = this.search(key);

		// insert the new key
		this.insertAt(index, key, leftChild, rightNode);

		// check whether current node need to be split
		if (this.isOverflow()) {
			return this.dealOverflow();
		}
		else {
			return this.getParent() == null ? this : null;
		}
	}




	/* The codes below are used to support delete operation */

	private void deleteAt(int index) throws IOException {		//Multicounter 2
		int i;
		for (i = index; i < this.getKeyCount() - 1; ++i) {
			this.setKey(i, this.getKey(i + 1));
			this.setChild(i + 1, this.getChild(i + 2));			//Multicounter 2
		}
		this.setKey(i, null);
		this.setChild(i + 1, null);
		--this.keyCount;
		setDirty();
	}


	@Override
	protected void processChildrenTransfer(BTreeNode<TKey> borrower, BTreeNode<TKey> lender, int borrowIndex) throws IOException {
		//Multicounter 2
		int borrowerChildIndex = 0;
		while (borrowerChildIndex < this.getKeyCount() + 1 && this.getChild(borrowerChildIndex) != borrower)		//Multicounter 2
			++borrowerChildIndex;

		if (borrowIndex == 0) {
			// borrow a key from right sibling
			TKey upKey = borrower.transferFromSibling(this.getKey(borrowerChildIndex), lender, borrowIndex);		//Multicounter 2
			this.setKey(borrowerChildIndex, upKey);
		}
		else {
			// borrow a key from left sibling
			TKey upKey = borrower.transferFromSibling(this.getKey(borrowerChildIndex - 1), lender, borrowIndex);	//Multicounter 2
			this.setKey(borrowerChildIndex - 1, upKey);
		}
	}

	@Override
	protected BTreeNode<TKey> processChildrenFusion(BTreeNode<TKey> leftChild, BTreeNode<TKey> rightChild) throws IOException {
		//Multicounter 2
		int index = 0;
		while (index < this.getKeyCount() && this.getChild(index) != leftChild)			//Multicounter 2
			++index;
		TKey sinkKey = this.getKey(index);

		// merge two children and the sink key into the left child node
		leftChild.fusionWithSibling(sinkKey, rightChild);								//Multicounter 2

		// remove the sink key, keep the left child and abandon the right child
		this.deleteAt(index);															//Multicounter 2

		// check whether is needed to propagate borrow or fusion to parent
		if (this.isUnderflow()) {
			if (this.getParent() == null) {												//Multicounter 2
				// current node is root, only remove keys or delete the whole root node
				if (this.getKeyCount() == 0) {
					leftChild.setParent(null);
					return leftChild;
				}
				else {
					return null;
				}
			}

			return this.dealUnderflow();												//Multicounter 2
		}

		return null;
	}


	@Override
	protected void fusionWithSibling(TKey sinkKey, BTreeNode<TKey> rightSibling) throws IOException {//Multicounter 2
		BTreeInnerNode<TKey> rightSiblingNode = (BTreeInnerNode<TKey>)rightSibling;

		int j = this.getKeyCount();
		this.setKey(j++, sinkKey);

		for (int i = 0; i < rightSiblingNode.getKeyCount(); ++i) {
			this.setKey(j + i, rightSiblingNode.getKey(i));
		}
		for (int i = 0; i < rightSiblingNode.getKeyCount() + 1; ++i) {
			this.setChild(j + i, rightSiblingNode.getChild(i));				//Multicounter 2
		}
		this.keyCount += 1 + rightSiblingNode.getKeyCount();

		this.setRightSibling(rightSiblingNode.getRightSibling());			//Multicounter 2
		if (rightSiblingNode.rightSibling != null)
			rightSiblingNode.getRightSibling().setLeftSibling(this);		//Multicounter 2
	}

	@Override
	protected TKey transferFromSibling(TKey sinkKey, BTreeNode<TKey> sibling, int borrowIndex) throws IOException {	//Multicounter 2
		BTreeInnerNode<TKey> siblingNode = (BTreeInnerNode<TKey>)sibling;

		TKey upKey;
		if (borrowIndex == 0) {
			// borrow the first key from right sibling, append it to tail
			int index = this.getKeyCount();
			this.setKey(index, sinkKey);
			this.setChild(index + 1, siblingNode.getChild(borrowIndex));	//Multicounter 2
			this.keyCount += 1;

			upKey = siblingNode.getKey(0);
			siblingNode.deleteAt(borrowIndex);
		}
		else {
			// borrow the last key from left sibling, insert it to head
			this.insertAt(0, sinkKey, siblingNode.getChild(borrowIndex + 1), this.getChild(0));
			upKey = siblingNode.getKey(borrowIndex);
			siblingNode.deleteAt(borrowIndex);			//Multicounter 2
		}
		setDirty();
		return upKey;
	}

	protected byte[] toByteArray() throws IOException {
		// must include the index of the data page to the left sibling (int == 4 bytes), to the right sibling,
		// to the parent node, the number of keys (keyCount), the type of node (inner node/leaf node) and the list of keys and list of children (each key 4 byte int, each children 4 byte int pointing to the data page offeset)
		// We do not need the isDirty flag and the storageDataPage,
		// so we need
		// 4 bytes for marking this as an inner node (e.g. an int with value = 1 for inner node and 2 for leaf node)
		// 4 bytes for left sibling
		// 4 bytes for right sibling
		// 4 bytes for parent
		// 4 bytes for the number of keys
		// The rest in our data page are for the list of pointers to children, and the keys. Depending on the size of our data page
		// we can calculate the order our tree
		ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
		DataOutputStream out = new DataOutputStream(bos);
		out.writeInt(BTreeInnerNode.identifier);	//1 for inner node
		out.writeInt((this.parentNode == null) ? -1 : this.parentNode);	//-1 if node has no parent
		out.writeInt((this.leftSibling == null) ? -1 : this.leftSibling);
		out.writeInt((this.rightSibling == null) ? -1 : this.rightSibling);
		out.writeInt(this.keyCount);
		for(int i = 0; i < this.keyCount; i++) {
			out.writeInt(this.children[i]);		//write children
			out.writeInt((int)this.keys[i]);		//write keys
		}

		out.writeInt(this.children[this.keyCount]);		//write last child to output stream
		out.close();
		byte [] byteArray = bos.toByteArray();
		byte[] byteArray2return = new byte[PageSize];
		System.arraycopy(byteArray, 0, byteArray2return, 0, byteArray.length);
		bos.close();

		return byteArray2return;
		//byteArray [] = identifier, parent, leftSib, rightSib, keysCount,  child, key consequently(last element child)
	}
	protected BTreeInnerNode<TKey> fromByteArray(byte[] byteArray, int dataPageOffset) {
		// this takes a byte array of fixed size, and transforms it to a BTreeInnerNode
		// it takes the format we store our node (as specified in BTreeInnerNode.toByteArray()) and constructs the BTreeInnerNode
		// We need as parameter the dataPageOffset in order to set it
		this.setStorageDataPage(dataPageOffset);
		ByteConverter bc = new ByteConverter();
		int[]intArray = bc.byteArrayToIntArray(byteArray);


		//now created int [] from byte []| int[] = {identifier, Parent, LeftSib, RightSib, keysCount,child, key consequently}
		this.parentNode = intArray[1];
		this.leftSibling = intArray[2];
		this.rightSibling = intArray[3];
		this.keyCount = intArray[4];
		int j = 5;
		for(int i=0; i<this.keyCount ; i++) {
			this.children[i] = intArray[j];
			this.keys[i] =  intArray[j+1];
			j+=2;
		}
		this.children[this.keyCount] = intArray[j];	//insert last child
		return this;
	}
}