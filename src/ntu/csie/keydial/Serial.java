package ntu.csie.keydial;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;

public class Serial {

	static InputStream connect(String portName) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			throw new Exception("Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(Serial.class.getName(), 2000);

			if (commPort instanceof SerialPort) {
				final SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

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
