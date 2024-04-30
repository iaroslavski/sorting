/*
 * Copyright (c) 2009, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.RecursiveTask;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import jdk.internal.vm.annotation.ForceInline;

/**
 * This class implements powerful and fully optimized versions, both
 * sequential and parallel, of the Dual-Pivot Quicksort algorithm by
 * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
 * offers O(n log(n)) performance on all data sets, and is typically
 * faster than traditional (one-pivot) Quicksort implementations.
 *
 * There are also additional algorithms, invoked from the Dual-Pivot
 * Quicksort, such as mixed insertion sort, merging of runs and heap
 * sort, counting sort and parallel merge sort.
 *
 * @author Vladimir Yaroslavskiy
 * @author Jon Bentley
 * @author Josh Bloch
 * @author Doug Lea
 *
 * @version 2018.08.18
 *
 * @since 1.7 * 14
 */
final class DualPivotQuicksort_b01_mrg {

    /**
     * Prevents instantiation.
     */
    private DualPivotQuicksort_b01_mrg() {}

    /**
     * Max array size to use mixed insertion sort.
     */
    private static final int MAX_MIXED_INSERTION_SORT_SIZE = 65;

    /**
     * Max array size to use insertion sort.
     */
    private static final int MAX_INSERTION_SORT_SIZE = 44;

    /**
     * Min array size to perform sorting in parallel.
     */
    private static final int MIN_PARALLEL_SORT_SIZE = 4 << 10;

    
    
    /* ----------------- Merging sort section ----------------- */

    /**
     * Min array size to use merging sort.
     */
    private static final int MIN_MERGING_SORT_SIZE = 512;

    /**
     * Min size of run to continue scanning.
     */
    private static final int MIN_RUN_SIZE = 128;

    
    /**
     * Max size of additional buffer in bytes,
     *      limited by max_heap / 16 or 2 GB max.
     */
    private static final int MAX_BUFFER_SIZE =
            (int) Math.min(Runtime.getRuntime().maxMemory() >>> 4, Integer.MAX_VALUE);
    
    
    
    /**
     * Min size of a byte array to use counting sort.
     */
    private static final int MIN_BYTE_COUNTING_SORT_SIZE = 64;

    /**
     * Min size of a short or char array to use counting sort.
     */
    private static final int MIN_SHORT_OR_CHAR_COUNTING_SORT_SIZE = 1750;

    /**
     * Threshold of mixed insertion sort is incremented by this value.
     */
    private static final int DELTA = 3 << 1;

    /**
     * Max recursive partitioning depth before using heap sort.
     */
    private static final int MAX_RECURSION_DEPTH = 64 * DELTA;

    /**
     * Represents a function that accepts the array and sorts the specified range
     * of the array into ascending order.
     */
    @FunctionalInterface
    private static interface SortOperation<A> {
        /**
         * Sorts the specified range of the array.
         *
         * @param a the array to be sorted
         * @param low the index of the first element, inclusive, to be sorted
         * @param high the index of the last element, exclusive, to be sorted
         */
        void sort(A a, int low, int high);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     *
     * @param elemType the class of the elements of the array to be sorted
     * @param array the array to be sorted
     * @param offset the relative offset, in bytes, from the base address of
     * the array to sort, otherwise if the array is {@code null},an absolute
     * address pointing to the first element to sort from.
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @param so the method reference for the fallback implementation
     */
    @IntrinsicCandidate
    @ForceInline
    private static <A> void sort(Class<?> elemType, A array, long offset, int low, int high, SortOperation<A> so) {
        so.sort(array, low, high);
    }

    /**
     * Represents a function that accepts the array and partitions the specified range
     * of the array using the pivots provided.
     */
    @FunctionalInterface
    interface PartitionOperation<A> {
        /**
         * Partitions the specified range of the array using the given pivots.
         *
         * @param a the array to be partitioned
         * @param low the index of the first element, inclusive, to be partitioned
         * @param high the index of the last element, exclusive, to be partitioned
         * @param pivotIndex1 the index of pivot1, the first pivot
         * @param pivotIndex2 the index of pivot2, the second pivot
         */
        int[] partition(A a, int low, int high, int pivotIndex1, int pivotIndex2);
    }

