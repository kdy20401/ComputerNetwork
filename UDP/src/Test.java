import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Test {
    public static void main(String[] args) throws IOException {
        String msg = "하앍";
        System.out.println(msg.length());
        System.out.println(msg.getBytes().length);

        byte[] bytes = msg.getBytes();
        for (byte b: bytes) {
            System.out.println(b);
        }
    }
}
