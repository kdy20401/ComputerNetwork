import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Map;


class ServerThread extends Thread {
    static Map<String, LinkedList<String>> chattingRoomList = new Hashtable<>();
    static Map<String, LinkedList<Socket>> socketListInChattingRoom = new Hashtable<>();

    Socket socket;
    String userName;
    String chattingRoomName;
    Boolean isJoined;

    public ServerThread(Socket socket) {
        this.socket = socket;
        this.isJoined = false;
    }

    public void run() {
        try {
            // allocate input & output stream for a client
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            String clientInput = "";

            while (!clientInput.startsWith("#TERMINATE")) {
                // wait for client input
                clientInput = reader.readLine();
                String[] args = clientInput.split(" "); // ex, #CREATE cnet deok
                String command = args[0];

                if (command.equals("#CREATE")) {
                    chattingRoomName = args[1];
                    userName = args[2];

                    // a chatting room under specific name already exists
                    if (chattingRoomList.containsKey(chattingRoomName)) {
                        writer.println("#CREATE fail");

                    } else {
                        chattingRoomList.put(chattingRoomName, new LinkedList<>());
                        chattingRoomList.get(chattingRoomName).add(userName);

                        socketListInChattingRoom.put(chattingRoomName, new LinkedList<>());
                        socketListInChattingRoom.get(chattingRoomName).add(socket);

                        isJoined = true;

                        writer.println("#CREATE success");
                    }

                }else if (command.equals("#JOIN")) {
                    chattingRoomName = args[1];
                    userName = args[2];

                    // a target chatting room doesn't exist
                    if (!chattingRoomList.containsKey(chattingRoomName)) {
                        writer.println("#JOIN fail");

                    } else {
                        chattingRoomList.get(chattingRoomName).add(userName);

                        socketListInChattingRoom.get(chattingRoomName).add(socket);

                        isJoined = true;

                        writer.println("#JOIN success");
                    }

                }else if (command.equals("#STATUS")) {
                    String statusString = "chatting room name: " + chattingRoomName + ", users: " + chattingRoomList.get(chattingRoomName);
                    writer.println(statusString);

                }else if (command.equals("#EXIT")) {
                    chattingRoomList.get(chattingRoomName).remove(userName);
                    socketListInChattingRoom.get(chattingRoomName).remove(socket);

                    if (chattingRoomList.get(chattingRoomName).isEmpty()) {
                        chattingRoomList.remove(chattingRoomName);
                        socketListInChattingRoom.remove(chattingRoomName);
                    }

                    writer.println("#EXIT");

                    isJoined = false;

                }else if (command.equals("#TERMINATE")) {
                    System.out.println("A client disconnected.");
                    break;

                }else if (isJoined){
                    // send message to all users in the chatting room except the sender
                    for(Socket sckt: socketListInChattingRoom.get(chattingRoomName)) {
                        if(!sckt.equals(socket)) {
                            PrintWriter wrt = new PrintWriter(sckt.getOutputStream(), true);
                            wrt.println(userName + ": " + clientInput);
                        }
                    }
                }
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Server {

    public static void main(String[] args) {
        int messagePort = Integer.parseInt(args[0]);
        int filePort = Integer.parseInt(args[1]); // not implemented

        // wait in welcoming socket
        try (ServerSocket serverSocket = new ServerSocket(messagePort)) {
            System.out.println("Server is running on port " + messagePort);
            System.out.println("Press ctrl + c to terminate server.");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected!");

                // allocate a thread for each client
                new ServerThread(socket).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}