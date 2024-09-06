// Student name: Dmitry Kryukov
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.xml.crypto.Data;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Producer extends Node {
	static final InetSocketAddress SERVER_PORT = new InetSocketAddress("broker", 50000);
	// ArrayList<Integer> streams = new ArrayList<Integer>();
	
	// array of integers with the size of number of streams(for example 2)
	static int[] streams = new int[2];
	File reqFile;
	byte[] fileContent;
	private final Object lock = new Object();
	static final int HEADER_LENGTH = 1 + 4;
	static final int STREAMIDBYTE = HEADER_LENGTH - 1;
	static final int PRODUCERIDBYTE = HEADER_LENGTH - 4;
	
	static int rememberedFrame1 = 1;
	static int rememberedAudio1 = 1;

	static int rememberedFrame2 = 1;
	
	static boolean stream1live = false;
	static boolean stream2live = false;
	
	static String message1 = "ABCD0101";
	static String message2 = "ABCD0102";
	/*
	 *	Create a new worker on the network with a certain port
	 */
	Producer(int port) {
		try {
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public void onReceipt(DatagramPacket packet) {
		try {
			
			// int type;
			// type = PacketContent.fromDatagramPacket(packet).getType();
			// System.out.println(type);
			
			// PacketContent content= PacketContent.fromDatagramPacket(packet);
			System.out.println("Received packet" );

			
			// DatagramPacket response;
			byte[] data = packet.getData();

			switch (data[0]) {
				case PacketContent.SUBSCRIBE: // change to SUBSCRIBE

					// increase the corresponding stream counter when received from the last of 4 bytes of the header from the packet
					System.out.println("Received subscribe");
					// System.out.println((PacketContent.getStreamID(packet)[3] & 0xFF));
					streams[(Integer) (PacketContent.getStreamID(packet)[3] & 0xFF) - 1]++;
					// synchronized (lock) {
					// 	lock.notifyAll();
					// }
					// start sending files if the stream counter is 1 or more


					// String fname = ((GetFileContent)content).filename;
                    // try {
					//     reqFile = new File(fname);
					//     fileContent = Files.readAllBytes(reqFile.toPath());
                    //     response = new FileInfoContent(fileContent.length).toDatagramPacket();
                    // } catch (Exception e) {
                    //     response = new NoFile().toDatagramPacket();
                    // }
					// response.setSocketAddress(packet.getSocketAddress());
					// socket.send(response);
					// System.out.println("Sending packet size");
					
					if (streams[0] == 1 && !stream1live) {
						stream1live = true;
						startStreaming(0);
					}
					if (streams[1] == 1 && !stream2live) {
						stream2live = true;
						startStreaming(1);
					}
					
					break;
				case PacketContent.UNSUBSCRIBE:
					System.out.println("Received unsubscribe");
					// decrease the corresponding stream counter when received from the last of 4 bytes of the header from the packet
					streams[(Integer) (PacketContent.getStreamID(packet)[3] & 0xFF) - 1]--;
					// synchronized (lock) {
					// 	lock.notifyAll();
					// }
					break;
				case PacketContent.ACKPACKET:
					// if (fileContent != null) {
					// 	byte[] newData = new byte[fileContent.length - ((AckPacketContent)content).size];
					// 	for(int i = 0; i < newData.length; i++){
					// 		newData[i] = fileContent[((AckPacketContent)content).size + i];
					// 	}
					// 	fileContent = newData;
					// 	if (fileContent.length > PACKETSIZE){
					// 		byte[] slice;
					// 		slice = new byte[PACKETSIZE];
					// 		for(int i = 0; i < PACKETSIZE; i++){
					// 			slice[i] = fileContent[i];
					// 		}
					// 		response = new FileContent(slice).toDatagramPacket();
							
					// 	} else{
					// 		response = new FileContent(fileContent).toDatagramPacket();
					// 		fileContent = null;
					// 	}
					// 	response.setSocketAddress(packet.getSocketAddress());
					// 	socket.send(response);
					// 	System.out.println("Sending packet");
					// }
					break;
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}


	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		DatagramPacket packet;
		// packet = new Register().toDatagramPacket();
		// put all the streams and their IDs in the packet
		// stream IDs are in the form of ABCDxxyy where the xx is the producers ID and the yy is the stream number/ID
		// for example ABCD0101 is the first stream of the first producer
		// ABCD0201 is the first stream of the second producer
		// ABCD0102 is the second stream of the first producer
		byte[] header = new byte[5];
		header[0] = PacketContent.REGISTER;
		byte numberOfStreams = 2;
		header[1] = numberOfStreams;
		// the rest bytes are all 0
		for (int i = 2; i < 5; i++) {
			header[i] = 0;
		}

		// Create a byte array to hold the header and the message
		byte[] data = new byte[5 + 8];

		// Copy the header to the beginning of the array
		System.arraycopy(header, 0, data, 0, 5);

		// Copy the encoded string to the array after the header
		for (int i = 0; i < 8; i++) {
			if (i < 4) {
				data[i + 5] = (byte) Integer.parseInt(message1.substring(i * 2, i * 2 + 2), 16);
			} else {
				data[i + 5] = (byte) Integer.parseInt(message2.substring((i - 4) * 2, (i - 4) * 2 + 2), 16);
			}
		}

		// Create a datagram packet, containing a message
		packet = new DatagramPacket(data, data.length, SERVER_PORT);

		System.out.println("Sending a registration packet with streams");
		packet.setSocketAddress(SERVER_PORT);
		socket.send(packet);
		
		System.out.println("Stream 1 ready");
		System.out.println("Stream 2 ready");
		this.wait();
	}
	
	public static void startStreaming(int a) {
		String baseFramePath = "./FrameSamples/FrameSamples";
		String baseSoundPath = "./SoundSamples/SoundSamples";
		if (a == 0) {
			Thread stream1Thread = new Thread(() -> {
				try {
					byte[] header1 = new byte[5];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(message1.substring(i * 2, i * 2 + 2), 16);
					}
					// print header1
					// for (int i = 0; i < 5; i++) {
					// 	System.out.println(header1[i]);
					// }
					int currentFrame1 = rememberedFrame1;
					while (true) {
						// synchronized (lock) {
						// 	while (streams[0] <= 0) {
						// 		try {
						// 			lock.wait();
						// 		} catch (InterruptedException e) {
						// 			e.printStackTrace();
						// 		}
						// 	}
						// }
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

							
							byte[] data1 = new byte[header1.length + fileData.length];
							System.arraycopy(header1, 0, data1, 0, header1.length);
							System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
							framepacket = new DatagramPacket(data1, data1.length);
							framepacket.setSocketAddress(SERVER_PORT);
							System.out.println("Sending frame number " + currentFrame1);
							socket.send(framepacket);
						}

						currentFrame1++;
						// every 18 frames send a frame with the sound
						if (currentFrame1 % 18 == 0) {
							// call the function to send the audio file
							sendAudio();
						}

						if (streams[0] <= 0) {
							rememberedFrame1 = currentFrame1;
							stream1live = false;
							break;
						}
						TimeUnit.SECONDS.sleep(1);
					}

					// socket1.close(); // You might want to add this in a more complex scenario
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
			stream1Thread.start();
		} else if (a == 1) {
			Thread stream2Thread = new Thread(() -> {
				try {
					byte[] header1 = new byte[5];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(message2.substring(i * 2, i * 2 + 2), 16);
					}
					int currentFrame2 = rememberedFrame2;
					while (true) {
						// synchronized (lock) {
						// 	while (streams[1] <= 0) {
						// 		try {
						// 			lock.wait();
						// 		} catch (InterruptedException e) {
						// 			e.printStackTrace();
						// 		}
						// 	}
						// }
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

							// framepacket = new FileContent(fileData).toDatagramPacket();

							// make the first byte of the header the type of the packet, which is FILESEND and convert the fileData into a packet without using toDatagram
							// byte[] header1 = new byte[5];
							// header1[0] = PacketContent.FILESEND;
							// the rest bytes are the file itself
							// for (int i = 1; i < 5; i++) {
							// 	header1[i] = fileData[i - 1];
							// }
							byte[] data1 = new byte[header1.length + fileData.length];
							System.arraycopy(header1, 0, data1, 0, header1.length);
							System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
							framepacket = new DatagramPacket(data1, data1.length);
							framepacket.setSocketAddress(SERVER_PORT);
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

						TimeUnit.SECONDS.sleep(10);
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
		byte[] header1 = new byte[5];
					header1[0] = PacketContent.FILESEND;
					for (int i = 0; i < 4; i++) {
						header1[i + 1] = (byte) Integer.parseInt(message2.substring(i * 2, i * 2 + 2), 16);
					}
					// Read and send data for the first stream
		// currently is hardcoded to send txt files for the sake of testing. Can be changed to send audio files by changin .txt to any other audio file format
		String audioFileName = String.format("file%03d.txt", rememberedAudio1);
		String filePath = baseSoundPath + "/" + audioFileName;
		File file = new File(filePath);
		// print out the file path and file name
		// System.out.println("File path: " + filePath);
		System.out.println("File name: " + audioFileName);
		if (file.exists()) {
			byte[] fileData = new byte[(int) file.length()];
			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				fileInputStream.read(fileData);
			}
			DatagramPacket framepacket = null;

			// framepacket = new FileContent(fileData).toDatagramPacket();

			// make the first byte of the header the type of the packet, which is FILESEND and convert the fileData into a packet without using toDatagram
			// byte[] header1 = new byte[5];
			// header1[0] = PacketContent.FILESEND;
			// the rest bytes are the file itself
			// for (int i = 1; i < 5; i++) {
			// 	header1[i] = fileData[i - 1];
			// }
			byte[] data1 = new byte[header1.length + fileData.length];
			System.arraycopy(header1, 0, data1, 0, header1.length);
			System.arraycopy(fileData, 0, data1, header1.length, fileData.length);
			framepacket = new DatagramPacket(data1, data1.length);
			framepacket.setSocketAddress(SERVER_PORT);
			System.out.println("Sending audio file number " + rememberedAudio1);
			socket.send(framepacket);
		}
	}
	
	
	
	/*
	 *
	 */
	public static void main(String[] args) {
		try {
            // System.out.println("Please enter the desired port number: ");
			// int portNum= Integer.parseInt(System.console().readLine());
			int portNum = 50001;
			(new Producer(portNum)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}