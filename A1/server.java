import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;


public class server {
	public static void main(String[] args) throws Exception {
		int r_port;
		int n_port = 1024;
		int max = 2048;
		int min = 1025;
		int range = max - min;
		
		/*	Print n_port to let client know the port number and set up n_port to recevie client's request */
		System.out.println(String.valueOf(n_port));
		ServerSocket welcomeSocket = new ServerSocket(n_port);
		
		while(true){
/* Stage 1,TCP */
			/*	accept client's request for r_port and generate a random port number between 1025 and 2048 */
			Socket connectionSocket = welcomeSocket.accept();
			
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			
			Random rand = new Random();
			r_port = rand.nextInt(range) + min;
			
			outToClient.writeBytes(String.valueOf(r_port) + '\n');
			
/* Stage 2, UDP */
			/*	Set up UDP socket */
			DatagramSocket serverSocket = new DatagramSocket(r_port);
			
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];

			/*	recevie client's msg*/
			DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
			serverSocket.receive(receivePacket);
			String msg = new String(receivePacket.getData());
			
			/*	reverse the msg and send back to client*/
			InetAddress clientIPAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			String rvsMsg = new StringBuffer(msg).reverse().toString();
			sendData = rvsMsg.getBytes();
			
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,clientIPAddress,clientPort);
			serverSocket.send(sendPacket);
			
			serverSocket.close();
		}
	}
}