package org.openjdk.bench.java.util;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

import org.openjdk.jmh.annotations.Benchmark;

public class Main extends ArraysSort {

    private static final int PARALLELISM = ForkJoinPool.getCommonPoolParallelism();

    public static class Int extends ArraysSort.Int {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, PARALLELISM, 0, gold.length);
        }
    }

    public static class Long extends ArraysSort.Long {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, PARALLELISM, 0, gold.length);
        }
    }

    public static class Byte extends ArraysSort.Byte {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }
    }

    public static class Char extends ArraysSort.Char {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }
    }

    public static class Short extends ArraysSort.Short {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, 0, gold.length);
        }
    }

    public static class Float extends ArraysSort.Float {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, PARALLELISM, 0, gold.length);
        }
    }

    public static class Double extends ArraysSort.Double {

        @Benchmark
        public void newSort() {
            DualPivotQuicksort.sort(gold, 0, 0, gold.length);
        }

        @Benchmark
        public void newParallelSort() {
            DualPivotQuicksort.sort(gold, PARALLELISM, 0, gold.length);
        }
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }
}
