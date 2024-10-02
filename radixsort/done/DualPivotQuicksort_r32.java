/*
 * Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountedCompleter;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * This class implements powerful and fully optimized versions, both
 * sequential and parallel, of the Dual-Pivot Quicksort algorithm by
 * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
 * offers O(n log(n)) performance on all data sets, and is typically
 * faster than traditional (one-pivot) Quicksort implementations.<p>
 *
 * There are also additional algorithms, invoked from the Dual-Pivot
 * Quicksort such as merging sort, sorting network, Radix sort, heap
 * sort, mixed (simple, pin, pair) insertion sort, counting sort and
 * parallel merge sort.
 *
 * @author Vladimir Yaroslavskiy
 * @author Jon Bentley
 * @author Josh Bloch
 * @author Doug Lea
 *
 * @version 2024.06.14
 *
 * @since 1.7 * 14 ^ 24
 */
final class DualPivotQuicksort_r32 {

    /**
     * Prevents instantiation.
     */
//  private DualPivotQuicksort_r32() {} // todo

    /* ---------------- Insertion sort section ---------------- */

    /**
     * Max array size to use insertion sort.
     */
    private static final int MAX_INSERTION_SORT_SIZE = 51;

    /* ----------------- Merging sort section ----------------- */

    /**
     * Min array size to use merging sort.
     */
    private static final int MIN_MERGING_SORT_SIZE = 512;

    /**
     * Min size of run to continue scanning.
     */
    private static final int MIN_RUN_SIZE = 64;

    /**
     * Max capacity of the index array to track the runs.
     */
    private static final int MAX_RUN_CAPACITY = 10 << 10;

    /**
     * Min array size to start merging of parts.
     */
    private static final int MIN_MERGE_PART_SIZE = 4 << 10;

    /* ------------------ Radix sort section ------------------ */

    /**
     * Min array size to use Radix sort.
     */
    private static final int MIN_RADIX_SORT_SIZE = 640;

    /* ------------------ Counting sort section --------------- */

    /**
     * Min size of a byte array to use counting sort.
     */
    private static final int MIN_BYTE_COUNTING_SORT_SIZE = 32;

    /**
     * Min size of a char array to use counting sort.
     */
    private static final int MIN_CHAR_COUNTING_SORT_SIZE = 2300;

    /**
     * Min size of a short array to use counting sort.
     */
    private static final int MIN_SHORT_COUNTING_SORT_SIZE = 2300;

    /* -------------------- Common section -------------------- */

    /**
     * Min array size to perform sorting in parallel.
     */
    private static final int MIN_PARALLEL_SORT_SIZE = 4 << 10;

    /**
     * Max recursive depth before switching to heap sort.
     */
    private static final int MAX_RECURSION_DEPTH = 64 << 1;

    /**
     * Max size of additional buffer in bytes,
     *      limited by max_heap / 16 or 2 GB max.
     */
    private static final int MAX_BUFFER_SIZE =
        (int) Math.min(Runtime.getRuntime().maxMemory() >>> 4, Integer.MAX_VALUE);

    /**
     * Represents a function that accepts the array and sorts
     * the specified range of the array into ascending order.
     *
     * @param <T> type of // todo
     */
    @FunctionalInterface
    private interface SortOperation<T> {

        /**
         * Sorts the specified range of the array.
         *
         * @param a the array to be sorted
         * @param low the index of the first element, inclusive, to be sorted
         * @param high the index of the last element, exclusive, to be sorted
         */
        void sort(T a, int low, int high);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     *
     * @param <T> type of // todo
     * @param elemType the class of the elements of the array to be sorted
     * @param a the array to be sorted
     * @param offset the relative offset, in bytes, from the base
     *        address of the array to partition, otherwise if the
     *        array is {@code null}, an absolute address pointing
     *        to the first element to partition from
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @param so the method reference for the fallback implementation
     */
    @ForceInline
    @IntrinsicCandidate
    private static <T> void sort(Class<?> elemType, T a, long offset,
            int low, int high, SortOperation<T> so) {
        so.sort(a, low, high);
    }

    /**
     * Represents a function that accepts the array and partitions
     * the specified range of the array using the given pivots.
     *
     * @param <T> type of // todo
     */
    @FunctionalInterface
    private interface PartitionOperation<T> {

        /**
         * Partitions the specified range of the array using the given pivots.
         *
         * @param a the array for partitioning
         * @param low the index of the first element, inclusive, for partitioning
         * @param high the index of the last element, exclusive, for partitioning
         * @param pivotIndex1 the index of pivot1, the first pivot
         * @param pivotIndex2 the index of pivot2, the second pivot
         * @return indices of parts after partitioning
         */
        int[] partition(T a, int low, int high, int pivotIndex1, int pivotIndex2);
    }

    /**
     * Partitions the specified range of the array using the given pivots.
     *
     * @param <T> type of // todo
     * @param elemType the class of the array for partitioning
     * @param a the array for partitioning
     * @param offset the relative offset, in bytes, from the base
     *        address of the array to partition, otherwise if the
     *        array is {@code null}, an absolute address pointing
     *        to the first element to partition from
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @param po the method reference for the fallback implementation
     * @return indices of parts after partitioning
     */
    @ForceInline
    @IntrinsicCandidate
    private static <T> int[] partition(Class<?> elemType, T a, long offset,
            int low, int high, int pivotIndex1, int pivotIndex2, PartitionOperation<T> po) {
        return po.partition(a, low, high, pivotIndex1, pivotIndex2);
    }

    /**
     * Sorts the specified range of the array using parallel merge
     * sort and/or Dual-Pivot Quicksort.<p>
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
        if (parallelism > 1 && high - low > MIN_PARALLEL_SORT_SIZE) {
            new Sorter<>(a, parallelism, low, high - low, 0).invoke();
        } else {
            sort(null, a, 0, low, high);
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(Sorter<int[]> sorter, int[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Run adaptive mixed insertion sort on small non-leftmost parts.
             */
            if (size < MAX_INSERTION_SORT_SIZE + bits && (bits & 1) > 0) {
                sort(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::mixedInsertionSort);
                return;
            }

