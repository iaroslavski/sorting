import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Statistic {

    public static void main(String[] args) {
        new Statistic().processFile(args[0], null, null);
    }

    protected void doAfter(String number, String name) {
        System.out.println();
        System.out.println("winners: " + myWinners[0] + " / " + myWinners[1]);
        System.out.println("    avg: " + round(mult() * 100.0));
    }

    protected void processFile(String file, String number, String name) {
        List<String> lines = getLines(file);
        myTime = new double[2][lines.size()];

        for (int i = 0; i < lines.size(); ++i) {
            processLine(lines.get(i), i);
        }
        doAfter(number, name);
    }

    protected double mult() {
        double mult = 1.0d;
        int length = myTime[0].length;

        for (int i = 0; i < length; ++i) {
            if (Math.pow(mult * (myTime[0][i] / myTime[1][i]), 1.0 / (i + 1)) == 0.0) {
                return Math.pow(mult, 1.0 / (i));
            }
            mult *= myTime[0][i] / myTime[1][i];
        }
        return Math.pow(mult, 1.0 / length);
    }

    protected List<String> getLines(String file) {
        List<String> lines = new ArrayList<String>();
        String line;

        try {
            InputStream is = new FileInputStream(new File(file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            while ((line = reader.readLine()) != null) {
                if (
                    line.contains("RANDOM") ||
                    line.contains("STAGGER") ||
                    line.contains("PLATEAU") ||
                    line.contains("SHUFFLE") ||
                    line.contains("SAWTOOTH")
                ) {
                    lines.add(line.replace(',', '.'));
                }
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    protected void processLine(String line, int i) {
        StringTokenizer stk = new StringTokenizer(line, " \t");
        String value;
//System.out.println("-- '" + line + "'");
        value = stk.nextToken();
        value = stk.nextToken();
        value = stk.nextToken();
        value = stk.nextToken();
        value = stk.nextToken();

        myTime[0][i] = getDouble(value);
//System.out.print("Line " + i + ": " + value + " " + myTime[0][i]);

        value = stk.nextToken();
        myTime[1][i] = getDouble(value);
//System.out.println(" " + value + " " + myTime[1][i]);

        int winnerIndex = getWinner(i);
        myWinners[winnerIndex]++;
    }

    private int getWinner(int row) {
        int winerIndex = 0;
        double winer = myTime[0][row];

        for (int k = 1; k < 2; ++k) {
            if (myTime[k][row] < winer) {
                winerIndex = k;
                winer = myTime[k][row];
            }
        }
        return winerIndex;
    }

    private double getDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -0.0d;
        }
    }

    private static String round(double value) {
        String s = "" + (((long) Math.round(value * 10000.0)) / 10000.0);
        int k = s.length() - s.indexOf(".");

        for (int i = k; i <= 4; ++i) {
            s = s + "0";
        }
        return s;
    }

    private double[][] myTime;
    private int[] myWinners = new int[2];
}
