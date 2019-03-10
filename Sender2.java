import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

class Sender2 {

    public static void main(String[] args) throws IOException {
        new SenderView().frmRdtSender.setVisible(true);
    }

    private static class SenderView {
        private JFrame frmRdtSender;
        private JButton btnSend;
        private JTextField txtAddr;
        private JTextField txtFile;
        private JLabel lblTransTime;
        private JSpinner spnPort;
        private JSpinner spnMyPort;
        private JSpinner spnMDS;
        private JSpinner spnTimeout;

        private SenderThread senderThread = null;

        private class ButtonListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent evt) {

                if (!txtAddr.getText().matches("[0-9\\.]+") && !txtAddr.getText().toUpperCase().equals("LOCALHOST")) {
                    JOptionPane.showMessageDialog(null,
                            "An IP address can only contain digits and '.'. Alternatively, write \"localhost\" for your own IP address",
                            "Invalid IP Address", JOptionPane.ERROR_MESSAGE);

                } else if (txtFile.getText() == "") {
                    JOptionPane.showMessageDialog(null, "A file must be specified.", "Missing File",
                            JOptionPane.ERROR_MESSAGE);

                } else {
                    try {
                        // Disable the UI.
                        setEnabledAll(false);

                        // Start the thread.
                        InetAddress addr = InetAddress.getByName(txtAddr.getText());
                        int port = (int) spnPort.getValue();
                        int myPort = (int) spnMyPort.getValue();
                        int mds = (int) spnMDS.getValue();
                        int timeout = (int) spnTimeout.getValue();
                        File file = new File(txtFile.getText());
                        senderThread = new SenderThread(addr, port, myPort, timeout, mds, file);
                        senderThread.addPropertyChangeListener(new AttributesListener());
                        senderThread.start();

                    } catch (UnknownHostException e) {
                        JOptionPane.showMessageDialog(null,
                                "The IP address specified cannot be resolved. Please check this address.",
                                "Bad Address", JOptionPane.ERROR_MESSAGE);
                        // Enable the UI.
                        setEnabledAll(true);

                    } catch (SocketException e) {
                        JOptionPane.showMessageDialog(null,
                                "Couldn't connect to the destination. Please check the IP address and port number, then check your internet connection.",
                                "Can't Connect", JOptionPane.ERROR_MESSAGE);
                        // Enable the UI.
                        setEnabledAll(true);

                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null,
                                "The file couldn't be read. Please check the path and make sure it exists.",
                                "Bad File Path", JOptionPane.ERROR_MESSAGE);
                        // Enable the UI.
                        setEnabledAll(true);

                    }
                }
            }
        }

        private class AttributesListener implements PropertyChangeListener {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {

                switch (evt.getPropertyName()) {
                case ("transTime"): {
                    JOptionPane.showMessageDialog(null, "The file has been sent.", "Transfer Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    // Reenable the UI.
                    setEnabledAll(true);
                    lblTransTime.setText(
                            String.format("Transfer time: %d seconds.", (int) evt.getNewValue() / (1000 * 1000)));
                    break;
                }
                case ("hasError"): {
                    JOptionPane.showMessageDialog(null, "The file transfer was unsuccessful", "Transfer Failed",
                            JOptionPane.ERROR_MESSAGE);
                    // Reenable the UI.
                    setEnabledAll(true);
                    lblTransTime.setText(String.format("Transfer failed!"));
                    break;
                }
                }
            }
        }

        public SenderView() {
            initialize();
            registerListeners();
        }

        private void initialize() {
            frmRdtSender = new JFrame();
            frmRdtSender.setTitle("RDT Sender");
            frmRdtSender.setBounds(100, 100, 471, 239);
            frmRdtSender.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frmRdtSender.getContentPane().setLayout(null);

            txtAddr = new JTextField("localhost");
            txtAddr.setHorizontalAlignment(SwingConstants.RIGHT);
            txtAddr.setBounds(10, 27, 191, 20);
            frmRdtSender.getContentPane().add(txtAddr);
            txtAddr.setColumns(10);

            spnPort = new JSpinner();
            spnPort.setModel(new SpinnerNumberModel(9897, 0, 65535, 1));
            spnPort.setBounds(213, 27, 111, 20);
            frmRdtSender.getContentPane().add(spnPort);

            spnMyPort = new JSpinner();
            spnMyPort.setModel(new SpinnerNumberModel(9898, 0, 65535, 1));
            spnMyPort.setBounds(334, 27, 111, 20);
            frmRdtSender.getContentPane().add(spnMyPort);

            txtFile = new JTextField();
            txtFile.setHorizontalAlignment(SwingConstants.RIGHT);
            txtFile.setColumns(10);
            txtFile.setBounds(10, 74, 435, 20);
            frmRdtSender.getContentPane().add(txtFile);

            spnMDS = new JSpinner();
            spnMDS.setModel(new SpinnerNumberModel(256, 3, 32767, 1));
            spnMDS.setBounds(259, 105, 186, 20);
            frmRdtSender.getContentPane().add(spnMDS);

            spnTimeout = new JSpinner();
            spnTimeout.setModel(new SpinnerNumberModel(new Integer(10000), new Integer(0), null, new Integer(1)));
            spnTimeout.setBounds(259, 136, 186, 20);
            frmRdtSender.getContentPane().add(spnTimeout);

            btnSend = new JButton("Send");
            btnSend.setBounds(10, 167, 89, 23);
            frmRdtSender.getContentPane().add(btnSend);

            lblTransTime = new JLabel();
            lblTransTime.setHorizontalAlignment(SwingConstants.LEFT);
            lblTransTime.setBounds(109, 167, 336, 20);
            frmRdtSender.getContentPane().add(lblTransTime);

            JLabel lblAddr = new JLabel("Receiver IP address:");
            lblAddr.setHorizontalAlignment(SwingConstants.LEFT);
            lblAddr.setBounds(10, 11, 186, 14);
            frmRdtSender.getContentPane().add(lblAddr);

            JLabel lblColon = new JLabel(":");
            lblColon.setHorizontalAlignment(SwingConstants.CENTER);
            lblColon.setBounds(201, 30, 13, 14);
            frmRdtSender.getContentPane().add(lblColon);

            JLabel lblPort = new JLabel("Port number:");
            lblPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblPort.setBounds(213, 11, 111, 14);
            frmRdtSender.getContentPane().add(lblPort);

            JLabel lblMyPort = new JLabel("My port number:");
            lblMyPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblMyPort.setBounds(334, 11, 111, 14);
            frmRdtSender.getContentPane().add(lblMyPort);

            JLabel lblFile = new JLabel("File to send:");
            lblFile.setHorizontalAlignment(SwingConstants.LEFT);
            lblFile.setBounds(10, 58, 435, 14);
            frmRdtSender.getContentPane().add(lblFile);

            JLabel lblMDS = new JLabel("Maximum datagram size (bytes):");
            lblMDS.setHorizontalAlignment(SwingConstants.LEFT);
            lblMDS.setBounds(10, 105, 244, 20);
            frmRdtSender.getContentPane().add(lblMDS);

            JLabel lblTimeout = new JLabel("Timeout (milliseconds):");
            lblTimeout.setHorizontalAlignment(SwingConstants.LEFT);
            lblTimeout.setBounds(10, 136, 244, 20);
            frmRdtSender.getContentPane().add(lblTimeout);

        }

        private void registerListeners() {
            btnSend.addActionListener(new ButtonListener());

        }

        private void setEnabledAll(Boolean status) {
            btnSend.setEnabled(status);
            txtAddr.setEnabled(status);
            txtFile.setEnabled(status);
            spnPort.setEnabled(status);
            spnMyPort.setEnabled(status);
            spnMDS.setEnabled(status);
            spnTimeout.setEnabled(status);

            if (!status) {
                lblTransTime.setText("");
            }
        }
    }

    private static class SenderThread extends Thread {

        public static class Header {
            private Boolean handshake;
            private Boolean fin;
            private Boolean ack;
            private int seq;

            public Header(boolean handshake, boolean fin, boolean ack, int seq) {
                this.handshake = handshake;
                this.fin = fin;
                this.ack = ack;
                this.seq = seq == 0 ? 0 : 1;
            }

            public Header(byte header) {
                this.handshake = ((header >> 7) & 1) == 1;
                this.fin = ((header >> 6) & 1) == 1;
                this.ack = ((header >> 5) & 1) == 1;
                this.seq = (header >> 4) & 1;
            }

            public Boolean isHandshake() {
                return this.handshake;
            }

            public Boolean isAck() {
                return this.ack;
            }

            public Boolean isFin() {
                return this.fin;
            }

            public int getSeq() {
                return this.seq;
            }

            public byte toByte() {
                char[] bitArr = { this.handshake ? '1' : '0', this.fin ? '1' : '0', this.ack ? '1' : '0',
                        this.seq != 0 ? '1' : '0', '0', '0', '0', '0' };
                return (byte) Integer.parseInt(new String(bitArr), 2);
            }

        }

        private static final int FIN_ATTEMPTS = 4; // Only try to send FIN 4 times before stopping.

        private PropertyChangeSupport pcs;
        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }
        
        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private InetAddress addr;
        private int port;
        private int mds;
        private int seq;
        private byte[] fileBytes;
        private long startTime;

        public SenderThread(InetAddress addr, int port, int myPort, int timeoutMs, int mds, File file)
                throws SocketException, IOException {

            // Get the starting time for transmission.
            this.startTime = System.nanoTime();

            // Get the bytes of the provided file.
            this.fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(this.fileBytes);
            fis.close();

            // Set the MDS.
            this.mds = mds;

            // Ready the output socket.
            this.addr = addr;
            this.port = port;
            this.outSocket = new DatagramSocket();
            this.outSocket.setSoTimeout(timeoutMs);

            // Ready the input socket.
            this.inSocket = new DatagramSocket(myPort);
            this.inSocket.setSoTimeout(timeoutMs);

            // Start seq at 0.
            this.seq = 0;
        }

        public void run() {
            try {
                handshake();
                sendFile();
                endConnection();
                inSocket.close();
                outSocket.close();
                pcs.firePropertyChange("transTime", 0, System.nanoTime() - this.startTime);
            } catch (Exception e) {
                System.out.println(e.toString());
                pcs.firePropertyChange("hasError", false, true);
            }
        }

        private void handshake() throws IOException {
            // Make first handshake packet, containing a body of the MDS.
            DatagramPacket sizePacket = makeDatagramPacket(new Header(true, false, false, seq),
                    ByteBuffer.allocate(2).putShort((short) mds).array());

            while (true) {
                try {
                    // Send first sequence of handshake.
                    outSocket.send(sizePacket);

                    // Receive response from first sequence of handshake.
                    DatagramPacket inPacket = receive();
                    Header inHeader = new Header(inPacket.getData()[0]);

                    // Check for ACK & handshake bit.
                    if (inHeader.isHandshake() && inHeader.isAck() && inHeader.getSeq() == seq)
                        break;

                } catch (SocketTimeoutException e) {
                    System.out.println(String.format("Handshake timeout!"));
                }
            }

            // Increment sequence number.
            seq = ++seq % 2;
        }

        private void sendFile() throws IOException {
            // Send each packet. byteIndex represents the first unACKd byte of the file.
            for (int byteIndex = 0; byteIndex < fileBytes.length; byteIndex += (mds - 1)) {

                // Form the DatagramPacket.
                byte[] data = new byte[Math.min(mds - 1, fileBytes.length - byteIndex)];
                System.arraycopy(fileBytes, byteIndex, data, 0, data.length);
                DatagramPacket dataPacket = makeDatagramPacket(new Header(false, false, false, seq), data);

                // Send same packet until appropriate ACK is received.
                Boolean acked = false;
                do {
                    try {
                        // Send packet.
                        outSocket.send(dataPacket);

                        // Receive responses until timeout OR until appropriate ACK received.
                        do {
                            // Receive response from first sequence of handshake.
                            DatagramPacket inPacket = receive();
                            Header inHeader = new Header(inPacket.getData()[0]);

                            // Check that the ACK received is for the sent packet.
                            acked = (inHeader.isAck() && inHeader.getSeq() == seq);

                        } while (!acked);

                    } catch (SocketTimeoutException e) {
                        System.out.println(String.format("Data transfer timeout!"));
                    }

                } while (!acked);

                // Increment sequence number.
                seq = ++seq % 2;
            }
        }

        private void endConnection() throws IOException {
            // Fin message.
            DatagramPacket finPacket = makeDatagramPacket(new Header(false, true, false, seq), new byte[0]);

            // Attempt to send FIN no more than FIN_ATTEMPTS times.
            int finAttempts = 0;
            while (finAttempts++ < FIN_ATTEMPTS) {
                try {
                    // Send packet.
                    outSocket.send(finPacket);

                    // Receive response from first sequence of end of connection procedure.
                    DatagramPacket inPacket = receive();
                    Header inHeader = new Header(inPacket.getData()[0]);

                    // Check for ACK.
                    if (inHeader.isAck() && inHeader.getSeq() == seq)
                        break;

                } catch (SocketTimeoutException e) {
                    System.out.println(String.format("Fin timeout!"));
                }
            }
        }

        private DatagramPacket makeDatagramPacket(Header header, byte[] data) {
            // add appropriate header to the front of the contents.
            byte[] contents = new byte[1 + data.length];
            byte[] headerByteArr = { header.toByte() };
            System.arraycopy(headerByteArr, 0, contents, 0, 1);
            System.arraycopy(data, 0, contents, 1, data.length);
            return new DatagramPacket(contents, contents.length, this.addr, this.port);
        }

        private DatagramPacket receive() throws IOException, SocketTimeoutException {
            byte[] inBuffer = new byte[mds];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            inSocket.receive(inPacket);
            return inPacket;
        }

    }

}