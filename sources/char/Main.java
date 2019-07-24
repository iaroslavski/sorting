import java.util.concurrent.ForkJoinPool;
import java.util.Locale;
import java.util.Random;

public final class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("us", "US"));

        System.out.println("# Data type = char, Sorting type = " + (parallelism == 0 ? "sequential" : "parallel " + (parallelism + 1)) + ", Length = " + MAX_N);
        System.out.println("test_name;jdk_time;dpqs_time;gain\n");

        warm = true;
        doSort();
        warm = false;

        totalTime1 = 0.0;
        totalTime2 = 0.0;

        doSort();

        printRow("TOTAL", totalTime1, totalTime2);
    }

    private static void doSort() {
        random();     doIteration("random");

        equal();      doIteration("equal");
        ascending();  doIteration("ascending");
        descending(); doIteration("descending");

        period(4);    doIteration("period[4]");
        period(5);    doIteration("period[5]");
        period(6);    doIteration("period[6]");

        stagger(15);  doIteration("stagger[15]");
        stagger(24);  doIteration("stagger[24]");
        stagger(33);  doIteration("stagger[33]");
        
        shuffle(6);   doIteration("shuffle[6]");
        shuffle(7);   doIteration("shuffle[7]");
        shuffle(8);   doIteration("shuffle[8]");
    }                                                                

    private static void doIteration(String name) {
        time1 = -1.0;
        time2 = -1.0;

        doSort("jdk", new Sortable() { public void sortArray(char[] a) { DualPivotQuicksort_jdk.sort(a, parallelism, LEFT, HIGH ); }});
        doSort("__ ", new Sortable() { public void sortArray(char[] a) { DualPivotQuicksort    .sort(a,              LEFT, HIGH ); }});

        printRow(name, time1, time2);
    }

    private static void doSort(String name, Sortable sortable) {
        long startTime;
        long endTime;
        long minTime = Long.MAX_VALUE;
        long allTime = 0;

        int count = warm ? (MAX_N < 1500 ? 104000 : WARM_COUNT) : COUNT;
        
        for (int i = 0; i < count; ++i) {
            for (int k = 0; k < MAX_N; ++k) {
                test[k] = golden[k];
            }
            startTime = System.nanoTime();
            sortable.sortArray(test);
            endTime = System.nanoTime();

            long time = endTime - startTime;
            minTime = Math.min(minTime, time);
            allTime += time;
        }
        if (CHECK && !warm) check(name, test, golden, LEFT, RIGHT);

        long div = 1L;

        if (MAX_N > 500000) {
            div = 1_000_000L;
        } else if (MAX_N > 10000) {
            div = 1_000L;
        }

/*
        if (time1 < 0.0) {
            time1 = ((double) allTime) / count / div;
            totalTime1 += time1;
        } else if (time2 < 0.0) {
            time2 = ((double) allTime) / count / div;
            totalTime2 += time2;
        }
*/
        if (time1 < 0.0) {
            time1 = ((double) minTime) / div;
            totalTime1 += time1;
        } else if (time2 < 0.0) {
            time2 = ((double) minTime) / div;
            totalTime2 += time2;
        }
    }

    private static void printRow(String name, double time1, double time2) {
        if (!warm) System.out.printf("%s;%.2f;%.2f;%.2f\n", name, time1, time2, (time1 - time2) / time1);
    }

    private static void random() {
        for (int i = 0; i < MAX_N; ++i) {
            golden[i] = (char) random.nextInt();
        }
    }

    private static void equal() {
        for (int i = 0; i < MAX_N; ++i) {
            golden[i] = 0;
        }
    }

    private static void ascending() {
        int k = 0;

        for ( ; k < MAX_N; ++k) {
            golden[k] = (char) k;

            if (k == MAX_VALUE) {
                break;
            }
        }
        for ( ; k < MAX_N; ++k) {
            golden[k] = MAX_VALUE;
        }
    }

    private static void reverse() {
        reverse(golden, 0, golden.length - 1);
    }
    
    private static void reverse(char[] a, int start, int end) {
        while (start < end) {
            char t = a[start];
            a[start++] = a[end];
            a[end--] = t;
        }
    }

    private static void descending() {
        ascending();
        reverse();
    }

    private static void period(int m) {
        for (int i = 0; i < MAX_N; ++i) {
            golden[i] = (char) (i % m);
        }
    }

    private static void stagger(int m) {
        for (int i = 0; i < MAX_N; ++i) {
            golden[i] = (char) ((i * m + i) % MAX_N);
        }
    }

    private static void shuffle(int m) {
        for (int i = 0, j = 0, k = 1; i < MAX_N; ++i) {
            golden[i] = (char) (random.nextInt(m) > 0 ? (j += 2) : (k += 2));
        }
    }

    public static void check(String name, char[] a1, char[] a2, int left, int right) {
        String message;

        for (int i = left; i < right; ++i) {
            if (a1[i] > a1[i + 1]) {
                message = "\n\n" + name + ": Array is not sorted at index " + i + ": " + a1[i] + " " + a1[i+1];
                System.out.println(message);
                System.err.println(message);
                throw new RuntimeException(message);
            }
        }
        int plusCheckSum1 = 0;
        int plusCheckSum2 = 0;
        int xorCheckSum1 = 0;
        int xorCheckSum2 = 0;

        for (int i = left; i <= right; ++i) {
            plusCheckSum1 += (int) a1[i];
            plusCheckSum2 += (int) a2[i];
            xorCheckSum1 ^= (int) a1[i];
            xorCheckSum2 ^= (int) a2[i];
        }
        if (plusCheckSum1 != plusCheckSum2) {
            message = "\n\n" + name + ": Array is not sorted correctly [+].";
            System.out.println(message);
            System.err.println(message);
            throw new RuntimeException(message);
        }
        if (xorCheckSum1 != xorCheckSum2) {
            message = "\n\n" + name + ": Array is not sorted correctly [^].";
            System.out.println(message);
            System.err.println(message);
            throw new RuntimeException(message);
        }
    }

    public static void outArr(char[] a) {
        outArr(a, 0, a.length - 1);
    }

    public static void outArr(char[] a, int left, int right) {
        for (int i = left; i <= right; ++i) {
            System.out.print(a[i] + (i == right ? "" : ", "));
        }
        System.out.println();
    }

    private interface Sortable {
        void sortArray(char[] a);
    }



    private static final int MAX_N = 150;
    private static final int COUNT = 2000000;

//  private static final int MAX_N = 30000;   // 30K
//  private static final int COUNT = 10000;

//  private static final int MAX_N = 1000000; // 1M
//  private static final int COUNT = 150;

    
    
    
//  private static final int parallelism = 0;
    private static final int parallelism = ForkJoinPool.getCommonPoolParallelism();

    private static double time1;
    private static double time2;
    private static double totalTime1;
    private static double totalTime2;
    private static boolean warm;

    private static final int LEFT = 0;
    private static final int HIGH = MAX_N - LEFT;
    private static final int RIGHT = HIGH - 1;
    private static final Random random = new Random(777);

    private static final char[] test = new char[MAX_N];
    private static final char[] golden = new char[MAX_N];

    private static final int WARM_COUNT = 30;
    private static final char MAX_VALUE = Character.MAX_VALUE;
    private static final boolean CHECK = true;
}
