// Student name: Dmitry Kryukov
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.Data;

public class Router extends Node {
	static final int DEFAULT_PORT = 50002;
	static final int IDlength = 4;
	static int END_PORT = 50002;
	// static boolean FIRST_ROUTER = true;
	static InetAddress ignoredAddress;
	// a hashmap, where the key is the inetaddress and the value is inetAddress and the timing
	Map<String, InetAddressAndTime> addressMap = new HashMap<>();
	ArrayList<byte[]>  packetStorage = new ArrayList<>();


	
	Router(int port) throws SocketException {
		socket = new DatagramSocket(port);
		listener.go();
	}

	

	public void onReceipt(DatagramPacket packet) {
		try {
			InetAddress sourceAddress = ((InetSocketAddress) packet.getSocketAddress()).getAddress(); 
			// ignore the packet
			if(ignoreAddress(sourceAddress)) {
				ignoredAddress = null;
				return;
			}
			System.out.println("Received packet");
			System.out.println("Source address: " + sourceAddress);
			
			byte[] data = packet.getData();

			StringBuilder srcIDBuilder = new StringBuilder();
			StringBuilder dStringBuilder = new StringBuilder();
			for (int i = 1; i <= 4; i++) {
				srcIDBuilder.append(String.format("%02X", data[i]));
			}
			for (int i = 5; i <= 8; i++) {
				dStringBuilder.append(String.format("%02X", data[i]));
			}
			String srcID = srcIDBuilder.toString();
			String dstID = dStringBuilder.toString();
			
			// print out the packets first 5 bytes
			System.out.println("First 5 bytes of the packet: " + packet.getData()[0] + " " + packet.getData()[1] + " "
					+ packet.getData()[2] + " "
					+ packet.getData()[3] + " " + packet.getData()[4]);
					
			

			if (!addressMap.containsKey(srcID) && data[0] != PacketContent.FINISHSEND) {
				addressMap.put(srcID, new InetAddressAndTime(sourceAddress, System.currentTimeMillis()));
			}
			
			switch (data[0]) {
				case PacketContent.PATHREQUEST:
					// the required address after the header
					// get the required address from the packet after the header
					System.out.println("Required endpoint ID: " + dstID);
					if (addressMap.containsKey(dstID)) {
						byte[] response = new byte[HEADER_LENGTH];
						response[0] = PacketContent.ACKPACKET;
						System.arraycopy(data, 1, response, 5, IDlength);
						System.arraycopy(data, 5, response, 1, IDlength);
						DatagramPacket responsePacket = new DatagramPacket(response, response.length,
								addressMap.get(srcID).getAddress(),
								DEFAULT_PORT);
						socket.send(responsePacket);
						return;
					}
					
					broadcastPacket(data, sourceAddress, PacketContent.PATHREQUEST);
					break;
				case PacketContent.ACKPACKET:
					System.out.println("Received ACK packet, establishing the path");
					// remember the path
					System.out.println("ACK destination: " + dstID);
					if (addressMap.containsKey(dstID)) {
						System.out.println("Found the address in the map");
					}
					else{
						System.out.println("Address not found in the map");
					}
					byte[] ackData = new byte[HEADER_LENGTH];
					ackData[0] = PacketContent.ACKPACKET;
					System.arraycopy(data, 1, ackData, 1, IDlength);
					System.arraycopy(data, 5, ackData, 5, IDlength);
					DatagramPacket forwardPacket1 = new DatagramPacket(ackData, ackData.length, addressMap.get(dstID).getAddress(), DEFAULT_PORT);
					// --------------------------------------------------
					socket.send(forwardPacket1);
					break;
				case PacketContent.NOPATH:
					// if the path is not established, add some time to the timer until the program stops
					break;
				case PacketContent.FILESEND:
					// if the path is not established, ask every router for the path
					System.out.println("Received file");
					// say if the path is established or not
					if (!addressMap.containsKey(dstID)) {
						packetStorage.add(data);
						// store the packet until the path is established

						broadcastPacket(data, sourceAddress, PacketContent.PATHREQUEST);
					}
					// if the path is established, forward the packet to the next router in the path
					else {
						// if the storage is not empty, forward packets from the storage first
						if(packetStorage.size() > 0) {
							for(byte[] packetData : packetStorage) {
								DatagramPacket forwardPacket = new DatagramPacket(packetData, packetData.length, addressMap.get(dstID).getAddress(), END_PORT);
								socket.send(forwardPacket);
							}
							packetStorage.clear();
						}
						System.out.println("Forwarding the packet");
						
						System.out.println("Forwarding the packet to: " + addressMap.get(dstID).getAddress());
						DatagramPacket forwardPacket = new DatagramPacket(data, data.length, addressMap.get(dstID).getAddress(), END_PORT);
						socket.send(forwardPacket);
					}
					break;
				case PacketContent.FINISHSEND:
					// remove the address of the sender from the map
					System.out.println("Removing the address of given ID from the map: " + srcID);
					System.out.println("Map before removing the address: " + addressMap);
					addressMap.remove(srcID);
					// print the map
					System.out.println("Map after removing the address: " + addressMap);
					// ask every other router to remove that address from the map
					// broadcast the packet to all the routers on the network different from the one the packet came from
					broadcastPacket(data, sourceAddress, PacketContent.FINISHSEND);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void broadcastPacket(byte[] data, InetAddress sourceAddress, byte type) {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

			// reset the enumeration
			networkInterfaces = NetworkInterface.getNetworkInterfaces();


			// get the broadcast address from the source address
			ArrayList<InetAddress> badNetworks = new ArrayList<>();
			InetAddress badNetwork = null;
			while (networkInterfaces.hasMoreElements()) {
				
				NetworkInterface networkInterface = networkInterfaces.nextElement();

				// if the network is localhost, skip it
				if (networkInterface.isLoopback()) {
					badNetwork = networkInterface.getInterfaceAddresses().get(0).getBroadcast();;
					badNetworks.add(badNetwork);
					continue;
				}


				InetAddress mask = getSubnetMask(networkInterface);
                InetAddress broadcastAddress = networkInterface.getInterfaceAddresses().get(0).getBroadcast();
				InetAddress networkAddress = getBroadcastFromAddress(sourceAddress, mask);
				
				if (broadcastAddress.equals(networkAddress)) {
					badNetwork = broadcastAddress;
					badNetworks.add(badNetwork);
					continue;
				}
				
			}
			
			// reset the enumeration
			networkInterfaces = NetworkInterface.getNetworkInterfaces();


			while (networkInterfaces.hasMoreElements()) {
				// if the network interface is not the one the packet came from, send the packet to the network interface
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				// get the broadcast address from the source address

				// if the network interface has the same broadcast address as the source address, skip it
				if(badNetworks.contains(networkInterface.getInterfaceAddresses().get(0).getBroadcast())) {
					continue;
				}
				// reset the enumeration of the getInetAddresses
				System.out.println("Sending a broadcast packet");
				// get the address of the network interface by apllying the subnet mask to the first address
				InetAddress broadcastAddress = networkInterface.getInterfaceAddresses().get(0).getBroadcast();
				System.out.println("Broadcast address: " + broadcastAddress);
				if (broadcastAddress == null) {
					System.out.println("Broadcast address is not available for this network interface.");
					continue;
				}
				// send the packet
				byte[] broadcastData = new byte[HEADER_LENGTH];
				broadcastData[0] = type;
				System.arraycopy(data, 1, broadcastData, 1, HEADER_LENGTH-1);
				DatagramPacket broadcastPacket = new DatagramPacket(broadcastData, broadcastData.length, broadcastAddress, DEFAULT_PORT);
				ignoredAddress = ((InetSocketAddress) broadcastPacket.getSocketAddress()).getAddress();
				socket.send(broadcastPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// any cleanup code goes here
		}
	}
	

	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		socket.setBroadcast(true);
		this.wait();
	}

	// get the subnet mask from the network interface
	private InetAddress getSubnetMask(NetworkInterface networkInterface) throws UnknownHostException {
		// get the subnet mask from the network interface
		short prefixLength = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
		int subnetMask = 0xffffffff << (32 - prefixLength);
		byte[] subnetBytes = new byte[] { (byte) (subnetMask >>> 24), (byte) (subnetMask >> 16 & 0xff),
				(byte) (subnetMask >> 8 & 0xff), (byte) (subnetMask & 0xff) };
		InetAddress subnetMaskAddress = InetAddress.getByAddress(subnetBytes);
		return subnetMaskAddress;
	}

	// get the broadcast address from the source address
	private InetAddress getBroadcastFromAddress(InetAddress address, InetAddress subnetMask)
			throws UnknownHostException {
		byte[] addressBytes = address.getAddress();
		byte[] subnetMaskBytes = subnetMask.getAddress();
		byte[] broadcastBytes = new byte[addressBytes.length];

		for (int i = 0; i < addressBytes.length; i++) {
			broadcastBytes[i] = (byte) (addressBytes[i] | ~subnetMaskBytes[i]);
		}

		InetAddress broadcastAddress = InetAddress.getByAddress(broadcastBytes);
		return broadcastAddress;
	}

	private boolean ignoreAddress(InetAddress address) {
		// try {
		// 	Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		// 	while (networkInterfaces.hasMoreElements()) {
		// 		NetworkInterface networkInterface = networkInterfaces.nextElement();
		// 		InetAddress mask = getSubnetMask(networkInterface);
		// 		InetAddress addressBroadcast = getBroadcastFromAddress(address, mask);
		// 		if (addressBroadcast.equals(ignoredAddress)) {
		// 			return true;
		// 		}
		// 	}
		// 	return false;
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// }
		// return false;
	
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				// System.out.println("Interface: " + networkInterface.getName());

				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					if (inetAddress.equals(address)) {
						return true;
					}
					// System.out.println("  Address: " + inetAddress.getHostAddress());
				}

				// System.out.println("----------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}



	/*
	 *
	 */
	public static void main(String[] args) {
		try {
			// add the temporary socket to the list of sockets with the loopback address
			(new Router(DEFAULT_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}