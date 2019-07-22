import java.util.concurrent.ForkJoinPool;
import java.util.Locale;
import java.util.Random;

public final class Main {

    public static void main(String[] args) {
        out("# Data type = char, Sorting type = " + (parallelism == 0 ? "sequential" : "parallel " + (parallelism + 1)) + ", Length = " + MAX_N);
        out("test_name;jdk_time;dpqs_time;gain\n");
        random = new Random();
        Locale.setDefault(new Locale("us", "US"));

        if (COUNT > 1) {
            warm = true;
            doSort();
            warm = false;
        }
        totalTime1 = 0.0;
        totalTime2 = 0.0;

        doSort();

        if (!warm) System.out.printf("%s;%.2f;%.2f;%.2f\n", "TOTAL", totalTime1, totalTime2, (totalTime1 - totalTime2) / totalTime1);
    }

    private static void doSort() {
        common();
    }                                                                

    private static void doIteration(String name) {
        time1 = -1.0;
        time2 = -1.0;

        doSort("jdk", new Sortable() { public void sortArray(char[] a) { DualPivotQuicksort_jdk   .sort(a, parallelism, LEFT, HIGH ); }});
        doSort("__ ", new Sortable() { public void sortArray(char[] a) { DualPivotQuicksort       .sort(a,              LEFT, HIGH ); }});

        if (!warm) System.out.printf("%s;%.2f;%.2f;%.2f\n", name, time1, time2, (time1 - time2) / time1);
    }

    private static void doSort(String name, Sortable sortable) {
//outArr("STARTED:", golden);
        long startTime;
        long endTime;
        long minTime = Long.MAX_VALUE;
        long allTime = 0;

        prepareGolden();

        int count = warm ? (MAX_N < 1500 ? 104000 : WARM_COUNT) : COUNT;
        
        for (int i = 0; i < count; i++) {
            for (int k = 0; k < MAX_N; k++) {
                test[k] = (char) golden[k];
                save[k] = test[k];
            }
            startTime = System.nanoTime();
            sortable.sortArray(test);
            endTime = System.nanoTime();

            long time = endTime - startTime;
            minTime = Math.min(minTime, time);
            allTime += time;
        }
//outArr("END:", test);

        if (CHECK && !warm) check(name, test, save, LEFT, RIGHT);

        long div = 1L;

        if (MAX_N > 500000) {
            div = 1_000_000L;
        } else if (MAX_N > 10000) {
            div = 1_000L;
        }
        if (time1 < 0.0) {
            time1 = ((double) allTime) / count / div;
            totalTime1 += time1;
        } else if (time2 < 0.0) {
            time2 = ((double) allTime) / count / div;
            totalTime2 += time2;
        }
    }

    private static void staggerRandom() {
        Random random = new Random(randomInit);
        final int MAX_RUN_COUNT = 66;
        int count = MAX_RUN_COUNT - random.nextInt(10);
        int step = MAX_N / count;
//out();
//out();
//out("count: " + count);
//out("step: " + step);

//          int len = random.nextInt(step)  + 1;
//          int len = random.nextInt(MAX_N) + 1;

        for (int i = 0, k = 0; i < count && k < MAX_N; i++, k++) {
//          boolean asc = random.nextBoolean();
            int init = random.nextInt(MAX_RUN_COUNT);
//out("init: " + init + " " + asc);
//out("len: " + len);
            golden[k] = init;

            int delta = random.nextInt(5) + 1;

            for (int s = 0; ++k < MAX_N && s < step; s++) {
                golden[k] = golden[k - 1] + delta;
            }
        }
    }

    private static void common() {
        random();      doIteration("random");

        equal();       doIteration("equal");
        ascending();   doIteration("ascending");
        descending();  doIteration("descending");
        // organPipes();  doIteration("organPipes");

        period(4);     doIteration("period[4]");
        period(5);     doIteration("period[5]");
        period(6);     doIteration("period[6]");

        // stagger( 2);   doIteration("stagger: 2");
        // stagger( 4);   doIteration("stagger: 4");
        stagger(15);   doIteration("stagger[15]");
        stagger(24);   doIteration("stagger[24]");
        stagger(33);   doIteration("stagger[33]");
        // stagger(64);   doIteration("stagger: 64");
        
        // latch(4);      doIteration("latch: 4");
        // latch(8);      doIteration("latch: 8");
        // latch(9);      doIteration("latch: 9");
        
        shuffle(6);    doIteration("shuffle[6]");
        shuffle(7);    doIteration("shuffle[7]");
        shuffle(8);    doIteration("shuffle[8]");
    }

