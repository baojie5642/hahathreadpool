package com.baojie.zk.example.concurrent.algorithm.haha;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class QuickSort {

    private static final int HOW = 10;

    private static final int CUTOFF = 3;

    public QuickSort() {

    }

    // as要排序的数组, b起始点, e结束点, 都是下标
    // 判断的时候以b e下标为准, 然后在根据真实长度判断
    // 但是可能仅仅是排序数组中的部分数据
    // 所以不根据数组的实际长度判断, 仅仅根据下标
    public void quickSort(int[] as, int b, int e) {
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
        } else if (length <= HOW) {
            bubble(as, b, e);
        } //else if (as.length <= HOW) {
        //bubble(as, b, e);}
        else {
            quick(as, b, e);
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
            // j这里可以<=size-1,但是如果这样写,下面的b的判断就要改了
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
        if (as[a] < as[b]) {
            swap(as, a, b);
        }
    }

    private void swap(int[] as, int a, int b) {
        int temp = as[a];
        as[a] = as[b];
        as[b] = temp;
    }

    private void quick(int[] as, int b, int e) {
        if (e - b < HOW) {
            bubble(as, b, e);
        } else if (b + CUTOFF > e) {
            bubble(as, b, e);
        } else {
            int m = partition(as, b, e);
            // 第一个数已经小于划分，所以l=b+1
            int l = b, r = e - 1;
            for (; ; ) {
                for (; ; ) {
                    int ll = l + 1;
                    if (ll > r) {
                        break;
                    } else {
                        l = ll;
                        if (as[ll] > m) {
                            break;
                        }
                    }
                }
                for (; ; ) {
                    int rr = r - 1;
                    if (rr < l) {
                        break;
                    } else {
                        r = rr;
                        if (as[rr] < m) {
                            break;
                        }
                    }
                }
                if (l < r) {
                    swap(as, l, r);
                } else {
                    break;
                }
            }
            swap(as, l, e - 1);
            quick(as, b, l - 1);
            quick(as, l + 1, e);
        }
    }

    private int partition(int[] as, int b, int e) {
        int m = (e - b) / 2;
        if (as[b] > as[m]) {
            swap(as, b, m);
        }
        if (as[b] > as[e]) {
            swap(as, b, e);
        }
        if (as[m] > as[e]) {
            swap(as, m, e);
        }
        // 交换中间位置与倒数第二位置上的数
        // 这样直接处理掉边界问题
        swap(as, m, e - 1);
        return as[e - 1];
    }

    public static void main(String args[]) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int nums[] = new int[2500];
        for (int i = 0; i < 2500; i++) {
            int r = random.nextInt(2500);
            nums[i] = r;
        }
        System.out.println("init complete, data=" + new Date());
        QuickSort qs = new QuickSort();
        qs.quickSort(nums, 0, nums.length - 1);


        //qs.bubbleV2(nums, 0, nums.length - 1);
        for (int t : nums) {
            System.out.println(t);
        }

    }
}
