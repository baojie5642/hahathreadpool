package com.baojie.zk.example.concurrent.algorithm.haha;

//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.VarHandle;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class ConcurrentQuickSort {

    private static final int OFF_SIT = 3;
    private static final int INSERT_FLAG = 13;

    public void sort(int[] array, int left, int right) {
        if (null == array) {
            return;
        } else if (array.length <= 1) {
            return;
        } else if (left < 0 || right < 0) {
            return;
        } else if (right - left <= 1) {
            return;
        } else {
            quick(array, left, right);
        }
    }

    private void quick(int[] array, int left, int right) {
        if (left + OFF_SIT <= right) {
            if (left + INSERT_FLAG <= right) {
                int p = partition(array, left, right);
                int i = left, j = right - 1;
                for (; ; ) {
                    for (; ; ) {
                        i = i + 1;
                        if (array[i] >= p) {
                            break;
                        }
                    }
                    for (; ; ) {
                        j = j - 1;
                        if (array[j] <= p) {
                            break;
                        }
                    }
                    if (i < j) {
                        swap(array, i, j);
                    } else {
                        break;
                    }
                }
                swap(array, i, right - 1);
                quick(array, left, i - 1);
                quick(array, i + 1, right);
            } else {
                insertion(array, left, right);
            }
        } else {
            insertion(array, left, right);
        }
    }

    private int partition(int[] array, int left, int right) {
        int c = (left + right) / 2;
        if (array[left] > array[c]) {
            swap(array, left, c);
        }
        if (array[left] > array[right]) {
            swap(array, left, right);
        }
        if (array[c] > array[right]) {
            swap(array, c, right);
        }
        swap(array, c, right - 1);
        return array[right - 1];
    }

    private void swap(int[] array, int left, int right) {
        int temp = array[left];
        array[left] = array[right];
        array[right] = temp;
    }

    public void insertion(int[] array, int left, int right) {
        first(array, left, right);
        for (int i = left + 2; i <= right; i++) {
            int temp = array[i];
            int j = i;
            for (; temp < array[j - 1]; j--) {
                array[j] = array[j - 1];
            }
            array[j] = temp;
        }
    }

    private void first(int[] array, int left, int right) {
        int sm = left;
        for (int i = left; i <= right; i++) {
            if (array[i] < array[sm]) {
                sm = i;
            }
        }
        swap(array, left, sm);
    }


//    private static final VarHandle HEAD;
//    private static final VarHandle TAIL;
//    static final VarHandle ITEM;
//    static final VarHandle NEXT;
//    static {
//        try {
//            MethodHandles.Lookup l = MethodHandles.lookup();
//            HEAD = l.findVarHandle(ConcurrentQuickSort.class, "head",
//                    ConcurrentQuickSort.class);
//            TAIL = l.findVarHandle(ConcurrentQuickSort.class, "tail",
//                    ConcurrentQuickSort.class);
//            ITEM = l.findVarHandle(ConcurrentQuickSort.class, "item", Object.class);
//            NEXT = l.findVarHandle(ConcurrentQuickSort.class, "next", ConcurrentQuickSort.class);
//        } catch (ReflectiveOperationException e) {
//            throw new Error(e);
//        }
//    }


    public static void main(String args[]) {
        ConcurrentQuickSort qs = new ConcurrentQuickSort();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int t = 0; t < 100; t++) {
            int nums[] = new int[99999999];
            for (int i = 0; i < 99999999; i++) {
                int r = random.nextInt(999999999);
                nums[i] = r;
            }
            System.out.println("quick sort init finish ......");
            //qs.insertion(nums,0,nums.length-1);
            long start = System.nanoTime();
            qs.sort(nums, 0, nums.length - 1);
            long finish = System.nanoTime();
            System.out.println("quick sort finish ......cust nano time=" + (finish - start));
            for (int i = 0; i < nums.length - 1; i++) {
                if (nums[i] > nums[i + 1]) {
                    throw new IllegalStateException();
                }
            }
            System.out.println("...... check finish ,time=" + t + ",  ......");
        }
    }























}
