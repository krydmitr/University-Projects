// Student name: Dmitry Kryukov

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Scanner;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * Client class
 *
 * An instance accepts user input
 *
 */
public class Consumer extends Node {
	static final int DEFAULT_SRC_PORT = 50004;
	static final int DEFAULT_DST_PORT = 50000;
	static final String DEFAULT_DST_NODE = "broker";
	File reqFile;
	byte[] fileData;
	int bytesRec = 0;
	InetSocketAddress dstAddress;
	String fname;

	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Consumer(String dstHost, int dstPort, int srcPort) {
		try {
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}


	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		
		DatagramPacket response;
		System.out.println("Recieved packet");
		// get the type of the packet
		byte[] data = packet.getData();
		switch (data[0]) {
			case PacketContent.FILEINFO:
				// System.out.println("It was a FILEINFO CONTENT");
				// fileData = new byte[((FileInfoContent) content).size];
				// response = new AckPacketContent("Acknowledge receive", 0).toDatagramPacket();
				// response.setSocketAddress(packet.getSocketAddress());
				// try {
				// 	socket.send(response);
				// 	System.out.println("Sending Acknowladge");
				// } catch (IOException e) {
				// 	e.printStackTrace();
				// }
				break;
			case PacketContent.FILESEND:
				// for(int i = 0; i < ((FileContent)content).file.length; i++){
				// 	fileData[i + bytesRec] = ((FileContent)content).file[i];
				// }
				// bytesRec += ((FileContent)content).file.length;
				// if(bytesRec == fileData.length){
				// 	try {
				// 		reqFile.createNewFile();
				// 		Files.write(reqFile.toPath(), fileData);
				// 	} catch (Exception e){
				// 		e.printStackTrace();
				// 		System.out.println("An exception occurred creating new file: " + e);
				// 	}
				// 	reqFile = null;
				// 	fileData = null;
				// 	fname = null;
				// 	bytesRec = 0;
				// 	this.notify();
				// }
				// response= new AckPacketContent("Acknowledge receive", ((FileContent)content).file.length).toDatagramPacket();
				// response.setSocketAddress(packet.getSocketAddress());
				// try {
				// 	socket.send(response);
				// 	System.out.println("Sending packet");
				// } catch (IOException e) {
				// 	e.printStackTrace();
				// }
				System.out.println("Received file");

				try {
					// print out the file data without FileContent

					// PacketContent content = PacketContent.fromDatagramPacket(packet);
					// ByteArrayInputStream bin;
					// ObjectInputStream oin;

					// data= packet.getData();  // use packet content as seed for stream
					// bin= new ByteArrayInputStream(data);
					// oin= new ObjectInputStream(bin);
					// PacketContent receivedContent = new FileContent(oin);
					// oin.close();
					// bin.close();
					// System.out.println(new String(((FileContent) receivedContent).file));
				}
				catch(Exception e) {e.printStackTrace();}
				// print out the file data
				break;
			case PacketContent.NOFILE:
				System.out.println("No file with that name could be found");
				reqFile = null;
				fileData = null;
				fname = null;
				bytesRec = 0;
				this.notify();
				break;
		}
	}


	/**
	 * Sender Method
	 *
	 */
	public synchronized void start() throws Exception {

		// handle the input from the user
		while (true) {
			System.out.println("Enter the stream ID you want to subscribe/unsubcribe to/from: ");
			Scanner input = new Scanner(System.in);
			String streamID = input.nextLine();
			// input.close();
			System.out.println(streamID);
			// encode the stream ID into the 2-5 bytes of the header
			byte[] streamIDBytes = streamID.getBytes();
			byte[] header = new byte[5];
			for (int i = 0; i < 4; i++) {
				header[i + 1] = (byte) Integer.parseInt(streamID.substring(i * 2, i * 2 + 2), 16);
			}

			// send the packet to the broker
			DatagramPacket packet = new DatagramPacket(header, header.length);
			packet.setSocketAddress(dstAddress);

			System.out.println("Subscribe or unsubscribe?");
			String subOrUnsub = input.nextLine();
			if (subOrUnsub.equals("subscribe")) {
				header[0] = PacketContent.SUBSCRIBE;
				packet.setData(header);
			} else if (subOrUnsub.equals("unsubscribe")) {
				header[0] = PacketContent.UNSUBSCRIBE;
				packet.setData(header);
			}
			if (header[0] == PacketContent.SUBSCRIBE) {
				System.out.println("Sending subscribe packet");
				System.out.println("stream ID: " + streamID);
			} else if (header[0] == PacketContent.UNSUBSCRIBE) {
				System.out.println("Sending unsubscribe packet");
				System.out.println("stream ID: " + streamID);
			}
			socket.send(packet);
			// this.wait();
		}






		// GetFileContent fcontent;
		// DatagramPacket packet= null;

		// fname= System.console().readLine("Name of file: ");
		// fname = "message.txt";
		// reqFile= new File(fname);				// Reserve buffer for length of file and read file

		// fcontent= new GetFileContent(fname);

		// System.out.println("Sending packet: " + fname); // Send packet with file name
		// packet= fcontent.toDatagramPacket();
		// packet.setSocketAddress(dstAddress);
		// socket.send(packet);
		// System.out.println("Sending packet");
	}


	/**
	 * Test method
	 *
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			(new Consumer(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}