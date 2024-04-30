package java.util;

public class JavaBenchmarkHarness {

    private int[][] arrays = new int[][] { new int[600], new int[3_000], new int[90_000], new int[400_000], new int[1_000_000] };

    public static void main(String[] args) {
        new JavaBenchmarkHarness().main();
    }

    private void main() {
        init();

        benchmark("b01     ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_b01       .sort(a, 0, 0, a.length); }});
        benchmark("r30     ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30       .sort(a, 0, 0, a.length); }});
        benchmark("r30_a   ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_a     .sort(a, 0, 0, a.length); }});
        benchmark("r30_5   ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_5     .sort(a, 0, 0, a.length); }});

        benchmark("p_b01   ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_b01       .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30   ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30       .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_a ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_a     .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_11", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_11    .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_12", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_12    .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_13", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_13    .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_14", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_14    .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_21", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_21    .sort(a, PARALLELISM, 0, a.length); }});
        benchmark("p_r30_23", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r30_23    .sort(a, PARALLELISM, 0, a.length); }});

    }

    private static enum Builder {

        RANDOM(1) {
            @Override
            void build(int[] b) {
                Random random = new Random(0x777);

                for (int i = 0; i < b.length; ++i) {
                    b[i] = random.nextInt();
                }
            }
        },

        REPEATED(2) {
            @Override
            void build(int[] b) {
                Random random = new Random(0x111);

                for (int i = 0; i < b.length; ++i) {
                    b[i] = random.nextInt(5);
                }
            }
        },

        SAWTOOTH(6) {
            @Override
            void build(int[] b) {
                int m = b.length / 2;

                for (int i = 0; i < b.length; ++i) {
                    b[i] = i % m;
                }
            }
        },

        STAGGER(6) {
            @Override
            void build(int[] b) {
                for (int i = 0; i < b.length; ++i) {
                    b[i] = (i * 8) % b.length;
                }
            }
        },

        SHUFFLE(1) {
            @Override
            void build(int[] b) {
                Random random = new Random(0x777);
  
                for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                    b[i] = random.nextInt(11) > 0 ? (j += 2) : (k += 2);
                }
            }
        };

        abstract void build(int[] b);

        private Builder(int factor) {
            this.factor = factor;
        }

        private int factor;
    }

    private void init() {
        System.out.println("name        builder     size  mode   count       score\n");
    }

    private void benchmark(String name, Sorter sorter) {
        for (Builder builder : Builder.values()) {
            for (int[] a : arrays) {
                benchmark(a, name, sorter, builder, true);
                benchmark(a, name, sorter, builder, false);
            }
        }
    }

    private void benchmark(int[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
        long startTime, idleTime, allTime;

        if (warmup) {
            System.out.print(extend(name, 7) + " " + extend(10, builder) + " " + extend(8, a.length));
        }
        int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));
        startTime = System.nanoTime();
  
        for (int i = 0; i < count; ++i) {
            builder.build(a);
        }
        idleTime = System.nanoTime() - startTime;
        startTime = System.nanoTime();

        for (int i = 0; i < count; ++i) {
            builder.build(a);
            sorter.sort(a);
        }
        allTime = System.nanoTime() - startTime;

        if (warmup) {
            System.out.print("  avg " + extend(8, count) + " ");
        } else {
            System.out.println(extend(11, String.format("%.3f", ((double) (allTime - idleTime)) / count / 1_000.0)));
        }
    }

    private String extend(int max, Object object) {
        String value = object.toString();

        for (int i = value.length(); i < max; ++i) {
            value = " " + value;
        }
        return value;
    }

    private String extend(String value, int max) {
        for (int i = value.length(); i < max; ++i) {
            value += " ";
        }
        return value;
    }

    private static interface Sorter {
        void sort(int[] a);
    }

    private static int PARALLELISM = java.util.concurrent.ForkJoinPool.getCommonPoolParallelism();
}
