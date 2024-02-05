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
 * Microbenchmarking of Arrays.sort() and Arrays.parallelSort().
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
public class ArraysSortNew {

    private static final int PARALLELISM = java.util.concurrent.ForkJoinPool.getCommonPoolParallelism(); // todo

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

    public static class Int extends ArraysSortNew {

        @Setup(Level.Invocation)
        public void build() {
            builder.build(b);
        }

        @Benchmark
        public void sort() {
            java.util.DualPivotQuicksort(b, 0, 0, b.length);
        }

        @Benchmark
        public void p_sort() {
            java.util.DualPivotQuicksort.sort(b, PARALLELISM, 0, b.length);
        }
    }
}
