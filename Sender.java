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

    public static class FileSender {

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

            public Boolean getHandshakeBit() {
                return this.handshake;
            }

            public Boolean getAckBit() {
                return this.ack;
            }

            public Boolean getFinBit() {
                return this.fin;
            }

            public Boolean getSeq() {
                return this.seq;
            }

            public byte toByte() {
                char[] bitArr = {
                    this.handshake ? '1' : '0',
                    this.ack ? '1' : '0',
                    this.fin ? '1' : '0',
                    this.seq != 0 ? '1' : '0',
                    '0',
                    '0',
                    '0',
                    '0'
                };
                return (byte) Byte.parseByte(String.valueOf(bitArr), 2);
            }

        }

        public static void send(InetAddress addr, int port, int timeoutMicro, int mds, File file)
                throws SocketException, IOException {

            int seq = 0;
            DatagramSocket socket = new DatagramSocket(port, addr);
            socket.setSoTimeout((int) (timeoutMicro / 1000));

            handshake(socket);

            Iterable<DatagramPacket> datagrams = makeDatagrams(file, mds, seq);

            try {
                while (true) {
                    // TODO send datagrams, receive ACKs, timeouts.
                    try {

                    } catch (Exception te) {

                    }
                }
            } catch (Exception e) {

            } finally {
                socket.close();
            }

        }

        private static void handshake(DatagramSocket socket) {
            
        }
        
        private static Iterable<DatagramPacket> makeDatagrams(File file, int mds, int startSeq) throws IOException {
            List<DatagramPacket> datagramList = new ArrayList<DatagramPacket>();

            byte[] fileBytes = Files.readAllBytes(file.toPath());

            for (int i = 0; i < Math.ceil(fileBytes.length / (mds - 1)); i++) {
                // add a header to the front of the contents.
                byte[] contents = new byte[mds];
                byte[] header = makeHeader(0, 0, i % 2);
                System.arraycopy(header, 0, contents, 0, header.length);
                System.arraycopy(fileBytes, (mds - header.length) * i, contents, header.length, (mds - header.length));
                datagramList.add(new DatagramPacket(contents, contents.length));
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