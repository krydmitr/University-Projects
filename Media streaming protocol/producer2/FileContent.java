// Student name: Dmitry Kryukov
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Class for packet content that represents file data
 *
 */
public class FileContent extends PacketContent {
	byte[] file;
	/**
	 * Constructor that takes in file data
	 * @param file bytes from a file
	 */
	FileContent(byte[] file) {
		type= FILESEND;
		this.file = file;
	}

	/**
	 * Constructs an object out of a datagram packet.
	 * @param packet Packet that contains file data.
	 */
	protected FileContent(ObjectInputStream oin) {
		ArrayList<Byte> bytelist = new ArrayList<Byte>();
		try {
			type= FILESEND;
			for(int i = 0; i < Node.PACKETSIZE; i++){
				bytelist.add(oin.readByte());
			}
		}
		catch(Exception e) {
			file = new byte[bytelist.size()+5];
			for (int i = 5; i < bytelist.size(); i++) {
				file[i] = bytelist.get(i);
			}
			// make the first byte the type of the file
			file[0] = FILESEND;
		}
	}

	/**
	 * Writes the content into an ObjectOutputStream
	 *
	 */
	protected void toObjectOutputStream(ObjectOutputStream oout) {
		try {
			for(int i = 0; i < file.length; i++){
				oout.writeByte(file[i]);
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}


	/**
	 * Returns the content of the packet as String.
	 *
	 * @return Returns the content of the packet as String.
	 */
	public String toString() {
		String returnString = "\nFile contents: ";
		for(int i = 0; i < file.length; i++){
			returnString += (char) file[i];
		}
		return returnString;
	}

	/**
	 * Returns the file data contained in the packet.
	 *
	 * @return Returns the file data contained in the packet.
	 */
	public byte[] getBytes() {
		return file;
	}
}