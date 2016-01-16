
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;


public class TFTPClient {

	static DatagramSocket sock;
	static DatagramPacket send_packet;
	static DatagramPacket recieve_packet;
	static byte[] buffer = new byte[516];
	static String mode = "octet";
	static String server="";

	public TFTPClient(){
		try{
			sock = new DatagramSocket();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	static String readError(DatagramPacket p){
		byte[] send_buffer = p.getData();
		
		byte[] message = new byte[send_buffer.length-5];
		
		byte[] ercode =  new byte[2];
		System.arraycopy(send_buffer, 2, error, 0, 2);
		
		System.arraycopy(send_buffer, 4, message, 0, send_buffer.length-5);
		String s="Unknown";
		try {
			s = new String(ercode, "UTF-8")+new String(message, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		return s;
	}

	static int readOpcode(DatagramPacket p){
		byte[] send_buffer = p.getData();

		int i = (send_buffer[0] & 255) << 8 | (send_buffer[1] & 255);

		return i;
	}

	static int readBlock(DatagramPacket p){
		byte[] send_buffer = p.getData();

		int i = (send_buffer[2] & 255) << 8 | (send_buffer[3] & 255);

		return i;
	}
	static byte[] sendAck(int block){
		byte[] send_buffer = new byte[4];
		
		System.out.println("Sending Block#" + block);

		send_buffer[0] = 0;
		send_buffer[1] = 4;

		send_buffer[2] = (byte)(block/256);
		send_buffer[3] = (byte)(block%256);
		
		return send_buffer;
	}

	static byte[] sendRequest(String mode, String filename) 
	{

		byte[] send_buffer = new byte[filename.getBytes().length+mode.getBytes().length+4];

		send_buffer[0]=0;
		send_buffer[1]=1;

		//filename
		int temp = 2;
		for(int i=0;i<filename.getBytes().length;i++){
			send_buffer[temp]=filename.getBytes()[i];
			temp++;
		}

		send_buffer[temp]=0;

		//mode
		int temp1 = temp+1;
		for(int i=0;i<mode.getBytes().length;i++){
			send_buffer[temp1]=mode.getBytes()[i];
			temp1++;
		}
		
		System.out.println(Arrays.toString(send_buffer));
		return send_buffer;
	}  

	public static void sendPacket(byte[] buffer, int port)
	{
		try 
		{
			send_packet = new DatagramPacket(buffer,buffer.length,InetAddress.getByName(server),port);
			sock.send(send_packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		TFTPClient client = new TFTPClient();
		System.out.println("Client");

		Scanner sc = new Scanner(System.in);
		String message ="";

		URL location = TFTPClient.class.getProtectionDomain().getCodeSource().getLocation();
		System.out.println("Location :"+location.getFile());

		String filelocation = location.getFile();;

		int block = 0 ;
		int port;
		int opcode = 0;
		while(true){	
			message = sc.next();
			if(message.equals("get")){
				System.out.println("Usage: get host:file host:file ... file, or");
				System.out.println("\t\tget file file ... file   if connected, or");
				System.out.println("\t\tget host:rfile lfile");
			}
			
			if(message.equals("connect")){
				server = sc.next();
				System.out.println("Connected to "+server);

				message = sc.next();
				if(message.equals("get")){
					System.out.println("message :"+message);

					//inputs and reads file name
					String file = sc.next();
					System.out.println("filename :"+file);

					String path = filelocation+file.substring(file.lastIndexOf('/')+1);

					//read request and send it to server
					buffer = sendRequest(mode,file);
					sendPacket(buffer, 69);

					//returning to store file in home
					File f = new File(path);
					FileOutputStream fos = new FileOutputStream(f);

					byte[] recieved = new byte[516];
					
					while (true){
						//sock.setSoTimeout(5000);
						recieve_packet = new DatagramPacket(recieved, recieved.length);//,InetAddress.getByName(server),69);
						sock.receive(recieve_packet);
						port = recieve_packet.getPort();
						System.out.println("Sending to port #" + port);
						System.out.println("buffer length :"+recieved.length);
						

						opcode = readOpcode(recieve_packet);
						System.out.println("Opcode=" + opcode);
						if(opcode == 03){
							block = readBlock(recieve_packet);
							System.out.println("Received block #" + block);

							fos.write(recieve_packet.getData(),4,recieve_packet.getLength()-4);
							//System.out.println(Arrays.toString(recieve_packet.getData()));
							if(recieve_packet.getLength() < 516) {
								break;
							}
							buffer = sendAck(block);
							System.out.println(Arrays.toString(buffer));
							sendPacket(buffer, port);
						}
						else if(opcode == 05){
							String Error = readError(recieve_packet);
							//fos.write(recieve_packet.getData(),4,recieve_packet.getLength()-5);
							System.out.println("ERROR "+error);
							System.out.println(Arrays.toString(recieve_packet.getData()));
							break;
						}

					}
					System.out.println("File recieved and stored at: "+path);
				}
			}
		}

	}
}


