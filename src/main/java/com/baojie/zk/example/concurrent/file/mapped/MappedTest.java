package com.baojie.zk.example.concurrent.file.mapped;


import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class MappedTest {


    public void copyFile(String filename, String srcpath, String destpath) throws IOException {
        File source = new File(srcpath + "/" + filename);
        File dest = new File(destpath + "/" + filename);
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
            //buf.force();// 将此缓冲区所做的内容更改强制写入包含映射文件的存储设备中

            in.close();
            out.close();
            //clean(buf);
            Files.deleteIfExists(Paths.get(srcpath + "/" + filename));

            boolean deleSuc = source.delete();//文件复制完成后，删除源文件
            if (deleSuc) {
                System.out.println("delete source file suc");
            } else {
                System.out.println("delete source file fail");
                clean(buf);
                source.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
    }

    public static void unmap(MappedByteBuffer bb) {
        if (null == bb) {
            return;
        }
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if (cl != null) {
            cl.clean();
        }
    }

    public static void clean(final MappedByteBuffer buffer) throws Exception {
        if (null == buffer) {
            return;
        }
        //buffer.force();
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
                    getCleanerMethod.setAccessible(true);
                    Cleaner cleaner = (Cleaner) getCleanerMethod.invoke(buffer, new Object[0]);
                    cleaner.clean();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

    }


    public static void main(String args[]) throws Exception {
        RandomAccessFile source =null;
        RandomAccessFile target = null;

        MappedByteBuffer mbbi = null;
        MappedByteBuffer mbbo = null;
        for (int k = 0; k < 10; k++) {
             source = new RandomAccessFile("/home/baojie/liuxin/source/big_cow/test_4_mapped.zip", "r");
             target = new RandomAccessFile("/home/baojie/liuxin/source/small_cow/test_4_mapped.zip", "rw");
            FileChannel in = source.getChannel();
            FileChannel out = target.getChannel();
            final long size = in.size();
            boolean bigger = false;
            if (size > Integer.MAX_VALUE) {
                bigger = true;
            }
            long has = 0L;
            long start = System.currentTimeMillis();
            if (bigger) {
                for (; ; ) {
                    if (has >= size) {
                        break;
                    }
                    if (has <= 0) {
                        mbbi = in.map(FileChannel.MapMode.READ_ONLY, 0, Integer.MAX_VALUE);
                        mbbo = out.map(FileChannel.MapMode.READ_WRITE, 0, Integer.MAX_VALUE);
                        for (int i = 0; i < Integer.MAX_VALUE; i++) {
                            byte b = mbbi.get(i);
                            mbbo.put(i, b);
                        }
                        has = has + Integer.MAX_VALUE;
                        mbbo.force();

                        unmap(mbbo);
                        clean(mbbo);

                        unmap(mbbi);
                        clean(mbbi);
                    } else {
                        long remain = size - has;
                        if (remain < Integer.MAX_VALUE) {
                            mbbi = in.map(FileChannel.MapMode.READ_ONLY, has, remain);
                            mbbo = out.map(FileChannel.MapMode.READ_WRITE, has, remain);
                            for (int i = 0; i < remain; i++) {
                                byte b = mbbi.get(i);
                                mbbo.put(i, b);
                            }
                            has = has + remain;
                            mbbo.force();

                            unmap(mbbo);
                            clean(mbbo);

                            unmap(mbbi);
                            clean(mbbi);
                        } else {
                            mbbi = in.map(FileChannel.MapMode.READ_ONLY, has, Integer.MAX_VALUE);
                            mbbo = out.map(FileChannel.MapMode.READ_WRITE, has, Integer.MAX_VALUE);
                            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                                byte b = mbbi.get(i);
                                mbbo.put(i, b);
                            }
                            has = has + Integer.MAX_VALUE;
                            mbbo.force();

                            unmap(mbbo);
                            clean(mbbo);

                            unmap(mbbi);
                            clean(mbbi);
                        }
                    }
                }
            } else {
                mbbi = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                mbbo = out.map(FileChannel.MapMode.READ_WRITE, 0, size);
                for (int i = 0; i < size; i++) {
                    byte b = mbbi.get(i);
                    mbbo.put(i, b);
                }
                has = has + size;
                mbbo.force();

                unmap(mbbo);
                clean(mbbo);

                unmap(mbbi);
                clean(mbbi);
            }
            System.out.println("Spend: " + (System.currentTimeMillis() - start) + "ms");
            unmap(mbbo);
            clean(mbbo);
            unmap(mbbi);
            clean(mbbi);
            source.close();
            target.close();

            Files.deleteIfExists(Paths.get("/home/baojie/liuxin/source/small_cow/test_4_mapped.zip"));
        }
    }


}
