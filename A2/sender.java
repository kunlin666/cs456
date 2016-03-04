import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;


public class sender {
    static int emulatorUDPPort;
    static DatagramSocket sendSocket;
    static DatagramSocket receiveSocket;
    static int senderUDPPort;
    static String fileName;
    static int windowSize = 10;
    static int bufferSize = 500;
    static InetAddress IPAddress;
    static ArrayList <char[]> bufferList = new ArrayList<char[]>();
    static volatile Map <Integer, packet> window = new HashMap<Integer, packet>();
    static ArrayList <Integer> seqNumListForRemoving = new ArrayList<Integer>();
    static volatile Map <Integer, Timer> timerArray = new HashMap<Integer, Timer>();
    static DatagramPacket sendPacket;
    static String emulatorHostAddr;
    static int index = 0;
    static int expect = 0;
    static Semaphore windowLock = new Semaphore(1, true);
    static Semaphore timerLock = new Semaphore(1, true);
    static PrintWriter seqnumLog;
    static PrintWriter ackLog;
    static BufferedReader readFromFile;
    
    public static void main(String[] args) throws Exception {
        Logger log = Logger.getLogger("debug sender");
        log.setLevel(Level.FINE);
        if(args.length != 4){
            String usage = "invalid usage! \n"
            + "usage: java sender "
            + "<host address of the network emulator>, "
            + "<UDP port number used by the emulator to receive data from the sender>, \n"
            + "<UDP port number used by the sender to receive ACKs from the emulator>, "
            + "<name of the file to be transferred>";
            System.err.println(usage);
            System.exit(1);
        }
        
        emulatorHostAddr = args[0];
        emulatorUDPPort = Integer.parseInt(args[1]);
        senderUDPPort = Integer.parseInt(args[2]);
        fileName = args[3];
        
        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(senderUDPPort);
        IPAddress = InetAddress.getByName(emulatorHostAddr);
        byte[] sendData = new byte[1024];
        packet myPacket;
        
        
        readFromFile = new BufferedReader(new FileReader(fileName));
        File file = new File(fileName);
        int numOfPackets = (int) (file.length() / bufferSize);
        int lengthOfLastPacket = (int)(file.length() % bufferSize);
        
        seqnumLog= new PrintWriter(new FileWriter("./seqnum.log"));
        
        /* Read input file into an array of char[] of length 500 */
        for (int i = 0; i < numOfPackets; i++) {
            char[] buffer = new char [bufferSize];
            readFromFile.read(buffer, 0, bufferSize);
            bufferList.add(buffer);
        }
        if(lengthOfLastPacket != 0){
            char [] last = new char[(int)(lengthOfLastPacket)];
            readFromFile.read(last, 0, lengthOfLastPacket);
            bufferList.add(last);
        }

        /* start sending packets */
        Thread thread = new Thread(new runACK());
        thread.start();
        while(true){
            if(index >= bufferList.size()){
            	while(window.size() != 0){
                    // wait until every packets in the window are sent and ack
                }
                myPacket = packet.createEOT(index);
                sendData = myPacket.getUDPdata();
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, emulatorUDPPort);
                sendSocket.send(sendPacket);
                
                seqnumLog.println(myPacket.getSeqNum());
                
                break;
            }
            else{
                myPacket = packet.createPacket(index, new String(bufferList.get(index)));
                sendData = myPacket.getUDPdata();
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, emulatorUDPPort);
            }
            
            while(window.size() >= windowSize){
                /* do nothing, wait until there is windowLock space in window */
            }
            windowLock.acquire();
            window.put(index%32, myPacket);
            windowLock.release();
            sendSocket.send(sendPacket);
            
            seqnumLog.println(myPacket.getSeqNum());
            
            /* Create timer for current packets */
            Timer timer = new Timer(String.valueOf(index%32));
            resend(timer,index%32);
            timerLock.acquire();
            timerArray.put(index%32,timer);
            timerLock.release();
            
            index++;
        }
        
    }
    
    public static void resend(Timer t,final int seqnum) throws IOException{
    	t.scheduleAtFixedRate(new TimerTask() {
    		public void run() {
                packet resendPacket =  window.get(seqnum);
                sendPacket = new DatagramPacket(resendPacket.getUDPdata(), 
                		resendPacket.getUDPdata().length, IPAddress, emulatorUDPPort);
                try {
                	sendSocket.send(sendPacket);
                	} catch (IOException e) {
                		e.printStackTrace();
                		}
                }
    		}, 500,500);
    	}
    
    
    public static void recevieACK() throws Exception{
        byte[] recevieData = new byte[1024];
        Logger log1 = Logger.getLogger("debug");
        ackLog= new PrintWriter(new FileWriter("./ack.log"));
        
        while(true){
            DatagramPacket receivePacket = new DatagramPacket(recevieData, recevieData.length);
            receiveSocket.receive(receivePacket);
            packet receivePacketInFormat = packet.parseUDPdata(receivePacket.getData());
            int ack = receivePacketInFormat.getSeqNum();
            ackLog.println(ack);
            
            if(receivePacketInFormat.getType() == 2){
                /*EOT*/
                sendSocket.close();
                readFromFile.close();
                seqnumLog.close();
                ackLog.close();
                receiveSocket.close();
                break;
            }
            
            
            
            if(window.containsKey(ack) && timerArray.containsKey(ack)){
            	ArrayList<Integer> range1 = new ArrayList<Integer>();
            	
            	int e = expect%32;
            	for (int i = 0; i < 10; i++) {
            		int a = (e+i)%32;
            		if (a<0) a += 32;
					range1.add(a);
				}
                
                windowLock.acquire();
                window.put(ack, null);
                windowLock.release();
                
                timerLock.acquire();
                timerArray.get(ack).cancel();
                timerArray.remove(ack);
                timerLock.release();
                
                if(ack == expect%32){ 	
                	seqNumListForRemoving.add(ack);
                    
                	
                	while(!seqNumListForRemoving.isEmpty()){
                    	if(seqNumListForRemoving.contains(expect)){
                    		windowLock.acquire();
                			window.remove(expect);
                			windowLock.release();
                			seqNumListForRemoving.remove(seqNumListForRemoving.indexOf(expect));
                			expect++;
                			expect = expect%32;
                    	}
                    	else{
                    		break;
                    	}
                	}
                }
                else if(range1.contains(ack)){
                	seqNumListForRemoving.add(ack);
                }
                
            }
            else{
                // log1.INFO("seq num: " + ack + " ,ACK not found?!!");
            }
        }
    }
    
    public static class runACK implements Runnable{
        public void run(){
            try {
                recevieACK();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}

