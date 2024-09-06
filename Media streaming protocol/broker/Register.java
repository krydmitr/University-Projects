// Student name: Dmitry Kryukov
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents file information
 *
 */
public class Register extends PacketContent {

	/**
	 * Constructor that takes in information about a file.
	 * @param filename Initial filename.
	 * @param size Size of filename.
	 */
	Register() {
	super.type = PacketContent.REGISTER;
	/*No Content to display, simply register this worker */
}

	protected void toObjectOutputStream(ObjectOutputStream out) {}

	public String toString() {
		return "Registering";
	}
}