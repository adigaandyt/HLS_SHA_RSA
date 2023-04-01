package il.ac.kinneret.mjmay.hls.hlsjava.model;

import java.nio.ByteBuffer;
/**
 * Class to combine byte arrays
 * @author Andy Thaok
 * @version 1.0
 */
public class ArrayEditor {

    //maybe make it all into a single function with bytestream
    //5
    public static byte[] combine(byte[] ar1,byte[] ar2,byte[] ar3,byte[] ar4,byte[] ar5){
        ByteBuffer byteBuffer = ByteBuffer.allocate(ar1.length + ar2.length + ar3.length + ar4.length+ar5.length);
        byteBuffer.put(ar1);
        byteBuffer.put(ar2);
        byteBuffer.put(ar3);
        byteBuffer.put(ar4);
        byteBuffer.put(ar5);
        return byteBuffer.array();
    }
    //4
    public static byte[] combine(byte[] ar1,byte[] ar2,byte[] ar3,byte[] ar4){
        ByteBuffer byteBuffer = ByteBuffer.allocate(ar1.length + ar2.length + ar3.length + ar4.length);
        byteBuffer.put(ar1);
        byteBuffer.put(ar2);
        byteBuffer.put(ar3);
        byteBuffer.put(ar4);
        return byteBuffer.array();
    }
    //3
    public static byte[] combine(byte[] ar1,byte[] ar2,byte[] ar3){
        ByteBuffer byteBuffer = ByteBuffer.allocate(ar1.length + ar2.length + ar3.length);
        byteBuffer.put(ar1);
        byteBuffer.put(ar2);
        byteBuffer.put(ar3);
        return byteBuffer.array();
    }
    //2
    public static byte[] combine(byte[] ar1,byte[] ar2){
        ByteBuffer byteBuffer = ByteBuffer.allocate(ar1.length + ar2.length);
        byteBuffer.put(ar1);
        byteBuffer.put(ar2);
        return byteBuffer.array();
    }
}