    /**
     * Partitions the specified range of the array using the two pivots provided.
     *
     * @param elemType the class of the array to be partitioned
     * @param array the array to be partitioned
     * @param offset the relative offset, in bytes, from the base address of
     * the array to partition, otherwise if the array is {@code null},an absolute
     * address pointing to the first element to partition from.
     * @param low the index of the first element, inclusive, to be partitioned
     * @param high the index of the last element, exclusive, to be partitioned
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @param po the method reference for the fallback implementation
     */
    @IntrinsicCandidate
    @ForceInline
    private static <A> int[] partition(Class<?> elemType, A array, long offset, int low, int high, int pivotIndex1, int pivotIndex2, PartitionOperation<A> po) {
        return po.partition(array, low, high, pivotIndex1, pivotIndex2);
    }

    /**
     * Calculates the double depth of parallel merging.
     * Depth is negative, if tasks split before sorting.
     *
     * @param parallelism the parallelism level
     * @param size the target size
     * @return the depth of parallel merging
     */
    private static int getDepth(int parallelism, int size) {
        int depth = 0;

        while ((parallelism >>= 3) > 0 && (size >>= 2) > 0) {
            depth -= 2;
        }
        return depth;
    }

    /**
     * Sorts the specified range of the array using parallel merge
     * sort and/or Dual-Pivot Quicksort.
     *
     * To balance the faster splitting and parallelism of merge sort
     * with the faster element partitioning of Quicksort, ranges are
     * subdivided in tiers such that, if there is enough parallelism,
     * the four-way parallel merge is started, still ensuring enough
     * parallelism to process the partitions.
     *
     * @param a the array to be sorted
     * @param parallelism the parallelism level
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(int[] a, int parallelism, int low, int high) {
        int size = high - low;

        if (parallelism > 1 && size > MIN_PARALLEL_SORT_SIZE) {
            int depth = getDepth(parallelism, size >> 12);
            int[] b = depth == 0 ? null : new int[size];
            new Sorter<>(null, a, b, low, size, low, depth).invoke();
        } else {
            sort(null, a, 0, low, high);
        }
    }

    /**
     * Sorts the specified array using the Dual-Pivot Quicksort and/or
     * other sorts in special-cases, possibly with parallel partitions.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that array is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(Sorter<int[]> sorter, int[] a, int bits, int low, int high) {
        while (true) {
            int end = high - 1, size = high - low;
            /*
             * Run mixed insertion sort on small non-leftmost parts.
             */
            if (size < MAX_MIXED_INSERTION_SORT_SIZE + bits && (bits & 1) > 0) {
                sort(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET, low, high, DualPivotQuicksort_b01_mrg::mixedInsertionSort);
                return;
            }

            /*
             * Invoke insertion sort on small leftmost part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                sort(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET, low, high, DualPivotQuicksort_b01_mrg::insertionSort);
                return;
            }

            /*
             * Try merging sort on large part.
             */
            if (size > MIN_MERGING_SORT_SIZE * bits
                    && tryMergingSort(sorter, a, low, high)) {
                return;
            }

            /*
             * Switch to heap sort if execution
             * time is becoming quadratic.
             */
            if ((bits += DELTA) > MAX_RECURSION_DEPTH) {
                heapSort(a, low, high);
                return;
            }

            /*
             * Use an inexpensive approximation of the golden ratio
             * to select five sample elements and determine pivots.
             */
            int step = (size >> 3) * 3 + 3;

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = end - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            int a3 = a[e3];

            /*
             * Sort these elements in place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *    5 ------o-----------o------------
             *            |           |
             *    4 ------|-----o-----o-----o------
             *            |     |           |
             *    2 ------o-----|-----o-----o------
             *                  |     |
             *    1 ------------o-----o------------
             */
            if (a[e5] < a[e2]) { int t = a[e5]; a[e5] = a[e2]; a[e2] = t; }
            if (a[e4] < a[e1]) { int t = a[e4]; a[e4] = a[e1]; a[e1] = t; }
            if (a[e5] < a[e4]) { int t = a[e5]; a[e5] = a[e4]; a[e4] = t; }
            if (a[e2] < a[e1]) { int t = a[e2]; a[e2] = a[e1]; a[e1] = t; }
            if (a[e4] < a[e2]) { int t = a[e4]; a[e4] = a[e2]; a[e2] = t; }

