package tree;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import main.MultiCounter;

/**
 * Basic singleton handling retrieving and storing BTree Nodes to node/index file and Data to data file.
 * @author sk
 *
 */
public class StorageCache {
	private static final String NODE_STORAGE_FILENAME = "plh201_node.bin";
	private static final String DATA_STORAGE_FILENAME = "plh201_data.bin";
	
	private static StorageCache instance;
	
	private static HashMap retrievedNodes = null;
	private static HashMap retrievedData = null;
	
	// make this private so that no one can create instances of this class
	private StorageCache() {
		
	}
	
	protected void cacheNode(int dataPageIndex, BTreeNode node) {
		if (StorageCache.retrievedNodes == null) {
			StorageCache.retrievedNodes = new HashMap();
		}
		StorageCache.retrievedNodes.put(dataPageIndex, node);
	}
	private void cacheData(int dataByteOffset, Data data) {
		if (StorageCache.retrievedData == null) {
			StorageCache.retrievedData = new HashMap();
		}
		StorageCache.retrievedData.put(dataByteOffset, data);
	}
	
	private BTreeNode getNodeFromCache(Integer dataPageIndex) {
		if (StorageCache.retrievedNodes == null) {
			return null;
		}
		
		return (BTreeNode)StorageCache.retrievedNodes.get(dataPageIndex);		//returns null if it doesn't exists
	}
	private Data getDataFromCache(int dataByteOffset) {
		if (StorageCache.retrievedData == null) {
			return null;
		}
		
		return (Data)StorageCache.retrievedData.get(dataByteOffset);		//returns null if it doesn't exists
	}	
	
	public static StorageCache getInstance() {
		if (StorageCache.instance == null) {
			StorageCache.instance = new StorageCache();
		}
		return StorageCache.instance;
	}
	
	public void flush() throws IOException {	//MultiCounter 3
		flushData();		//page where data where written in data file
		flushNodes();	
	}
	
	// checks each node in retrievedNodes whether it is dirty
	// If they are dirty, writes them to disk
	private void flushNodes() throws IOException {	//MultiCounter 3
		BTreeNode node;
		if (StorageCache.retrievedNodes == null)
			return;
		for ( Object dataPageIndex : StorageCache.retrievedNodes.keySet() ) {
			node = (BTreeNode)StorageCache.retrievedNodes.get(dataPageIndex);
			if (node.isDirty()) {
				byte[] byteArray = node.toByteArray();
				RandomAccessFile fl = new RandomAccessFile (getNodeStorageFilename(), "rw");
				fl.seek(BTreeNode.PageSize*(int)dataPageIndex);			//go to page I want
				fl.write(byteArray);
				int lenb = byteArray.length;
				int lenfl = (int)fl.length();
				fl.close();	
				
				// store byteArray to node/index file at byte position dataPageIndex * DATA_PAGE_SIZE				
				// ******************************
				// we just wrote a data page to our file. This is a good location to increase our counter!!!!!
				// ******************************
				MultiCounter.increaseCounter(3);
			}
		}
		
		// reset it
		StorageCache.retrievedNodes = null;	
	}
	
	
	private void flushData() throws IOException {	//MultiCounter 3
		Data data;
		int dataPageIndex;
		if (StorageCache.retrievedData == null)
			return ;
		for ( Object storageByteOffset : StorageCache.retrievedData.keySet() ) {
			data = (Data)StorageCache.retrievedData.get(storageByteOffset);
			if (data.isDirty()) {
				// data.storageByteIndex tells us at which byte offset in the data file this data is stored
				// From this value, and knowing our data page size, we can calculate the dataPageIndex of the data page in the data file
				// This process may result in writing each data page multiple times if it contains multiple dirty Data

				byte[] byteArray = data.toByteArray();
				//dataPageIndex = data.getStorageByteOffset() / BTreeNode.PageSize;
				dataPageIndex = data.getStorageByteOffset() / 64;
				RandomAccessFile fl = new RandomAccessFile (DATA_STORAGE_FILENAME, "rw");
				fl.seek(data.getStorageByteOffset());			//go to page I want
				fl.write(byteArray);
				fl.close();	
				// read datapage given by calculated dataPageIndex from data file
				// copy byteArray to correct position of read bytes
				// store it again to file
				
			
				// ******************************
				// we just wrote a data page to our file. This is a good location to increase our counter!!!!!
				// ******************************
				MultiCounter.increaseCounter(3);
			}
		}
		
		// reset it
		StorageCache.retrievedData = null;
	}
	

