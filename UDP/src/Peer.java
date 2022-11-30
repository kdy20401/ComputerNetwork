import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class MessageSenderUDPThread extends Thread {
    String groupIP;
    int portNumber;
    String userName;
    MulticastSocket socket;
    InetAddress group;

    public MessageSenderUDPThread(String groupIP, int portNumber, String userName) {
        this.groupIP = groupIP;
        this.portNumber = portNumber;
        this.userName = userName;
    }

    public void run() {
        try {
            group = InetAddress.getByName(groupIP);
            socket = new MulticastSocket();
        } catch(IOException e) {
            e.printStackTrace();
        }

        while(true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();

                if(input.equals("#EXIT")) {
                    socket.close();
                    break;
                } else {
                    String message = "Peer" + userName + ": " + input;
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, portNumber);
                    socket.send(packet);
                }

            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}


class MessageReceiverUDPThread extends Thread{
    String groupIP;
    int portNumber;
    MulticastSocket socket;

    public MessageReceiverUDPThread(String groupIP, int portNumber) {
        this.groupIP = groupIP;
        this.portNumber = portNumber;
    }

    public void run(){
        try {
            // create socket binded to specific port number
            // and let socket join multicast address group to allow multicasting
            InetAddress group = InetAddress.getByName(groupIP);
            socket = new MulticastSocket(portNumber);
            socket.joinGroup(group);

            int turn = 0;
            while(turn < 100) {
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println((new String(buffer)).trim());
                turn++;
            }
            socket.close();
        } catch(Exception e) {
//            e.printStackTrace();
        }
    }

    @Override
    public void interrupt(){
        try {
            super.interrupt();
            socket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

public class Peer {
    public static void main(String[] args) throws IOException {
        // specify port number when starting program
        int portNumber = Integer.parseInt(args[0]);

        while(true) {
            // instruction for user
            System.out.println("==========================================================");
            System.out.println("Welcome to UDP chatting room program!");
            System.out.println("Possible options.");
            System.out.println("-IN waiting room,");
            System.out.println("#JOIN <chat room name> <user name>: join the chat room");
            System.out.println("#QUIT: quit this program");
            System.out.println("\n-IN chatting room,");
            System.out.println("#EXIT: exit from the chatting room room");
            System.out.println("==========================================================");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String[] input = reader.readLine().split(" "); // ex, #JOIN cnet deok
            String option = input[0];

            // join to specific chatting room
            if (option.equals("#JOIN")) {
                try {
                    String chattingRoomName = input[1]; // ex, cnet
                    String userName = input[2];         // ex, deok
                    String groupIP;

                    // make multicast address characterized by chatting room name
                    // 'cnet' -> 225.x.y.z
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(chattingRoomName.getBytes());
                    byte[] bytes = md.digest();
                    byte x = bytes[bytes.length - 3];
                    byte y = bytes[bytes.length - 2];
                    byte z = bytes[bytes.length - 1];
                    x = (x >= 0) ? (byte)x : (byte)-x;
                    y = (y >= 0) ? (byte)y : (byte)-y;
                    z = (z >= 0) ? (byte)z : (byte)-z;
                    String[] arr = {"225", Byte.toString(x), Byte.toString(y), Byte.toString(z)};
                    groupIP = String.join(".", arr);

                    // create two threads:
                    // one for receiving user input and sending message to the other users,
                    // the other for receiving messages from the other users.
                    MessageSenderUDPThread messageSender = new MessageSenderUDPThread(groupIP, portNumber, userName);
                    MessageReceiverUDPThread messageReceiver = new MessageReceiverUDPThread(groupIP, portNumber);

                    messageSender.start();
                    messageReceiver.start();
                    messageSender.join();        // wait until user exits from the chatting room
                    messageReceiver.interrupt(); // clear the thread for receiving messages after exiting

                } catch(Exception e) {
                    e.printStackTrace();
                }
            // terminate program
            }else if(option.equals("#QUIT")) {
                break;
            }
        }
    }
}