            if (a3 < a[e2]) {
                if (a3 < a[e1]) {
                    a[e3] = a[e2]; a[e2] = a[e1]; a[e1] = a3;
                } else {
                    a[e3] = a[e2]; a[e2] = a3;
                }
            } else if (a3 > a[e4]) {
                if (a3 > a[e5]) {
                    a[e3] = a[e4]; a[e4] = a[e5]; a[e5] = a3;
                } else {
                    a[e3] = a[e4]; a[e4] = a3;
                }
            }

            // Pointers
            int lower; // The index of the last element of the left part
            int upper; // The index of the first element of the right part

            /*
             * Partitioning with 2 pivots in case of different elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {
                /*
                 * Use the first and fifth of the five sorted elements as
                 * the pivots. These values are inexpensive approximation
                 * of tertiles. Note, that pivot1 < pivot2.
                 */
                int[] pivotIndices = partition(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET, low, high, e1, e5, DualPivotQuicksort_b01_mrg::partitionDualPivot);
                lower = pivotIndices[0];
                upper = pivotIndices[1];



                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, lower + 1, upper);
                    sorter.fork(bits | 1, upper + 1, high);
                } else {
                    sort(sorter, a, bits | 1, lower + 1, upper);
                    sort(sorter, a, bits | 1, upper + 1, high);
                }

            } else { // Use single pivot in case of many equal elements

                /*
                 * Use the third of the five sorted elements as the pivot.
                 * This value is inexpensive approximation of the median.
                 */
                int[] pivotIndices = partition(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET, low, high, e3, e3, DualPivotQuicksort_b01_mrg::partitionSinglePivot);
                lower = pivotIndices[0];
                upper = pivotIndices[1];
                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, upper, high);
                } else {
                    sort(sorter, a, bits | 1, upper, high);
                }
            }
            high = lower; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using the two pivots provided.
     *
     * @param array the array to be partitioned
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     *
     */
    @ForceInline
    private static int[] partitionDualPivot(int[] a, int low, int high, int pivotIndex1, int pivotIndex2) {
        int end = high - 1;
        int lower = low;
        int upper = end;

        int e1 = pivotIndex1;
        int e5 = pivotIndex2;
        int pivot1 = a[e1];
        int pivot2 = a[e5];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[e1] = a[lower];
        a[e5] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *   left part                 central part          right part
         * +------------------------------------------------------------+
                  * |  < pivot1  |   ?   |  pivot1 <= && <= pivot2  |  > pivot2  |
         * +------------------------------------------------------------+
         *             ^       ^                            ^
         *             |       |                            |
         *           lower     k                          upper
         *
         * Invariants:
         *
         *              all in (low, lower] < pivot1
         *    pivot1 <= all in (k, upper)  <= pivot2
         *              all in [upper, end) > pivot2
         *
         * Pointer k is the last index of ?-part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            int ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left side
                while (lower < k) {
                    if (a[++lower] >= pivot1) {
                        if (a[lower] > pivot2) {
                            a[k] = a[--upper];
                            a[upper] = a[lower];
                        } else {
                            a[k] = a[lower];
                        }
                        a[lower] = ak;
                        break;
                    }
                }
            } else if (ak > pivot2) { // Move a[k] to the right side
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low] = a[lower]; a[lower] = pivot1;
        a[end] = a[upper]; a[upper] = pivot2;

        return new int[] {lower, upper};
    }

    /**
     * Partitions the specified range of the array using a single pivot provided.
     *
     * @param array the array to be partitioned
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     *
     */
    @ForceInline
    private static int[] partitionSinglePivot(int[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        int end = high - 1;
        int lower = low;
        int upper = end;
        int e3 = pivotIndex1;
        int pivot = a[e3];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[e3] = a[lower];

        /*
         * Traditional 3-way (Dutch National Flag) partitioning
         *
         *   left part                 central part    right part
         * +------------------------------------------------------+
         * |   < pivot   |     ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *              ^           ^                ^
         *              |           |                |
         *            lower         k              upper
         *
         * Invariants:
         *
         *   all in (low, lower] < pivot
         *   all in (k, upper)  == pivot
         *   all in [upper, end] > pivot
         *
         * Pointer k is the last index of ?-part
         */
        for (int k = ++upper; --k > lower; ) {
            int ak = a[k];

            if (ak != pivot) {
                a[k] = pivot;

                if (ak < pivot) { // Move a[k] to the left side
                    while (a[++lower] < pivot);

                    if (a[lower] > pivot) {
                        a[--upper] = a[lower];
                    }
                    a[lower] = ak;
                } else { // ak > pivot - Move a[k] to the right side
                    a[--upper] = ak;
                }
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;
        return new int[] {lower, upper};
    }

    /**
     * Sorts the specified range of the array using mixed insertion sort.
     *
     * Mixed insertion sort is combination of simple insertion sort,
     * pin insertion sort and pair insertion sort.
     *
     * In the context of Dual-Pivot Quicksort, the pivot element
     * from the left part plays the role of sentinel, because it
     * is less than any elements from the given part. Therefore,
     * expensive check of the left range can be skipped on each
     * iteration unless it is the leftmost call.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void mixedInsertionSort(int[] a, int low, int high) {
        int size = high - low;
        int end = high - 3 * ((size >> 5) << 3);
        if (end == high) {

            /*
             * Invoke simple insertion sort on tiny array.
             */
            for (int i; ++low < end; ) {
                int ai = a[i = low];

                while (ai < a[--i]) {
                    a[i + 1] = a[i];
                }
                a[i + 1] = ai;
            }
        } else {

            /*
             * Start with pin insertion sort on small part.
             *
             * Pin insertion sort is extended simple insertion sort.
             * The main idea of this sort is to put elements larger
             * than an element called pin to the end of array (the
             * proper area for such elements). It avoids expensive
             * movements of these elements through the whole array.
             */
            int pin = a[end];

            for (int i, p = high; ++low < end; ) {
                int ai = a[i = low];

                if (ai < a[i - 1]) { // Small element

                    /*
                     * Insert small element into sorted part.
                     */
                    a[i] = a[--i];

                    while (ai < a[--i]) {
                        a[i + 1] = a[i];
                    }
                    a[i + 1] = ai;

                } else if (p > i && ai > pin) { // Large element

                    /*
                     * Find element smaller than pin.
                     */
                    while (a[--p] > pin);

                    /*
                     * Swap it with large element.
                     */
                    if (p > i) {
                        ai = a[p];
                        a[p] = a[i];
                    }

                    /*
                     * Insert small element into sorted part.
                     */
                    while (ai < a[--i]) {
                        a[i + 1] = a[i];
                    }
                    a[i + 1] = ai;
                }
            }

            /*
             * Continue with pair insertion sort on remain part.
             */
            for (int i; low < high; ++low) {
                int a1 = a[i = low], a2 = a[++low];

                /*
                 * Insert two elements per iteration: at first, insert the
                 * larger element and then insert the smaller element, but
                 * from the position where the larger element was inserted.
                 */
                if (a1 > a2) {

                    while (a1 < a[--i]) {
                        a[i + 2] = a[i];
                    }
                    a[++i + 1] = a1;

                    while (a2 < a[--i]) {
                        a[i + 1] = a[i];
                    }
                    a[i + 1] = a2;

                } else if (a1 < a[i - 1]) {

                    while (a2 < a[--i]) {
                        a[i + 2] = a[i];
                    }
                    a[++i + 1] = a2;

                    while (a1 < a[--i]) {
                        a[i + 1] = a[i];
                    }
                    a[i + 1] = a1;
                }
            }
        }
    }

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void insertionSort(int[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            int ai = a[i = k];

            if (ai < a[i - 1]) {
                while (--i >= low && ai < a[i]) {
                    a[i + 1] = a[i];
                }
                a[i + 1] = ai;
            }
        }
    }

    /**
     * Sorts the specified range of the array using heap sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void heapSort(int[] a, int low, int high) {
        for (int k = (low + high) >>> 1; k > low; ) {
            pushDown(a, --k, a[k], low, high);
        }
        while (--high > low) {
            int max = a[low];
            pushDown(a, low, a[high], low, high);
            a[high] = max;
        }
    }

    /**
     * Pushes specified element down during heap sort.
     *
     * @param a the given array
     * @param p the start index
     * @param value the given element
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void pushDown(int[] a, int p, int value, int low, int high) {
        for (int k ;; a[p] = a[p = k]) {
            k = (p << 1) - low + 2; // Index of the right child

            if (k > high) {
                break;
            }
            if (k == high || a[k] < a[k - 1]) {
                --k;
            }
            if (a[k] <= value) {
                break;
            }
        }
        a[p] = value;
    }

    /**
     * Tries to sort the specified range of the array using merging sort.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @return {@code true} if the array is finally sorted, otherwise {@code false}
     */
    static boolean tryMergingSort(Sorter<int[]> sorter, int[] a, int low, int high) {

        /*
         * The element run[i] holds the start index
         * of i-th sequence in non-descending order.
         */
        int count = 1;
        int[] run = null;

        /*
         * Identify all possible runs.
         */
        for (int k = low + 1, last = low; k < high; ) {

            /*
             * Find the next run.
             */
            if (a[k - 1] < a[k]) {

                // Identify ascending sequence
                while (++k < high && a[k - 1] <= a[k]);

            } else if (a[k - 1] > a[k]) {

                // Identify descending sequence
                while (++k < high && a[k - 1] >= a[k]);

                // Reverse into ascending order
                for (int i = last - 1, j = k; ++i < --j && a[i] > a[j]; ) {
                    int ai = a[i]; a[i] = a[j]; a[j] = ai;
                }
            } else { // Identify constant sequence
                for (int ak = a[k]; ++k < high && ak == a[k]; );

                if (k < high) {
                    continue;
                }
            }

            /*
             * Process the run.
             */
            if (run == null) {

                if (k == high) {
                    /*
                     * Array is monotonous sequence
                     * and therefore already sorted.
                     */
                    return true;
                }

                if (k - low < MIN_RUN_SIZE) {
 
                    /*
                     * The first run is too small
                     * to proceed with scanning.
                     */
                    return false;
                }

                run = new int[((high - low) >> 9) & 0x1FF | 0x3F];
                run[0] = low;

            } else if (a[last - 1] > a[last]) { // Start the new run

                /*
                 * Check if the runs are too long to continue scanning.
                 */
                if (k - low < count * MIN_RUN_SIZE) {
                    return false;
                }

                if (++count == run.length) {
                    /*
                     * Array is not highly structured.
                     */
                    return false;
                }
            }

            /*
             * Save the current run.
             */
            run[count] = (last = k);

            /*
             * Check single-element run at the end.
             */
            if (++k == high) {
                --k;
            }
        }

        /*
         * Merge all runs.
         */
        if (count > 1) {
            int[] b; int offset = low;

            if (sorter != null && (b = sorter.b) != null) {
                offset = sorter.offset;
            } else if ((b = tryAllocate(int[].class, high - low)) == null) {
                return false;
            }
            mergeRuns(a, b, offset, 1, sorter != null, run, 0, count);
        }
        return true;
    }

    /**
     * Merges the specified runs.
     *
     * @param a the source array
     * @param b the temporary buffer used in merging
     * @param offset the start index in the source, inclusive
     * @param aim specifies merging: to source ( > 0), buffer ( < 0) or any ( == 0)
     * @param parallel indicates whether merging is performed in parallel
     * @param run the start indexes of the runs, inclusive
     * @param lo the start index of the first run, inclusive
     * @param hi the start index of the last run, inclusive
     * @return the destination where runs are merged
     */
    private static int[] mergeRuns(int[] a, int[] b, int offset,
            int aim, boolean parallel, int[] run, int lo, int hi) {

        if (hi - lo == 1) {
            if (aim >= 0) {
                return a;
            }
            System.arraycopy(a, run[lo], b, run[lo] - offset, run[hi] - run[lo]);
            return b;
        }

        /*
         * Split into approximately equal parts.
         */
        int mi = lo, rmi = (run[lo] + run[hi]) >>> 1;
        while (run[++mi + 1] <= rmi);

        /*
         * Merge runs of each part.
         */
        int[] a1 = mergeRuns(a, b, offset, -aim, parallel, run, lo, mi);
        int[] a2 = mergeRuns(a, b, offset,    0, parallel, run, mi, hi);
        int[] dst = a1 == a ? b : a;

        int k   = a1 == a ? run[lo] - offset : run[lo];
        int lo1 = a1 == b ? run[lo] - offset : run[lo];
        int hi1 = a1 == b ? run[mi] - offset : run[mi];
        int lo2 = a2 == b ? run[mi] - offset : run[mi];
        int hi2 = a2 == b ? run[hi] - offset : run[hi];

        /*
         * Merge the left and right parts.
         */
        if (hi1 - lo1 > MIN_PARALLEL_SORT_SIZE && parallel) {
            new Merger<>(null, dst, k, a1, lo1, hi1, a2, lo2, hi2).invoke();
        } else {
            mergeParts(null, dst, k, a1, lo1, hi1, a2, lo2, hi2);
        }
        return dst;
    }

    /**
     * Merges the sorted parts.
     *
     * @param merger parallel context
     * @param dst the destination where parts are merged
     * @param k the start index of the destination, inclusive
     * @param a1 the first part
     * @param lo1 the start index of the first part, inclusive
     * @param hi1 the end index of the first part, exclusive
     * @param a2 the second part
     * @param lo2 the start index of the second part, inclusive
     * @param hi2 the end index of the second part, exclusive
     */
    private static void mergeParts(Merger<int[]> merger, int[] dst, int k,
            int[] a1, int lo1, int hi1, int[] a2, int lo2, int hi2) {

        /*
         * Merge sorted parts in parallel.
         */
        if (merger != null && a1 == a2) {

            while (true) {

                /*
                 * The first part must be larger.
                 */
                if (hi1 - lo1 < hi2 - lo2) {
                    int lo = lo1; lo1 = lo2; lo2 = lo;
                    int hi = hi1; hi1 = hi2; hi2 = hi;
                }

                /*
                 * Terminate, if the second part is empty.
                 */
                if (lo2 == hi2) {
                    System.arraycopy(a1, lo1, dst, k, hi1 - lo1);
                    return;
                }

                /*
                 * Small parts will be merged sequentially.
                 */
                if (hi1 - lo1 < MIN_PARALLEL_SORT_SIZE) {
                    break;
                }

                /*
                 * Find the median of the larger part.
                 */
                int mi1 = (lo1 + hi1) >>> 1;
                int key = a1[mi1];
                int mi2 = hi2;

                /*
                 * Divide the smaller part.
                 */
                for (int mi0 = lo2; mi0 < mi2; ) {
                    int m = (mi0 + mi2) >>> 1;

                    if (key > a2[m]) {
                        mi0 = m + 1;
                    } else {
                        mi2 = m;
                    }
                }

                /*
                 * Reserve space for the left parts.
                 */
                int space = mi2 - lo2 + mi1 - lo1;

                /*
                 * Merge other parts in parallel.
                 */
                merger.fork(k + space, mi1, hi1, mi2, hi2);

                /*
                 * Iterate along the left parts.
                 */
                hi1 = mi1;
                hi2 = mi2;
            }
        }

        /*
         * Merge small parts sequentially.
         */
        if (lo2 < hi2 && a1[hi1 - 1] > a2[lo2]) {
            while (lo1 < hi1 && lo2 < hi2) {
                dst[k++] = a1[lo1] < a2[lo2] ? a1[lo1++] : a2[lo2++];
            }
        }
        if (dst != a1 || k < lo1) {
            while (lo1 < hi1) {
                dst[k++] = a1[lo1++];
            }
        }
        if (dst != a2 || k < lo2) {
            while (lo2 < hi2) {
                dst[k++] = a2[lo2++];
            }
        }
    }

// [class]

    /**
     * This class implements parallel sorting.
     */
    private static final class Sorter<T> extends CountedCompleter<Void> {

        private static final long serialVersionUID = 123456789L;

        @SuppressWarnings("serial")
        private final T a, b;
        private final int low, size, offset, depth;

        @SuppressWarnings("unchecked")
        private Sorter(T a, int parallelism, int low, int size, int depth) {
            this.a = a;
            this.low = low;
            this.size = size;
            this.offset = low;

            while ((parallelism >>= 2) > 0 && (size >>= 2) > 0) {
                depth -= 2;
            }
            this.b = (T) tryAllocate(a.getClass(), this.size);
            this.depth = b == null ? 0 : depth;
        }

        private Sorter(CountedCompleter<?> parent,
                T a, T b, int low, int size, int offset, int depth) {
            super(parent);
            this.a = a;
            this.b = b;
            this.low = low;
            this.size = size;
            this.offset = offset;
            this.depth = depth;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void compute() {
            if (depth < 0) {
                setPendingCount(2);
                int half = size >> 1;
                new Sorter<>(this, b, a, low, half, offset, depth + 1).fork();
                new Sorter<>(this, b, a, low + half, size - half, offset, depth + 1).compute();
            } else {
                if (a instanceof int[]) {
                    sort((Sorter<int[]>) this, (int[]) a, depth, low, low + size);
                } else {
                    throw new IllegalArgumentException("Unknown array: " + a.getClass().getName());
                }
            }
            tryComplete();
        }

        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            if (depth < 0) {
                int mi = low + (size >> 1);
                boolean src = (depth & 1) == 0;

                new Merger<>(null,
                    a,
                    src ? low : low - offset,
                    b,
                    src ? low - offset : low,
                    src ? mi - offset : mi,
                    b,
                    src ? mi - offset : mi,
                    src ? low + size - offset : low + size
                ).invoke();
            }
        }

        private void fork(int depth, int low, int high) {
            addToPendingCount(1);
            new Sorter<>(this, a, b, low, high - low, offset, depth).fork();
        }
    }

    /**
     * This class implements parallel merging.
     */
    private static final class Merger<T> extends CountedCompleter<Void> {

        private static final long serialVersionUID = 123456789L;

        @SuppressWarnings("serial")
        private final T dst, a1, a2;
        private final int k, lo1, hi1, lo2, hi2;

        private Merger(CountedCompleter<?> parent, T dst, int k,
                T a1, int lo1, int hi1, T a2, int lo2, int hi2) {
            super(parent);
            this.dst = dst;
            this.k = k;
            this.a1 = a1;
            this.lo1 = lo1;
            this.hi1 = hi1;
            this.a2 = a2;
            this.lo2 = lo2;
            this.hi2 = hi2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void compute() {
            if (dst instanceof int[]) {
                mergeParts((Merger<int[]>) this, (int[]) dst, k,
                    (int[]) a1, lo1, hi1, (int[]) a2, lo2, hi2);
            } else {
                throw new IllegalArgumentException("Unknown array: " + dst.getClass().getName());
            }
            propagateCompletion();
        }

        private void fork(int k, int lo1, int hi1, int lo2, int hi2) {
            addToPendingCount(1);
            new Merger<>(this, dst, k, a1, lo1, hi1, a2, lo2, hi2).fork();
        }
    }

    /**
     * Tries to allocate additional buffer.
     *
     * @param clazz the given array class
     * @param length the length of additional buffer
     * @return {@code null} if requested buffer is too big or there is not enough memory,
     *         otherwise created buffer
     */
    @SuppressWarnings("unchecked")
    private static <T> T tryAllocate(Class<T> clazz, int length) {
        try {
            int maxLength = MAX_BUFFER_SIZE >>
                (clazz == int[].class || clazz == float[].class ? 2 : 3);
            return length > maxLength ? null :
                (T) U.allocateUninitializedArray(clazz.componentType(), length);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static final Unsafe U = Unsafe.getUnsafe();
}
