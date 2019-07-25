final class DualPivotQuicksort_jdk {

    public static void sort(int[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(long[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(byte[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(char[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(short[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(float[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }

    public static void sort(double[] a, int parallelism, int low, int high) {
        if (parallelism > 0) {
            java.util.Arrays.parallelSort(a, low, high);
        } else {
            java.util.Arrays.sort(a, low, high);
        }
    }
}
