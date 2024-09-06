// Student name: Dmitry Kryukov
import java.net.DatagramPacket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * The class is the basis for packet contents of various types.
 *
 *
 */
public abstract class PacketContent {

	// need to change from int to byte
	public static final byte ACKPACKET=    1;
	public static final byte NOPATH=       2;
	public static final byte FILESEND=     3;
	public static final byte FINISHSEND =  4;
	public static final byte PATHREQUEST = 8;

	int type= 0;
	/**
	 * Constructs an object out of a datagram packet.
	 * @param packet Packet to analyse.
	 */
	public static PacketContent fromDatagramPacket(DatagramPacket packet) {
		PacketContent content= null;

		try {
			int type;

			byte[] data;
			ByteArrayInputStream bin;
			ObjectInputStream oin;

			data= packet.getData();  // use packet content as seed for stream
			bin= new ByteArrayInputStream(data);
			oin= new ObjectInputStream(bin);

			type= oin.readInt();  // read type from beginning of packet

			switch(data[0]) {   // depending on type create content object
			case ACKPACKET:
				content= new AckPacketContent(oin);
				break;
			case FILESEND:
				content = new FileContent(oin);
				break;
			default:
				content= null;
				break;
			}
			oin.close();
			bin.close();

		}
		catch(Exception e) {e.printStackTrace();}

		return content;
	}


	/**
	 * This method is used to transform content into an output stream.
	 *
	 * @param out Stream to write the content for the packet to.
	 */
	protected abstract void toObjectOutputStream(ObjectOutputStream out);

	/**
	 * Returns the content of the object as DatagramPacket.
	 *
	 * @return Returns the content of the object as DatagramPacket.
	 */
	public DatagramPacket toDatagramPacket() {
		DatagramPacket packet= null;

		try {
			ByteArrayOutputStream bout;
			ObjectOutputStream oout;
			byte[] data;

			bout= new ByteArrayOutputStream();
			oout= new ObjectOutputStream(bout);

			oout.writeInt(type);         // write type to stream
			toObjectOutputStream(oout);  // write content to stream depending on type
			oout.flush();
			data= bout.toByteArray(); // convert content to byte array

			packet= new DatagramPacket(data, data.length); // create packet from byte array
			oout.close();
			bout.close();
		}
		catch(Exception e) {e.printStackTrace();}

		return packet;
	}


	/**
	 * Returns the content of the packet as String.
	 *
	 * @return Returns the content of the packet as String.
	 */
	public abstract String toString();

	/**
	 * Returns the type of the packet.
	 *
	 * @return Returns the type of the packet.
	 */


	// public byte[] getData() {
	// 	return data;
	// }

	public int getType() {
		// return the type of the packet from the first 2 bytes of the header
		return type;
	}

	public static byte[] getProducerID(DatagramPacket packet) {
		byte[] data;
		data = packet.getData();
		byte[] producerID = new byte[3]; // initialize the producerID array
		producerID[0] = data[1];
		producerID[1] = data[2];
		producerID[2] = data[3];
		return producerID;
	}

	public static byte[] getStreamID(DatagramPacket packet) {
		byte[] data;
		data = packet.getData();
		byte[] streamID = new byte[4]; // initialize the streamID array
		streamID[0] = data[1];
		streamID[1] = data[2];
		streamID[2] = data[3];
		streamID[3] = data[4];
		return streamID;
	} 

}