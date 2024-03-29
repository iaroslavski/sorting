/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.bench.java.util;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Microbenchmark for Arrays.sort() and Arrays.parallelSort().
 *
 * @author Vladimir Yaroslavskiy
 *
 * @version 2022.06.14
 *
 * @since 22
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value=1, jvmArgsAppend={"-XX:CompileThreshold=1", "-XX:-TieredCompilation"})
public class ArraysSort {

    private static final int PARALLELISM = java.util.concurrent.ForkJoinPool.getCommonPoolParallelism();

    @Param({ "600", "2000", "90000", "400000", "3000000" })
    int size;

    @Param
    Builder builder;

    int[] b;

    @Setup
    public void init() {
        b = new int[size];
    }

    public enum Builder {

        RANDOM {
            @Override
            void build(int[] b) {
                Random random = new Random(0x777);

                for (int i = 0; i < b.length; ++i) {
                    b[i] = random.nextInt();
                }
            }
        },

        REPEATED {
            @Override
            void build(int[] b) {
                Random random = new Random(0x777);

                for (int i = 0; i < b.length; ++i) {
                    b[i] = random.nextInt(3);
                }
            }
        },

        STAGGER {
            @Override
            void build(int[] b) {
                int m = b.length / 2;

                for (int i = 0; i < b.length; ++i) {
                    b[i] = i % m;
                }
            }
        },

        SHUFFLE {
            @Override
            void build(int[] b) {
                Random random = new Random(0x777);

                for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                    b[i] = random.nextInt(11) > 0 ? (j += 2) : (k += 2);
                }
            }
        };

        abstract void build(int[] b);
    }

    public static class Int extends ArraysSort {

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);
        }

        @Benchmark
        public void jdk() {
            Arrays.sort(b);
        }

        @Benchmark
        public void a15() {
            DualPivotQuicksort_a15.sort(b, 0, 0, b.length);
        }

        @Benchmark
        public void r20s() {
            DualPivotQuicksort_r20s.sort(b, 0, 0, b.length);
        }

        @Benchmark
        public void r20p() {
            DualPivotQuicksort_r20p.sort(b, 0, 0, b.length);
        }

        @Benchmark
        public void p_jdk() {
            Arrays.parallelSort(b);
        }
  
        @Benchmark
        public void p_a15() {
            DualPivotQuicksort_a15.sort(b, PARALLELISM, 0, b.length);
        }

        @Benchmark
        public void p_r20s() {
            DualPivotQuicksort_r20s.sort(b, PARALLELISM, 0, b.length);
        }

        @Benchmark
        public void p_r20p() {
            DualPivotQuicksort_r20p.sort(b, PARALLELISM, 0, b.length);
        }
    }

/*
    public static class Long extends ArraysSort {

        long[] a;

        @Setup
        public void setup() {
            a = new long[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }

    public static class Byte extends ArraysSort {

        byte[] a;

        @Setup
        public void setup() {
            a = new byte[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = (byte) b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }

    public static class Char extends ArraysSort {

        char[] a;

        @Setup
        public void setup() {
            a = new char[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = (char) b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }

    public static class Short extends ArraysSort {

        short[] a;

        @Setup
        public void setup() {
            a = new short[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = (short) b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }

    public static class Float extends ArraysSort {

        float[] a;

        @Setup
        public void setup() {
            a = new float[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }

    public static class Double extends ArraysSort {

        double[] a;

        @Setup
        public void setup() {
            a = new double[size];
        }

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);

            for (int i = 0; i < size; ++i) {
                a[i] = b[i];
            }
        }

        @Benchmark
        public void testSort() {
            Arrays.sort(a);
        }

        @Benchmark
        public void testParallelSort() {
            Arrays.parallelSort(a);
        }
    }
*/
}
