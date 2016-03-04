import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;


public class receiver {
	static volatile Map<Integer, String> bufferList = new TreeMap<Integer, String>();
	static Semaphore bufferLock = new Semaphore(1, true);
	
    public static void main(String[] args) throws Exception {
        if(args.length != 4){
            String usage = "invalid usage! \n"
            + "usage: java receiver "
            + "<hostname for the network emulator>, "
            + "<UDP port number used by the link emulator to receive ACKs from the receiver>, \n"
            + "<UDP port number used by the receiver to receive data from the emulator>, "
            + "<name of the file into which the received data is written>";
            System.err.println(usage);
            System.exit(1);
        }
        
        String emulatorHostAddr = args[0];
        int emulatorUDPPort = Integer.parseInt(args[1]);
        int receiverUDPPort = Integer.parseInt(args[2]);
        String fileName = args[3];
        
        
        
        DatagramSocket receiveSocket = new DatagramSocket(receiverUDPPort);
        DatagramPacket receivePacket, sendPackert;
        packet myReceivePacket, myACKPacket;
        byte[] receiveData = new byte[1024];
        byte[] sendData  = new byte[1024];
        int expect = 0;
        
        PrintWriter writer = new PrintWriter(new FileWriter(fileName));
        PrintWriter arrival= new PrintWriter(new FileWriter("./arrival.log"));
        
        while(true){
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            receiveSocket.receive(receivePacket);
            
            myReceivePacket = packet.parseUDPdata(receivePacket.getData());
            int packetSeqNum = myReceivePacket.getSeqNum();
            arrival.println(packetSeqNum);

            if(myReceivePacket.getType() == 2){
                // EOT
                myACKPacket=packet.createEOT(packetSeqNum);
                
                sendData = myACKPacket.getUDPdata();
                sendPackert = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(emulatorHostAddr), emulatorUDPPort);
                receiveSocket.send(sendPackert);
                arrival.close();
                writer.close();
                receiveSocket.close();
                break;             
            }
            else if(myReceivePacket.getType() == 1){
                // Receive packets
            	String msg = new String(myReceivePacket.getData());
            	
            	
            	
            	ArrayList<Integer> range1 = new ArrayList<Integer>();
            	ArrayList<Integer> range2 = new ArrayList<Integer>();
            	int e = expect%32;
            	
            	
            	for (int i = 1; i < 10; i++) {
            		int a = (e+i)%32;
            		if (a<0){
            			a += 32;
            		}
					range1.add(a);
				}
            	
            	for (int i = 1; i < 11; i++) {
            		int a = (e-i)%32;
            		if (a<0){
            			a += 32;
            		}
					range2.add(a);
				}
            	
            	if(packetSeqNum == expect%32){
                    bufferLock.acquire();
                    bufferList.put(packetSeqNum, msg);
                    bufferLock.release();
            		while(!bufferList.isEmpty()){
            			if(bufferList.containsKey(expect%32)){
            				writer.print(bufferList.get(expect%32));
	                		bufferLock.acquire();
	                		bufferList.remove(expect%32);
	                		bufferLock.release();
	                		expect++;
	                	}
	                	else{
	                		break;
	                		}
            			}
            		}
            	else if (range1.contains(packetSeqNum)){
                    // seq# add into buffer
                    bufferLock.acquire();
                    bufferList.put(packetSeqNum, msg);
                    bufferLock.release();
                }
            	else if (range2.contains(e)){
                    // premature timeout
                }
            	
            	myACKPacket = packet.createACK(packetSeqNum);
            	sendData = myACKPacket.getUDPdata();
            	sendPackert = new DatagramPacket(sendData, sendData.length, 
                    InetAddress.getByName(emulatorHostAddr), emulatorUDPPort);
            	receiveSocket.send(sendPackert); 
            }
        }    
    }
}
