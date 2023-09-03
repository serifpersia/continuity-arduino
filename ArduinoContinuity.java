import java.awt.Color;
import java.awt.GridLayout;

import javax.sound.sampled.*;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class ArduinoContinuity {
    private static JFrame frame;
    private static JComboBox<String> portComboBox;
    private static JButton connectButton;
    private static JPanel colorPanel;
    private static boolean isTonePlaying = false;
    private static SerialPort serialPort;
    private static SourceDataLine sourceDataLine;

    public static void main(String[] args) {
        initializeGUI();
    }

    private static void initializeGUI() {
        frame = new JFrame("Arduino Continuity");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new GridLayout(3, 1));
        frame.setBackground(Color.BLACK);

        createPortSelectionPanel();
        createColorPanel();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void createPortSelectionPanel() {
        JPanel controlsPanel = new JPanel();
        JLabel portLabel = new JLabel("Select Serial Port: ");
        String[] availablePorts = SerialPortList.getPortNames();
        portComboBox = new JComboBox<>(availablePorts);
        controlsPanel.add(portLabel);
        controlsPanel.add(portComboBox);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> toggleConnection());
        controlsPanel.add(connectButton);

        frame.add(controlsPanel);
    }

    private static void createColorPanel() {
        colorPanel = new JPanel();
        colorPanel.setBackground(Color.BLACK);
        frame.add(colorPanel);
    }

    private static void toggleConnection() {
        if (!isTonePlaying) {
            String selectedPort = (String) portComboBox.getSelectedItem();
            connectToSerialPort(selectedPort);
            connectButton.setText("Disconnect");
        } else {
            disconnectFromSerialPort();
            connectButton.setText("Connect");
        }
    }

    private static void connectToSerialPort(String portName) {
        serialPort = new SerialPort(portName);

        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent event) {
                    handleSerialEvent(event);
                }
            });

            System.out.println("Listening for Arduino data. Press Ctrl+C to exit.");
        } catch (SerialPortException ex) {
            System.out.println("Error opening serial port: " + ex.getMessage());
        }
    }

    private static void disconnectFromSerialPort() {
        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.closePort();
                stopTone();
                colorPanel.setBackground(Color.BLACK);
            } catch (SerialPortException ex) {
                System.out.println("Error closing serial port: " + ex.getMessage());
            }
        }
    }

    private static void handleSerialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte[] buffer = serialPort.readBytes();
                String data = new String(buffer);

                if (data.contains("C")) {
                    if (!isTonePlaying) {
                        playTone(1000);
                        isTonePlaying = true;
                    }
                    System.out.println("Continuity true");
                    colorPanel.setBackground(Color.RED);
                } else if (data.contains("N")) {
                    stopTone();
                    System.out.println("Continuity false");
                    colorPanel.setBackground(Color.BLACK);
                }
            } catch (SerialPortException ex) {
                System.out.println("Error reading from serial port: " + ex.getMessage());
            }
        }
    }

	private static void playTone(int frequency) {
		Thread toneThread = new Thread(() -> {
			try {
				AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
				DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
				sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

				sourceDataLine.open(audioFormat);
				sourceDataLine.start();

				double phase = 0.0;
				double phaseIncrement = 2.0 * Math.PI * frequency / audioFormat.getSampleRate();
				byte[] buffer = new byte[2];
				int fadeDuration = 1000; // Duration of fade-in and fade-out in milliseconds

				// Apply fade-in
				for (int i = 0; i < fadeDuration; i++) {
					double volume = (double) i / fadeDuration;
					short sample = (short) (Short.MAX_VALUE * volume * Math.sin(phase));
					buffer[0] = (byte) (sample & 0xFF);
					buffer[1] = (byte) ((sample >> 8) & 0xFF);
					sourceDataLine.write(buffer, 0, 2);
					phase += phaseIncrement;
					if (phase >= 2.0 * Math.PI) {
						phase -= 2.0 * Math.PI;
					}
				}

				// Continue playing the tone
				while (isTonePlaying) {
					short sample = (short) (Short.MAX_VALUE * Math.sin(phase));
					buffer[0] = (byte) (sample & 0xFF);
					buffer[1] = (byte) ((sample >> 8) & 0xFF);
					sourceDataLine.write(buffer, 0, 2);
					phase += phaseIncrement;
					if (phase >= 2.0 * Math.PI) {
						phase -= 2.0 * Math.PI;
					}
				}

				// Apply fade-out
				for (int i = 0; i < fadeDuration; i++) {
					double volume = 1.0 - (double) i / fadeDuration;
					short sample = (short) (Short.MAX_VALUE * volume * Math.sin(phase));
					buffer[0] = (byte) (sample & 0xFF);
					buffer[1] = (byte) ((sample >> 8) & 0xFF);
					sourceDataLine.write(buffer, 0, 2);
					phase += phaseIncrement;
					if (phase >= 2.0 * Math.PI) {
						phase -= 2.0 * Math.PI;
					}
				}

				sourceDataLine.stop();
				sourceDataLine.close();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		});

		toneThread.start();
	}

	private static void stopTone() {
		isTonePlaying = false;
	}
}
