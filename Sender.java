import java.io.File;
import java.io.IOException;

import java.nio.file.Files; // Read byte array from file.

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

class Sender {

    public static class SenderView {
        private JFrame frmRdtSender;
        private JTextField txtAddr;
        private JTextField txtFile;

        /**
         * Create the application.
         */
        public SenderView() {
            initialize();
        }

        /**
         * Initialize the contents of the frame.
         */
        private void initialize() {
            frmRdtSender = new JFrame();
            frmRdtSender.setTitle("RDT Sender");
            frmRdtSender.setBounds(100, 100, 384, 238);
            frmRdtSender.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frmRdtSender.getContentPane().setLayout(null);

            JLabel lblAddr = new JLabel("Receiver IP address:");
            lblAddr.setHorizontalAlignment(SwingConstants.LEFT);
            lblAddr.setBounds(10, 11, 152, 14);
            frmRdtSender.getContentPane().add(lblAddr);

            txtAddr = new JTextField();
            txtAddr.setHorizontalAlignment(SwingConstants.RIGHT);
            txtAddr.setBounds(10, 27, 152, 20);
            frmRdtSender.getContentPane().add(txtAddr);
            txtAddr.setColumns(10);

            JLabel lblColon = new JLabel(":");
            lblColon.setHorizontalAlignment(SwingConstants.CENTER);
            lblColon.setBounds(162, 30, 13, 14);
            frmRdtSender.getContentPane().add(lblColon);

            JLabel lblPort = new JLabel("Port number:");
            lblPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblPort.setBounds(173, 11, 81, 14);
            frmRdtSender.getContentPane().add(lblPort);

            JLabel lblMyPort = new JLabel("My port number:");
            lblMyPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblMyPort.setBounds(270, 11, 88, 14);
            frmRdtSender.getContentPane().add(lblMyPort);

            JLabel lblFile = new JLabel("File to send:");
            lblFile.setHorizontalAlignment(SwingConstants.LEFT);
            lblFile.setBounds(10, 58, 138, 14);
            frmRdtSender.getContentPane().add(lblFile);

            txtFile = new JTextField();
            txtFile.setHorizontalAlignment(SwingConstants.RIGHT);
            txtFile.setColumns(10);
            txtFile.setBounds(10, 74, 348, 20);
            frmRdtSender.getContentPane().add(txtFile);

            JLabel lblMDS = new JLabel("Maximum datagram size (bytes):");
            lblMDS.setHorizontalAlignment(SwingConstants.LEFT);
            lblMDS.setBounds(10, 105, 162, 20);
            frmRdtSender.getContentPane().add(lblMDS);

            JLabel lblTimeout = new JLabel("Timeout (milliseconds):");
            lblTimeout.setHorizontalAlignment(SwingConstants.LEFT);
            lblTimeout.setBounds(10, 136, 162, 20);
            frmRdtSender.getContentPane().add(lblTimeout);

            JButton btnSend = new JButton("Send");
            btnSend.setBounds(10, 165, 89, 23);
            frmRdtSender.getContentPane().add(btnSend);

            JLabel lblTransTime = new JLabel("");
            lblTransTime.setHorizontalAlignment(SwingConstants.LEFT);
            lblTransTime.setBounds(108, 167, 250, 20);
            frmRdtSender.getContentPane().add(lblTransTime);

            JSpinner spnMDS = new JSpinner();
            spnMDS.setModel(new SpinnerNumberModel(new Integer(256), new Integer(1), null, new Integer(1)));
            spnMDS.setBounds(172, 105, 186, 20);
            frmRdtSender.getContentPane().add(spnMDS);

            JSpinner spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(new Integer(10000), new Integer(0), null, new Integer(1)));
            spinner.setBounds(172, 136, 186, 20);
            frmRdtSender.getContentPane().add(spinner);

            JSpinner spnPort = new JSpinner();
            spnPort.setBounds(172, 27, 88, 20);
            frmRdtSender.getContentPane().add(spnPort);

