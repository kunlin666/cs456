iimport java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;


public class client {
	public static void main(String[] args) throws Exception {
		/*Get server address, n_port and message from command line arguments */
		if(args.length != 3){
			System.out.println("usage: java client <host address> <port> string");
			System.exit(1);
		}
		String serverAddr = args[0];
		String n_port = args[1];
		String msg = args[2];
		String r_port;
		
/*	Stage 1, TCP */
		/*	Create TCP socket */
		Socket clientSocket = new Socket(serverAddr, Integer.parseInt(n_port));
		
		/*	Receive r_port from server */
		BufferedReader inFromServer = new BufferedReader(
				new InputStreamReader(clientSocket.getInputStream()));
		
		r_port = inFromServer.readLine();
		//System.out.println("r_port: " + r_port);
		
		/*	Close socket*/
		clientSocket.close();

/* Stage 2, UDP */
		/*	Set up UDP socket */
		DatagramSocket UDPclientSocket = new DatagramSocket(); 
		
		InetAddress IPAddress = InetAddress.getByName(serverAddr);
		
		byte[] sendData = new byte[1024]; 
		byte[] receiveData = new byte[1024];

		/*	Package msg and sent */
		sendData = msg.getBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.parseInt(r_port));
		UDPclientSocket.send(sendPacket);
		
		/*	Recevie reversed msg from server and print it to standard output*/
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		UDPclientSocket.receive(receivePacket);
		String rvsMsg = new String(receivePacket.getData());
		System.out.println(rvsMsg); 
		
		/*	close UDP socket */
		UDPclientSocket.close();
	}
}
