import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

class Receiver2 {

    private static class ReceiverView {

        private JFrame frmRdtReceiver;
        private JTextField txtAddr;
        private JSpinner spnPort;
        private JSpinner spnMyPort;
        private JTextField txtFile;
        private JCheckBox chkUnreliable;
        private JButton btnReceive;
        private JLabel lblReceived;

        public ReceiverView() {
            initialize();
            registerListeners();
        }

        /**
         * Initialize the contents of the frame.
         */
        private void initialize() {
            frmRdtReceiver = new JFrame();
            frmRdtReceiver.setTitle("RDT Receiver");
            frmRdtReceiver.setBounds(100, 100, 474, 201);
            frmRdtReceiver.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frmRdtReceiver.getContentPane().setLayout(null);

            txtAddr = new JTextField();
            txtAddr.setHorizontalAlignment(SwingConstants.RIGHT);
            txtAddr.setColumns(10);
            txtAddr.setBounds(10, 27, 191, 20);
            frmRdtReceiver.getContentPane().add(txtAddr);

            spnPort = new JSpinner();
            spnPort.setBounds(213, 27, 111, 20);
            frmRdtReceiver.getContentPane().add(spnPort);

            spnMyPort = new JSpinner();
            spnMyPort.setBounds(334, 27, 111, 20);
            frmRdtReceiver.getContentPane().add(spnMyPort);

            txtFile = new JTextField();
            txtFile.setHorizontalAlignment(SwingConstants.RIGHT);
            txtFile.setColumns(10);
            txtFile.setBounds(10, 74, 435, 20);
            frmRdtReceiver.getContentPane().add(txtFile);

            chkUnreliable = new JCheckBox("Unreliable");
            chkUnreliable.setBounds(10, 105, 435, 23);
            frmRdtReceiver.getContentPane().add(chkUnreliable);

            btnReceive = new JButton("Receive");
            btnReceive.setBounds(10, 132, 89, 23);
            frmRdtReceiver.getContentPane().add(btnReceive);

            lblReceived = new JLabel("Received in-order packets: 0");
            lblReceived.setHorizontalAlignment(SwingConstants.LEFT);
            lblReceived.setBounds(109, 133, 336, 20);
            frmRdtReceiver.getContentPane().add(lblReceived);

            JLabel lblFile = new JLabel("File to write to:");
            lblFile.setHorizontalAlignment(SwingConstants.LEFT);
            lblFile.setBounds(10, 58, 435, 14);
            frmRdtReceiver.getContentPane().add(lblFile);

            JLabel lblAddr = new JLabel("Receiver IP address:");
            lblAddr.setHorizontalAlignment(SwingConstants.LEFT);
            lblAddr.setBounds(10, 11, 186, 14);
            frmRdtReceiver.getContentPane().add(lblAddr);

            JLabel lblPort = new JLabel("Port number:");
            lblPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblPort.setBounds(213, 11, 111, 14);
            frmRdtReceiver.getContentPane().add(lblPort);

            JLabel lblColon = new JLabel(":");
            lblColon.setHorizontalAlignment(SwingConstants.CENTER);
            lblColon.setBounds(201, 30, 13, 14);
            frmRdtReceiver.getContentPane().add(lblColon);

            JLabel lblMyPort = new JLabel("My port number:");
            lblMyPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblMyPort.setBounds(334, 11, 111, 14);
            frmRdtReceiver.getContentPane().add(lblMyPort);
        }

        private void registerListeners() {

        }

    }

    private static class ReceiverWorker extends Thread {

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

        static final int DROP_AT = 9;

        private File outFile;
        private ReceiverView view;
        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private int mds;
        private Boolean reliable;
        private int dropCounter;
        private int seq;

        public ReceiverWorker(ReceiverView view, InetAddress addr, int port, int myPort, Boolean reliable, File outFile)
                throws SocketException, IOException {

            this.outFile = outFile;

            // Ready output socket.
            this.outSocket = new DatagramSocket(port, addr);

            // Ready input socket.
            this.inSocket = new DatagramSocket(myPort);

            // Set initial handshake MDS.
            this.mds = 3;

            // Set seq to 0.
            this.seq = 0;

            this.reliable = reliable;
            this.dropCounter = 0;

        }

        public void run() {
            try {
                byte[] fileBytes = receiveFile();
                outFile.getParentFile().mkdirs();
                outFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(outFile, false);
                fos.write(fileBytes);
                fos.close();
                this.inSocket.close();
                this.outSocket.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }

        private byte[] receiveFile() throws IOException {

            List<Byte> fileBytes = new ArrayList<Byte>();

            byte[] inBuffer = new byte[3];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            Header inHeader;

            // Do handshake until handshake complete.
            do {
                // Get packet.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inBuffer[0]);
                dropCounter = (dropCounter + 1) % DROP_AT;

                // Send ACK and set MDS appropriately.
                if (inHeader.isHandshake() && (dropCounter != DROP_AT || reliable)) {
                    mds = (int) ByteBuffer.wrap(extractData(inPacket)).getShort();
                    outSocket.send(makeDatagramPacket(new Header(true, false, true, inHeader.getSeq()), new byte[0]));
                }

            } while (inHeader.isHandshake());

            // Set seq to whatever the first data packet had.
            seq = inHeader.getSeq();

            // Start accepting data packets until FIN received.
            do {
                // Process packet contents if the packet received is appropriate.
                if (inHeader.getSeq() == seq) {
                    for (byte pByte : extractData(inPacket))
                        fileBytes.add(pByte);
                    seq++;
                }

                // Send ACK for last packet received.
                if (dropCounter != DROP_AT || reliable) {
                    outSocket.send(makeDatagramPacket(new Header(false, false, true, inHeader.getSeq()), new byte[0]));
                }

                // Get next packet.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inBuffer[0]);
                dropCounter = (dropCounter + 1) % DROP_AT;

            } while (!(inHeader.isFin() && inHeader.getSeq() == seq));

            // FIN actions.
            Boolean finAcked = false;
            do {
                // Send ACK of FIN + own FIN.
                if (dropCounter != DROP_AT || reliable) {
                    outSocket.send(makeDatagramPacket(new Header(false, true, true, inHeader.getSeq()), new byte[0]));
                }

                // Receive ACK of FIN.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inBuffer[0]);
                finAcked = (inHeader.isAck() && inHeader.getSeq() == seq);
                dropCounter = (dropCounter + 1) % DROP_AT;

            } while (!finAcked);

            byte[] fileBytesArr = new byte[fileBytes.size()];
            for (int i = 0; i < fileBytes.size(); i++) {
                fileBytesArr[i] = fileBytes.get(i);
            }

            return fileBytesArr;
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

    public static void main(String[] args) {
        new ReceiverView().frmRdtReceiver.setVisible(true);

    }

}