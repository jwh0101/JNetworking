package lib.net;

import lib.Command;
import lib.Data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/*
 * The Client that can be run both as a console or a GUI
 */
public abstract class Client {

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    //private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;


    public Client(String server_ip, int port, String username) {
        this.server = server_ip;
        this.port = port;
        this.username = username;
    }

    /*
     * To connect the dialog
     */
    public void connect() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }
        // if it failed not much I can so
        catch(Exception ec) {
            display("Error connecting to server:" + ec);
        }

        display("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());

        /* Creating both Data Stream */
        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be Data objects
        try
        {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("Exception logging in : " + eIO);
            disconnect();
        }
        // success we inform the caller that it worked
    }



    public abstract void display(String message);

    /*
     * To send a message to the server
     */
    public void sendMessage(Data output) {
        try {
            sOutput.writeObject(output);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
            e.printStackTrace();
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {} // not much else I can do

        // inform the GUI
//        if(cg != null)
//            cg.connectionFailed();

    }

    public String getUsername() {
        return username;
    }

    public void broadcast(String input) throws IOException {
        sOutput.writeObject(new Data(input, username));
        display("You: " + input);
    }

    public void sendTo(String message, String... clients) {
        LinkedList<String> recipients = new LinkedList<>();
        recipients.addAll(Arrays.asList(clients));

        Data toSend = new Data(message, this.username);
        toSend.setRecipients(recipients);

        try {
            sOutput.writeObject(toSend);
            display("You --> " + recipients + ": " + message);
        } catch (IOException e) {
            display("Error Sending Message: " + e);
        }
    }

    public void sendTo(String message, List<String> recipients) {
        Data toSend = new Data(message, this.username);
        toSend.setRecipients(recipients);

        try {
            sOutput.writeObject(toSend);
            display("You --> " + recipients + ": " + message);
        } catch (IOException e) {
            display("Error Sending Message: " + e);
        }
    }

    public void getUsers() {
        sendCommand(Command.getPlayers);
    }

    public void sendCommand(Command command) {
        try {
            sOutput.writeObject(new Data(command, username));
        } catch (IOException e) {
            display("Command: " + command.getName() + " send failed: " + e);
        }
    }

    public String sendToAllDisplayFormat(String message, String sender) {
        return sender + ": " + message;
    }

    public String sendToSpecificClientsFormat(String message, String sender, List<String> recipients) {
        return sender + " --> " + recipients + ": " + message;
    }


    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    Data input = (Data) sInput.readObject();
                    // if console mode print the message and add back the prompt
                    //if(cg == null) {

                    if (input.getType() == Data.COMMAND) {
                        //input.getCommand().getRunnable().run();
                    } else if (input.getType() == Data.MESSAGE) {
                        String msg = input.getMessage();
                        if (!messageIsFromRecipient(input)) {
                            if (input.isSendToAll())
                                display(sendToAllDisplayFormat(msg, input.getSender()));
                            else
                                display(sendToSpecificClientsFormat(msg, input.getSender(), input.getRecipients()));
                        }
                    }

                    //}
//                    else {
//                        cg.append(msg);
//                    }
                }
                catch(IOException e) {
                    display("Server has closed the connection: " + e);
//                    if(cg != null)
//                        cg.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch(ClassNotFoundException e2) {
                }
            }
        }

        private boolean thisContainedInRecipientList(Data input) {
            for (String player : input.getRecipients()) {
                if (player.equals(username))
                    return true;
            }
            return false;
        }

        private boolean messageIsFromRecipient(Data input) {
            if (input.getSender().equals(Data.FROM_SERVER))
                return false;
            return input.getSender().equals(username);
        }
    }
}
