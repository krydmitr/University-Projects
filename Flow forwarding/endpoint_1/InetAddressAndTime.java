// Student name: Dmitry Kryukov
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class InetAddressAndTime {
    private InetAddress address;
    private long timestamp;

    public InetAddressAndTime(InetAddress address, long timestamp) {
        this.address = address;
        this.timestamp = timestamp;
    }

    public InetAddress getAddress() {
        return address;
    }

    public long getTimestamp() {
        return timestamp;
    }
}