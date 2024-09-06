// Student name: Dmitry Kryukov
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;

public class Broker extends Node {
	static final int DEFAULT_PORT = 50000;
	static SocketAddress CONSUMER_PORT;
	static SocketAddress PRODUCER_PORT;
	static int PRODUCERS_WITHOUT_FILE = 0;
	
	static final int HEADER_LENGTH = 1 + 4;
	static final int STREAMIDBYTE = HEADER_LENGTH - 1;
	static final int PRODUCERIDBYTE = HEADER_LENGTH - 4;

	ArrayList<InetSocketAddress> producerAddresses = new ArrayList<InetSocketAddress>(); // addresses of the producers
	ArrayList<ArrayList<byte[]>> streamID = new ArrayList<ArrayList<byte[]>>(); // id of the streams in the byte format
	
	ArrayList<InetSocketAddress> consumers = new ArrayList<InetSocketAddress>(); // addresses of the consumers
	ArrayList<ArrayList<byte[]>> consumerSubs = new ArrayList<ArrayList<byte[]>>(); // addresses of the consumers
	
	/*
	 *
	 */

	Broker(int port) {
		try {
			socket = new DatagramSocket(port);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public void onReceipt(DatagramPacket packet) {
		try {
			// PacketContent content = PacketContent.fromDatagramPacket(packet);
			System.out.println("Received packet");
			// print out the packets first 5 bytes
			System.out.println("First 5 bytes of the packet: " + packet.getData()[0] + " " + packet.getData()[1] + " " + packet.getData()[2] + " "
					+ packet.getData()[3] + " " + packet.getData()[4]);
			System.out.println();


			byte[] data = packet.getData();
			switch (data[0]) {
				case PacketContent.SUBSCRIBE:
					PRODUCER_PORT = null;
					CONSUMER_PORT = packet.getSocketAddress();

					System.out.println("number of producers: " + producerAddresses.size());
					System.out.println("Number of streams: " + streamID.size());
					// change the PRODUCER_PORT to the address of the producer of the desired stream, found in the list of producers
					for (int i = 0; i < streamID.size(); i++) {
						// all the variables need to be equal
						System.out.println("Current i value: " + i);
						if(PacketContent.getProducerID(packet)[0] == streamID.get(i).get(0)[0] && 
						   PacketContent.getProducerID(packet)[1] == streamID.get(i).get(0)[1] && 
								PacketContent.getProducerID(packet)[2] == streamID.get(i).get(0)[2]) {
							// System.out.println("Producer found, index: " + i);
							PRODUCER_PORT = producerAddresses.get(i);
							break;
						}
					}
					// create a new consumer if it is not in the list of consumers or if the list is empty
					if (!consumers.contains(((InetSocketAddress) CONSUMER_PORT)) || consumers.isEmpty()) {
						System.out.println("New consumer");
						consumers.add((InetSocketAddress) CONSUMER_PORT);
						consumerSubs.add(new ArrayList<byte[]>());
					}
					// i is the index of the consumer in the list of consumers
					int i = 0;
					for(int j = 0; j < consumers.size(); j++){
						if(consumers.get(j) == (InetSocketAddress)CONSUMER_PORT){
							i = j;
							break;
						}
					}
					System.out.println("current index of the consumer: " + i);
					// System.out.println("current size of consumerSubs " + consumerSubs.size());;
					//if an already existing consumer wants to subscribe to only one stream
					if (data[STREAMIDBYTE] != 0) {
						consumerSubs.get(i).add(PacketContent.getStreamID(packet));
					}
					// if an already existing consumer wants to subscribe all the streams from the provided producer
					else if(data[STREAMIDBYTE] == 0){
						// find the corresponding producer in the list of producers
						int producer = 0;
						for(; producer < streamID.size(); producer++){
							// if the bytes 3-5 are the same as the bytes 1-3 of the streamID of the current producer
							if(PacketContent.getProducerID(packet)[0] == streamID.get(producer).get(0)[0] && 
							   PacketContent.getProducerID(packet)[1] == streamID.get(producer).get(0)[1] && 
							   PacketContent.getProducerID(packet)[2] == streamID.get(producer).get(0)[2]){
								break;
							}
						}
						for(int j = 0; j < streamID.get(producer).size(); j++){
							if(!consumerSubs.get(i).contains(streamID.get(producer).get(j))){
								consumerSubs.get(i).add(streamID.get(producer).get(j));
							}
						}
					}
				
					PRODUCERS_WITHOUT_FILE = 0;
					// change to the address of the producer of the current stream or the producer 

					packet.setSocketAddress(PRODUCER_PORT);
					socket.send(packet);
					System.out.println("Sending packet");
					break;
				case PacketContent.UNSUBSCRIBE:
					// remove the stream from the list of streams the consumer is subscribed to
					if((data[STREAMIDBYTE] & 0xFF) != 0){
						for(int a = 0; a < consumers.size(); a++){
							if(consumers.get(a) == (InetSocketAddress)CONSUMER_PORT){
								for(int b = 0; b < consumerSubs.get(a).size(); b++){
									if(Arrays.equals(consumerSubs.get(a).get(b), PacketContent.getStreamID(packet))){
										consumerSubs.get(a).remove(b);
										break;
									}
								}
							}
						}
					}
					// remove all the streams from the list of streams the consumer is subscribed to
					else if (data[STREAMIDBYTE] == 0) {
						for (int a = 0; a < consumers.size(); a++) {
							if (consumers.get(a) == (InetSocketAddress) CONSUMER_PORT) {
								for (int b = 0; b < consumerSubs.get(a).size(); b++) {
									// if first 3 bytes of the subscried stream are the same as the given producer
									if (consumerSubs.get(a).get(b)[0] == PacketContent.getProducerID(packet)[0] &&
											consumerSubs.get(a).get(b)[1] == PacketContent.getProducerID(packet)[1] &&
											consumerSubs.get(a).get(b)[2] == PacketContent.getProducerID(packet)[2]) {
										consumerSubs.get(a).remove(b);
										break;
									}
								}
								break;
							}
						}
					}
					for (int q = 0; q < streamID.size(); q++) {
						
						if(PacketContent.getProducerID(packet)[0] == streamID.get(q).get(0)[0] && 
						   PacketContent.getProducerID(packet)[1] == streamID.get(q).get(0)[1] && 
								PacketContent.getProducerID(packet)[2] == streamID.get(q).get(0)[2]) {
							System.out.println("Producer found: " + q);
							PRODUCER_PORT = producerAddresses.get(q);
							System.out.println("Sending unsubscribe packet");
							packet.setSocketAddress(PRODUCER_PORT);
							socket.send(packet);
							break;
						}
					}
					// send the packet to the producer
					break;
				case PacketContent.FILEINFO:
					if(PRODUCER_PORT == null){
						PRODUCER_PORT = packet.getSocketAddress();
						packet.setSocketAddress(CONSUMER_PORT);	// change to the every consumer who is subscribed
						socket.send(packet);
						System.out.println("Sending packet");
					}
					break;
				case PacketContent.ACKPACKET:
					packet.setSocketAddress(PRODUCER_PORT); // change to the address of the producer of the current stream or the producer
					socket.send(packet);
					System.out.println("Sending packet");
					break;
				case PacketContent.FILESEND:
					// Receiving the frames packet from the producer and forwarding it to the consumer
					System.out.println("Received the frames packet from the producer");
					// if the any consumer is subscribed to the received stream ID
					for (int a = 0; a < consumerSubs.size(); a++) {
						for (int b = 0; b < consumerSubs.get(a).size(); b++) {
							
							if (Arrays.equals(consumerSubs.get(a).get(b), PacketContent.getStreamID(packet))) {
								packet.setSocketAddress(consumers.get(a));
								socket.send(packet);
								// System.out.println("Sending the frames packet to the consumer number: " + (a + 1));
								break;
							}
						}
					}

					break;
				case PacketContent.REGISTER:	// implementation for different streams from the same producer
					producerAddresses.add(
							(InetSocketAddress) packet.getSocketAddress()
					);
					System.out.println("Current number of producers: " + producerAddresses.size());
					
					// translate the streamIDs to byte arrays and add them to the streamID array
					// works for any number of streams 
					int a = producerAddresses.size() - 1;
					// get the number of streams from the second byte of the packet
					int numberOfStreams = data[1];
					for (int j = 0; j < numberOfStreams; j++) {
						// System.out.println("Adding a stream");
						ArrayList<byte[]> temp = new ArrayList<byte[]>();
						// temp.add(Arrays.copyOfRange(data, 5 + j * 4, 5 + j * 4 + 4));
						streamID.add(temp);
						
						byte [] byteArr = new byte[4];
						byteArr[0] = data[j * 4 + 5];
						byteArr[1] = data[j * 4 + 6];
						byteArr[2] = data[j * 4 + 7];
						byteArr[3] = data[j * 4 + 8];
						streamID.get(a).add(byteArr);
					}

					System.out.println("Producer remembered");

					break;
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}


	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		this.wait();
	}

	/*
	 *
	 */
	public static void main(String[] args) {
		try {
			(new Broker(DEFAULT_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}