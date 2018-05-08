package com.baojie.zk.example.concurrent.algorithm;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Sort {

    public static <AnyType extends Comparable<? super AnyType>> void insertionSort(AnyType[] a) {
        int j;

        for (int p = 1; p < a.length; p++) {
            AnyType tmp = a[p];
            for (j = p; j > 0 && tmp.compareTo(a[j - 1]) < 0; j--)
                a[j] = a[j - 1];
            a[j] = tmp;
        }
    }

    public static <AnyType extends Comparable<? super AnyType>> void shellsort(AnyType[] a) {
        int j;

        for (int gap = a.length / 2; gap > 0; gap /= 2)
            for (int i = gap; i < a.length; i++) {
                AnyType tmp = a[i];
                for (j = i; j >= gap &&
                        tmp.compareTo(a[j - gap]) < 0; j -= gap)
                    a[j] = a[j - gap];
                a[j] = tmp;
            }
    }

    private static int leftChild(int i) {
        return 2 * i + 1;
    }

    private static <AnyType extends Comparable<? super AnyType>> void percDown(AnyType[] a, int i, int n) {
        int child;
        AnyType tmp;

        for (tmp = a[i]; leftChild(i) < n; i = child) {
            child = leftChild(i);
            if (child != n - 1 && a[child].compareTo(a[child + 1]) < 0) {
                child++;
            }
            if (tmp.compareTo(a[child]) < 0) {
                a[i] = a[child];
            } else {
                break;
            }
        }
        a[i] = tmp;
    }


    public static <AnyType extends Comparable<? super AnyType>> void heapsort(AnyType[] a) {
        // 先构建堆
        for (int i = a.length / 2 - 1; i >= 0; i--) {
            percDown(a, i, a.length);
        }
        // 从数组的末尾从前往后，将对顶元素与末尾元素交换
        for (int i = a.length - 1; i > 0; i--) {
            swapReferences(a, 0, i);
            percDown(a, 0, i);
        }
    }


    public static <AnyType extends Comparable<? super AnyType>> void mergeSort(AnyType[] a) {
        AnyType[] tmpArray = (AnyType[]) new Comparable[a.length];

        mergeSort(a, tmpArray, 0, a.length - 1);
    }


    private static <AnyType extends Comparable<? super AnyType>> void mergeSort(AnyType[] a, AnyType[] tmpArray,
            int left, int right) {
        if (left < right) {
            int center = (left + right) / 2;
            mergeSort(a, tmpArray, left, center);
            mergeSort(a, tmpArray, center + 1, right);
            merge(a, tmpArray, left, center + 1, right);
        }
    }

    private static <AnyType extends Comparable<? super AnyType>> void merge(AnyType[] a, AnyType[] tmpArray,
            int leftPos, int rightPos, int rightEnd) {
        int leftEnd = rightPos - 1;
        int tmpPos = leftPos;
        int numElements = rightEnd - leftPos + 1;

        // Main loop
        while (leftPos <= leftEnd && rightPos <= rightEnd)
            if (a[leftPos].compareTo(a[rightPos]) <= 0) {
                tmpArray[tmpPos++] = a[leftPos++];
            } else {
                tmpArray[tmpPos++] = a[rightPos++];
            }

        while (leftPos <= leftEnd)    // Copy rest of first half
            tmpArray[tmpPos++] = a[leftPos++];

        while (rightPos <= rightEnd)  // Copy rest of right half
            tmpArray[tmpPos++] = a[rightPos++];

        // Copy tmpArray back
        for (int i = 0; i < numElements; i++, rightEnd--)
            a[rightEnd] = tmpArray[rightEnd];
    }

    public static <AnyType extends Comparable<? super AnyType>> void quicksort(AnyType[] a) {
        quicksort(a, 0, a.length - 1);
    }

    private static final int CUTOFF = 3;

    public static <AnyType> void swapReferences(AnyType[] a, int index1, int index2) {
        AnyType tmp = a[index1];
        a[index1] = a[index2];
        a[index2] = tmp;
    }

    private static <AnyType extends Comparable<? super AnyType>> AnyType median3(AnyType[] a, int left, int right) {
        int center = (left + right) / 2;
        if (a[center].compareTo(a[left]) < 0) {
            swapReferences(a, left, center);
        }
        if (a[right].compareTo(a[left]) < 0) {
            swapReferences(a, left, right);
        }
        if (a[right].compareTo(a[center]) < 0) {
            swapReferences(a, center, right);
        }

        // Place pivot at position right - 1
        swapReferences(a, center, right - 1);
        return a[right - 1];
    }


    private static <AnyType extends Comparable<? super AnyType>> void quicksort(AnyType[] a, int left, int right) {
        if (left + CUTOFF <= right) {
            AnyType pivot = median3(a, left, right);
            int i = left, j = right - 1;
            for (; ; ) {
                while (a[++i].compareTo(pivot) < 0) {
                }
                while (a[--j].compareTo(pivot) > 0) {
                }
                if (i < j) {
                    swapReferences(a, i, j);
                } else {
                    break;
                }
            }
            swapReferences(a, i, right - 1);
            quicksort(a, left, i - 1);
            quicksort(a, i + 1, right);
        } else {
            insertionSort(a, left, right);
        }
    }


    private static <AnyType extends Comparable<? super AnyType>> void insertionSort(AnyType[] a, int left, int right) {
        for (int p = left + 1; p <= right; p++) {
            AnyType tmp = a[p];
            int j;
            for (j = p; j > left && tmp.compareTo(a[j - 1]) < 0; j--)
                a[j] = a[j - 1];
            a[j] = tmp;
        }
    }


    public static <AnyType extends Comparable<? super AnyType>> void quickSelect(AnyType[] a, int k) {
        quickSelect(a, 0, a.length - 1, k);
    }


    private static <AnyType extends Comparable<? super AnyType>> void quickSelect(AnyType[] a, int left, int right,
            int k) {
        if (left + CUTOFF <= right) {
            AnyType pivot = median3(a, left, right);
            int i = left, j = right - 1;
            for (; ; ) {
                while (a[++i].compareTo(pivot) < 0) {
                }
                while (a[--j].compareTo(pivot) > 0) {
                }
                if (i < j) {
                    swapReferences(a, i, j);
                } else {
                    break;
                }
            }
            swapReferences(a, i, right - 1);
            if (k <= i) {
                quickSelect(a, left, i - 1, k);
            } else if (k > i + 1) {
                quickSelect(a, i + 1, right, k);
            }
        } else {
            insertionSort(a, left, right);
        }
    }

    private static final int NUM_ITEMS = 10;
    private static int theSeed = 1;

    private static void checkSort(Integer[] a) {
        for (int i = 0; i < a.length; i++)
            if (a[i] != i) {
                System.out.println("Error at " + i);
            }
        System.out.println("Finished checksort");
    }

    private static final int SHUFFLE_THRESHOLD = 5;

    public static void shuffle(Integer[] array, Random rnd) {
        if (null == array || null == rnd) {
            return;
        }
        int size = array.length;
        for (int i = size; i > 1; i--)
            swap(array, i - 1, rnd.nextInt(i));
    }

    public static void swap(List<?> list, int i, int j) {
        final List l = list;
        l.set(i, l.set(j, l.get(i)));
    }

    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public static void main(String[] args) {
        Integer[] a = new Integer[NUM_ITEMS];
        for (int i = 0; i < a.length; i++)
            a[i] = i;
        for (theSeed = 0; theSeed < 20; theSeed++) {
            shuffle(a, new Random(System.nanoTime()));
            long nano = System.nanoTime();
            Sort.insertionSort(a);
            long cust = System.nanoTime() - nano;
            System.out.println("insertionSort cust=" + cust + ", millis=" + TimeUnit.MILLISECONDS.convert(cust,
                    TimeUnit.NANOSECONDS));
            checkSort(a);

            shuffle(a, new Random(System.nanoTime()));
            long nano1 = System.nanoTime();
            Sort.heapsort(a);
            long cust1 = System.nanoTime() - nano1;
            System.out.println("heapsort cust1=" + cust1 + ", millis=" + TimeUnit.MILLISECONDS.convert(cust1,
                    TimeUnit.NANOSECONDS));
            checkSort(a);

            shuffle(a, new Random(System.nanoTime()));
            long nano2 = System.nanoTime();
            Sort.shellsort(a);
            long cust2 = System.nanoTime() - nano2;
            System.out.println("shellsort cust2=" + cust2 + ", millis=" + TimeUnit.MILLISECONDS.convert(cust2,
                    TimeUnit.NANOSECONDS));
            checkSort(a);

            shuffle(a, new Random(System.nanoTime()));
            long nano3 = System.nanoTime();
            Sort.mergeSort(a);
            long cust3 = System.nanoTime() - nano3;
            System.out.println("mergeSort cust3=" + cust3 + ", millis=" + TimeUnit.MILLISECONDS.convert(cust3,
                    TimeUnit.NANOSECONDS));
            checkSort(a);

            shuffle(a, new Random(System.nanoTime()));
            long nano4 = System.nanoTime();
            Sort.quicksort(a);
            long cust4 = System.nanoTime() - nano4;
            System.out.println("quicksort cust4=" + cust4 + ", millis=" + TimeUnit.MILLISECONDS.convert(cust4,
                    TimeUnit.NANOSECONDS));
            checkSort(a);

            shuffle(a, new Random(System.nanoTime()));
            long nano5 = System.nanoTime();
            Sort.quickSelect(a, NUM_ITEMS / 2);
            long cust5 = System.nanoTime() - nano5;
            System.out.println("quickSelect cust5=" + cust5 + ", millis=" + TimeUnit.MILLISECONDS.convert(cust5,
                    TimeUnit.NANOSECONDS));
            System.out.println(a[NUM_ITEMS / 2 - 1] + " " + NUM_ITEMS / 2);
            System.out.println("*****************  loop  =" + theSeed + "=   break   **********************");
        }

        System.out.println("*****************  loop     break   **********************");
        Integer[] b = new Integer[10_000_000];
        for (int i = 0; i < b.length; i++)
            b[i] = i;

        shuffle(b, new Random(System.nanoTime()));
        long start = System.currentTimeMillis();
        Sort.quickSelect(b, b.length / 2);
        long end = System.currentTimeMillis();
        System.out.println("Selection for N = " + b.length + " takes " + (end - start) + "ms.");
        System.out.println(b[b.length / 2 - 1] + " " + b.length / 2);
    }
}
