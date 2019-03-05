import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import javax.swing.SwingWorker;

class Sender {

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
                if (!txtAddr.getText().matches("[0-9\\.]+")) {
                    JOptionPane.showMessageDialog(null, "An IP address can only contain digits and '.'.",
                            "Invalid IP Address", JOptionPane.ERROR_MESSAGE);
                } else if (txtFile.getText() == "") {
                    JOptionPane.showMessageDialog(null, "A file must be specified.", "Missing File",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        InetAddress addr = InetAddress.getByName(txtAddr.getText());
                        int port = (int) spnPort.getValue();
                        int myPort = (int) spnMyPort.getValue();
                        int mds = (int) spnMDS.getValue();
                        int timeout = (int) spnTimeout.getValue();
                        File file = new File(txtFile.getText());

                        senderThread = new SenderThread(SenderView.this, addr, port, myPort, mds, timeout, file);
                    } catch (UnknownHostException e) {
                        JOptionPane.showMessageDialog(null,
                                "The IP address specified cannot be resolved. Please check this address.",
                                "Bad Address", JOptionPane.ERROR_MESSAGE);
                        return;
                    } catch (SocketException e) {
                        JOptionPane.showMessageDialog(null,
                                "Couldn't connect to the destination. Please check the IP address and port number.",
                                "Can't Connect", JOptionPane.ERROR_MESSAGE);
                        return;
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null,
                                "The file couldn't be read. Please check the path and make sure it exists.",
                                "Bad File Path", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    senderThread.execute();
                }
            }

        }

        /**
         * Create the application.
         */
        public SenderView() {
            initialize();
            registerListeners();
        }

        /**
         * Initialize the contents of the frame.
         */
        private void initialize() {
            frmRdtSender = new JFrame();
            frmRdtSender.setTitle("RDT Sender");
            frmRdtSender.setBounds(100, 100, 471, 239);
            frmRdtSender.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frmRdtSender.getContentPane().setLayout(null);

            txtAddr = new JTextField();
            txtAddr.setHorizontalAlignment(SwingConstants.RIGHT);
            txtAddr.setBounds(10, 27, 191, 20);
            frmRdtSender.getContentPane().add(txtAddr);
            txtAddr.setColumns(10);

            spnPort = new JSpinner();
            spnPort.setModel(new SpinnerNumberModel(0, 0, 65535, 1));
            spnPort.setBounds(213, 27, 111, 20);
            frmRdtSender.getContentPane().add(spnPort);

            spnMyPort = new JSpinner();
            spnMyPort.setModel(new SpinnerNumberModel(0, 0, 65535, 1));
            spnMyPort.setBounds(334, 27, 111, 20);
            frmRdtSender.getContentPane().add(spnMyPort);

            txtFile = new JTextField();
            txtFile.setHorizontalAlignment(SwingConstants.RIGHT);
            txtFile.setColumns(10);
            txtFile.setBounds(10, 74, 435, 20);
            frmRdtSender.getContentPane().add(txtFile);

            spnMDS = new JSpinner();
            spnMDS.setModel(new SpinnerNumberModel(256, 3, 65535, 1));
            spnMDS.setBounds(259, 105, 186, 20);
            frmRdtSender.getContentPane().add(spnMDS);

            spnTimeout = new JSpinner();
            spnTimeout.setModel(new SpinnerNumberModel(new Integer(10000), new Integer(0), null, new Integer(1)));
            spnTimeout.setBounds(259, 136, 186, 20);
            frmRdtSender.getContentPane().add(spnTimeout);

            btnSend = new JButton("Send");
            btnSend.setBounds(10, 167, 89, 23);
            frmRdtSender.getContentPane().add(btnSend);

            lblTransTime = new JLabel("Transmission time: 0 ms");
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

        private void threadComplete() {

        }
    }

    private static class SenderThread extends SwingWorker<Void, Void> {

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
                char[] bitArr = { this.handshake ? '1' : '0', this.ack ? '1' : '0', this.fin ? '1' : '0',
                        this.seq != 0 ? '1' : '0', '0', '0', '0', '0' };
                return (byte) Integer.parseInt(new String(bitArr), 2);
            }

        }

        private SenderView view;
        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private int mds;
        private int seq;

        private byte[] fileBytes;

        public SenderThread(SenderView view, InetAddress addr, int port, int myPort, int timeoutMs, int mds, File file)
                throws SocketException, IOException {

            // Get the bytes of the provided file.
            this.fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(this.fileBytes);
            fis.close();

            // Ready output socket.
            this.outSocket = new DatagramSocket(port, addr);
            this.outSocket.setSoTimeout(timeoutMs);

            // Ready input socket.
            this.inSocket = new DatagramSocket(myPort);
            this.inSocket.setSoTimeout(timeoutMs);

            // Set seq to 0.
            this.seq = 0;
        }

        public Void doInBackground() {
            try {
                handshake();
                sendFile();
                endConnection();
                this.inSocket.close();
                this.outSocket.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            return null;
        }

        public void done() {
            view.threadComplete();
        }

        private void handshake() throws IOException {
            Boolean acked = false;
            DatagramPacket packet = makeDatagramPacket(new Header(true, false, false, seq),
                    ByteBuffer.allocate(2).putShort((short) this.mds).array());
            do {
                try {
                    // Send first sequence of handshake.
                    this.outSocket.send(packet);
                    acked = false;

                    // Receive response from first sequence of handshake.
                    byte[] inBuffer = new byte[mds];
                    DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                    inSocket.receive(inPacket);

                    // Check for ACK & handshake bit.
                    Header inHeader = new Header(inBuffer[0]);
                    acked = (inHeader.isHandshake() && inHeader.isAck() && inHeader.getSeq() == this.seq);

                } catch (SocketTimeoutException e) {
                    System.out.println(e.toString());
                }
            } while (!acked);

            // Increment sequence number.
            this.seq++;
        }

        private void sendFile() throws IOException {
            // Send each packet. byteIndex represents the first unACKd byte of the file.
            for (int byteIndex = 0; byteIndex < this.fileBytes.length; byteIndex += (this.mds - 1)) {
                // Form the DatagramPacket.
                byte[] data = new byte[Math.min(this.mds - 1, this.fileBytes.length - byteIndex)];
                System.arraycopy(this.fileBytes, byteIndex, data, 0, data.length);
                DatagramPacket packet = makeDatagramPacket(new Header(false, false, false, seq), data);

                // Send same packet until appropriate ACK is received.
                Boolean acked = false;
                do {
                    try {
                        // Send packet.
                        this.outSocket.send(packet);
                        acked = false;

                        // Receive responses until timeout OR until appropriate ACK received.
                        do {
                            // Receive response from first sequence of handshake.
                            byte[] inBuffer = new byte[mds];
                            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                            inSocket.receive(inPacket);

                            // Check that the ACK received is for the sent packet.
                            Header inHeader = new Header(inBuffer[0]);
                            acked = (inHeader.isAck() && inHeader.getSeq() == this.seq);
                        } while (!acked);

                    } catch (SocketTimeoutException e) {
                        System.out.println(e.toString());
                    }
                } while (!acked);

                // Increment sequence number.
                this.seq++;
            }
        }

        private void endConnection() throws IOException {
            Boolean acked = false;
            // Send fin message and wait for appropriate ACK.
            DatagramPacket packet = makeDatagramPacket(new Header(false, true, false, seq), new byte[0]);
            do {
                try {
                    this.outSocket.send(packet);
                    acked = false;

                    // Receive response from first sequence of end of connection procedure.
                    byte[] inBuffer = new byte[mds];
                    DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                    inSocket.receive(inPacket);

                    // Check for ACK.
                    Header inHeader = new Header(inBuffer[0]);
                    acked = (inHeader.isAck() && inHeader.getSeq() == this.seq);

                } catch (SocketTimeoutException e) {
                    System.out.println(e.toString());
                }
            } while (!acked);

            // Increment sequence number.
            seq++;

            // Await FIN message from Receiver. Double timeout on inSocket so Receiver can
            // re-send FIN message if Sender's ACK gets lost.
            this.inSocket.setSoTimeout(2 * this.inSocket.getSoTimeout());
            try {
                while (true) {
                    // Ignore non-FIN messages.
                    Boolean hasFin = false;
                    do {
                        // Wait for notification that Receiver is FIN.
                        byte[] inBuffer = new byte[mds];
                        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                        inSocket.receive(inPacket);

                        // Check that message is FIN.
                        Header inHeader = new Header(inBuffer[0]);
                        hasFin = (inHeader.isFin() && inHeader.getSeq() == this.seq);
                    } while (!hasFin);

                    // Send ACK of Receiver's FIN.
                    this.outSocket.send(makeDatagramPacket(new Header(false, true, false, seq), new byte[0]));
                }
            } catch (SocketTimeoutException e) {
                // Timeout occurred after sending ACK of Receiver's FIN. Connection over.
            }
        }

        private DatagramPacket makeDatagramPacket(Header header, byte[] data) {
            // add appropriate header to the front of the contents.
            byte[] contents = new byte[1 + data.length];
            byte[] headerByteArr = { header.toByte() };
            System.arraycopy(headerByteArr, 0, contents, 0, 1);
            System.arraycopy(data, 0, contents, 1, data.length);
            return new DatagramPacket(contents, contents.length, this.outSocket.getLocalSocketAddress());
        }

        private byte[] extractData(DatagramPacket packet) {
            byte[] contents = packet.getData();
            byte[] data = new byte[contents.length - 1];
            System.arraycopy(contents, 1, data, 0, contents.length - 1);
            return data;
        }
    }
}