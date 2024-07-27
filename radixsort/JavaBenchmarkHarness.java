package java.util;

public class JavaBenchmarkHarness {

    public static void main(String[] args) {
        new JavaBenchmarkHarness.Ints().main();
        new JavaBenchmarkHarness.Longs().main();
        new JavaBenchmarkHarness.Bytes().main();
        new JavaBenchmarkHarness.Chars().main();
        new JavaBenchmarkHarness.Shorts().main();
        new JavaBenchmarkHarness.Floats().main();
        new JavaBenchmarkHarness.Doubles().main();
    }

    private static class Ints {

        private int[][] arrays = new int[][] { new int[600], new int[3_000], new int[90_000], new int[400_000], new int[1_000_000] };

        private void main() {
            init();

            benchmark("Int.b01  ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_b01.sort(a, 0, 0, a.length); }});
            benchmark("Int.r32  ", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r32.sort(a, 0, 0, a.length); }});
            benchmark("Int.p_b01", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_b01.sort(a, PARALLELISM, 0, a.length); }});
            benchmark("Int.p_r32", new Sorter() { public void sort(int[] a) { DualPivotQuicksort_r32.sort(a, PARALLELISM, 0, a.length); }});
        }

        private enum Builder {

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
                    Random random = new Random(0x999);
      
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
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (int[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(int[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(int[] a);
        }
    }

    private static class Longs {

        private long[][] arrays = new long[][] { new long[600], new long[3_000], new long[90_000], new long[400_000], new long[1_000_000] };

        private void main() {
            init();

            benchmark("Long.b01  ", new Sorter() { public void sort(long[] a) { DualPivotQuicksort_b01.sort(a, 0, 0, a.length); }});
            benchmark("Long.r32  ", new Sorter() { public void sort(long[] a) { DualPivotQuicksort_r32.sort(a, 0, 0, a.length); }});
            benchmark("Long.p_b01", new Sorter() { public void sort(long[] a) { DualPivotQuicksort_b01.sort(a, PARALLELISM, 0, a.length); }});
            benchmark("Long.p_r32", new Sorter() { public void sort(long[] a) { DualPivotQuicksort_r32.sort(a, PARALLELISM, 0, a.length); }});
        }

        private enum Builder {

            RANDOM(1) {
                @Override
                void build(long[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextLong();
                    }
                }
            },

            REPEATED(2) {
                @Override
                void build(long[] b) {
                    Random random = new Random(0x111);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextInt(5);
                    }
                }
            },
  
            STAGGER(6) {
                @Override
                void build(long[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (i * 8) % b.length;
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(long[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = random.nextLong(11) > 0 ? (j += 2) : (k += 2);
                    }
                }
            };
  
            abstract void build(long[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }

            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (long[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(long[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(long[] a);
        }
    }

    private static class Bytes {

        private byte[][] arrays = new byte[][] { new byte[600], new byte[3_000], new byte[90_000], new byte[400_000], new byte[1_000_000] };

        private void main() {
            init();

            benchmark("Byte.b01", new Sorter() { public void sort(byte[] a) { DualPivotQuicksort_b01.sort(a, 0, a.length); }});
            benchmark("Byte.r32", new Sorter() { public void sort(byte[] a) { DualPivotQuicksort_r32.sort(a, 0, a.length); }});
        }

        private enum Builder {

            RANDOM(1) {
                @Override
                void build(byte[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (byte) random.nextInt();
                    }
                }
            },
  
            REPEATED(2) {
                @Override
                void build(byte[] b) {
                    Random random = new Random(0x111);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (byte) random.nextInt(5);
                    }
                }
            },

            STAGGER(3) {
                @Override
                void build(byte[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (byte) ((i * 8) % b.length);
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(byte[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = (byte) (random.nextInt(11) > 0 ? (j += 2) : (k += 2));
                    }
                }
            };

            abstract void build(byte[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }

            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (byte[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(byte[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(byte[] a);
        }
    }

    private static class Chars {

        private char[][] arrays = new char[][] { new char[600], new char[3_000], new char[90_000], new char[400_000], new char[1_000_000] };

        private void main() {
            init();

            benchmark("Char.b01", new Sorter() { public void sort(char[] a) { DualPivotQuicksort_b01.sort(a, 0, a.length); }});
            benchmark("Char.r32", new Sorter() { public void sort(char[] a) { DualPivotQuicksort_r32.sort(a, 0, a.length); }});
        }

        private enum Builder {

            RANDOM(1) {
                @Override
                void build(char[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (char) random.nextInt();
                    }
                }
            },
  
            REPEATED(2) {
                @Override
                void build(char[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (char) random.nextInt(3);
                    }
                }
            },

            STAGGER(3) {
                @Override
                void build(char[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (char) ((i * 8) % b.length);
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(char[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = (char) (random.nextInt(11) > 0 ? (j += 2) : (k += 2));
                    }
                }
            };

            abstract void build(char[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }
  
            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (char[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(char[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(char[] a);
        }
    }

    private static class Shorts {

        private short[][] arrays = new short[][] { new short[600], new short[3_000], new short[90_000], new short[400_000], new short[1_000_000] };

        private void main() {
            init();

            benchmark("Short.b01", new Sorter() { public void sort(short[] a) { DualPivotQuicksort_b01.sort(a, 0, a.length); }});
            benchmark("Short.r32", new Sorter() { public void sort(short[] a) { DualPivotQuicksort_r32.sort(a, 0, a.length); }});
        }

        private enum Builder {
  
            REPEATED(2) {
                @Override
                void build(short[] b) {
                    Random random = new Random(0x111);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (short) random.nextInt(5);
                    }
                }
            },
    
            STAGGER(3) {
                @Override
                void build(short[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (short) ((i * 8) % b.length);
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(short[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = (short) (random.nextInt(11) > 0 ? (j += 2) : (k += 2));
                    }
                }
            },
  
            RANDOM(1) {
                @Override
                void build(short[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (short) random.nextInt();
                    }
                }
            };

            abstract void build(short[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }

            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (short[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(short[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(short[] a);
        }
    }

    private static class Floats {

        private float[][] arrays = new float[][] { new float[600], new float[3_000], new float[90_000], new float[400_000], new float[1_000_000] };

        private void main() {
            init();

            benchmark("Float.b01  ", new Sorter() { public void sort(float[] a) { DualPivotQuicksort_b01.sort(a, 0, 0, a.length); }});
            benchmark("Float.r32  ", new Sorter() { public void sort(float[] a) { DualPivotQuicksort_r32.sort(a, 0, 0, a.length); }});
            benchmark("Float.p_b01", new Sorter() { public void sort(float[] a) { DualPivotQuicksort_b01.sort(a, PARALLELISM, 0, a.length); }});
            benchmark("Float.p_r32", new Sorter() { public void sort(float[] a) { DualPivotQuicksort_r32.sort(a, PARALLELISM, 0, a.length); }});
        }

        private enum Builder {

            RANDOM(1) {
                @Override
                void build(float[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextInt();
                    }
                }
            },

            REPEATED(2) {
                @Override
                void build(float[] b) {
                    Random random = new Random(0x111);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextInt(5);
                    }
                }
            },
      
            STAGGER(6) {
                @Override
                void build(float[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (i * 8) % b.length;
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(float[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = random.nextInt(11) > 0 ? (j += 2) : (k += 2);
                    }
                }
            };
      
            abstract void build(float[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }

            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (float[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(float[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(float[] a);
        }
    }

    private static class Doubles {

        private double[][] arrays = new double[][] { new double[600], new double[3_000], new double[90_000], new double[400_000], new double[1_000_000] };

        private void main() {
            init();

            benchmark("Double.b01  ", new Sorter() { public void sort(double[] a) { DualPivotQuicksort_b01.sort(a, 0, 0, a.length); }});
            benchmark("Double.r32  ", new Sorter() { public void sort(double[] a) { DualPivotQuicksort_r32.sort(a, 0, 0, a.length); }});
            benchmark("Double.p_b01", new Sorter() { public void sort(double[] a) { DualPivotQuicksort_b01.sort(a, PARALLELISM, 0, a.length); }});
            benchmark("Double.p_r32", new Sorter() { public void sort(double[] a) { DualPivotQuicksort_r32.sort(a, PARALLELISM, 0, a.length); }});
        }

        private enum Builder {

            RANDOM(1) {
                @Override
                void build(double[] b) {
                    Random random = new Random(0x777);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextInt();
                    }
                }
            },

            REPEATED(2) {
                @Override
                void build(double[] b) {
                    Random random = new Random(0x111);
      
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = random.nextInt(5);
                    }
                }
            },
      
            STAGGER(6) {
                @Override
                void build(double[] b) {
                    for (int i = 0; i < b.length; ++i) {
                        b[i] = (i * 8) % b.length;
                    }
                }
            },

            SHUFFLE(1) {
                @Override
                void build(double[] b) {
                    Random random = new Random(0x999);
      
                    for (int i = 0, j = 0, k = 1; i < b.length; ++i) {
                        b[i] = random.nextInt(11) > 0 ? (j += 2) : (k += 2);
                    }
                }
            };

            abstract void build(double[] b);
      
            private Builder(int factor) {
                this.factor = factor;
            }

            private int factor;
        }
    
        private void benchmark(String name, Sorter sorter) {
            for (Builder builder : Builder.values()) {
                for (double[] a : arrays) {
                    benchmark(a, name, sorter, builder, true);
                    benchmark(a, name, sorter, builder, false);
                }
            }
        }

        private void benchmark(double[] a, String name, Sorter sorter, Builder builder, boolean warmup) {
            long time1, time2, time3;

            if (warmup) {
                System.out.print(extend(name, 14) + " " + extend(9, builder) + " " + extend(8, a.length));
            }
            int count = (int) (2_500_000_000.0 * builder.factor / a.length / Math.log(a.length) / (warmup ? 2 : 1));

            time1 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
            }
            time2 = System.nanoTime();

            for (int i = 0; i < count; ++i) {
                builder.build(a);
                sorter.sort(a);
            }
            time3 = System.nanoTime();

            if (warmup) {
                System.out.print("  avg " + extend(8, count) + " ");
            } else {
                System.out.println(extend(11, String.format("%.3f", ((double) (time3 - time2 - (time2 - time1))) / count / 1_000.0)));
            }
        }

        private interface Sorter {
            void sort(double[] a);
        }
    }
    

    private static void init() {
        System.out.println("\nname             builder     size  mode   count       score\n");
    }

    private static String extend(String value, int max) {
        for (int i = value.length(); i < max; ++i) {
            value += " ";
        }
        return value;
    }

    private static String extend(int max, Object object) {
        String value = object.toString();

        for (int i = value.length(); i < max; ++i) {
            value = " " + value;
        }
        return value;
    }

    private static int PARALLELISM = java.util.concurrent.ForkJoinPool.getCommonPoolParallelism();
}
