package com.baojie.zk.example.concurrent.algorithm;

import java.util.concurrent.ThreadLocalRandom;

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
            insertion(a, 0, size - 1);
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
            insertion(a, left, right);
        }
    }

    private void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    public void insertion(int[] a, int left, int right) {
        for (int p = left + 1; p <= right; p++) {
            int tmp = a[p], j;
            for (j = p; j > left && tmp < a[j - 1]; j--) {
                a[j] = a[j - 1];
            }
            a[j] = tmp;
        }
    }

    public void insertionV2(int[] as, int left, int right) {
        if (null == as) {
            return;
        } else if (left < 0 || right < 0) {
            return;
        } else {
            int size = as.length;
            if (size <= 0) {
                return;
            } else if (right - left <= 0) {
                return;
            } else {
                int i;
                for (i = right; i > left; i--) {
                    compare(as, i - 1, i);
                }
                for (i = left + 2; i <= right; i++) {
                    int j = i;
                    int v = as[i];
                    while (v < as[j - 1]) {
                        as[j] = as[j - 1];
                        j--;
                    }
                    as[j] = v;
                }
            }
        }
    }

    // 两种个冒泡的实现其中一种
    public void bubble(int[] as, int b, int e) {
        if (b < 0 || e < 0) {
            return;
        }
        int length = e - b;
        if (null == as) {
            return;
        } else if (length <= 0) {
            return;
        } else if (as.length <= 0) {
            return;
        } else {
            for (int j = b; j < e; j++) {
                for (int i = e; i > j; i--) {
                    compare(as, i, i - 1);
                }
            }
        }
    }

    // 两种个冒泡的实现其中一种
    public void bubbleV2(int[] as, int b, int e) {
        if (b < 0 || e < 0) {
            return;
        }
        int length = e - b;
        if (null == as) {
            return;
        } else if (length <= 0) {
            return;
        } else if (as.length <= 0) {
            return;
        } else {
            for (int j = b; j <= e; j++) {
                for (int i = e; ; i--) {
                    int t = i - 1;
                    if (t >= j) {
                        compare(as, i, t);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private void compare(int[] as, int a, int b) {
        if (as[a] > as[b]) {
            swap(as, a, b);
        }
    }

    // left,right均为数组下标
    public void selection(int as[], int left, int right) {
        if (null == as) {
            return;
        } else if (left < 0 || right < 0) {
            return;
        } else {
            int size = as.length;
            if (size <= 0) {
                return;
            } else if (right - left <= 0) {
                return;
            } else {
                int i, j;
                for (i = left; i < right; i++) {
                    int min = i;
                    for (j = i + i; j <= right; j++) {
                        if (as[j] < as[min]) {
                            min = j;
                        }
                    }
                    swap(as, i, min);
                }
            }
        }
    }

    // left,right均为数组下标
    public void selectionV2(int as[], int left, int right) {
        if (null == as) {
            return;
        } else if (left < 0 || right < 0) {
            return;
        } else {
            int size = as.length;
            if (size <= 0) {
                return;
            } else if (right - left <= 0) {
                return;
            } else {
                for (int i = left; i < right; i++) {
                    int min = i;
                    for (int j = i + 1; j <= right; j++) {
                        if (as[j] < as[min]) {
                            min = j;
                        }
                    }
                    swap(as, i, min);
                }
            }
        }
    }

    public void shardingBubble(int a[], int left, int right) {
        if (null == a) {
            return;
        } else if (left < 0 || right < 0) {
            return;
        } else if (a.length <= 0) {
            return;
        } else if (right - left <= 0) {
            return;
        } else {
            int lf = left;
            int rf = right;
            out:
            for (int i = 0; i <= right; i++) {
                // 从右边开始，也就是小的上浮
                if ((i % 2) == 0) {
                    boolean swap = false;
                    for (int j = rf; j > lf; j--) {
                        if (a[j] < a[j - 1]) {
                            swap(a, j - 1, j);
                            swap = true;
                        }
                    }
                    lf = lf + 1;
                    if (!swap) {
                        break out;
                    }
                } else {
                    // 从左边开始，也就是大的下沉
                    boolean swap = false;
                    for (int k = lf; k < rf; k++) {
                        if (a[k] > a[k + 1]) {
                            swap(a, k, k + 1);
                            swap = true;
                        }
                    }
                    rf = rf - 1;
                    if (!swap) {
                        break out;
                    }
                }
                if (lf >= rf) {
                    break out;
                }
            }
        }
    }

    public static void main(String args[]) {
        MyQuickSort mqs = new MyQuickSort();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int nums[] = new int[13];
        for (int i = 0; i < 13; i++) {
            int r = random.nextInt(2500);
            nums[i] = r;
        }
        mqs.shardingBubble(nums, 0, nums.length - 1);
        for (int n : nums) {
            System.out.println(n);
        }


    }


}
