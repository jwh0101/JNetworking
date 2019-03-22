import lib.net.JServer;

import java.util.List;
import java.util.Scanner;

public class ExampleServer extends JServer {

    public static void main(String[] args) {
        ExampleServer exampleServer = new ExampleServer(1500);
        exampleServer.start();
        Scanner sc = new Scanner(System.in);
        sc.next();

    }

    public ExampleServer(int port) {
        super(port);
    }

    @Override
    public void display(String msg) {
        System.out.println(msg);
    }

    @Override
    public String sentToFormat(String message, List<String> recipients) {
        return "Sent to --> " + recipients.toString() + ": " + message;
    }

}