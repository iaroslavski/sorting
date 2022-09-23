import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MainUUE {

    public static void main(String[] arg) {
        if (arg.length != 2) {
            return;
        }
        String inName = arg[0];
        String outName = arg[1];

        try {
            if (inName.endsWith(".uue")) {
                decode(inName, outName);
            } else {
                encode(inName, outName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void encode(String inName, String outName) throws IOException {
        RandomAccessFile inFile = new RandomAccessFile(inName, "r");
        BufferedWriter outFile = new BufferedWriter(new FileWriter(outName));
        int length = (int) inFile.length();
        byte[] bytes = new byte[length];
//System.out.println("Start");
        inFile.read(bytes);
//System.out.println("Read input file");

        for (int i = 0; i < length; ++i) {
            int b = (int) bytes[i];
            outFile.write(HEX[ ((b >> 4) /*^ i*/) & 0xF ] + HEX[ (b /*^ i*/) & 0xF ]);
        }
//System.out.println("Encoded");
        outFile.close();
//System.out.println("End.");
    }

    private static void decode(String inName, String outName) throws IOException {
        RandomAccessFile outFile = new RandomAccessFile(outName, "rw");
        RandomAccessFile inFile = new RandomAccessFile(inName, "r");
//System.out.println("Start");
        int length = (int) inFile.length();
        byte[] data = new byte[length];
        inFile.read(data);
        String content = new String(data);
        byte[] bytes = new byte[length/2];
//System.out.println("Read input file");

        for (int i = 0, k = -1; i < length; i += 2) {
            byte hi = (byte) ((toByte(content.charAt(i    )) /*^  i     */) << 4);
            byte lo = (byte) ( toByte(content.charAt(i + 1)) /*^ (i + 1)*/     );
            bytes[++k] = (byte) (hi | lo);
        }
//System.out.println("Decoded");
        outFile.write(bytes);
        outFile.close();
//System.out.println("End.");
    }
    
    private static byte toByte(char ch) {
        switch (ch) {
            case '0': return  0;
            case '1': return  1;
            case '2': return  2;
            case '3': return  3;
            case '4': return  4;
            case '5': return  5;
            case '6': return  6;
            case '7': return  7;
            case '8': return  8;
            case '9': return  9;
            case 'A': return 10;
            case 'B': return 11;
            case 'C': return 12;
            case 'D': return 13;
            case 'E': return 14;
            case 'F': return 15;
            default: {
System.out.println("!!!!!!!!!!!!: " + ch);
                return 0;
            }
        }
    }

    private static final String[] HEX = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
}
