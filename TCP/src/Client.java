import java.net.*;
import java.io.*;


// user keyboard input -> server
class MessageSenderTCPThread extends Thread {
    PrintWriter writer;

    public MessageSenderTCPThread(PrintWriter writer) {
        this.writer = writer;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = "";

            do {
                input = reader.readLine();
                writer.println(input);
            } while (!input.equals("#EXIT"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// server -> user console output
class MessageReceiverTCPThread extends Thread{
    BufferedReader reader;

    public MessageReceiverTCPThread(BufferedReader reader) {
        this.reader = reader;
    }

    public void run() {
        String input = "";

        while (true) {
            try {
                input = reader.readLine();
                if (input.equals("#EXIT")) {
                    System.out.println("#EXIT success");
                    break;
                }
                System.out.println(input);
            } catch (IOException e) {
                break;
            }
        }
    }
}

public class Client {

    public static void startChatting(Socket socket, BufferedReader reader, PrintWriter writer) throws InterruptedException, IOException {
        MessageSenderTCPThread messageSender = new MessageSenderTCPThread(writer);
        MessageReceiverTCPThread messageReceiver = new MessageReceiverTCPThread(reader);

        messageSender.start();
        messageReceiver.start();
        messageSender.join();
        messageReceiver.join();
    }

    public static void printInstruction() {
        // instruction for user
        System.out.println("==========================================================");
        System.out.println("Welcome to TCP chatting program!");
        System.out.println("Possible commands.");
        System.out.println("-IN waiting room,");
        System.out.println("#CREATE <chat room name> <user name>: create a chat room");
        System.out.println("#JOIN <chat room name> <user name>: join a chat room");
        System.out.println("#TERMINATE: terminate this program");
        System.out.println("\n-IN chatting room,");
        System.out.println("#EXIT: exit from the chatting room");
        System.out.println("==========================================================");
    }

    public static void main(String[] args) {
        String hostname = args[0]; // 127.0.0.1
        int messagePort = Integer.parseInt(args[1]);
        int filePort = Integer.parseInt(args[2]); // not implemented

        // connect socket to server
        try (Socket socket = new Socket(hostname, messagePort)) {
            label:
            while(true) {
                try {
                    printInstruction();

                    // input from user (ex, #CREATE <chat room name> <user name>)
                    BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                    String[] consoleInput = console.readLine().split(" ");
                    String command = consoleInput[0];

                    switch (command) {
                        case "#CREATE": {
                            String chattingRoomName = consoleInput[1]; // ex, cnet
                            String userName = consoleInput[2];         // ex, deok

                            // get input & output stream
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                            // request room creation to server
                            writer.println("#CREATE" + " " + chattingRoomName + " " + userName);

                            String retMessage = reader.readLine();
                            System.out.println(retMessage);

                            // request failed
                            if (!retMessage.equals("#CREATE success")) {
                                continue;
                            }

                            System.out.println("now start chatting!");
                            System.out.println("===========================================");

                            startChatting(socket, reader, writer);

                            break;
                        }
                        case "#JOIN": {
                            String chattingRoomName = consoleInput[1]; // ex, cnet
                            String userName = consoleInput[2];         // ex, deok

                            // get input & output stream
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                            // request joining room to server
                            writer.println("#JOIN" + " " + chattingRoomName + " " + userName);
                            String retMessage = reader.readLine();
                            System.out.println(retMessage);

                            // request failed
                            if (!retMessage.equals("#JOIN success")) {
                                continue;
                            }

                            System.out.println("now start chatting!");
                            System.out.println("===========================================");

                            startChatting(socket, reader, writer);

                            break;
                        }
                        case "#TERMINATE": {
                            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                            writer.println("#TERMINATE");
                            socket.close();

                            break label; // terminate program. socket automatically closes.
                        }
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}