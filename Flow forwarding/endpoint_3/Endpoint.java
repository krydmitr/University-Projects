// Student name: Dmitry Kryukov
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// import java.nio.file.Files;
// import java.util.ArrayList;
// import javax.xml.crypto.Data;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Endpoint extends Node {
	static String myNumber = "3";
	static String routerName = "router_" + myNumber;
	static final InetSocketAddress SERVER_PORT = new InetSocketAddress(routerName, 50002);
	static final int DEFAULT_PORT = 50002;
	// ArrayList<Integer> streams = new ArrayList<Integer>();
	static InetAddress routerAddress = null;
	// array of integers with the size of number of streams(for example 2)
	static int[] streams = new int[2];
	File reqFile;
	byte[] fileContent;
	static final int HEADER_LENGTH = 1 + 4 + 4;
	static final int STREAMIDBYTE = HEADER_LENGTH - 1;
	static final int PRODUCERIDBYTE = HEADER_LENGTH - 4;
	
	static int rememberedFrame1 = 1;
	static int rememberedAudio1 = 1;

	static int rememberedFrame2 = 1;
	
	static boolean stream1live = false;
	static boolean stream2live = false;
	
	static String myID = "ABCD010" + myNumber;
	static String dstID = "";
	static boolean testEndpoint = true;
	static int IDlength = 4;
	
	// address of the endpoint_2 = 172.25.0.2
	// static final String FINAL_ADDRESS = "SOME TEST ADDRESS";

	Endpoint(int port) {
		try {
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public void onReceipt(DatagramPacket packet) {
		try {
			byte[] data = packet.getData();
			routerAddress = packet.getAddress();
			StringBuilder srcIDBuilder = new StringBuilder();
			for (int i = 1; i <= 4; i++) {
				srcIDBuilder.append(String.format("%02X", data[i]));
			}
			String srcID = srcIDBuilder.toString();
			StringBuilder destIDBuilder = new StringBuilder();
			for (int i = 5; i <= 8; i++) {
				destIDBuilder.append(String.format("%02X", data[i]));
			}
			String destID = destIDBuilder.toString();
			if (data[0] != PacketContent.FINISHSEND) {
				if (!destID.equals(myID)) {
					return;
				}
			}
			else {
				if (!srcID.equals(dstID)) {
					return;
				}
			}
			System.out.println("Received packet");
			System.out.println("First 5 bytes of the packet: " + packet.getData()[0] + " " + packet.getData()[1] + " "
					+ packet.getData()[2] + " "
					+ packet.getData()[3] + " " + packet.getData()[4]);
			switch (data[0]) {
				case PacketContent.PATHREQUEST:
					// ------------------------------------------------------------
					
					System.out.println("Destination ID: " + destID);
					// parse hexidecimal ID intro string
					System.out.println("My ID: " + myID);
					// check if the ID is correct
					if (destID.equals(myID)) {
						dstID = srcID;
						// send and ACK packet back to the router
						System.out.println("Received path request");

						byte[] ackData = new byte[HEADER_LENGTH];
						ackData[0] = PacketContent.ACKPACKET;
						System.arraycopy(data, 5, ackData, 1, IDlength);
						System.arraycopy(data, 1, ackData, 5, IDlength);
						DatagramPacket responsePacket = new DatagramPacket(ackData, ackData.length, routerAddress, DEFAULT_PORT);
						try {
							socket.send(responsePacket);
							System.out.println("Sending ACK packet");
							// start the stream
							System.out.println("Starting the stream");
							stream1live = true;
							streams[0] = 1;
							startStreaming(0);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("Wrong ID");
					}
					
					break;

				case PacketContent.ACKPACKET:
					System.out.println("Received ack"); 
					break;
				case PacketContent.FINISHSEND:
					// check if the ID in the packet is the same as the ID of the final destination
					if (srcID.equals(dstID)) {
						// stop the stream
						streams[0] = 0;
						stream1live = false;
						System.out.println("The receiving end is offline, stopping the stream");
					}
					break;
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}


	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		socket.setBroadcast(true);
		if (!dstID.equals("")) {
			DatagramPacket packet;

			System.out.println("requesting a path to: " + dstID);

			byte[] header1 = new byte[9];
			header1[0] = PacketContent.FILESEND;
			for (int i = 0; i < 4; i++) {
				header1[i + 1] = (byte) Integer.parseInt(myID.substring(i * 2, i * 2 + 2), 16);
			}
			for (int i = 0; i < 4; i++) {
				header1[i + 5] = (byte) Integer.parseInt(dstID.substring(i * 2, i * 2 + 2), 16);
			}
			byte[] data1 = new byte[header1.length];
			System.arraycopy(header1, 0, data1, 0, header1.length);
			packet = new DatagramPacket(data1, data1.length);
			packet.setSocketAddress(SERVER_PORT);
			socket.send(packet);

			// wait for 10 seconds
			TimeUnit.SECONDS.sleep(5);

			stream1live = true;
			streams[0] = 1;
			startStreaming(0);
		}
		this.wait();
	}
	
	public static void startStreaming(int a) {
		String baseFramePath = "./FrameSamples/FrameSamples";
		String baseSoundPath = "./SoundSamples/SoundSamples";
		if (a == 0) {
			Thread stream1Thread = new Thread(() -> {
				try {
					byte[] header1 = new byte[HEADER_LENGTH];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(myID.substring(i * 2, i * 2 + 2), 16);
					}
					for(int i = 0; i < 4; i++) {
						header1[i + 5] = (byte) Integer.parseInt(dstID.substring(i * 2, i * 2 + 2), 16);
					}
					int currentFrame1 = rememberedFrame1;
					while (true) {
						System.out.println("Stream 1");

						// Read and send data for the first stream
						String frameFileName = String.format("frame%03d.png", currentFrame1);
						String filePath = baseFramePath + "/" + frameFileName;
						File file = new File(filePath);
						// print out the file path and file name
						// System.out.println("File path: " + filePath);
						System.out.println("File name: " + frameFileName);
						if (file.exists()) {
							byte[] fileData = new byte[(int) file.length()];
							try (FileInputStream fileInputStream = new FileInputStream(file)) {
								fileInputStream.read(fileData);
							}
							DatagramPacket framepacket = null;

							// framepacket = new FileContent(fileData).toDatagramPacket();

							// byte[] address = FINAL_ADDRESS.getBytes();
							byte[] data1 = new byte[header1.length + fileData.length];
							System.arraycopy(header1, 0, data1, 0, header1.length);
							// translate FINAL_ADDRESS into byte array
							// include address of the endpoint_2 in the packet after the header
							// System.arraycopy(address, 0, data1, header1.length, address.length);
							System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
							if (routerAddress == null) {
								framepacket = new DatagramPacket(data1, data1.length, SERVER_PORT);
							}
							else {
								framepacket = new DatagramPacket(data1, data1.length, routerAddress, DEFAULT_PORT);
							}
							System.out.println("Sending frame number " + currentFrame1);
							currentFrame1++;
							socket.send(framepacket);
						}
						else {
							System.out.println("File does not exist");
							System.out.println("File path: " + filePath);
							System.out.println("File name: " + frameFileName);
						}

						if (streams[0] <= 0 || !stream1live || currentFrame1 == 10) {
							rememberedFrame1 = currentFrame1;
							stream1live = false;
							stopStreaming(0);
							break;
						}

						TimeUnit.SECONDS.sleep(1);
					}

				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
			stream1Thread.start();
		} else if (a == 1) {
			Thread stream2Thread = new Thread(() -> {
				try {
					byte[] header1 = new byte[HEADER_LENGTH];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(myID.substring(i * 2, i * 2 + 2), 16);
					}
					int currentFrame2 = rememberedFrame2;
					while (true) {
						System.out.println("Stream 2");

						// Read and send data for the first stream
						String frameFileName = String.format("file%03d.txt", currentFrame2);
						String filePath = baseSoundPath + "/" + frameFileName;
						File file = new File(filePath);
						// print out the file path and file name
						// System.out.println("File path: " + filePath);
						System.out.println("File name: " + frameFileName);
						if (file.exists()) {
							byte[] fileData = new byte[(int) file.length()];
							try (FileInputStream fileInputStream = new FileInputStream(file)) {
								fileInputStream.read(fileData);
							}
							DatagramPacket framepacket = null;

							byte[] data1 = new byte[header1.length + fileData.length];
							System.arraycopy(header1, 0, data1, 0, header1.length);
							System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
							framepacket = new DatagramPacket(data1, data1.length, routerAddress, DEFAULT_PORT);
							System.out.println("Sending file number " + currentFrame2);
							socket.send(framepacket);
						}

						currentFrame2++;
						// every 18 frames send a frame with the sound
						if (currentFrame2 % 18 == 0) {
							// call the function to send the audio file
							// sendAudio();
						}

						if (streams[1] <= 0) {
							rememberedFrame2 = currentFrame2;
							stream2live = false;
							break;
						}

						TimeUnit.SECONDS.sleep(5);
					}

					// socket1.close(); // You might want to add this in a more complex scenario
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
			stream2Thread.start();
		}
	}

	// function to send audio files
	public static void sendAudio() throws FileNotFoundException, IOException {
		System.out.println("Stream 1 audio");
		String baseSoundPath = "./SoundSamples/SoundSamples";
		byte[] header1 = new byte[HEADER_LENGTH];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(myID.substring(i * 2, i * 2 + 2), 16);
					}
		String audioFileName = String.format("file%03d.txt", rememberedAudio1);
		String filePath = baseSoundPath + "/" + audioFileName;
		File file = new File(filePath);
		// print out the file path and file name
		System.out.println("File name: " + audioFileName);
		if (file.exists()) {
			byte[] fileData = new byte[(int) file.length()];
			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				fileInputStream.read(fileData);
			}
			DatagramPacket framepacket = null;

			byte[] data1 = new byte[header1.length + fileData.length];
			System.arraycopy(header1, 0, data1, 0, header1.length);
			System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
			framepacket = new DatagramPacket(data1, data1.length, routerAddress, DEFAULT_PORT);
			System.out.println("Sending audio file number " + rememberedAudio1);
			socket.send(framepacket);
		}
	}
	
	public static void stopStreaming(int a) {
		if (a == 0) {
			stream1live = false;
			streams[0] = 0;
		} else if (a == 1) {
			stream2live = false;
			streams[1] = 0;
		}
		DatagramPacket packet;
		byte[] header = new byte[HEADER_LENGTH];
		header[0] = PacketContent.FINISHSEND;
		for (int i = 0; i < 4; i++) {
			header[i + 1] = (byte) Integer.parseInt(myID.substring(i * 2, i * 2 + 2), 16);
		}
		byte[] data = new byte[header.length];
		System.arraycopy(header, 0, data, 0, header.length);
		packet = new DatagramPacket(data, data.length, routerAddress, DEFAULT_PORT);
		try {
			socket.send(packet);
			System.out.println("Sending finish packet");
			// close the socket and stop to listen for packets
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *
	 */
	public static void main(String[] args) {
		try {
			int portNum = 50002;
			(new Endpoint(portNum)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}