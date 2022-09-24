public class BentleyBasher {

    private static final int MAX_N = 1_000_000;
    private static final int REPS = 3;

    public static void main(String[] args) {
        warmUp();
        sort();
    }

    private static void sort() {
        System.out.print("                            ");

        for (IntSorter sorter : IntSorter.values()) {
            System.out.print("         " + sorter);
        }
        System.out.println("\n");

        long startTime;
        long endTime;
        long minTime;

        for (int m = 1; m < 2 * MAX_N; m *= 2) {
            for (ParamIntArrayBuilder iab : ParamIntArrayBuilder.values()) {
                iab.build(golden, m);

                for (IntArrayTweaker iat : IntArrayTweaker.values()) {
                    iat.tweak(golden, result);
                    System.out.print(MAX_N + " " + m + "  " + space(iab.toString()) + "  " + space(iat.toString()));
                    int i = 0;

                    for (IntSorter sorter : IntSorter.values()) {
                        minTime = Long.MAX_VALUE;

                        for (int k = 0; k < REPS; ++k) {
                            IntArrayTweaker.copy(test, result);
                            startTime = System.nanoTime();
                            sorter.sort(test);
                            endTime = System.nanoTime();
                            minTime = Math.min(minTime, endTime - startTime);
                        }
                        check(test, result);
                        System.out.print("  " + round(minTime / 1000000.0));
                        times[i++] = minTime;
                    }
                    double ratio = times[0] / times[1];
                    System.out.print("  " + round(ratio));
                    String mark = "";

                    if        (1.00 <= ratio && ratio < 1.05) {
                        mark = " ..";
                    } else if (1.05 <= ratio && ratio < 1.10) {
                        mark = " .!.";
                    } else if (1.10 <= ratio && ratio < 1.20) {
                        mark = " .!!.";
                    } else if (1.20 <= ratio && ratio < 1.60) {
                        mark = " .!!!.";
                    } else if (1.60 <= ratio && ratio < 2.00) {
                        mark = " .!!!!.";
                    } else if (2.00 <= ratio) {
                        mark = " .!!!!!.";
                    }
                    System.out.println(mark);
                }
            }
        }
    }

    private static void warmUp() {
        System.out.println("start warm up");

        for (int m = 1; m < 2 * MAX_N; m *= 2) {
            for (ParamIntArrayBuilder iab : ParamIntArrayBuilder.values()) {
                iab.build(golden, m);

                for (IntArrayTweaker iat : IntArrayTweaker.values()) {
                    iat.tweak(golden, result);

                    for (IntSorter sorter : IntSorter.values()) {
                        IntArrayTweaker.copy(test, result);
                        sorter.sort(test);
                    }
                }
            }
        }
        System.out.println("  end warm up\n");
    }

    private static String round(double value) {
        String s = "" + (((long) Math.round(value * 1000000.0)) / 1000000.0);
        int k = s.length() - s.indexOf(".");

        for (int i = k; i <= 6; ++i) {
            s = s + "0";
        }
        for (int i = s.length(); i < 10; ++i) {
            s = " " + s;
        }
        return s;
    }

    private static String space(String value) {
        return value.equals("REVERSE_FR") || value.equals("REVERSE_BA") ? value : value.replace("_", " ");
    }

    private static void check(int[] a1, int[] a2) {
        for (int i = 0; i < a1.length - 1; ++i) {
            if (a1[i] > a1[i + 1]) {
                throw new RuntimeException("!!! Array is not sorted at: " + i);
            }
        }
        int plusCheckSum1 = 0;
        int plusCheckSum2 = 0;
        int xorCheckSum1 = 0;
        int xorCheckSum2 = 0;

        for (int i = 0; i < a1.length; ++i) {
            plusCheckSum1 += a1[i];
            plusCheckSum2 += a2[i];
            xorCheckSum1 ^= a1[i];
            xorCheckSum2 ^= a2[i];
        }
        if (plusCheckSum1 != plusCheckSum2) {
            throw new RuntimeException("!!! Array is not sorted correctly [+].");
        }
        if (xorCheckSum1 != xorCheckSum2) {
            throw new RuntimeException("!!! Array is not sorted correctly [^].");
        }
    }

    
    private static double[] times = new double[2];
    private static int[] golden = new int[MAX_N];
    private static int[] result = new int[MAX_N];
    private static int[] test = new int[MAX_N];
}
