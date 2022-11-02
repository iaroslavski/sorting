import java.util.Random;

public enum ParamIntArrayBuilder {

    STAGGER_ { int element(int i, int m, int n) { return (i * m + i) % n;                             }},
    SAWTOOTH { int element(int i, int m, int n) { return i % m;                                       }},
    RANDOM__ { int element(int i, int m, int n) { return random.nextInt(m);                           }},
    PLATEAU_ { int element(int i, int m, int n) { return Math.min(i, m);                              }},
    SHUFFLE_ { int element(int i, int m, int n) { return random.nextInt(m) > 0 ? (j += 2) : (k += 2); }};

    abstract int element(int i, int m, int n);

    public void build(int[] r, int m) {
        random = new Random(0x777);
        j = 0; k = 1;

        for (int i = 0; i < r.length; ++i) {
            r[i] = element(i, m, r.length);
        }
    }

    private static int j, k;
    private static Random random;
}