    private static void bugTest_() {
        int length = golden.length;

        for (int i = 0; i < length; i++) {
            golden[i] = (100 - i); //%2 == 0 ? 11 : 12;
        }
        int left = 0;
        int right = length - 1;

        int seventh = (length >> 4) + (length >> 7) + 1;

        int e3 = (left + right) >>> 1; // The midpoint
        int e2 = e3 - seventh;
        int e1 = e2 - seventh;
        int e4 = e3 + seventh;
        int e5 = e4 + seventh;

        golden[e1] = 1;
        golden[e2] = 2;
        golden[e3] = 3;
        golden[e4] = 4;
        golden[e5] = 5;

//outArr(golden);
    }
/*
    private static void palki1() { // is it desc ???
        ascending();                    //   ---- 1
        reverse(golden, 0, golden.length - 1);
    }

    private static void palki2() {
        ascending();
        reverse(golden, 0, 2);
    }
*/
    private static void random() {
        Random random = new Random(randomInit);
        
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = random.nextInt();
        }
    }
    
    private static void ascending() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = i;
        }
    }

    private static void descending() {                                                       
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = (MAX_N - i);
        }
    }

    private static void equal() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 0;
        }
    }

    private static void equalM1() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = -1;
        }
    }

    private static void ascMergeDesc() {
        ascending();

        for (int i = MAX_N / 2; i < MAX_N; i++) {
            golden[i] = (MAX_N * 2 - i);
        }
    }

    private static void ascMergeDesc10() {
        ascending();

        for (int i = MAX_N / 2; i < MAX_N; i++) {
            golden[i] = (MAX_N + 10 - i);
        }
    }

    // Ring
    private static void ring_A_EB_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = 0;
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_A_EB_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = 0;
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }

    private static void ring_A_EC_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = (level / 2);
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_A_EC_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = (level / 2);
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }

    private static void ring_A_ET_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = level;
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_A_ET_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = 0; i < k1; i++) {
            golden[i] = k++;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = level;
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }

    private static void ring_D_EB_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = 0;
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_D_EB_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = 0;
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }

    private static void ring_D_EC_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = (level / 2);
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_D_EC_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = (level / 2);
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }

    private static void ring_D_ET_A() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = level;
        }
        for (int i = k2, k = 0; i < MAX_N; i++) {
            golden[i] = k++;
        }
    }

    private static void ring_D_ET_D() {
        int k1 = MAX_N / 3;
        int k2 = MAX_N / 3 * 2;
        int level = MAX_N / 3;

        for (int i = 0, k = level; i < k1; i++) {
            golden[i] = k--;
        }
        for (int i = k1; i < k2; i++) {
            golden[i] = level;
        }
        for (int i = k2, k = level; i < MAX_N; i++) {
            golden[i] = k--;
        }
    }
    
    // Pearl.equal
    private static void pearl_E_1_LT() {
        equal();
        golden[LEFT] = 10;
    }

    private static void pearl_E_2_LT() {
        equal();
        golden[LEFT] = 10;
        golden[MAX_N / 2] = 10;
    }

    private static void pearl_E_2_LC() {
        equal();
        golden[LEFT] = 10;
        golden[MAX_N / 2] = -10;
    }

    private static void pearl_E_2_LD() {
        equal();
        golden[LEFT] = -10;
        golden[MAX_N / 2] = 10;
    }

    private static void pearl_E_1_LB() {
        equal();
        golden[LEFT] = -10;
    }

    private static void pearl_E_2_LB() {
        equal();
        golden[LEFT] = -10;
        golden[MAX_N / 2] = -10;
    }

    private static void pearl_E_2_LZ() {
        equal();
        golden[LEFT] = LEFT - 10;
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    private static void pearl_E_1_CT() {
        equal();
        golden[MAX_N / 2] = 10;
    }

    private static void pearl_E_2_CT() {
        equal();
        golden[MAX_N / 3] = 10;
        golden[MAX_N / 3 * 2] = 10;
    }

    private static void pearl_E_2_CC() {
        equal();
        golden[MAX_N / 3] = 10;
        golden[MAX_N / 3 * 2] = -10;
    }

    private static void pearl_E_2_CD() {
        equal();
        golden[MAX_N / 3] = -10;
        golden[MAX_N / 3 * 2] = 10;
    }

    private static void pearl_E_1_CB() {
        equal();
        golden[MAX_N / 2] = -10;
    }

    private static void pearl_E_2_CB() {
        equal();
        golden[MAX_N / 3] = -10;
        golden[MAX_N / 3 * 2] = -10;
    }

    private static void pearl_E_1_RT() {
        equal();
        golden[RIGHT] = 10;
    }

    private static void pearl_E_2_RT() {
        equal();
        golden[RIGHT] = 10;
        golden[MAX_N / 2] = 10;
    }

    private static void pearl_E_2_RC() {
        equal();
        golden[RIGHT] = -10;
        golden[MAX_N / 2] = 10;
    }

    private static void pearl_E_2_RD() {
        equal();
        golden[RIGHT] = 10;
        golden[MAX_N / 2] = -10;
    }

    private static void pearl_E_1_RB() {
        equal();
        golden[RIGHT] = -10;
    }

    private static void pearl_E_2_RB() {
        equal();
        golden[RIGHT] = -10;
        golden[MAX_N / 2] = -10;
    }

    // Pearl.ascending
    private static void pearl_A_1_LT() {
        ascending();
        golden[LEFT] = LEFT + 10;
    }

    private static void pearl_A_2_LT() {
        ascending();
        golden[LEFT] = LEFT + 10;
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_A_1_LB() {
        ascending();
        golden[LEFT] = LEFT - 10;
    }

    private static void pearl_A_2_LB() {
        ascending();
        golden[LEFT] = LEFT - 10;
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    private static void pearl_A_1_CT() {
        ascending();
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_A_2_CT() {
        ascending();
        golden[MAX_N / 3] = MAX_N / 3 + 10;
        golden[MAX_N / 3 * 2] = MAX_N / 3 * 2 + 10;
    }

    private static void pearl_A_1_CB() {
        ascending();
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    private static void pearl_A_2_CB() {
        ascending();
        golden[MAX_N / 3] = MAX_N / 3 - 10;
        golden[MAX_N / 3 * 2] = MAX_N / 3 * 2 - 10;
    }

    private static void pearl_A_1_RT() {
        ascending();
        golden[RIGHT] = RIGHT + 10;
    }

    private static void pearl_A_2_RT() {
        ascending();
        golden[RIGHT] = RIGHT + 10;
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_A_1_RB() {
        ascending();
        golden[RIGHT] = RIGHT - 10;
    }

    private static void pearl_A_2_RB() {
        ascending();
        golden[RIGHT] = RIGHT - 10;
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    // Pearl.descending
    private static void pearl_D_1_LT() {
        descending();
        golden[LEFT] = LEFT + 10;
    }

    private static void pearl_D_2_LT() {
        descending();
        golden[LEFT] = LEFT + 10;
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_D_1_LB() {
        descending();
        golden[LEFT] = LEFT - 10;
    }

    private static void pearl_D_2_LB() {
        descending();
        golden[LEFT] = LEFT - 10;
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    private static void pearl_D_1_CT() {
        descending();
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_D_2_CT() {
        descending();
        golden[MAX_N / 3] = MAX_N / 3 + 10;
        golden[MAX_N / 3 * 2] = MAX_N / 3 * 2 + 10;
    }

    private static void pearl_D_1_CB() {
        descending();
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }

    private static void pearl_D_2_CB() {
        descending();
        golden[MAX_N / 3] = MAX_N / 3 - 10;
        golden[MAX_N / 3 * 2] = MAX_N / 3 * 2 - 10;
    }

    private static void pearl_D_1_RT() {
        descending();
        golden[RIGHT] = RIGHT + 10;
    }

    private static void pearl_D_2_RT() {
        descending();
        golden[RIGHT] = RIGHT + 10;
        golden[MAX_N / 2] = MAX_N / 2 + 10;
    }

    private static void pearl_D_1_RB() {
        descending();
        golden[RIGHT] = RIGHT - 10;
    }

    private static void pearl_D_2_RB() {
        descending();
        golden[RIGHT] = RIGHT - 10;
        golden[MAX_N / 2] = MAX_N / 2 - 10;
    }
/*
    private static void testArr1() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 1;
        }
        golden[MAX_N / 3] = 0;
        golden[MAX_N / 3 * 2] = 0;
    }

    private static void testArr2() { // todo: use it: jdk 9 fails on it
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 0;
        }
        golden[MAX_N / 3] = 1;
        golden[MAX_N / 3 * 2] = 1;
    }
    
    private static void testArr3() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 1;
        }
        golden[MAX_N - 1] = 0;
    }
    
    private static void testArr4() { // todo: use it
        int k;
        int step = MAX_N / 16;
        
        int a1 = 0;
        int a2 = a1 + step;
        int a3 = a2 + step;
        int a4 = a3 + step * 2;
        int a5 = a4 + step * 2;
        int a6 = MAX_N;
        
        k = 0;
        for (int i = a1; i < a2; i++) {
            golden[i] = k++;
        }
        k = 0;
        for (int i = a2; i < a3; i++) {
            golden[i] = k++;
        }
        k = 0;
        for (int i = a3; i < a4; i++) {
            golden[i] = k++;
        }
        k = 0;
        for (int i = a4; i < a5; i++) {
            golden[i] = k++;
        }
        k = 0;
        for (int i = a5; i < a6; i++) {
            golden[i] = k++;
        }
    }

    private static void testArr5() { // todo: use it: jdk 9 fails on it
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 0;
        }
        golden[MAX_N / 2] = 1;
    }

    private static void testArr6() { // todo: use it
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = 1;
        }
        golden[MAX_N / 2] = 0;
    }
*/
    private static void killer() {
//      new DPQKiller().kill(golden);
    }

    private static void permutation() {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = i;
        }
        Random random = new Random(randomInit);
        
        for (int i = MAX_N; i > 1; i--) {
            int k = random.nextInt(i);
            int t = golden[i - 1]; golden[i - 1] = golden[k]; golden[k] = t;
        }
    }
    
    private static void plateauFr(int m) {
        plateau(m);
        reverse(golden, 0, golden.length / 2);
    }
    
    private static void plateauRe(int m) {
        plateau(m);
        reverse(golden, 0, golden.length);
    }
    
    private static void reverse() {
        reverse(golden, 0, golden.length);
    }
    
    private static void reverse(int[] a, int start, int hi) {
        hi--;
        while (start < hi) {
            int tmp = a[start];
            a[start++] = a[hi];
            a[hi--] = tmp;
        }
    }
    
    private static void asc(int m) {
        for (int i = 0; i < MAX_N - m; i++) {
            golden[i] = i;
        }
        for (int k = 0, i = MAX_N - m; i < MAX_N; i++, k++) {
            if (i < 0) i = 0;
            golden[i] = k;
        }
    }
    
    private static void ascend(int m) {
        ascending();
        
        for (int i = MAX_N - 1, j = 1; j <= m; j++, i--) {
            if (i < 0) i = 0;
            golden[i] = 0;
        }
    }

    private static void ascending(int m) {
        Random random = new Random(randomInit);
    
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = i;
        }
        for (int i = MAX_N - m; i < MAX_N; i++) {
            if (i < 0) i = 0;
            golden[i] = random.nextInt(MAX_N);
        }
    }

    private static void equal(int m) {
        equal();

        for (int i = MAX_N - m; i < MAX_N; i++) {
            if (i < 0) i = 0;
            golden[i] = -1;
        }
    }

    private static void plateau(int m) {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = Math.min(i, m);
        }
    }

    private static void table(int m) {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = Math.max(i, m);
        }
    }

    private static void shuffle() {
        Random random = new Random(randomInit);

        for (int i = 0, j = 0, k = 1; i < MAX_N; i++) {
            golden[i] = (random.nextBoolean() ? (j += 2) : (k += 2));
        }
    }

    private static void shuffle(int m) {
        Random random = new Random(randomInit);

        for (int i = 0, j = 0, k = 1; i < MAX_N; i++) {
            golden[i] = (random.nextInt(m) > 0 ? (j += 2) : (k += 2));
        }
    }

    private static void staggerFr(int m) {
        stagger(m);
        reverse(golden, 0, golden.length / 2);
    }

    private static void stagger(int m) {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = ((i * m + i) % MAX_N);
        }
    }

    private static void stagger2T(int m) {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = i;
        }
        for (int i = MAX_N - m, k = 0; i < MAX_N; ++i) {
            golden[i] = ++k;
        }
    }

    private static void period(int m) {
        for (int i = 0; i < MAX_N; i++) {
            golden[i] = (i % m);
        }
    }

    private static void random(int m) {
        Random random = new Random(randomInit);

        for (int i = 0; i < MAX_N; i++) {
            golden[i] = random.nextInt(m);
        }
    }

    private static void organPipes() {
        int middle = MAX_N / 2;

        for (int i = 0; i <= middle; i++) {
            golden[i] = i;
            golden[MAX_N - 1 - i] = i;
        }
    }

    private static void outArr(char[] a) {
        outArr(a, 0, a.length - 1);
    }

    public static void outArr(char[] a, int left, int right) {
        for (int i = left; i <= right; ++i) {
            System.out.print(a[i] + ", ");
        }
        System.out.println();
    }

    public static void copy(char[] src, char[] dst, int left, int right) {
        for (int i = left; i <= right; ++i) {
            dst[i] = src[i];
        }
    }

    public static void prepareGolden() {
        for (int i = 0; i < LEFT; i++) {
            golden[i] = Integer.MAX_VALUE;
        }
        for (int i = MAX_N - 1; i > RIGHT; i--) {
            golden[i] = Integer.MIN_VALUE;
        }
    }

    public static void check(String name, char[] a1, char[] a2, int left, int right) {
        check(name, a1, a2, left, right, false);
    }

    public static void check(String name, char[] a1, char[] a2, int left, int right, boolean show) {
        for (int i = 0; i < LEFT; i++) {
            if (a1[i] != Integer.MAX_VALUE) {
                throw new RuntimeException("\n\n" + name + ": Left.1 part is modified");
            }
            if (a2[i] != Integer.MAX_VALUE) {
                throw new RuntimeException("\n\n" + name + ": Left.2 part is modified");
            }
        }
        for (int i = MAX_N - 1; i > RIGHT; i--) {
            if (a1[i] != Integer.MIN_VALUE) {
                throw new RuntimeException("\n\n" + name + ": Right.1 part is modified");
            }
            if (a2[i] != Integer.MIN_VALUE) {
                throw new RuntimeException("\n\n" + name + ": Right.2 part is modified");
            }
        }

        for (int i = left; i < right; i++) {
            if (a1[i] > a1[i + 1]) {
                if (show) {
                    outArr(a2, left, right);
                    outArr(a1, left, right);
                }
//              out("!!! Array is not sorted at: " + i + " " + a1[i] + " " + a1[i+1]);
                throw new RuntimeException("\n\n" + name + ": Array is not sorted at index " + i + ": " + a1[i] + " " + a1[i+1]);
            }
        }
        int plusCheckSum1 = 0;
        int plusCheckSum2 = 0;
        int xorCheckSum1 = 0;
        int xorCheckSum2 = 0;

        for (int i = left; i <= right; i++) {
            plusCheckSum1 += a1[i];
            plusCheckSum2 += a2[i];
            xorCheckSum1 ^= a1[i];
            xorCheckSum2 ^= a2[i];
        }
        if (plusCheckSum1 != plusCheckSum2) {
            if (show) {
                outArr(a2);
                outArr(a1);
            }
            throw new RuntimeException("\n\n" + "!!! Array is not sorted correctly [+]");
        }
        if (xorCheckSum1 != xorCheckSum2) {
            if (show) {
                outArr(a2);
                outArr(a1);
            }
            throw new RuntimeException("\n\n" + "!!! Array is not sorted correctly [^]");
        }
    }
/*
    public static void check(String name, int[] a, int left, int right, boolean show) {
        for (int i = left; i < right; i++) {
            if (a[i] > a[i + 1]) {
                if (show) {
                    outArr(a, left, right);
                }
//              out("!!! Array is not sorted at: " + i + " " + a[i] + " " + a[i+1]);
                throw new RuntimeException("\n\n" + name + ": Array is not sorted at: " + i + " " + a[i] + " " + a[i+1]);
            }
        }
    }
*/
    private static void out() {
        if ( !warm) System.out.println();
    }

    private static void out(Object object) {
        if ( !warm) System.out.println(object);
    }
/*
    private static void warmUp() {
        out(" start warm up");
        int[] test;

        random();
        System.out.print(".");

        for (int i = 0; i < COUNT / 2; i++) {
            test = golden.clone(); DualPivotQuicksort_.sort(test);
            test = golden.clone(); DualPivotQuicksort__.sort(test);

//          test = golden.clone(); java.util.Arrays.sort(test);
//          test = golden.clone(); java.util.Arrays.parallelSort(test);
//          test = golden.clone(); Arrays.sort(test);

            if ((i & 1) == 0) System.out.print(".");
        }
        out();
        out("   end warm up");
    }                                                      
*/                                                           
    private interface Sortable {
        void sortArray(char[] a);
    }

//  private static final int MAX_N = 30;
//  private static final int COUNT = 5000000;

//  private static final int MAX_N = 50;
//  private static final int COUNT = 7000000;

//  private static final int MAX_N = 63;
//  private static final int COUNT = 9000000;

//  private static final int MAX_N = 70;
//  private static final int COUNT = 9000000;

//  private static final int MAX_N = 100;
//  private static final int COUNT = 9000000;

//  private static final int MAX_N = 200;
//  private static final int COUNT = 2000000;
 
//  private static final int MAX_N = 500;
//  private static final int COUNT = 300000;

//  private static final int MAX_N = 1000;
//  private static final int COUNT = 100000;

//  private static final int MAX_N = 50000;
//  private static final int COUNT = 3000;

//  private static final int MAX_N = 30000;
//  private static final int COUNT = 200;

//  private static final int MAX_N = 1000;
//  private static final int COUNT = 100000;

//  private static final int MAX_N = 10000;
//  private static final int COUNT = 10000;

//  private static final int MAX_N = 100000;
//  private static final int COUNT = 1300;

//  private static final int MAX_N = 20000;
//  private static final int COUNT = 5000;

//  private static final int MAX_N = 2000;
//  private static final int COUNT = 50000;
  
//  private static final int MAX_N = 1000000;
//  private static final int COUNT = 900;

//  private static final int MAX_N = 3000000;
//  private static final int COUNT = 300;

//  private static final int MAX_N = 9000000;
//  private static final int COUNT = 300;
//  private static final int COUNT =  1;




//  private static final int MAX_N = 150;
//  private static final int COUNT = 2000000;

//  private static final int MAX_N = 30000;   // 30K
//  private static final int COUNT = 10000;

    private static final int MAX_N = 1000000; // 1M
    private static final int COUNT = 150;




    private static int parallelism = 0;
//  private static int parallelism = ForkJoinPool.getCommonPoolParallelism();

    private static Random random = new Random();
    private static boolean warm;
    private static int randomInit;

    private static double time1;
    private static double time2;
    private static double totalTime1;
    private static double totalTime2;

    private static final int LEFT = 0;
    private static final int RIGHT = MAX_N - LEFT - 1;
    private static final int HIGH = RIGHT + 1;
    private static final int WARM_COUNT = 10;

    private static int[] golden = new int[MAX_N];
    private static char[] save = new char[MAX_N];
    private static char[] test = new char[MAX_N];
    private static final boolean CHECK = true;
}
