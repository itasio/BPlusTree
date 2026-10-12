package tree;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Contains our data. It is of fixed byte array size for writing to or reading to the data file
 * @author sk
 *
 */
public class Data {
	private int storageByteOffset; // this node is stored at byte index storageByteOffset in the data file. We must calculate the datapage this corresponds to in order to read or write it
	private int [] data = new int[8];
	private static int size = 8;
	private boolean dirty;
	
	public Data() {
	}
	public Data(int [] data) {
        System.arraycopy(data, 0, this.data, 0, data.length);
	}
	
	public Data(int a, int b, int c, int d,int e, int f, int g, int h) {
		this.data[0] =a;
		this.data[1] =b;
		this.data[2] =c;
		this.data[3] =d;
		this.data[4] =e;
		this.data[5] =f;
		this.data[6] =g;
		this.data[7] =h;
	}
	public int[] gatDataValues() {
		return this.data;
	}
	public boolean isDirty() {
		return this.dirty;
	}
	public void setDirty() {
		this.dirty = true;
	}
	public void setStorageByteOffset(int storageByteOffset) {
		this.storageByteOffset = storageByteOffset;
	}
	public int getStorageByteOffset() {
		return this.storageByteOffset;
	}
	
	@Override
	public String toString() {
		
		return "data1: "+data[0]+", data2: "+data[1]+", data3: "+data[2]+", data4: "+data[3]+",data5: "+data[4]+", data6: "+data[5]+", data7: "+data[6]+", data8: "+data[7];
	}
	

	/* takes a Data class, and transforms it to an array of bytes 
	  we can't store it as is to the file. We must calculate the data page based on storageByteIndex, load the datapage, replace
	  the part starting from storageByteIndex, and then store the data page back to the file
	  */ 
	
	
	protected byte[] toByteArray() throws IOException {
		byte[] byteArray;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
    	DataOutputStream out = new DataOutputStream(bos);
        for (int elem : data) {
            out.writeInt(elem);
        }
    	out.close();
    	byteArray = bos.toByteArray();
    	bos.close();
		return byteArray;
		
	}

	
	/* 
	 this takes a byte array of fixed size, and transforms it to a Data class instance
	 it takes the format we store our Data (as specified in toByteArray()) and constructs the Data
	 We need as parameter the storageByteIndex in order to set it
	 */
	protected Data fromByteArray(byte[] byteArray, int storageByteOffset) {
		Data result = new Data(); // 1,2,3,4 will be your data extracted from the byte array
		result.setStorageByteOffset(storageByteOffset);
		ByteConverter bc = new ByteConverter();
		result.data = bc.byteArrayToIntArray(byteArray);
		return result;
	}
	public static int getSize() {
		return size;
	}
	public static void setSize(int size) {
		Data.size = size;
	}
}
