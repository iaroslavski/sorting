/*
 * @(#)Arrays.java  1.71 06/04/21
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.*;

import java.lang.reflect.*;

/**
 * This class contains various methods for manipulating arrays (such as
 * sorting and searching).  This class also contains a static factory
 * that allows arrays to be viewed as lists.
 *
 * <p>The methods in this class all throw a <tt>NullPointerException</tt> if
 * the specified array reference is null, except where noted.
 *
 * <p>The documentation for the methods contained in this class includes
 * briefs description of the <i>implementations</i>.  Such descriptions should
 * be regarded as <i>implementation notes</i>, rather than parts of the
 * <i>specification</i>.  Implementors should feel free to substitute other
 * algorithms, so long as the specification itself is adhered to.  (For
 * example, the algorithm used by <tt>sort(Object[])</tt> does not have to be
 * a mergesort, but it does have to be <i>stable</i>.)
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @author  John Rose
 * @version 1.71, 04/21/06
 * @since   1.2
 */

public class Arrays {
    // Suppresses default constructor, ensuring non-instantiability.
    private Arrays() {
    }

    // Sorting

    /**
     * Sorts the specified array of longs into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(long[] a) {
    sort1(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of longs into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
     *
     * <p>The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     * <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(long[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort1(a, fromIndex, toIndex-fromIndex);
    }

    /**
     * Sorts the specified array of ints into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(int[] a) {
    sort1(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of ints into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
     *
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(int[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort1(a, fromIndex, toIndex-fromIndex);
    }

    /**
     * Sorts the specified array of shorts into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(short[] a) {
    sort1(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of shorts into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
     *
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(short[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort1(a, fromIndex, toIndex-fromIndex);
    }

    /**
     * Sorts the specified array of chars into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(char[] a) {
    sort1(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of chars into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
     *
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(char[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort1(a, fromIndex, toIndex-fromIndex);
    }

    /**
     * Sorts the specified array of bytes into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(byte[] a) {
    sort1(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of bytes into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
     *
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort1(a, fromIndex, toIndex-fromIndex);
    }

    /**
     * Sorts the specified array of doubles into ascending numerical order.
     * <p>
     * The <code>&lt;</code> relation does not provide a total order on
     * all floating-point values; although they are distinct numbers
     * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
     * compares neither less than, greater than, nor equal to any
     * floating-point value, even itself.  To allow the sort to
     * proceed, instead of using the <code>&lt;</code> relation to
     * determine ascending numerical order, this method uses the total
     * order imposed by {@link Double#compareTo}.  This ordering
     * differs from the <code>&lt;</code> relation in that
     * <code>-0.0</code> is treated as less than <code>0.0</code> and
     * NaN is considered greater than any other floating-point value.
     * For the purposes of sorting, all NaN values are considered
     * equivalent and equal.
     * <p>
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(double[] a) {
    sort2(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of doubles into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
     * <p>
     * The <code>&lt;</code> relation does not provide a total order on
     * all floating-point values; although they are distinct numbers
     * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
     * compares neither less than, greater than, nor equal to any
     * floating-point value, even itself.  To allow the sort to
     * proceed, instead of using the <code>&lt;</code> relation to
     * determine ascending numerical order, this method uses the total
     * order imposed by {@link Double#compareTo}.  This ordering
     * differs from the <code>&lt;</code> relation in that
     * <code>-0.0</code> is treated as less than <code>0.0</code> and
     * NaN is considered greater than any other floating-point value.
     * For the purposes of sorting, all NaN values are considered
     * equivalent and equal.
     * <p>
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(double[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort2(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array of floats into ascending numerical order.
     * <p>
     * The <code>&lt;</code> relation does not provide a total order on
     * all floating-point values; although they are distinct numbers
     * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
     * compares neither less than, greater than, nor equal to any
     * floating-point value, even itself.  To allow the sort to
     * proceed, instead of using the <code>&lt;</code> relation to
     * determine ascending numerical order, this method uses the total
     * order imposed by {@link Float#compareTo}.  This ordering
     * differs from the <code>&lt;</code> relation in that
     * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
     * NaN is considered greater than any other floating-point value.
     * For the purposes of sorting, all NaN values are considered
     * equivalent and equal.
     * <p>
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(float[] a) {
    sort2(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of floats into
     * ascending numerical order.  The range to be sorted extends from index
     * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
     * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
     * <p>
     * The <code>&lt;</code> relation does not provide a total order on
     * all floating-point values; although they are distinct numbers
     * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
     * compares neither less than, greater than, nor equal to any
     * floating-point value, even itself.  To allow the sort to
     * proceed, instead of using the <code>&lt;</code> relation to
     * determine ascending numerical order, this method uses the total
     * order imposed by {@link Float#compareTo}.  This ordering
     * differs from the <code>&lt;</code> relation in that
     * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
     * NaN is considered greater than any other floating-point value.
     * For the purposes of sorting, all NaN values are considered
     * equivalent and equal.
     * <p>
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *         <tt>toIndex &gt; a.length</tt>
     */
    public static void sort(float[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
    sort2(a, fromIndex, toIndex);
    }

    private static void sort2(double a[], int fromIndex, int toIndex) {
        final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
        /*
         * The sort is done in three phases to avoid the expense of using
         * NaN and -0.0 aware comparisons during the main sort.
         */

        /*
         * Preprocessing phase:  Move any NaN's to end of array, count the
         * number of -0.0's, and turn them into 0.0's.
         */
        int numNegZeros = 0;
        int i = fromIndex, n = toIndex;
        while(i < n) {
            if (a[i] != a[i]) {
        double swap = a[i];
                a[i] = a[--n];
                a[n] = swap;
            } else {
                if (a[i]==0 && Double.doubleToLongBits(a[i])==NEG_ZERO_BITS) {
                    a[i] = 0.0d;
                    numNegZeros++;
                }
                i++;
            }
        }

        // Main sort phase: quicksort everything but the NaN's
    sort1(a, fromIndex, n-fromIndex);

        // Postprocessing phase: change 0.0's to -0.0's as required
        if (numNegZeros != 0) {
            int j = binarySearch0(a, fromIndex, n, 0.0d); // posn of ANY zero
            do {
                j--;
            } while (j>=0 && a[j]==0.0d);

            // j is now one less than the index of the FIRST zero
            for (int k=0; k<numNegZeros; k++)
                a[++j] = -0.0d;
        }
    }


    private static void sort2(float a[], int fromIndex, int toIndex) {
        final int NEG_ZERO_BITS = Float.floatToIntBits(-0.0f);
        /*
         * The sort is done in three phases to avoid the expense of using
         * NaN and -0.0 aware comparisons during the main sort.
         */

        /*
         * Preprocessing phase:  Move any NaN's to end of array, count the
         * number of -0.0's, and turn them into 0.0's.
         */
        int numNegZeros = 0;
        int i = fromIndex, n = toIndex;
        while(i < n) {
            if (a[i] != a[i]) {
        float swap = a[i];
                a[i] = a[--n];
                a[n] = swap;
            } else {
                if (a[i]==0 && Float.floatToIntBits(a[i])==NEG_ZERO_BITS) {
                    a[i] = 0.0f;
                    numNegZeros++;
                }
                i++;
            }
        }

        // Main sort phase: quicksort everything but the NaN's
    sort1(a, fromIndex, n-fromIndex);

        // Postprocessing phase: change 0.0's to -0.0's as required
        if (numNegZeros != 0) {
            int j = binarySearch0(a, fromIndex, n, 0.0f); // posn of ANY zero
            do {
                j--;
            } while (j>=0 && a[j]==0.0f);

            // j is now one less than the index of the FIRST zero
            for (int k=0; k<numNegZeros; k++)
                a[++j] = -0.0f;
        }
    }


    /*
     * The code for each of the seven primitive types is largely identical.
     * C'est la vie.
     */

    /**
     * Sorts the specified sub-array of longs into ascending order.
     */
    private static void sort1(long x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    long v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(long x[], int a, int b) {
    long t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(long x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed longs.
     */
    private static int med3(long x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Sorts the specified sub-array of integers into ascending order.
     */
    private static void sort1(int x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    int v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(int x[], int a, int b) {
    int t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(int x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(int x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Sorts the specified sub-array of shorts into ascending order.
     */
    private static void sort1(short x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    short v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(short x[], int a, int b) {
    short t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(short x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed shorts.
     */
    private static int med3(short x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }


    /**
     * Sorts the specified sub-array of chars into ascending order.
     */
    private static void sort1(char x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    char v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(char x[], int a, int b) {
    char t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(char x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed chars.
     */
    private static int med3(char x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }


    /**
     * Sorts the specified sub-array of bytes into ascending order.
     */
    private static void sort1(byte x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    byte v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(byte x[], int a, int b) {
    byte t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(byte x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed bytes.
     */
    private static int med3(byte x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }


    /**
     * Sorts the specified sub-array of doubles into ascending order.
     */
    private static void sort1(double x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    double v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(double x[], int a, int b) {
    double t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(double x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed doubles.
     */
    private static int med3(double x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }


    /**
     * Sorts the specified sub-array of floats into ascending order.
     */
    private static void sort1(float x[], int off, int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
        for (int i=off; i<len+off; i++)
        for (int j=i; j>off && x[j-1]>x[j]; j--)
            swap(x, j, j-1);
        return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
        int l = off;
        int n = off + len - 1;
        if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len/8;
        l = med3(x, l,     l+s, l+2*s);
        m = med3(x, m-s,   m,   m+s);
        n = med3(x, n-2*s, n-s, n);
        }
        m = med3(x, l, m, n); // Mid-size, med of 3
    }
    float v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while(true) {
        while (b <= c && x[b] <= v) {
        if (x[b] == v)
            swap(x, a++, b);
        b++;
        }
        while (c >= b && x[c] >= v) {
        if (x[c] == v)
            swap(x, c, d--);
        c--;
        }
        if (b > c)
        break;
        swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    // Recursively sort non-partition-elements
    if ((s = b-a) > 1)
        sort1(x, off, s);
    if ((s = d-c) > 1)
        sort1(x, n-s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(float x[], int a, int b) {
    float t = x[a];
    x[a] = x[b];
    x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(float x[], int a, int b, int n) {
    for (int i=0; i<n; i++, a++, b++)
        swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed floats.
     */
    private static int med3(float x[], int a, int b, int c) {
    return (x[a] < x[b] ?
        (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
        (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Check that fromIndex and toIndex are in range, and throw an
     * appropriate exception if they aren't.
     */
    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                       ") > toIndex(" + toIndex+")");
        if (fromIndex < 0)
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        if (toIndex > arrayLen)
            throw new ArrayIndexOutOfBoundsException(toIndex);
    }
    private static int binarySearch0(float[] a, int fromIndex, int toIndex,
                     float key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        float midVal = a[mid];

            int cmp;
            if (midVal < key) {
                cmp = -1;   // Neither val is NaN, thisVal is smaller
            } else if (midVal > key) {
                cmp = 1;    // Neither val is NaN, thisVal is larger
            } else {
                int midBits = Float.floatToIntBits(midVal);
                int keyBits = Float.floatToIntBits(key);
                cmp = (midBits == keyBits ?  0 : // Values are equal
                       (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1));                     // (0.0, -0.0) or (NaN, !NaN)
            }

        if (cmp < 0)
        low = mid + 1;
        else if (cmp > 0)
        high = mid - 1;
        else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
    }

    private static int binarySearch0(double[] a, int fromIndex, int toIndex,
                     double key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        double midVal = a[mid];

            int cmp;
            if (midVal < key) {
                cmp = -1;   // Neither val is NaN, thisVal is smaller
            } else if (midVal > key) {
                cmp = 1;    // Neither val is NaN, thisVal is larger
            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                cmp = (midBits == keyBits ?  0 : // Values are equal
                       (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1));                     // (0.0, -0.0) or (NaN, !NaN)
            }

        if (cmp < 0)
        low = mid + 1;
        else if (cmp > 0)
        high = mid - 1;
        else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
    }
}
