package com.baojie.zk.example.concurrent.algorithm;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MyQuickSort {

    private static final int CUTOFF = 3;

    private static final int INSERT_FLAG = 13;

    public void sort(int[] a) {
        if (null == a) {
            return;
        }
        int size = a.length;
        if (size <= 0) {
            return;
        } else if (size <= INSERT_FLAG) {
            insertionSort(a, 0, size - 1);
        } else {
            quicksort(a, 0, size - 1);
        }
    }

    private int partition(int[] a, int left, int right) {
        int center = (left + right) / 2;
        if (a[center] < a[left]) {
            swap(a, left, center);
        }
        if (a[right] < a[left]) {
            swap(a, left, right);
        }
        if (a[right] < a[center]) {
            swap(a, center, right);
        }
        swap(a, center, right - 1);
        return a[right - 1];
    }

    /**
     * while (a[++i] < pivot) {}
     * while (a[--j] > pivot) {}
     **/

    private void quicksort(int[] a, int left, int right) {
        if (left + CUTOFF <= right) {
            int pivot = partition(a, left, right);
            int i = left, j = right - 1;
            for (; ; ) {
                for (; ; ) {
                    int ii = i + 1;
                    if (ii > j) {
                        break;
                    } else {
                        i = ii;
                        if (a[i] > pivot) {
                            break;
                        }
                    }
                }
                for (; ; ) {
                    int jj = j - 1;
                    if (jj < i) {
                        break;
                    } else {
                        j = jj;
                        if (a[j] < pivot) {
                            break;
                        }
                    }
                }
                // 这里的边界问题还要在考虑
                // 重点是如何巧妙的处理
                //while (a[++i] < pivot) {
                //}
                //while (a[--j] > pivot) {
                //}
                if (i < j) {
                    swap(a, i, j);
                } else {
                    break;
                }
            }
            swap(a, i, right - 1);
            quicksort(a, left, i - 1);
            quicksort(a, i + 1, right);
        } else {
            insertionSort(a, left, right);
        }
    }

    private void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    private void insertionSort(int[] a, int left, int right) {
        for (int p = left + 1; p <= right; p++) {
            int tmp = a[p];
            int j;
            for (j = p; j > left && tmp < a[j - 1]; j--)
                a[j] = a[j - 1];
            a[j] = tmp;
        }
    }

    public void shuffle(int[] array, Random rnd) {
        if (null == array || null == rnd) {
            return;
        }
        int size = array.length;
        for (int i = size; i > 1; i--)
            swap(array, i - 1, rnd.nextInt(i));
    }

    public static void main(String[] args) {
        int size = 200000000;
        int[] a = new int[size];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < size; i++) {
            a[i] = random.nextInt(size);
        }
        System.out.println("start sort**********");
        MyQuickSort myQuickSort = new MyQuickSort();
        for (int i = 0; i < 20; i++) {
            myQuickSort.shuffle(a, new Random(System.nanoTime()));
            long s = System.nanoTime();
            myQuickSort.sort(a);
            long cust = System.nanoTime() - s;
            System.out.println("has done sort, cust nano=" + cust + ", millis=" + TimeUnit.MILLISECONDS.convert(cust,
                    TimeUnit.NANOSECONDS));

        }
        //for (int i = 0; i < size; i++) {
        //    System.out.println(a[i]);
        //}
    }

}
