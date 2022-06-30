public class StatisticA extends Statistic {

    public static void main(String[] args) {
        new StatisticA().processFile(args[0], args[1], args[2]);
    }

    @Override
    protected void doAfter(String number, String name) {
        if (number.endsWith("01")) {
            System.out.print("\n" + name + ": ");
        }
        System.out.print(", " + round(mult() * 100.0));
    }

    private static String round(double value) {
        String s = "" + (((long) Math.round(value * 100.0)) / 100.0);
        int k = s.length() - s.indexOf(".");

        for (int i = k; i <= 2; ++i) {
            s = s + "0";
        }
        return s;
    }
}
