package $org.example;

import java.io.*;
import java.net.*;

public class RingElection {
    private static final int TOTAL_PROCESSES = 5;
    private static final int COORDINATOR_PORT = 5000;
    private static final int PROCESS_BASE_PORT = 6000;

    private static int processId;
    private static int coordinatorId;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java RingElection <processId>");
            return;
        }

        processId = Integer.parseInt(args[0]);
        coordinatorId = -1;

        Thread coordinatorThread = new Thread(() -> startCoordinator());
        coordinatorThread.start();

        Thread processThread = new Thread(() -> startProcess());
        processThread.start();
    }

    private static void sendMessage(int port, String message) {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String receiveMessage(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port);
             Socket clientSocket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void startCoordinator() {
        try (ServerSocket serverSocket = new ServerSocket(COORDINATOR_PORT)) {
            System.out.println("Coordenador iniciado na porta " + COORDINATOR_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String receivedMessage = in.readLine();
                if (receivedMessage.equals("ELECTION")) {
                    System.out.println("Recebido pedido de eleição do processo " + clientSocket.getPort());
                    sendMessage(clientSocket.getPort(), "OK");
                    coordinatorId = processId;
                    System.out.println("Processo " + processId + " se declarou como coordenador.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startProcess() {
        int nextProcessId = (processId + 1) % TOTAL_PROCESSES;

        try (ServerSocket serverSocket = new ServerSocket(PROCESS_BASE_PORT + processId)) {
            System.out.println("Processo " + processId + " iniciado na porta " + serverSocket.getLocalPort());
            sendMessage(PROCESS_BASE_PORT + nextProcessId, "ELECTION");

            while (true) {
                String receivedMessage = receiveMessage(PROCESS_BASE_PORT + processId);
                if (receivedMessage.equals("OK")) {
                    coordinatorId = nextProcessId;
                    System.out.println("Processo " + processId + " reconheceu o coordenador " + coordinatorId);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
