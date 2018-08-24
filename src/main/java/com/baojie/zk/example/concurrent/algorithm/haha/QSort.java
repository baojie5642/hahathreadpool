package com.baojie.zk.example.concurrent.algorithm.haha;

import java.util.concurrent.ThreadLocalRandom;

public class QSort {

    // 选择枢轴时候要求数据个数大于3
    private static final int OFF_SIT = 3;
    // 选择快排还是插入的界限
    private static final int INSERT_FLAG = 13;

    // 数组的有效下标
    public void sort(int[] array, int left, int right) {
        // 一些无用的校验
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
        // 如果数组中的数据太少，使用插入排序
        // 根据下面选择枢轴的方法，太少也是不允许的
        // 大量实验证明，数据在5到20内的数据，插入排序快于快速排序
        // 这里选择的是13，实验发现13比较好
        if (left + OFF_SIT <= right) {
            if (left + INSERT_FLAG <= right) {
                int p = partition(array, left, right);
                // 实际比较是从left-1，right-2开始的
                // 因为left，right-1，right三者已经有序
                // 具体参考枢轴的选择策略partition
                int i = left, j = right - 1;
                for (; ; ) {
                    for (; ; ) {
                        // 直接加就好了，不需要判断是否越界
                        // 原因就是有三个数已经控制了边界
                        i = i + 1;
                        // 这里说明下:
                        // 为什么大于或者等于都要停止指针移动？
                        // 因为大量实验发现，当等于的时候也停止指针移动是较好的
                        // 从工程角度看两个指针都要判断等于才行
                        // 因为如果只有一边判断会造成不平衡性问题
                        // 也就是因为快排每趟都会分大于小于枢轴的两个集合的
                        // 如果只有一边判断等于那么很容易将等于枢轴的数分到一个集合中
                        // 造成了另一个集合没有等于枢轴的数
                        // 从工程学角度是不合理的
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
                    // i>=j，说明都比较完了，应该跳出循环
                    if (i < j) {
                        swap(array, i, j);
                    } else {
                        break;
                    }
                }
                // {left,2,3,4,5,i,i+1,8,9,right-1,right}
                // 对照数组就很明了了
                swap(array, i, right - 1);
                // 是一个递归调用，后续考虑实现非递归调用的高效算法
                // 因为是要实现并发的快排的
                quick(array, left, i - 1);
                quick(array, i + 1, right);
            } else {
                insertion(array, left, right);
            }
        } else {
            insertion(array, left, right);
        }
    }

    // 枢轴的选择，采用中位数法
    // 1)对头，中，尾三个数进行排序
    // 2)将已经排好序的中间位置的数与倒数第二个数交换
    // 3)这样做的好处是:排序的部分就是第二个位置与倒数第三个位置的中间部分
    // 中间没有枢轴，移动指针的时候无需判断越界
    // 因为两边的边界已经确定了
    // 这样还有个好处就是数组中已经有三个数的排好序的了
    // 并且这三个数不需要参与移动
    // 仅仅作为一些判断标志以及控制
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

    // 插入排序
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

    // 这里的选择最小数也是控制手段，简单的选择，并不是像冒泡那样
    // 正确方法还是参考之前的具体算法吧
    private void first(int[] array, int left, int right) {
        int sm = left;
        for (int i = left; i <= right; i++) {
            if (array[i] < array[sm]) {
                sm = i;
            }
        }
        // 仅仅将最小的数置于最开始的位置
        swap(array, left, sm);
    }

    // 至于时间复杂度，空间复杂度大家百度吧，或者参考算法导论
    public static void main(String args[]) {
        QSort qs = new QSort();
        // 每次都是生成的不同的随机数
        // 如果使用同一组数，这样利用硬件特性，耗费的时间会更少
        // 可以试试
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
