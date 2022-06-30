import java.util.Arrays;

public enum IntArrayTweaker {

    IDENT_____ { public void tweak(int[] a, int[] r) { copy(r, a);                                                     }},
    REVERSE___ { public void tweak(int[] a, int[] r) { copy(r, a); reverse(r, 0, r.length);                            }},
    REVERSE_FR { public void tweak(int[] a, int[] r) { copy(r, a); reverse(r, 0, r.length / 2);                        }},
    REVERSE_BA { public void tweak(int[] a, int[] r) { copy(r, a); reverse(r, r.length / 2, r.length);                 }},
    SORT______ { public void tweak(int[] a, int[] r) { copy(r, a); Arrays.sort(r);                                     }},
    DITHER____ { public void tweak(int[] a, int[] r) { copy(r, a); for (int i = 0; i < r.length; ++i) r[i] += (i % 5); }};

    public abstract void tweak(int[] a, int[] r);

    private static void reverse(int[] r, int start, int end) {
        for (--end; start < end; ) {
            int t = r[start]; r[start++] = r[end]; r[end--] = t;
        }
    }

    static void copy(int[] r, int[] a) {
        for (int i = 0; i < a.length; ++i) {
            r[i] = a[i];
        }
    }
}
