import java.io.IOException;


public class DetectConsole {

    public static void main(String[] args) throws Exception {
        DetectConsole detectConsole = new DetectConsole();
        boolean consoleIsOk = detectConsole.isTerminal();
        System.out.println("Has good console: " + consoleIsOk);
    }

    public boolean isTerminal() {
        try {
            System.out.write("\005".getBytes());
            System.out.flush();
            char c = getChar(100);
            if (c == 0) {
                return false;
            }
            while (c != 0) {
                c = getChar(10);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static int[] getCursorPosition() {
        try {
            // System.out.write("\033E\033c\033[6n".getBytes());
            System.out.write("\005".getBytes());
            System.out.flush();
            char c = readChar(500);
            if (c != 27) {
                System.out.println("Failed to get ESC (" + (int)c + ")");
                return null;
            }
            c = readChar(200);
            if (c != '[') {
                System.out.println("Failed to get '[' (" + (int)c + ")");
                return null;
            }
            int[] res = new int[2];
            boolean hasOne = false;
            c = readChar(200);
            while (Character.isDigit(c)) {
                hasOne = true;
                res[0] = res[0] * 10 + c - '0';
                c = readChar(200);
            }
            if (!hasOne) {
                System.out.println("Failed to get digit (" + (int)c + ")");
                return null;
            }
            if (c != ';') {
                System.out.println("Failed to get ';' (" + (int)c + ")");
                return null;
            }
            hasOne = false;
            c = readChar(200);
            while (Character.isDigit(c)) {
                hasOne = true;
                res[1] = res[1] * 10 + c - '0';
                c = readChar(200);
            }
            if (c != 'R') {
                System.out.println("Failed to get 'R' (" + (int)c + ")");
                return null;
            }
            return res;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static char readChar(int timeout) {
        long now = System.currentTimeMillis();
        try {
            while ((System.in.available() == 0) 
                    && (System.currentTimeMillis() - now < timeout)) {
                // Thread.yield();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
                // System.out.println("Thread yield");
            }
            if (System.in.available() > 0) {
                System.out.println("System.in.available() > 0");
                return (char)System.in.read();
            }
            System.out.println("System.in.available() == 0");
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
        
        return (char)0;
    }
    
    protected synchronized char getChar(int timeout) throws IOException {

        while (true) {
            int available = System.in.available();
            if (available > 0) {
                break;
            } else {
                try {
                    wait(200);
                } catch (InterruptedException ie) {
                    // do nothing
                }
            }
        }

        return (char) System.in.read();
    }
}
