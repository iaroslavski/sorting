import java.util.Random;

public enum ParamIntArrayBuilder {

    STAGGER_ { int element(int i, int m, int n) { return (i * m + i) % n;                          }},
    SAWTOOTH { int element(int i, int m, int n) { return i % m;                                    }},
    RANDOM__ { int element(int i, int m, int n) { return rnd.nextInt(m);                           }},
    PLATEAU_ { int element(int i, int m, int n) { return Math.min(i, m);                           }},
    SHUFFLE_ { int element(int i, int m, int n) { return rnd.nextInt(m) > 0 ? (j += 2) : (k += 2); }};

    abstract int element(int i, int m, int n);

    public void build(int[] r, int m) {
        int length = r.length;
        j = 0; k = 1;

        for (int i = 0; i < length; ++i) {
            r[i] = element(i, m, length);
        }
    }

    private static int j, k;
    private static Random rnd = new Random(0x777);
}
