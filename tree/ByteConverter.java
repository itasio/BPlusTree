package tree;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 * A file converter converts byte arrays to int arrays and vice versa
 */
public class ByteConverter {

	public ByteConverter() {
		
	}
	
	/**
	 * takes input a byte array and converts it to int array
	 */
	public int [] byteArrayToIntArray(byte [] byteArray) {
		int shiftBits;
	    int byteNum = 0;
	    int[] dstInt = new int[byteArray.length/4]; //you might have to hard code the array length

	    //Convert array of source bytes (srcByte) into array of integers (dstInt)
	    for (int intNum = 0; intNum < byteArray.length/4; ++intNum) {  //for the four integers
	        dstInt[intNum] = 0;                                      //Start with the integer = 0

	        for(shiftBits = 24; shiftBits >= 0; shiftBits -= 8) { //Add in each data byte, highest first
	        	dstInt[intNum] |= (byteArray[byteNum++] & 0xFF) << shiftBits;
	        }
	    }
	return dstInt;
	}

	
	/**
	 * takes input an int array and converts it to byte array
	 */
	public byte [] intArrayToByteArray(int [] intArray) {
		try {
			byte [] byteArray;
	    	
			ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
	    	DataOutputStream out = new DataOutputStream(bos);
            for (int j : intArray) {
                out.writeInt(j);
            }
	    	out.close();
	    	byteArray = bos.toByteArray();
	    	bos.close();
	    	return byteArray;
		}catch (IOException e) {
				System.out.println("An error occured");
				return null;
		}
	}
}
