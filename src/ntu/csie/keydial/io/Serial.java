package ntu.csie.keydial.io;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;

public class Serial {

	static final String COMPORT = "COMPORT";
	static final String DEFAULT_PORT = "/dev/cu.usbmodem1421"; // YOU MAY NEED TO CHANGE THIS

	public static String getDefaultPort() {
		return System.getenv().getOrDefault(COMPORT, DEFAULT_PORT);
	}

	public static InputStream open(String port) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
		if (portIdentifier.isCurrentlyOwned()) {
			throw new Exception("Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(Serial.class.getName(), 2000);

			if (commPort instanceof SerialPort) {
				final SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				// configure blocking IO
				serialPort.disableReceiveTimeout();
				serialPort.enableReceiveThreshold(1);

				return serialPort.getInputStream();
			} else {
				throw new Exception("Only serial ports are handled");
			}
		}
	}

	public static void main(String[] args) {
		java.util.Enumeration<?> ids = CommPortIdentifier.getPortIdentifiers();
		while (ids.hasMoreElements()) {
			System.out.println(((CommPortIdentifier) ids.nextElement()).getName());
		}
	}

}
