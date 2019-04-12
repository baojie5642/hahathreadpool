package com.baojie.zk.example.concurrent.file;

import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author daofeng.xjf
 */
public class DirectIOTestMain {

    public static DirectIOLib directIOLib = DirectIOLib.getLibForPath("/");

    private static final int BLOCK_SIZE = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        DirectRandomAccessFile reader = null;
        DirectRandomAccessFile writer = null;

        for(int k=0;k<10;k++){
            ByteBuffer byteBuffer = DirectIOUtils.allocateForDirectIO(directIOLib, 640 * BLOCK_SIZE);
             reader = new DirectRandomAccessFile(
                    new File("/home/baojie/liuxin/source/big_cow/test_4_mapped.zip"), "r");
             writer = new DirectRandomAccessFile(
                    new File("/home/baojie/liuxin/source/small_cow/test_4_mapped.zip"), "rw");
            final long size = reader.length();
            long has = 0L;
            long start = System.currentTimeMillis();
            for (; ; ) {
                if (has >= size) {
                    break;
                }
                int tmp=reader.read(byteBuffer, has);
                byteBuffer.flip();
                writer.write(byteBuffer, has);
                has = has + tmp;

            }
            byteBuffer.flip();
            writer.write(byteBuffer, has);
            System.out.println("Spend: " + (System.currentTimeMillis() - start) + "ms");
            reader.close();
            writer.close();
            Files.deleteIfExists(Paths.get("/home/baojie/liuxin/source/small_cow/test_4_mapped.zip"));
        }

    }

    private static void write() throws IOException {
        if (DirectIOLib.isBinit()) {
            ByteBuffer byteBuffer = DirectIOUtils.allocateForDirectIO(directIOLib, 4 * BLOCK_SIZE);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                byteBuffer.putInt(i);
            }
            byteBuffer.flip();
            DirectRandomAccessFile directRandomAccessFile = new DirectRandomAccessFile(
                    new File("/home/baojie/liuxin/database.data"), "rw");
            directRandomAccessFile.write(byteBuffer, 0);
            directRandomAccessFile.close();
        } else {
            throw new RuntimeException("your system do not support direct io");
        }
    }

    public static void read() throws IOException {
        if (DirectIOLib.isBinit()) {
            ByteBuffer byteBuffer = DirectIOUtils.allocateForDirectIO(directIOLib, 4 * BLOCK_SIZE);
            DirectRandomAccessFile directRandomAccessFile = new DirectRandomAccessFile(
                    new File("/home/baojie/liuxin/database.data"), "rw");
            directRandomAccessFile.read(byteBuffer, 0);
            byteBuffer.flip();
            for (int i = 0; i < BLOCK_SIZE; i++) {
                System.out.print(byteBuffer.getInt() + " ");
            }
            directRandomAccessFile.close();
        } else {
            throw new RuntimeException("your system do not support direct io");
        }
    }

    /**
     * @param input
     * @return
     */
    public String test(String input) {
        return null;
    }

}
