public enum IntSorter {

    DPQ { public void sort(int[] a) {
        DualPivotQuicksort.sort(a, 0, 0, a.length);
    }},

    ARRAYS { public void sort(int[] a) {
        Arrays.sort(a, 0, a.length);
    }};

    public abstract void sort(int[] a);
}