	public BTreeNode retrieveNode(Integer dataPageIndex) throws IOException {	//MultiCounter 2
		// if we have this dataPageIndex already in the cache, return it

				// during a range search, we will potentially retrieve a large set of nodes, despite we will use them only once
				// We can optionally add here a case where "large" number of cached, NOT DIRTY (!) nodes, are removed from memory
				if (StorageCache.retrievedNodes != null && StorageCache.retrievedNodes.keySet().size() > 200) { // we do not want to have more than 200 nodes in cache
					BTreeNode node;
					for ( Object key : StorageCache.retrievedNodes.keySet() ) {
						node = (BTreeNode)StorageCache.retrievedNodes.get(dataPageIndex);
						if (!node.isDirty()) {
							StorageCache.retrievedNodes.remove(key);
						}
					}
				}
				
		BTreeNode result = this.getNodeFromCache(dataPageIndex);
		if (result != null || dataPageIndex == null || dataPageIndex == -1) {		//dataPageIndex == null or -1 when node has no parent
			return result;
		}
		
		RandomAccessFile fl = new RandomAccessFile (getNodeStorageFilename(), "r");
		fl.seek(BTreeNode.PageSize*dataPageIndex);			//go to page I want
		byte[] ReadDataPage = new byte[BTreeNode.PageSize];	
		int idtf = fl.readInt();	//read first four bytes -> identifier
		fl.seek(BTreeNode.PageSize*dataPageIndex);	//move again to start of page 
		fl.read(ReadDataPage);	
		if(idtf == BTreeInnerNode.identifier) {	//if it is inner node
			result = new BTreeInnerNode();
			result = result.fromByteArray(ReadDataPage, dataPageIndex);
		}else {		//it is leaf node
			result = new BTreeLeafNode();
			result = result.fromByteArray(ReadDataPage, dataPageIndex);
		}

		fl.close();
		// ******************************
		// we just read a data page from our file. This is a good location to increase our counter!!!!!
		// ******************************
		MultiCounter.increaseCounter(2);
		
		// before returning it, cache it for future reference
		this.cacheNode(dataPageIndex, result);
		
		
		return result;
		
	}
	
	
	
	public Data retrieveData(int dataByteOffset) throws IOException {
		// if we have this dataPageIndex already in the cache, return it
		
		// during a range search, we will potentially retrieve a large set of data, despite we will use them only once
		// We can optionally add here a case where "large" number of cached, NOT DIRTY (!) data, are removed from memory
		if (StorageCache.retrievedData != null && StorageCache.retrievedData.keySet().size() > 100) { // we do not want to have more than 100 data in cache
			Data data;
			for ( Object key : StorageCache.retrievedData.keySet() ) {
				data = (Data)StorageCache.retrievedData.get(dataByteOffset);
				if (!data.isDirty()) {
					StorageCache.retrievedData.remove(key);
				}
			}
		}
		Data result = this.getDataFromCache(dataByteOffset);
		if (result != null) {
			return result;
		}
		
		
		
		RandomAccessFile fl = new RandomAccessFile (DATA_STORAGE_FILENAME, "r");

		fl.seek(dataByteOffset);			//go to page I want
		byte[] ReadDataPage = new byte[BTreeNode.PageSize];	
		fl.read(ReadDataPage);	
		result = new Data();
		result = result.fromByteArray(ReadDataPage, dataByteOffset);

		// ******************************
		// we just read a data page from our file. This is a good location to increase our counter!!!!!
		// ******************************
		
		
		// before returning it, cache it for future reference
		this.cacheData(dataByteOffset, result);
		
		fl.close();
		return result;
		
	}
	
	public BTreeInnerNode newInnerNode() {
		BTreeInnerNode result = new BTreeInnerNode();
		this.acquireNodeStorage(result);
		result.setStorageDataPage(result.getStorageDataPage());////
		result.setDirty();
		this.cacheNode(result.getStorageDataPage(), result);
		return result;
	}
	public BTreeLeafNode newLeafNode() {
		BTreeLeafNode result = new BTreeLeafNode();
		this.acquireNodeStorage(result);
		result.setDirty();
		this.cacheNode(result.getStorageDataPage(), result);
		return result;
	}
	
	// opens our node/index file, calculates the dataPageIndex that corresponds to the end of the file (raf.length()) 
	// and sets it on given node
	private void acquireNodeStorage(BTreeNode node) {
		try {
			int dataPageIndex = 0;
			RandomAccessFile fl = new RandomAccessFile (getNodeStorageFilename(), "rw");

			int len = (int)fl.length();
			dataPageIndex = (len / BTreeNode.PageSize);	//only in creation of tree
			if (len != 0) {
				byte [] emptyByteArray = new byte[BTreeNode.PageSize];
				fl.seek(len);
				fl.write(emptyByteArray);		//write new page to file for subsequent nodes
				//fl.setLength(len + BTreeNode.PageSize);	
			}// open file, get length, and calculate the  dataPageIndex that corresponds to the next data page at the end of the file
			// Actually write DATA_PAGE_LENGTH bytes to the end file, so for that subsequent new nodes the new length is used 
			node.setStorageDataPage(dataPageIndex);
			fl.close();
		}catch(IOException e){
			System.out.println("Error in acquireNodeStorage");
		}
	}
	
	public int newData(Data result, int nextFreeDatafileByteOffset) {
		int NO_OF_DATA_BYTES = 32;
		result.setStorageByteOffset(nextFreeDatafileByteOffset);
		result.setDirty(); // so that it will be written to disk at next flush
		this.cacheData(result.getStorageByteOffset(), result);
		return nextFreeDatafileByteOffset + NO_OF_DATA_BYTES;
	}

	public static String getNodeStorageFilename() {
		return NODE_STORAGE_FILENAME;
	}

	public static String getDataStorageFilename() {
		return DATA_STORAGE_FILENAME;
	}

}