            /*
             * Invoke insertion sort on small leftmost part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                sort(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::insertionSort);
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
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            int a3 = a[e3];

            /*
             * Check if part is large and contains random
             * data, taking into account parallel context.
             */
            boolean isLargeRandom =
                size > MIN_RADIX_SORT_SIZE && sorter != null && bits > 0 &&
//              size > MIN_RADIX_SORT_SIZE && (sorter == null || bits > 0) &&
                (a[e1] > a[e2] || a[e2] > a3 || a3 > a[e4] || a[e4] > a[e5]);

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { int t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { int t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { int t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { int t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { int t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * Try Radix sort on large fully random data.
             */
            if (isLargeRandom
                    && a[e2] < a[e3] && a[e3] < a[e4]
                    && tryRadixSort(sorter, a, low, high)) {
                return;
            }

            /*
             * Switch to heap sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                heapSort(a, low, high);
                return;
            }

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partition(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET,
                    low, high, e1, e5, DualPivotQuicksort_r32::partitionWithTwoPivots);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[0] + 1, indices[1]);
                    sorter.fork(bits | 1, indices[1] + 1, high);
                } else {
                    sort(sorter, a, bits | 1, indices[0] + 1, indices[1]);
                    sort(sorter, a, bits | 1, indices[1] + 1, high);
                }

            } else { // Partitioning with one pivot

                indices = partition(int.class, a, Unsafe.ARRAY_INT_BASE_OFFSET,
                    low, high, e3, e3, DualPivotQuicksort_r32::partitionWithOnePivot);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[1], high);
                } else {
                    sort(sorter, a, bits | 1, indices[1], high);
                }
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            int[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final int pivot1 = a[pivotIndex1]; // todo final
        final int pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            int ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            int[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final int pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            int ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using mixed insertion sort.<p>
     *
     * Mixed insertion sort is combination of pin insertion sort,
     * simple insertion sort and pair insertion sort.<p>
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
    static void mixedInsertionSort(int[] a, int low, int high) {

        /*
         * Split part for pin and pair insertion sorts.
         */
        int end = high - 3 * ((high - low) >> 3 << 1);

        /*
         * Invoke simple insertion sort on small part.
         */
        if (end == high) {
            for (int i; ++low < high; ) {
                int ai = a[i = low];

                while (ai < a[i - 1]) {
                    a[i] = a[--i];
                }
                a[i] = ai;
            }
            return;
        }

        /*
         * Start with pin insertion sort.
         */
        for (int i, p = high; ++low < end; ) {
            int ai = a[i = low], pin = a[--p];

            /*
             * Swap larger element with pin.
             */
            if (ai > pin) {
                ai = pin;
                a[p] = a[i];
            }

            /*
             * Insert element into sorted part.
             */
            while (ai < a[i - 1]) {
                a[i] = a[--i];
            }
            a[i] = ai;
        }

        /*
         * Finish with pair insertion sort.
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

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(int[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            int ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
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
             * Terminate scanning, if the runs are too small.
             */
            if (k - low < count * MIN_RUN_SIZE) {
                return false;
            }

            /*
             * Process the current run.
             */
            if (run == null) {

                if (k == high) {

                    /*
                     * Array is monotonous sequence
                     * and therefore already sorted.
                     */
                    return true;
                }
                run = new int[ Math.min((high - low) >> 6, MAX_RUN_CAPACITY) | 8];
                run[0] = low;

            } else if (a[last - 1] > a[last]) { // Start the new run

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
            mergeRuns(sorter, a, b, offset, true, run, 0, count);
        }
        return true;
    }

    /**
     * Merges the specified runs.
     *
     * @param sorter parallel context
     * @param a the source array
     * @param b the temporary buffer used in merging
     * @param offset the start index in the source, inclusive
     * @param aim specifies merging: to source (+1), buffer (-1) or any (0) // todo javadoc update
     * @param run the start indexes of the runs, inclusive
     * @param lo the start index of the first run, inclusive
     * @param hi the start index of the last run, inclusive
     * @return the destination where the runs are merged // todo r
     */
    private static void mergeRuns(Sorter<int[]> sorter, int[] a, int[] b, int offset,
            boolean aim, int[] run, int lo, int hi) {

        if (hi - lo == 1) {
            if (!aim) {
                System.arraycopy(a, run[lo], b, run[lo] - offset, run[hi] - run[lo]);
            }
            return;
        }

        /*
         * Split the array into two approximately equal parts.
         */
        int mi = lo, rmi = (run[lo] + run[hi]) >>> 1;
        while (run[++mi + 1] <= rmi);

        /*
         * Merge the runs of all parts.
         */
        mergeRuns(sorter, a, b, offset, !aim, run, lo, mi);
        mergeRuns(sorter, a, b, offset, !aim, run, mi, hi);

        int k  = !aim ? run[lo] - offset : run[lo];
        int lo1 = aim ? run[lo] - offset : run[lo];
        int hi1 = aim ? run[mi] - offset : run[mi];
        int lo2 = aim ? run[mi] - offset : run[mi];
        int hi2 = aim ? run[hi] - offset : run[hi];

        int[] dst = aim ? a : b;
        int[] src = aim ? b : a;

        /*
         * Merge the left and right parts.
         */
        if (hi1 - lo1 > MIN_PARALLEL_SORT_SIZE && sorter != null) {
            new Merger<>(null, dst, k, src, lo1, hi1, lo2, hi2).invoke();
        } else {
            mergeParts(null, dst, k, src, lo1, hi1, lo2, hi2);
        }
    }

    /**
     * Merges the sorted parts.
     *
     * @param merger parallel context
     * @param dst the destination where parts are merged
     * @param k the start index of the destination, inclusive
     * @param a1 the first part // todo r
     * @param src the first part // todo a
     * @param lo1 the start index of the first part, inclusive
     * @param hi1 the end index of the first part, exclusive
     * @param a2 the second part // todo r
     * @param lo2 the start index of the second part, inclusive
     * @param hi2 the end index of the second part, exclusive
     */
    private static void mergeParts(Merger<int[]> merger, int[] dst, int k,
            int[] src, int lo1, int hi1, int lo2, int hi2) {

        /*
         * Merge the parts in parallel.
         */
        if (merger != null) {

            while (hi1 - lo1 > MIN_MERGE_PART_SIZE && hi2 - lo2 > MIN_MERGE_PART_SIZE) {

                /*
                 * The first part must be larger.
                 */
                if (hi1 - lo1 < hi2 - lo2) {
                    int lo = lo1; lo1 = lo2; lo2 = lo;
                    int hi = hi1; hi1 = hi2; hi2 = hi;
                }

                /*
                 * Find the median of the larger part.
                 */
                int mi1 = (lo1 + hi1) >>> 1;
                int key = src[mi1];
                int mi2 = hi2;

                /*
                 * Split the smaller part.
                 */
                for (int mid = lo2; mid < mi2; ) {
                    int d = (mid + mi2) >>> 1; // todo rename int m = ...

                    if (key > src[d]) {
                        mid = d + 1;
                    } else {
                        mi2 = d;
                    }
                }

                /*
                 * Merge other parts in parallel.
                 */
                merger.fork(k, lo1, mi1, lo2, mi2);

                /*
                 * Reserve space for the second parts.
                 */
                k += mi2 - lo2 + mi1 - lo1;

                /*
                 * Iterate along the second parts.
                 */
                lo1 = mi1;
                lo2 = mi2;
            }
        }

        /*
         * Merge small parts sequentially.
         */
        if (lo1 < hi1 && lo2 < hi2 && src[hi1 - 1] > src[lo2]) {

            if (src[hi1 - 1] < src[hi2 - 1]) {
                while (lo1 < hi1) {
                    int slo1 = src[lo1]; // todo rename: first, or key, or curr
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else if (src[hi1 - 1] > src[hi2 - 1]) {
                while (lo2 < hi2) {
                    int slo1 = src[lo1];
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else {
                while (lo1 < hi1 && lo2 < hi2) {
                    int slo1 = src[lo1];

                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            }
        }

        /*
         * Copy the tail of the left part.
         */
        if (lo1 < hi1) {
            System.arraycopy(src, lo1, dst, k, hi1 - lo1);
        }

        /*
         * Copy the tail of the right part.
         */
        if (lo2 < hi2) {
            System.arraycopy(src, lo2, dst, k + hi1 - lo1, hi2 - lo2);
        }
    }

    /**
     * Tries to sort the specified range of the array
     * using LSD (The Least Significant Digit) Radix sort.
     *
     * @param sorter // todo
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @return {@code true} if the array is finally sorted, otherwise {@code false}
     */
    static boolean tryRadixSort(Sorter<int[]> sorter, int[] a, int low, int high) {
        int[] b; int offset = low, size = high - low;

        /*
         * Allocate additional buffer.
         */
        if (sorter != null && (b = sorter.b) != null) {
            offset = sorter.offset;
        } else if ((b = tryAllocate(int[].class, size)) == null) {
            return false;
        }

        int start = low - offset;
        int last = high - offset;

        /*
         * Count the number of all digits.
         */
        int[] count1 = new int[1024];
        int[] count2 = new int[2048];
        int[] count3 = new int[2048];

        for (int i = low; i < high; ++i) {
            ++count1[ a[i]         & 0x3FF];
            ++count2[(a[i] >>> 10) & 0x7FF];
            ++count3[(a[i] >>> 21) ^ 0x400]; // Reverse the sign bit
        }

        /*
         * Detect digits to be processed.
         */
        boolean processDigit1 = processDigit(count1, size, low);
        boolean processDigit2 = processDigit(count2, size, low);
        boolean processDigit3 = processDigit(count3, size, low);

        /*
         * Process the 1-st digit.
         */
        if (processDigit1) {
            for (int i = high; i > low; ) {
                b[--count1[a[--i] & 0x3FF] - offset] = a[i];
            }
        }

        /*
         * Process the 2-nd digit.
         */
        if (processDigit2) {
            if (processDigit1) {
                for (int i = last; i > start; ) {
                    a[--count2[(b[--i] >>> 10) & 0x7FF]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count2[(a[--i] >>> 10) & 0x7FF] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 3-rd digit.
         */
        if (processDigit3) {
            if (processDigit1 ^ processDigit2) {
                for (int i = last; i > start; ) {
                    a[--count3[(b[--i] >>> 21) ^ 0x400]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count3[(a[--i] >>> 21) ^ 0x400] - offset] = a[i];
                }
            }
        }

        /*
         * Copy the buffer to original array, if we process ood number of digits.
         */
        if (processDigit1 ^ processDigit2 ^ processDigit3) {
            System.arraycopy(b, low - offset, a, low, size);
        }
        return true;
    }

    /**
     * Checks the count array and then computes the histogram.
     *
     * @param count the count array
     * @param total the total number of elements
     * @param low the index of the first element, inclusive
     * @return {@code true} if the digit must be processed, otherwise {@code false}
     */
    private static boolean processDigit(int[] count, int total, int low) {

        /*
         * Check if we can skip the given digit.
         */
        for (int c : count) {
            if (c == total) {
                return false;
            }
            if (c > 0) {
                break;
            }
        }

        /*
         * Compute the histogram.
         */
        count[0] += low;

        for (int i = 0; ++i < count.length; ) { // todo
            count[i] += count[i - 1];
        }
        return true;
    }

    /**
     * Sorts the specified range of the array using heap sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void heapSort(int[] a, int low, int high) {
        for (int k = (low + high) >>> 1; k > low; ) { // todo
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

// #[long]

    /**
     * Sorts the specified range of the array using parallel merge
     * sort and/or Dual-Pivot Quicksort.<p>
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
    static void sort(long[] a, int parallelism, int low, int high) {
        if (parallelism > 1 && high - low > MIN_PARALLEL_SORT_SIZE) {
            new Sorter<>(a, parallelism, low, high - low, 0).invoke();
        } else {
            sort(null, a, 0, low, high);
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(Sorter<long[]> sorter, long[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Run adaptive mixed insertion sort on small non-leftmost parts.
             */
            if (size < MAX_INSERTION_SORT_SIZE + bits && (bits & 1) > 0) {
                sort(long.class, a, Unsafe.ARRAY_LONG_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::mixedInsertionSort);
                return;
            }

            /*
             * Invoke insertion sort on small leftmost part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                sort(long.class, a, Unsafe.ARRAY_LONG_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::insertionSort);
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
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            long a3 = a[e3];

            /*
             * Check if part is large and contains random
             * data, taking into account parallel context.
             */
            boolean isLargeRandom =
                size > MIN_RADIX_SORT_SIZE && sorter != null && bits > 0 &&
//              size > MIN_RADIX_SORT_SIZE && (sorter == null || bits > 0) &&
                (a[e1] > a[e2] || a[e2] > a3 || a3 > a[e4] || a[e4] > a[e5]);

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { long t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { long t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { long t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { long t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { long t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * Try Radix sort on large fully random data.
             */
            if (isLargeRandom
                    && a[e2] < a[e3] && a[e3] < a[e4]
                    && tryRadixSort(sorter, a, low, high)) {
                return;
            }

            /*
             * Switch to heap sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                heapSort(a, low, high);
                return;
            }

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partition(long.class, a, Unsafe.ARRAY_LONG_BASE_OFFSET,
                    low, high, e1, e5, DualPivotQuicksort_r32::partitionWithTwoPivots);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[0] + 1, indices[1]);
                    sorter.fork(bits | 1, indices[1] + 1, high);
                } else {
                    sort(sorter, a, bits | 1, indices[0] + 1, indices[1]);
                    sort(sorter, a, bits | 1, indices[1] + 1, high);
                }

            } else { // Partitioning with one pivot

                indices = partition(long.class, a, Unsafe.ARRAY_LONG_BASE_OFFSET,
                    low, high, e3, e3, DualPivotQuicksort_r32::partitionWithOnePivot);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[1], high);
                } else {
                    sort(sorter, a, bits | 1, indices[1], high);
                }
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            long[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final long pivot1 = a[pivotIndex1]; // todo final
        final long pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            long ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            long[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final long pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            long ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using mixed insertion sort.<p>
     *
     * Mixed insertion sort is combination of pin insertion sort,
     * simple insertion sort and pair insertion sort.<p>
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
    static void mixedInsertionSort(long[] a, int low, int high) {

        /*
         * Split part for pin and pair insertion sorts.
         */
        int end = high - 3 * ((high - low) >> 3 << 1);

        /*
         * Invoke simple insertion sort on small part.
         */
        if (end == high) {
            for (int i; ++low < high; ) {
                long ai = a[i = low];

                while (ai < a[i - 1]) {
                    a[i] = a[--i];
                }
                a[i] = ai;
            }
            return;
        }

        /*
         * Start with pin insertion sort.
         */
        for (int i, p = high; ++low < end; ) {
            long ai = a[i = low], pin = a[--p];

            /*
             * Swap larger element with pin.
             */
            if (ai > pin) {
                ai = pin;
                a[p] = a[i];
            }

            /*
             * Insert element into sorted part.
             */
            while (ai < a[i - 1]) {
                a[i] = a[--i];
            }
            a[i] = ai;
        }

        /*
         * Finish with pair insertion sort.
         */
        for (int i; low < high; ++low) {
            long a1 = a[i = low], a2 = a[++low];

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

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(long[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            long ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
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
    static boolean tryMergingSort(Sorter<long[]> sorter, long[] a, int low, int high) {

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
                    long ai = a[i]; a[i] = a[j]; a[j] = ai;
                }
            } else { // Identify constant sequence
                for (long ak = a[k]; ++k < high && ak == a[k]; );

                if (k < high) {
                    continue;
                }
            }

            /*
             * Terminate scanning, if the runs are too small.
             */
            if (k - low < count * MIN_RUN_SIZE) {
                return false;
            }

            /*
             * Process the current run.
             */
            if (run == null) {

                if (k == high) {

                    /*
                     * Array is monotonous sequence
                     * and therefore already sorted.
                     */
                    return true;
                }
                run = new int[ Math.min((high - low) >> 6, MAX_RUN_CAPACITY) | 8];
                run[0] = low;

            } else if (a[last - 1] > a[last]) { // Start the new run

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
            long[] b; int offset = low;

            if (sorter != null && (b = sorter.b) != null) {
                offset = sorter.offset;
            } else if ((b = tryAllocate(long[].class, high - low)) == null) {
                return false;
            }
            mergeRuns(sorter, a, b, offset, true, run, 0, count);
        }
        return true;
    }

    /**
     * Merges the specified runs.
     *
     * @param sorter parallel context
     * @param a the source array
     * @param b the temporary buffer used in merging
     * @param offset the start index in the source, inclusive
     * @param aim specifies merging: to source (+1), buffer (-1) or any (0) // todo javadoc update
     * @param run the start indexes of the runs, inclusive
     * @param lo the start index of the first run, inclusive
     * @param hi the start index of the last run, inclusive
     * @return the destination where the runs are merged // todo r
     */
    private static void mergeRuns(Sorter<long[]> sorter, long[] a, long[] b, int offset,
            boolean aim, int[] run, int lo, int hi) {

        if (hi - lo == 1) {
            if (!aim) {
                System.arraycopy(a, run[lo], b, run[lo] - offset, run[hi] - run[lo]);
            }
            return;
        }

        /*
         * Split the array into two approximately equal parts.
         */
        int mi = lo, rmi = (run[lo] + run[hi]) >>> 1;
        while (run[++mi + 1] <= rmi);

        /*
         * Merge the runs of all parts.
         */
        mergeRuns(sorter, a, b, offset, !aim, run, lo, mi);
        mergeRuns(sorter, a, b, offset, !aim, run, mi, hi);

        int k  = !aim ? run[lo] - offset : run[lo];
        int lo1 = aim ? run[lo] - offset : run[lo];
        int hi1 = aim ? run[mi] - offset : run[mi];
        int lo2 = aim ? run[mi] - offset : run[mi];
        int hi2 = aim ? run[hi] - offset : run[hi];

        long[] dst = aim ? a : b;
        long[] src = aim ? b : a;

        /*
         * Merge the left and right parts.
         */
        if (hi1 - lo1 > MIN_PARALLEL_SORT_SIZE && sorter != null) {
            new Merger<>(null, dst, k, src, lo1, hi1, lo2, hi2).invoke();
        } else {
            mergeParts(null, dst, k, src, lo1, hi1, lo2, hi2);
        }
    }

    /**
     * Merges the sorted parts.
     *
     * @param merger parallel context
     * @param dst the destination where parts are merged
     * @param k the start index of the destination, inclusive
     * @param a1 the first part // todo r
     * @param src the first part // todo a
     * @param lo1 the start index of the first part, inclusive
     * @param hi1 the end index of the first part, exclusive
     * @param a2 the second part // todo r
     * @param lo2 the start index of the second part, inclusive
     * @param hi2 the end index of the second part, exclusive
     */
    private static void mergeParts(Merger<long[]> merger, long[] dst, int k,
            long[] src, int lo1, int hi1, int lo2, int hi2) {

        /*
         * Merge the parts in parallel.
         */
        if (merger != null) {

            while (hi1 - lo1 > MIN_MERGE_PART_SIZE && hi2 - lo2 > MIN_MERGE_PART_SIZE) {

                /*
                 * The first part must be larger.
                 */
                if (hi1 - lo1 < hi2 - lo2) {
                    int lo = lo1; lo1 = lo2; lo2 = lo;
                    int hi = hi1; hi1 = hi2; hi2 = hi;
                }

                /*
                 * Find the median of the larger part.
                 */
                int mi1 = (lo1 + hi1) >>> 1;
                long key = src[mi1];
                int mi2 = hi2;

                /*
                 * Split the smaller part.
                 */
                for (int mid = lo2; mid < mi2; ) {
                    int d = (mid + mi2) >>> 1; // todo rename int m = ...

                    if (key > src[d]) {
                        mid = d + 1;
                    } else {
                        mi2 = d;
                    }
                }

                /*
                 * Merge other parts in parallel.
                 */
                merger.fork(k, lo1, mi1, lo2, mi2);

                /*
                 * Reserve space for the second parts.
                 */
                k += mi2 - lo2 + mi1 - lo1;

                /*
                 * Iterate along the second parts.
                 */
                lo1 = mi1;
                lo2 = mi2;
            }
        }

        /*
         * Merge small parts sequentially.
         */
        if (lo1 < hi1 && lo2 < hi2 && src[hi1 - 1] > src[lo2]) {

            if (src[hi1 - 1] < src[hi2 - 1]) {
                while (lo1 < hi1) {
                    long slo1 = src[lo1]; // todo rename: first, or key, or curr
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else if (src[hi1 - 1] > src[hi2 - 1]) {
                while (lo2 < hi2) {
                    long slo1 = src[lo1];
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else {
                while (lo1 < hi1 && lo2 < hi2) {
                    long slo1 = src[lo1];

                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            }
        }

        /*
         * Copy the tail of the left part.
         */
        if (lo1 < hi1) {
            System.arraycopy(src, lo1, dst, k, hi1 - lo1);
        }

        /*
         * Copy the tail of the right part.
         */
        if (lo2 < hi2) {
            System.arraycopy(src, lo2, dst, k + hi1 - lo1, hi2 - lo2);
        }
    }

    /**
     * Tries to sort the specified range of the array
     * using LSD (The Least Significant Digit) Radix sort.
     *
     * @param sorter // todo
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @return {@code true} if the array is finally sorted, otherwise {@code false}
     */
    static boolean tryRadixSort(Sorter<long[]> sorter, long[] a, int low, int high) {
        long[] b; int offset = low, size = high - low;

        /*
         * Allocate additional buffer.
         */
        if (sorter != null && (b = sorter.b) != null) {
            offset = sorter.offset;
        } else if ((b = tryAllocate(long[].class, size)) == null) {
            return false;
        }

        int start = low - offset;
        int last = high - offset;

        /*
         * Count the number of all digits.
         */
        int[] count1 = new int[1024];
        int[] count2 = new int[2048];
        int[] count3 = new int[2048];
        int[] count4 = new int[2048];
        int[] count5 = new int[2048];
        int[] count6 = new int[1024];

        for (int i = low; i < high; ++i) {
            ++count1[(int)  (a[i]         & 0x3FF)];
            ++count2[(int) ((a[i] >>> 10) & 0x7FF)];
            ++count3[(int) ((a[i] >>> 21) & 0x7FF)];
            ++count4[(int) ((a[i] >>> 32) & 0x7FF)];
            ++count5[(int) ((a[i] >>> 43) & 0x7FF)];
            ++count6[(int) ((a[i] >>> 54) ^ 0x200)]; // Reverse the sign bit
        }

        /*
         * Detect digits to be processed.
         */
        boolean processDigit1 = processDigit(count1, size, low);
        boolean processDigit2 = processDigit(count2, size, low);
        boolean processDigit3 = processDigit(count3, size, low);
        boolean processDigit4 = processDigit(count4, size, low);
        boolean processDigit5 = processDigit(count5, size, low);
        boolean processDigit6 = processDigit(count6, size, low);

        /*
         * Process the 1-st digit.
         */
        if (processDigit1) {
            for (int i = high; i > low; ) {
                b[--count1[(int) (a[--i] & 0x3FF)] - offset] = a[i];
            }
        }

        /*
         * Process the 2-nd digit.
         */
        if (processDigit2) {
            if (processDigit1) {
                for (int i = last; i > start; ) {
                    a[--count2[(int) ((b[--i] >>> 10) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count2[(int) ((a[--i] >>> 10) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 3-rd digit.
         */
        if (processDigit3) {
            if (processDigit1 ^ processDigit2) {
                for (int i = last; i > start; ) {
                    a[--count3[(int) ((b[--i] >>> 21) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count3[(int) ((a[--i] >>> 21) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 4-th digit.
         */
        if (processDigit4) {
            if (processDigit1 ^ processDigit2 ^ processDigit3) {
                for (int i = last; i > start; ) {
                    a[--count4[(int) ((b[--i] >>> 32) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count4[(int) ((a[--i] >>> 32) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 5-th digit.
         */
        if (processDigit5) {
            if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4) {
                for (int i = last; i > start; ) {
                    a[--count5[(int) ((b[--i] >>> 43) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count5[(int) ((a[--i] >>> 43) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 6-th digit.
         */
        if (processDigit6) {
            if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4 ^ processDigit5) {
                for (int i = last; i > start; ) {
                    a[--count6[(int) ((b[--i] >>> 54) ^ 0x200)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count6[(int) ((a[--i] >>> 54) ^ 0x200)] - offset] = a[i];
                }
            }
        }

        /*
         * Copy the buffer to original array, if we process ood number of digits.
         */
        if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4 ^ processDigit5 ^ processDigit6) {
            System.arraycopy(b, low - offset, a, low, size);
        }
        return true;
    }

    /**
     * Sorts the specified range of the array using heap sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void heapSort(long[] a, int low, int high) {
        for (int k = (low + high) >>> 1; k > low; ) {
            pushDown(a, --k, a[k], low, high);
        }
        while (--high > low) {
            long max = a[low];
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
    private static void pushDown(long[] a, int p, long value, int low, int high) {
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

// #[byte]

    /**
     * Sorts the specified range of the array using
     * counting sort or insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(byte[] a, int low, int high) {
        if (high - low > MIN_BYTE_COUNTING_SORT_SIZE) {
            countingSort(a, low, high);
        } else {
            insertionSort(a, low, high);
        }
    }

    /**
     * The number of distinct byte values.
     */
    private static final int NUM_BYTE_VALUES = 1 << 8;

    /**
     * Sorts the specified range of the array using counting sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void countingSort(byte[] a, int low, int high) {
        int[] count = new int[NUM_BYTE_VALUES];

        /*
         * Compute the histogram.
         */
        for (int i = high; i > low; ++count[a[--i] & 0xFF]);

        /*
         * Put values on their final positions.
         */
        if (high - low > NUM_BYTE_VALUES) {
            for (int i = Byte.MIN_VALUE; high > low; ) {
                for (int k = count[--i & 0xFF]; k > 0; --k) {
                    a[--high] = (byte) i;
                }
            }
        } else {
            for (int i = Byte.MIN_VALUE; high > low; ) {
                while (count[--i & 0xFF] == 0);

                int num = count[i & 0xFF];

                do {
                    a[--high] = (byte) i;
                } while (--num > 0);
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
    static void insertionSort(byte[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            byte ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
    }

// #[char]

    /**
     * Sorts the specified range of the array using
     * counting sort or Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(char[] a, int low, int high) {
        if (high - low > MIN_CHAR_COUNTING_SORT_SIZE) {
            countingSort(a, low, high);
        } else {
            sort(a, 0, low, high);
        }
    }

    /**
     * The number of distinct char values.
     */
    private static final int NUM_CHAR_VALUES = 1 << 16;

    /**
     * Sorts the specified range of the array using counting sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void countingSort(char[] a, int low, int high) {
        int[] count = new int[NUM_CHAR_VALUES];

        /*
         * Compute the histogram.
         */
        for (int i = high; i > low; ++count[a[--i]]);

        /*
         * Put values on their final positions.
         */
        if (high - low > NUM_CHAR_VALUES) {
            for (int i = NUM_CHAR_VALUES; i > 0; ) {
//              for (low = high - count[--i]; high > low; ) { // todo
//                  a[--high] = (char) i;
//              }
                for (low = high - count[--i]; high > low;
                    a[--high] = (char) i
                );
            }
        } else {
            for (int i = NUM_CHAR_VALUES; high > low; ) {
                while (count[--i] == 0);

                int c = count[i]; // todo num

                do {
                    a[--high] = (char) i;
                } while (--c > 0);
            }
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(char[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Invoke insertion sort on small part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                insertionSort(a, low, high);
                return;
            }

            /*
             * Switch to counting sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                countingSort(a, low, high);
                return;
            }

            /*
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            char a3 = a[e3];

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { char t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { char t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { char t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { char t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { char t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partitionWithTwoPivots(a, low, high, e1, e5);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                sort(a, bits | 1, indices[0] + 1, indices[1]);
                sort(a, bits | 1, indices[1] + 1, high);

            } else { // Partitioning with one pivot

                indices = partitionWithOnePivot(a, low, high, e3, e3);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                sort(a, bits | 1, indices[1], high);
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            char[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final char pivot1 = a[pivotIndex1]; // todo final
        final char pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            char ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            char[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final char pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            char ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(char[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            char ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
    }

// #[short]

    /**
     * Sorts the specified range of the array using
     * counting sort or Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(short[] a, int low, int high) {
        if (high - low > MIN_SHORT_COUNTING_SORT_SIZE) {
            countingSort(a, low, high);
        } else {
            sort(a, 0, low, high);
        }
    }

    /**
     * The number of distinct short values.
     */
    private static final int NUM_SHORT_VALUES = 1 << 16;

    /**
     * Sorts the specified range of the array using counting sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    private static void countingSort(short[] a, int low, int high) {
        int[] count = new int[NUM_SHORT_VALUES];

        /*
         * Compute the histogram.
         */
        for (int i = high; i > low; ++count[a[--i] & 0xFFFF]);

        /*
         * Place values on their final positions.
         */
  
        if (high - low > NUM_SHORT_VALUES) {
            for (int i = Short.MIN_VALUE; high > low; ) {
                for (int k = count[--i & 0xFFFF]; k > 0; --k) {
                    a[--high] = (short) i;
                }
            }
        } else {
            for (int i = Short.MIN_VALUE; high > low; ) {
                while (count[--i & 0xFFFF] == 0);

                int num = count[i & 0xFFFF];

                do {
                    a[--high] = (short) i;
                } while (--num > 0);
            }
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(short[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Invoke insertion sort on small part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                insertionSort(a, low, high);
                return;
            }

            /*
             * Switch to counting sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                countingSort(a, low, high);
                return;
            }

            /*
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            short a3 = a[e3];

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { short t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { short t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { short t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { short t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { short t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partitionWithTwoPivots(a, low, high, e1, e5);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                sort(a, bits | 1, indices[0] + 1, indices[1]);
                sort(a, bits | 1, indices[1] + 1, high);

            } else { // Partitioning with one pivot

                indices = partitionWithOnePivot(a, low, high, e3, e3);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                sort(a, bits | 1, indices[1], high);
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            short[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final short pivot1 = a[pivotIndex1]; // todo final
        final short pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            short ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            short[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final short pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            short ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(short[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            short ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
    }

// #[float]

    /**
     * The number of distinct short values. // todo
     todo: double
     */
    private static final int FLOAT_NEGATIVE_ZERO = Float.floatToRawIntBits(-0.0f);

    /**
     * Sorts the specified range of the array using parallel merge
     * sort and/or Dual-Pivot Quicksort.<p>
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
    static void sort(float[] a, int parallelism, int low, int high) {
        /*
         * Phase 1. Count the number of negative zero -0.0f,
         * turn them into positive zero, and move all NaNs
         * to the end of the array.
         */
        int numNegativeZero = 0; // todo rename negativeZeroCount

        for (int k = high; k > low; ) {
            float ak = a[--k];

            if (Float.floatToRawIntBits(ak) == FLOAT_NEGATIVE_ZERO) { // ak is -0.0f    // todo double
                numNegativeZero++;
                a[k] = 0.0f;
            } else if (ak != ak) { // ak is NaN
                a[k] = a[--high];
                a[high] = ak;
            }
        }

        /*
         * Phase 2. Sort everything except NaNs,
         * which are already in place.
         */
        if (parallelism > 1 && high - low > MIN_PARALLEL_SORT_SIZE) {
            new Sorter<>(a, parallelism, low, high - low, 0).invoke();
        } else {
            sort(null, a, 0, low, high);
        }

        /*
         * Phase 3. Turn positive zero 0.0f
         * back into negative zero -0.0f.
         */
        if (++numNegativeZero == 1) {
            return;
        }

        /*
         * Find the position one less than
         * the index of the first zero.
         */
        while (low <= high) {
            int middle = (low + high) >>> 1;

            if (a[middle] < 0.0f) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        /*
         * Replace the required number of 0.0f by -0.0f.
         */
        while (--numNegativeZero > 0) {
            a[++high] = -0.0f;
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(Sorter<float[]> sorter, float[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Run adaptive mixed insertion sort on small non-leftmost parts.
             */
            if (size < MAX_INSERTION_SORT_SIZE + bits && (bits & 1) > 0) {
                sort(float.class, a, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::mixedInsertionSort);
                return;
            }

            /*
             * Invoke insertion sort on small leftmost part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                sort(float.class, a, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                        low, high, DualPivotQuicksort_r32::insertionSort);
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
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            float a3 = a[e3];

            /*
             * Check if part is large and contains random
             * data, taking into account parallel context.
             */
            boolean isLargeRandom =
                sorter != null && bits > 2 && size > MIN_RADIX_SORT_SIZE &&
//              size > MIN_RADIX_SORT_SIZE && (sorter == null || bits > 0) &&
                (a[e1] > a[e2] || a[e2] > a3 || a3 > a[e4] || a[e4] > a[e5]);

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { float t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { float t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { float t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { float t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { float t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * Try Radix sort on large fully random data.
             */
            if (isLargeRandom
                    && a[e2] < a[e3] && a[e3] < a[e4]
                    && tryRadixSort(sorter, a, low, high)) {
                return;
            }

            /*
             * Switch to heap sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                heapSort(a, low, high);
                return;
            }

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partition(float.class, a, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                    low, high, e1, e5, DualPivotQuicksort_r32::partitionWithTwoPivots);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[0] + 1, indices[1]);
                    sorter.fork(bits | 1, indices[1] + 1, high);
                } else {
                    sort(sorter, a, bits | 1, indices[0] + 1, indices[1]);
                    sort(sorter, a, bits | 1, indices[1] + 1, high);
                }

            } else { // Partitioning with one pivot

                indices = partition(float.class, a, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                    low, high, e3, e3, DualPivotQuicksort_r32::partitionWithOnePivot);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[1], high);
                } else {
                    sort(sorter, a, bits | 1, indices[1], high);
                }
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            float[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final float pivot1 = a[pivotIndex1]; // todo final
        final float pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            float ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            float[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final float pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            float ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using mixed insertion sort.<p>
     *
     * Mixed insertion sort is combination of pin insertion sort,
     * simple insertion sort and pair insertion sort.<p>
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
    static void mixedInsertionSort(float[] a, int low, int high) {

        /*
         * Split part for pin and pair insertion sorts.
         */
        int end = high - 3 * ((high - low) >> 3 << 1);

        /*
         * Invoke simple insertion sort on small part.
         */
        if (end == high) {
            for (int i; ++low < high; ) {
                float ai = a[i = low];

                while (ai < a[i - 1]) {
                    a[i] = a[--i];
                }
                a[i] = ai;
            }
            return;
        }

        /*
         * Start with pin insertion sort.
         */
        for (int i, p = high; ++low < end; ) {
            float ai = a[i = low], pin = a[--p];

            /*
             * Swap larger element with pin.
             */
            if (ai > pin) {
                ai = pin;
                a[p] = a[i];
            }

            /*
             * Insert element into sorted part.
             */
            while (ai < a[i - 1]) {
                a[i] = a[--i];
            }
            a[i] = ai;
        }

        /*
         * Finish with pair insertion sort.
         */
        for (int i; low < high; ++low) {
            float a1 = a[i = low], a2 = a[++low];

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

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(float[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            float ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
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
    static boolean tryMergingSort(Sorter<float[]> sorter, float[] a, int low, int high) {

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
                    float ai = a[i]; a[i] = a[j]; a[j] = ai;
                }
            } else { // Identify constant sequence
                for (float ak = a[k]; ++k < high && ak == a[k]; );

                if (k < high) {
                    continue;
                }
            }

            /*
             * Terminate scanning, if the runs are too small.
             */
            if (k - low < count * MIN_RUN_SIZE) {
                return false;
            }

            /*
             * Process the current run.
             */
            if (run == null) {

                if (k == high) {

                    /*
                     * Array is monotonous sequence
                     * and therefore already sorted.
                     */
                    return true;
                }
                run = new int[ Math.min((high - low) >> 6, MAX_RUN_CAPACITY) | 8];
                run[0] = low;

            } else if (a[last - 1] > a[last]) { // Start the new run

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
            float[] b; int offset = low;

            if (sorter != null && (b = sorter.b) != null) {
                offset = sorter.offset;
            } else if ((b = tryAllocate(float[].class, high - low)) == null) {
                return false;
            }
            mergeRuns(sorter, a, b, offset, true, run, 0, count);
        }
        return true;
    }

    /**
     * Merges the specified runs.
     *
     * @param sorter parallel context
     * @param a the source array
     * @param b the temporary buffer used in merging
     * @param offset the start index in the source, inclusive
     * @param aim specifies merging: to source (+1), buffer (-1) or any (0) // todo javadoc update
     * @param run the start indexes of the runs, inclusive
     * @param lo the start index of the first run, inclusive
     * @param hi the start index of the last run, inclusive
     * @return the destination where the runs are merged // todo r
     */
    private static void mergeRuns(Sorter<float[]> sorter, float[] a, float[] b, int offset,
            boolean aim, int[] run, int lo, int hi) {

        if (hi - lo == 1) {
            if (!aim) {
                System.arraycopy(a, run[lo], b, run[lo] - offset, run[hi] - run[lo]);
            }
            return;
        }

        /*
         * Split the array into two approximately equal parts.
         */
        int mi = lo, rmi = (run[lo] + run[hi]) >>> 1;
        while (run[++mi + 1] <= rmi);

        /*
         * Merge the runs of all parts.
         */
        mergeRuns(sorter, a, b, offset, !aim, run, lo, mi);
        mergeRuns(sorter, a, b, offset, !aim, run, mi, hi);

        int k  = !aim ? run[lo] - offset : run[lo];
        int lo1 = aim ? run[lo] - offset : run[lo];
        int hi1 = aim ? run[mi] - offset : run[mi];
        int lo2 = aim ? run[mi] - offset : run[mi];
        int hi2 = aim ? run[hi] - offset : run[hi];

        float[] dst = aim ? a : b;
        float[] src = aim ? b : a;

        /*
         * Merge the left and right parts.
         */
        if (hi1 - lo1 > MIN_PARALLEL_SORT_SIZE && sorter != null) {
            new Merger<>(null, dst, k, src, lo1, hi1, lo2, hi2).invoke();
        } else {
            mergeParts(null, dst, k, src, lo1, hi1, lo2, hi2);
        }
    }

    /**
     * Merges the sorted parts.
     *
     * @param merger parallel context
     * @param dst the destination where parts are merged
     * @param k the start index of the destination, inclusive
     * @param a1 the first part // todo r
     * @param src the first part // todo a
     * @param lo1 the start index of the first part, inclusive
     * @param hi1 the end index of the first part, exclusive
     * @param a2 the second part // todo r
     * @param lo2 the start index of the second part, inclusive
     * @param hi2 the end index of the second part, exclusive
     */
    private static void mergeParts(Merger<float[]> merger, float[] dst, int k,
            float[] src, int lo1, int hi1, int lo2, int hi2) {

        /*
         * Merge the parts in parallel.
         */
        if (merger != null) {

            while (hi1 - lo1 > MIN_MERGE_PART_SIZE && hi2 - lo2 > MIN_MERGE_PART_SIZE) {

                /*
                 * The first part must be larger.
                 */
                if (hi1 - lo1 < hi2 - lo2) {
                    int lo = lo1; lo1 = lo2; lo2 = lo;
                    int hi = hi1; hi1 = hi2; hi2 = hi;
                }

                /*
                 * Find the median of the larger part.
                 */
                int mi1 = (lo1 + hi1) >>> 1;
                float key = src[mi1];
                int mi2 = hi2;

                /*
                 * Split the smaller part.
                 */
                for (int mid = lo2; mid < mi2; ) {
                    int d = (mid + mi2) >>> 1; // todo rename int m = ...

                    if (key > src[d]) {
                        mid = d + 1;
                    } else {
                        mi2 = d;
                    }
                }

                /*
                 * Merge other parts in parallel.
                 */
                merger.fork(k, lo1, mi1, lo2, mi2);

                /*
                 * Reserve space for the second parts.
                 */
                k += mi2 - lo2 + mi1 - lo1;

                /*
                 * Iterate along the second parts.
                 */
                lo1 = mi1;
                lo2 = mi2;
            }
        }

        /*
         * Merge small parts sequentially.
         */
        if (lo1 < hi1 && lo2 < hi2 && src[hi1 - 1] > src[lo2]) {

            if (src[hi1 - 1] < src[hi2 - 1]) {
                while (lo1 < hi1) {
                    float slo1 = src[lo1]; // todo rename: first, or key, or curr
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else if (src[hi1 - 1] > src[hi2 - 1]) {
                while (lo2 < hi2) {
                    float slo1 = src[lo1];
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else {
                while (lo1 < hi1 && lo2 < hi2) {
                    float slo1 = src[lo1];

                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            }
        }

        /*
         * Copy the tail of the left part.
         */
        if (lo1 < hi1) {
            System.arraycopy(src, lo1, dst, k, hi1 - lo1);
        }

        /*
         * Copy the tail of the right part.
         */
        if (lo2 < hi2) {
            System.arraycopy(src, lo2, dst, k + hi1 - lo1, hi2 - lo2);
        }
    }

    /**
     * Tries to sort the specified range of the array
     * using LSD (The Least Significant Digit) Radix sort.
     *
     * @param a the array to be sorted
     * @param sorter // todo
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @return {@code true} if the array is finally sorted, otherwise {@code false}
     */
    static boolean tryRadixSort(Sorter<float[]> sorter, float[] a, int low, int high) {
        float[] b; int offset = low, size = high - low;

        /*
         * Allocate additional buffer.
         */
        if (sorter != null && (b = sorter.b) != null) {
            offset = sorter.offset;
        } else if ((b = tryAllocate(float[].class, size)) == null) {
            return false;
        }

        int start = low - offset;
        int last = high - offset;

        /*
         * Count the number of all digits.
         */
        int[] count1 = new int[1024];
        int[] count2 = new int[2048];
        int[] count3 = new int[2048];

        for (int i = low; i < high; ++i) {
            ++count1[ fti(a[i])         & 0x3FF];
            ++count2[(fti(a[i]) >>> 10) & 0x7FF];
            ++count3[(fti(a[i]) >>> 21) & 0x7FF];
        }

        /*
         * Detect digits to be processed.
         */
        boolean processDigit1 = processDigit(count1, size, low);
        boolean processDigit2 = processDigit(count2, size, low);
        boolean processDigit3 = processDigit(count3, size, low);

        /*
         * Process the 1-st digit.
         */
        if (processDigit1) {
            for (int i = high; i > low; ) {
                b[--count1[fti(a[--i]) & 0x3FF] - offset] = a[i];
            }
        }

        /*
         * Process the 2-nd digit.
         */
        if (processDigit2) {
            if (processDigit1) {
                for (int i = last; i > start; ) {
                    a[--count2[(fti(b[--i]) >>> 10) & 0x7FF]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count2[(fti(a[--i]) >>> 10) & 0x7FF] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 3-rd digit.
         */
        if (processDigit3) {
            if (processDigit1 ^ processDigit2) {
                for (int i = last; i > start; ) {
                    a[--count3[(fti(b[--i]) >>> 21) & 0x7FF]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count3[(fti(a[--i]) >>> 21) & 0x7FF] - offset] = a[i];
                }
            }
        }

        /*
         * Copy the buffer to original array, if we process ood number of digits.
         */
        if (processDigit1 ^ processDigit2 ^ processDigit3) {
            System.arraycopy(b, low - offset, a, low, size);
        }
        return true;
    }

    /**
     * Returns masked bits that represent the float value.
     *
     * @param f the given value
     * @return masked bits
     */
    private static int fti(float f) {
        int x = Float.floatToRawIntBits(f);
        return x ^ ((x >> 31) | 0x80000000);
    }

    /**
     * Sorts the specified range of the array using heap sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void heapSort(float[] a, int low, int high) {
        for (int k = (low + high) >>> 1; k > low; ) {
            pushDown(a, --k, a[k], low, high);
        }
        while (--high > low) {
            float max = a[low];
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
    private static void pushDown(float[] a, int p, float value, int low, int high) {
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

// #[double]

    /**
     * The number of distinct short values. // todo
     todo: double
     */
    private static final long DOUBLE_NEGATIVE_ZERO = Double.doubleToRawLongBits(-0.0d);

    /**
     * Sorts the specified range of the array using parallel merge
     * sort and/or Dual-Pivot Quicksort.<p>
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
    static void sort(double[] a, int parallelism, int low, int high) {
        /*
         * Phase 1. Count the number of negative zero -0.0d,
         * turn them into positive zero, and move all NaNs
         * to the end of the array.
         */
        int numNegativeZero = 0; // todo rename negativeZeroCount

        for (int k = high; k > low; ) {
            double ak = a[--k];

            if (Double.doubleToRawLongBits(ak) == DOUBLE_NEGATIVE_ZERO) { // ak is -0.0f    // todo double
                numNegativeZero++;
                a[k] = 0.0d;
            } else if (ak != ak) { // ak is NaN
                a[k] = a[--high];
                a[high] = ak;
            }
        }

        /*
         * Phase 2. Sort everything except NaNs,
         * which are already in place.
         */
        if (parallelism > 1 && high - low > MIN_PARALLEL_SORT_SIZE) {
            new Sorter<>(a, parallelism, low, high - low, 0).invoke();
        } else {
            sort(null, a, 0, low, high);
        }

        /*
         * Phase 3. Turn positive zero 0.0d
         * back into negative zero -0.0d.
         */
        if (++numNegativeZero == 1) {
            return;
        }

        /*
         * Find the position one less than
         * the index of the first zero.
         */
        while (low <= high) {
            int middle = (low + high) >>> 1;

            if (a[middle] < 0.0d) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        /*
         * Replace the required number of 0.0d by -0.0d.
         */
        while (--numNegativeZero > 0) {
            a[++high] = -0.0d;
        }
    }

    /**
     * Sorts the specified range of the array using Dual-Pivot Quicksort.
     *
     * @param sorter parallel context
     * @param a the array to be sorted
     * @param bits the combination of recursion depth and bit flag, where
     *        the right bit "0" indicates that range is the leftmost part
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void sort(Sorter<double[]> sorter, double[] a, int bits, int low, int high) {
        while (true) {
            int size = high - low;

            /*
             * Run adaptive mixed insertion sort on small non-leftmost parts.
             */
            if (size < MAX_INSERTION_SORT_SIZE + bits && (bits & 1) > 0) {
                sort(double.class, a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::mixedInsertionSort);
                return;
            }

            /*
             * Invoke insertion sort on small leftmost part.
             */
            if (size < MAX_INSERTION_SORT_SIZE) {
                sort(double.class, a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                    low, high, DualPivotQuicksort_r32::insertionSort);
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
             * Divide the given array into the golden ratio using
             * an inexpensive approximation to select five sample
             * elements and determine pivots.
             */
            int step = (size >> 2) + (size >> 3) + (size >> 7);

            /*
             * Five elements around (and including) the central element
             * will be used for pivot selection as described below. The
             * unequal choice of spacing these elements was empirically
             * determined to work well on a wide variety of inputs.
             */
            int e1 = low + step;
            int e5 = high - step;
            int e3 = (e1 + e5) >>> 1;
            int e2 = (e1 + e3) >>> 1;
            int e4 = (e3 + e5) >>> 1;
            double a3 = a[e3];

            /*
             * Check if part is large and contains random
             * data, taking into account parallel context.
             */
            boolean isLargeRandom =
                sorter != null && bits > 2 && size > MIN_RADIX_SORT_SIZE &&
//              size > MIN_RADIX_SORT_SIZE && (sorter == null || bits > 0) &&
                (a[e1] > a[e2] || a[e2] > a3 || a3 > a[e4] || a[e4] > a[e5]);

            /*
             * Sort these elements in-place by the combination
             * of 4-element sorting network and insertion sort.
             *
             *   1 ---------o---------------o-----------------
             *              |               |
             *   2 ---------|-------o-------o-------o---------
             *              |       |               |
             *   4 ---------o-------|-------o-------o---------
             *                      |       |
             *   5 -----------------o-------o-----------------
             */
            if (a[e1] > a[e4]) { double t = a[e1]; a[e1] = a[e4]; a[e4] = t; }
            if (a[e2] > a[e5]) { double t = a[e2]; a[e2] = a[e5]; a[e5] = t; }
            if (a[e4] > a[e5]) { double t = a[e4]; a[e4] = a[e5]; a[e5] = t; }
            if (a[e1] > a[e2]) { double t = a[e1]; a[e1] = a[e2]; a[e2] = t; }
            if (a[e2] > a[e4]) { double t = a[e2]; a[e2] = a[e4]; a[e4] = t; }

            /*
             * Insert the third element.
             */
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

            /*
             * Try Radix sort on large fully random data.
             */
            if (isLargeRandom
                    && a[e2] < a[e3] && a[e3] < a[e4]
                    && tryRadixSort(sorter, a, low, high)) {
                return;
            }

            /*
             * Switch to heap sort, if execution time is quadratic.
             */
            if ((bits += 2) > MAX_RECURSION_DEPTH) {
                heapSort(a, low, high);
                return;
            }

            /*
             * indices[0] - the index of the last element of the left part
             * indices[1] - the index of the first element of the right part
             */
            int[] indices;

            /*
             * Partitioning with two pivots on array of fully random elements.
             */
            if (a[e1] < a[e2] && a[e2] < a[e3] && a[e3] < a[e4] && a[e4] < a[e5]) {

                indices = partition(double.class, a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                    low, high, e1, e5, DualPivotQuicksort_r32::partitionWithTwoPivots);

                /*
                 * Sort non-left parts recursively (possibly in parallel),
                 * excluding known pivots.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[0] + 1, indices[1]);
                    sorter.fork(bits | 1, indices[1] + 1, high);
                } else {
                    sort(sorter, a, bits | 1, indices[0] + 1, indices[1]);
                    sort(sorter, a, bits | 1, indices[1] + 1, high);
                }

            } else { // Partitioning with one pivot

                indices = partition(double.class, a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                    low, high, e3, e3, DualPivotQuicksort_r32::partitionWithOnePivot);

                /*
                 * Sort the right part (possibly in parallel), excluding
                 * known pivot. All elements from the central part are
                 * equal and therefore already sorted.
                 */
                if (size > MIN_PARALLEL_SORT_SIZE && sorter != null) {
                    sorter.fork(bits | 1, indices[1], high);
                } else {
                    sort(sorter, a, bits | 1, indices[1], high);
                }
            }
            high = indices[0]; // Iterate along the left part
        }
    }

    /**
     * Partitions the specified range of the array using two given pivots.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of pivot1, the first pivot
     * @param pivotIndex2 the index of pivot2, the second pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithTwoPivots(
            double[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = --high;
        int lower = low;

        /*
         * Use the first and fifth of the five sorted elements as
         * the pivots. These values are inexpensive approximation
         * of tertiles. Note, that pivot1 < pivot2.
         */
        final double pivot1 = a[pivotIndex1]; // todo final
        final double pivot2 = a[pivotIndex2];

        /*
         * The first and the last elements to be sorted are moved
         * to the locations formerly occupied by the pivots. When
         * partitioning is completed, the pivots are swapped back
         * into their final positions, and excluded from the next
         * subsequent sorting.
         */
        a[pivotIndex1] = a[lower];
        a[pivotIndex2] = a[upper];

        /*
         * Skip elements, which are less or greater than the pivots.
         */
        while (a[++lower] < pivot1);
        while (a[--upper] > pivot2);

        /*
         * Backward 3-interval partitioning
         *
         *     left part                     central part          right part
         * +--------------+----------+--------------------------+--------------+        // todo m ---+---
         * |   < pivot1   |    ?     |  pivot1 <= .. <= pivot2  |   > pivot2   |
         * +--------------+----------+--------------------------+--------------+
         *               ^          ^                            ^
         *               |          |                            |
         *             lower        k                          upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int unused = --lower, k = ++upper; --k > lower; ) {
            double ak = a[k];

            if (ak < pivot1) { // Move a[k] to the left part
                while (a[++lower] < pivot1);

                if (lower > k) {
                    lower = k;
                    break;
                }
                if (a[lower] > pivot2) {
                    a[k] = a[--upper];
                    a[upper] = a[lower];
                } else {
                    a[k] = a[lower];
                }
                a[lower] = ak;
            } else if (ak > pivot2) { // Move a[k] to the right part
                a[k] = a[--upper];
                a[upper] = ak;
            }
        }

        /*
         * Swap the pivots into their final positions.
         */
        a[low]  = a[lower]; a[lower] = pivot1;
        a[high] = a[upper]; a[upper] = pivot2;

        return new int[] { lower, upper };
    }

    /**
     * Partitions the specified range of the array using one given pivot.
     *
     * @param a the array for partitioning
     * @param low the index of the first element, inclusive, for partitioning
     * @param high the index of the last element, exclusive, for partitioning
     * @param pivotIndex1 the index of single pivot
     * @param pivotIndex2 the index of single pivot
     * @return indices of parts after partitioning
     */
    private static int[] partitionWithOnePivot(
            double[] a, int low, int high, int pivotIndex1, int pivotIndex2) {

        /*
         * Pointers to the right and left parts.
         */
        int upper = high;
        int lower = low;

        /*
         * Use the third of the five sorted elements as the pivot.
         * This value is inexpensive approximation of the median.
         */
        final double pivot = a[pivotIndex1];

        /*
         * The first element to be sorted is moved to the
         * location formerly occupied by the pivot. After
         * completion of partitioning the pivot is swapped
         * back into its final position, and excluded from
         * the next subsequent sorting.
         */
        a[pivotIndex1] = a[lower];

        /*
         * Dutch National Flag partitioning
         *
         *     left part               central part    right part
         * +------------------------------------------------------+ // todo ---+----
         * |   < pivot    |    ?     |   == pivot   |   > pivot   |
         * +------------------------------------------------------+
         *               ^          ^                ^
         *               |          |                |
         *             lower        k              upper
         *
         * Pointer k is the last index of ?-part
         * Pointer lower is the last index of left part
         * Pointer upper is the first index of right part
         */
        for (int k = upper; --k > lower; ) {
            double ak = a[k];

            if (ak == pivot) {
                continue;
            }
            a[k] = pivot;

            if (ak < pivot) { // Move a[k] to the left part
                while (a[++lower] < pivot);

                if (a[lower] > pivot) {
                    a[--upper] = a[lower];
                }
                a[lower] = ak;
            } else { // ak > pivot - Move a[k] to the right part
                a[--upper] = ak;
            }
        }

        /*
         * Swap the pivot into its final position.
         */
        a[low] = a[lower]; a[lower] = pivot;

        return new int[] { lower, upper };
    }

    /**
     * Sorts the specified range of the array using mixed insertion sort.<p>
     *
     * Mixed insertion sort is combination of pin insertion sort,
     * simple insertion sort and pair insertion sort.<p>
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
    static void mixedInsertionSort(double[] a, int low, int high) {

        /*
         * Split part for pin and pair insertion sorts.
         */
        int end = high - 3 * ((high - low) >> 3 << 1);

        /*
         * Invoke simple insertion sort on small part.
         */
        if (end == high) {
            for (int i; ++low < high; ) {
                double ai = a[i = low];

                while (ai < a[i - 1]) {
                    a[i] = a[--i];
                }
                a[i] = ai;
            }
            return;
        }

        /*
         * Start with pin insertion sort.
         */
        for (int i, p = high; ++low < end; ) {
            double ai = a[i = low], pin = a[--p];

            /*
             * Swap larger element with pin.
             */
            if (ai > pin) {
                ai = pin;
                a[p] = a[i];
            }

            /*
             * Insert element into sorted part.
             */
            while (ai < a[i - 1]) {
                a[i] = a[--i];
            }
            a[i] = ai;
        }

        /*
         * Finish with pair insertion sort.
         */
        for (int i; low < high; ++low) {
            double a1 = a[i = low], a2 = a[++low];

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

    /**
     * Sorts the specified range of the array using insertion sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void insertionSort(double[] a, int low, int high) {
        for (int i, k = low; ++k < high; ) {
            double ai = a[i = k];

            if (ai < a[i - 1]) {
                do {
                    a[i] = a[--i];
                } while (i > low && ai < a[i - 1]);

                a[i] = ai;
            }
        }
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
    static boolean tryMergingSort(Sorter<double[]> sorter, double[] a, int low, int high) {

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
                    double ai = a[i]; a[i] = a[j]; a[j] = ai;
                }
            } else { // Identify constant sequence
                for (double ak = a[k]; ++k < high && ak == a[k]; );

                if (k < high) {
                    continue;
                }
            }

            /*
             * Terminate scanning, if the runs are too small.
             */
            if (k - low < count * MIN_RUN_SIZE) {
                return false;
            }

            /*
             * Process the current run.
             */
            if (run == null) {

                if (k == high) {

                    /*
                     * Array is monotonous sequence
                     * and therefore already sorted.
                     */
                    return true;
                }
                run = new int[ Math.min((high - low) >> 6, MAX_RUN_CAPACITY) | 8];
                run[0] = low;

            } else if (a[last - 1] > a[last]) { // Start the new run

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
            double[] b; int offset = low;

            if (sorter != null && (b = sorter.b) != null) {
                offset = sorter.offset;
            } else if ((b = tryAllocate(double[].class, high - low)) == null) {
                return false;
            }
            mergeRuns(sorter, a, b, offset, true, run, 0, count);
        }
        return true;
    }

    /**
     * Merges the specified runs.
     *
     * @param sorter parallel context
     * @param a the source array
     * @param b the temporary buffer used in merging
     * @param offset the start index in the source, inclusive
     * @param aim specifies merging: to source (+1), buffer (-1) or any (0) // todo javadoc update
     * @param run the start indexes of the runs, inclusive
     * @param lo the start index of the first run, inclusive
     * @param hi the start index of the last run, inclusive
     * @return the destination where the runs are merged // todo r
     */
    private static void mergeRuns(Sorter<double[]> sorter, double[] a, double[] b, int offset,
            boolean aim, int[] run, int lo, int hi) {

        if (hi - lo == 1) {
            if (!aim) {
                System.arraycopy(a, run[lo], b, run[lo] - offset, run[hi] - run[lo]);
            }
            return;
        }

        /*
         * Split the array into two approximately equal parts.
         */
        int mi = lo, rmi = (run[lo] + run[hi]) >>> 1;
        while (run[++mi + 1] <= rmi);

        /*
         * Merge the runs of all parts.
         */
        mergeRuns(sorter, a, b, offset, !aim, run, lo, mi);
        mergeRuns(sorter, a, b, offset, !aim, run, mi, hi);

        int k  = !aim ? run[lo] - offset : run[lo];
        int lo1 = aim ? run[lo] - offset : run[lo];
        int hi1 = aim ? run[mi] - offset : run[mi];
        int lo2 = aim ? run[mi] - offset : run[mi];
        int hi2 = aim ? run[hi] - offset : run[hi];

        double[] dst = aim ? a : b;
        double[] src = aim ? b : a;

        /*
         * Merge the left and right parts.
         */
        if (hi1 - lo1 > MIN_PARALLEL_SORT_SIZE && sorter != null) {
            new Merger<>(null, dst, k, src, lo1, hi1, lo2, hi2).invoke();
        } else {
            mergeParts(null, dst, k, src, lo1, hi1, lo2, hi2);
        }
    }

    /**
     * Merges the sorted parts.
     *
     * @param merger parallel context
     * @param dst the destination where parts are merged
     * @param k the start index of the destination, inclusive
     * @param a1 the first part // todo r
     * @param src the first part // todo a
     * @param lo1 the start index of the first part, inclusive
     * @param hi1 the end index of the first part, exclusive
     * @param a2 the second part // todo r
     * @param lo2 the start index of the second part, inclusive
     * @param hi2 the end index of the second part, exclusive
     */
    private static void mergeParts(Merger<double[]> merger, double[] dst, int k,
            double[] src, int lo1, int hi1, int lo2, int hi2) {

        /*
         * Merge the parts in parallel.
         */
        if (merger != null) {

            while (hi1 - lo1 > MIN_MERGE_PART_SIZE && hi2 - lo2 > MIN_MERGE_PART_SIZE) {

                /*
                 * The first part must be larger.
                 */
                if (hi1 - lo1 < hi2 - lo2) {
                    int lo = lo1; lo1 = lo2; lo2 = lo;
                    int hi = hi1; hi1 = hi2; hi2 = hi;
                }

                /*
                 * Find the median of the larger part.
                 */
                int mi1 = (lo1 + hi1) >>> 1;
                double key = src[mi1];
                int mi2 = hi2;

                /*
                 * Split the smaller part.
                 */
                for (int mid = lo2; mid < mi2; ) {
                    int d = (mid + mi2) >>> 1; // todo rename int m = ...

                    if (key > src[d]) {
                        mid = d + 1;
                    } else {
                        mi2 = d;
                    }
                }

                /*
                 * Merge other parts in parallel.
                 */
                merger.fork(k, lo1, mi1, lo2, mi2);

                /*
                 * Reserve space for the second parts.
                 */
                k += mi2 - lo2 + mi1 - lo1;

                /*
                 * Iterate along the second parts.
                 */
                lo1 = mi1;
                lo2 = mi2;
            }
        }

        /*
         * Merge small parts sequentially.
         */
        if (lo1 < hi1 && lo2 < hi2 && src[hi1 - 1] > src[lo2]) {

            if (src[hi1 - 1] < src[hi2 - 1]) {
                while (lo1 < hi1) {
                    double slo1 = src[lo1]; // todo rename: first, or key, or curr
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else if (src[hi1 - 1] > src[hi2 - 1]) {
                while (lo2 < hi2) {
                    double slo1 = src[lo1];
  
                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            } else {
                while (lo1 < hi1 && lo2 < hi2) {
                    double slo1 = src[lo1];

                    if (slo1 <= src[lo2]) {
                        dst[k++] = src[lo1++];
                    }
                    if (slo1 >= src[lo2]) {
                        dst[k++] = src[lo2++];
                    }
                }
            }
        }

        /*
         * Copy the tail of the left part.
         */
        if (lo1 < hi1) {
            System.arraycopy(src, lo1, dst, k, hi1 - lo1);
        }

        /*
         * Copy the tail of the right part.
         */
        if (lo2 < hi2) {
            System.arraycopy(src, lo2, dst, k + hi1 - lo1, hi2 - lo2);
        }
    }

    /**
     * Tries to sort the specified range of the array
     * using LSD (The Least Significant Digit) Radix sort.
     *
     * @param a the array to be sorted
     * @param sorter // todo
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     * @return {@code true} if the array is finally sorted, otherwise {@code false}
     */
    static boolean tryRadixSort(Sorter<double[]> sorter, double[] a, int low, int high) {
        double[] b; int offset = low, size = high - low;

        /*
         * Allocate additional buffer.
         */
        if (sorter != null && (b = sorter.b) != null) {
            offset = sorter.offset;
        } else if ((b = tryAllocate(double[].class, size)) == null) {
            return false;
        }

        int start = low - offset;
        int last = high - offset;

        /*
         * Count the number of all digits.
         */
        int[] count1 = new int[1024];
        int[] count2 = new int[2048];
        int[] count3 = new int[2048];
        int[] count4 = new int[2048];
        int[] count5 = new int[2048];
        int[] count6 = new int[1024];

        for (int i = low; i < high; ++i) {
            ++count1[(int)  (dtl(a[i])         & 0x3FF)];
            ++count2[(int) ((dtl(a[i]) >>> 10) & 0x7FF)];
            ++count3[(int) ((dtl(a[i]) >>> 21) & 0x7FF)];
            ++count4[(int) ((dtl(a[i]) >>> 32) & 0x7FF)];
            ++count5[(int) ((dtl(a[i]) >>> 43) & 0x7FF)];
            ++count6[(int) ((dtl(a[i]) >>> 54) & 0x3FF)];
        }

        /*
         * Detect digits to be processed.
         */
        boolean processDigit1 = processDigit(count1, size, low);
        boolean processDigit2 = processDigit(count2, size, low);
        boolean processDigit3 = processDigit(count3, size, low);
        boolean processDigit4 = processDigit(count4, size, low);
        boolean processDigit5 = processDigit(count5, size, low);
        boolean processDigit6 = processDigit(count6, size, low);

        /*
         * Process the 1-st digit.
         */
        if (processDigit1) {
            for (int i = high; i > low; ) {
                b[--count1[(int) (dtl(a[--i]) & 0x3FF)] - offset] = a[i];
            }
        }

        /*
         * Process the 2-nd digit.
         */
        if (processDigit2) {
            if (processDigit1) {
                for (int i = last; i > start; ) {
                    a[--count2[(int) ((dtl(b[--i]) >>> 10) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count2[(int) ((dtl(a[--i]) >>> 10) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 3-rd digit.
         */
        if (processDigit3) {
            if (processDigit1 ^ processDigit2) {
                for (int i = last; i > start; ) {
                    a[--count3[(int) ((dtl(b[--i]) >>> 21) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count3[(int) ((dtl(a[--i]) >>> 21) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 4-th digit.
         */
        if (processDigit4) {
            if (processDigit1 ^ processDigit2 ^ processDigit3) {
                for (int i = last; i > start; ) {
                    a[--count4[(int) ((dtl(b[--i]) >>> 32) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count4[(int) ((dtl(a[--i]) >>> 32) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 5-th digit.
         */
        if (processDigit5) {
            if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4) {
                for (int i = last; i > start; ) {
                    a[--count5[(int) ((dtl(b[--i]) >>> 43) & 0x7FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count5[(int) ((dtl(a[--i]) >>> 43) & 0x7FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Process the 6-th digit.
         */
        if (processDigit6) {
            if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4 ^ processDigit5) {
                for (int i = last; i > start; ) {
                    a[--count6[(int) ((dtl(b[--i]) >>> 54) & 0x3FF)]] = b[i];
                }
            } else {
                for (int i = high; i > low; ) {
                    b[--count6[(int) ((dtl(a[--i]) >>> 54) & 0x3FF)] - offset] = a[i];
                }
            }
        }

        /*
         * Copy the buffer to original array, if we process ood number of digits.
         */
        if (processDigit1 ^ processDigit2 ^ processDigit3 ^ processDigit4 ^ processDigit5 ^ processDigit6) {
            System.arraycopy(b, low - offset, a, low, size);
        }
        return true;
    }

    /**
     * Returns masked bits that represent the double value.
     *
     * @param d the given value
     * @return masked bits
     */
    private static long dtl(double d) {
        long x = Double.doubleToRawLongBits(d);
        return x ^ ((x >> 63) | 0x8000000000000000L);
    }

    /**
     * Sorts the specified range of the array using heap sort.
     *
     * @param a the array to be sorted
     * @param low the index of the first element, inclusive, to be sorted
     * @param high the index of the last element, exclusive, to be sorted
     */
    static void heapSort(double[] a, int low, int high) {
        for (int k = (low + high) >>> 1; k > low; ) {
            pushDown(a, --k, a[k], low, high);
        }
        while (--high > low) {
            double max = a[low];
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
    private static void pushDown(double[] a, int p, double value, int low, int high) {
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

// #[class]

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

            while ((parallelism >>= 1) > 0 && (size >>= 1) > 0) {
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
                } else if (a instanceof long[]) {
                    sort((Sorter<long[]>) this, (long[]) a, depth, low, low + size);
                } else if (a instanceof float[]) {
                    sort((Sorter<float[]>) this, (float[]) a, depth, low, low + size);
                } else if (a instanceof double[]) {
                    sort((Sorter<double[]>) this, (double[]) a, depth, low, low + size);
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
        private final T dst, src;
        private final int k, lo1, hi1, lo2, hi2;

        private Merger(CountedCompleter<?> parent, T dst, int k,
                T src, int lo1, int hi1, int lo2, int hi2) {
            super(parent);
            this.dst = dst;
            this.k = k;
            this.src = src;
            this.lo1 = lo1;
            this.hi1 = hi1;
            this.lo2 = lo2;
            this.hi2 = hi2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void compute() {
            if (dst instanceof int[]) {
                mergeParts((Merger<int[]>) this, (int[]) dst, k, (int[]) src, lo1, hi1, lo2, hi2);
            } else if (dst instanceof long[]) {
                mergeParts((Merger<long[]>) this, (long[]) dst, k, (long[]) src, lo1, hi1, lo2, hi2);
            } else if (dst instanceof float[]) {
                mergeParts((Merger<float[]>) this, (float[]) dst, k, (float[]) src, lo1, hi1, lo2, hi2);
            } else if (dst instanceof double[]) {
                mergeParts((Merger<double[]>) this, (double[]) dst, k, (double[]) src, lo1, hi1, lo2, hi2);
            } else {
                throw new IllegalArgumentException("Unknown array: " + dst.getClass().getName());
            }
            propagateCompletion();
        }

        private void fork(int k, int lo1, int hi1, int lo2, int hi2) {
            addToPendingCount(1);
            new Merger<>(this, dst, k, src, lo1, hi1, lo2, hi2).fork();
        }
    }

    /**
     * Tries to allocate additional buffer.
     *
     * @param <T> type of // todo
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