            JSpinner spnMyPort = new JSpinner();
            spnMyPort.setBounds(270, 27, 88, 20);
            frmRdtSender.getContentPane().add(spnMyPort);
        }
    }

    public static class FileSender extends Thread {

        public static class Header {
            private Boolean handshake;
            private Boolean ack;
            private Boolean fin;
            private int seq;

            public Header(boolean handshake, boolean ack, boolean fin, int seq) {
                this.handshake = handshake;
                this.ack = ack;
                this.fin = fin;
                this.seq = seq == 0 ? 0 : 1;
            }

            public Header(byte header) {
                this.handshake = ((header >> 7) & 1) == 1;
                this.ack = ((header >> 6) & 1) == 1;
                this.fin = ((header >> 6) & 1) == 1;
                this.seq = (header >> 6) & 1;
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
                return (byte) Byte.parseByte(String.valueOf(bitArr), 2);
            }

        }

        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private int mds;
        private int seq;
        private Iterable<DatagramPacket> fileDatagramPackets;

        public FileSender(InetAddress addr, int port, int myPort, int timeoutMs, int mds, File file)
                throws SocketException, IOException {
            // Format contents of file into a set of DatagramPackets with appropriate
            // headers.
            this.fileDatagramPackets = makeFileDatagramPackets(file, mds, addr, port);

            // Ready output socket.
            this.outSocket = new DatagramSocket(port, addr);
            this.outSocket.setSoTimeout(timeoutMs);

            // Ready input socket.
            this.inSocket = new DatagramSocket(myPort);
            this.inSocket.setSoTimeout(timeoutMs);

            // Set seq to 0.
            this.seq = 0;

        }

        public void run() {
            try {
                // Do handshake.
                handshake();

                int packetIndex = 0;

                // Start sending packets.
                while (true) {
                    try {
                        // Send packet.
                        this.outSocket.send(this.fileDatagramPackets[fileDatagramPackets]);

                        // Receive response from first sequence of handshake.
                        byte[] inBuffer = new byte[mds];
                        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                        inSocket.receive(inPacket);

                        Header resHeader = new Header(inBuffer[0]);
                        if (resHeader.isAck() && resHeader.getSeq() == this.seq) {
                            seq++;
                        }  

                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout!");
                        continue;
                    }
                }

                this.inSocket.close();
                this.outSocket.close();

            } catch (Exception e) {
                System.out.println(e.toString());
            }
            return;
        }

        private void handshake() throws IOException {
            while (true) {
                try {
                    // Send first sequence of handshake.
                    this.outSocket.send(makeOutDatagramPacket(new Header(true, false, false, seq), new byte[0]));

                    // Receive response from first sequence of handshake.
                    byte[] inBuffer = new byte[mds];
                    DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                    inSocket.receive(inPacket);

                    // Check for ACK & handshake bit.
                    Header resHeader = new Header(inBuffer[0]);
                    if (resHeader.isHandshake() && resHeader.isAck() && resHeader.getSeq() == this.seq) {
                        this.seq++;
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    // Try again (resend).
                    System.out.println("Timeout!");
                }
            }
        }

        public void send(InetAddress addr, int port, int myPort, int timeoutMs, int mds, File file)
                throws SocketException, IOException {

        }

        private DatagramPacket makeOutDatagramPacket(Header header, byte[] data) {
            // add appropriate header to the front of the contents.
            byte[] contents = new byte[1 + data.length];
            byte[] headerByteArr = { header.toByte() };
            System.arraycopy(headerByteArr, 0, contents, 0, 1);
            System.arraycopy(data, 0, contents, 1, data.length);
            return new DatagramPacket(contents, contents.length, this.outSocket.getInetAddress(),
                    this.outSocket.getPort());
        }

        private Iterable<DatagramPacket> makeFileDatagramPackets(File file, int mds, InetAddress addr, int port)
                throws IOException {
            List<DatagramPacket> datagramList = new ArrayList<DatagramPacket>();
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            for (int i = 0; i < fileBytes.length; i += (mds - 1)) {
                byte[] data = new byte[mds - 1];
                System.arraycopy(fileBytes, i, data, 0, Math.min(mds - 1, fileBytes.length - i));
                datagramList.add(makeOutDatagramPacket(new Header(false, false, false, i % 2), data));
            }

            return datagramList;
        }

    }

    public static void main(String[] args) throws IOException {
        File file = new File("C:/Users/Nathaniel/Desktop/CP367 MT.txt");
        Iterable<DatagramPacket> datagrams = RDTFileTransfer.makeDatagrams(file, 5, 0);

        List<Byte> bytes = new ArrayList<Byte>();
        for (DatagramPacket packet : datagrams) {
            byte[] pBytes = packet.getData();
            for (int i = 1; i < pBytes.length; i++) {
                bytes.add(pBytes[i]);
            }
        }

        byte[] byteArr = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            byteArr[i] = bytes.get(i);
        }

        System.out.println(RDTFileTransfer.byteArrToString(byteArr));
    }

}