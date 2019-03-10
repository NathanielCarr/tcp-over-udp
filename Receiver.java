import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

class Receiver {

    public static void main(String[] args) {
        new ReceiverView().frmRdtReceiver.setVisible(true);
    }

    private static class ReceiverView {

        private JFrame frmRdtReceiver;
        private JTextField txtAddr;
        private JSpinner spnPort;
        private JSpinner spnMyPort;
        private JTextField txtFile;
        private JCheckBox chkUnreliable;
        private JButton btnReceive;
        private JLabel lblReceived;

        private ReceiverThread receiverThread;

        /**
         * Updates the attributes of the model in the view.
         */
        private class PacketsListener implements PropertyChangeListener {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (!evt.getPropertyName().equals("FIN")) {
                    ReceiverView.this.lblReceived
                            .setText(String.format("Received in-order packets: %d", (int) evt.getNewValue()));
                } else {
                    JOptionPane.showMessageDialog(null, "The file has been received.", "Transfer Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    ReceiverView.this.setEnabledAll(true);
                }
            }
        }

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
                        InetAddress addr = InetAddress.getByName(txtAddr.getText());
                        int port = (int) spnPort.getValue();
                        int myPort = (int) spnMyPort.getValue();
                        String filePath = txtFile.getText().trim();
                        Boolean reliable = !chkUnreliable.isSelected();

                        receiverThread = new ReceiverThread(addr, port, myPort, reliable, filePath);
                        receiverThread.addPropertyChangeListener(new PacketsListener());
                        ReceiverView.this.setEnabledAll(false);
                        receiverThread.start();
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
                                "The file path couldn't be read. Please check the path and make sure there are no typos.",
                                "Bad File Path", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

        }

        private void setEnabledAll(Boolean status) {
            spnMyPort.setEnabled(status);
            spnPort.setEnabled(status);
            txtFile.setEnabled(status);
            txtAddr.setEnabled(status);
            chkUnreliable.setEnabled(status);
            btnReceive.setEnabled(status);

            if (!status)
                lblReceived.setText(String.format("Received in-order packets: %d", 0));
        }

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

            txtAddr = new JTextField("localhost");
            txtAddr.setHorizontalAlignment(SwingConstants.RIGHT);
            txtAddr.setColumns(10);
            txtAddr.setBounds(10, 27, 191, 20);
            frmRdtReceiver.getContentPane().add(txtAddr);

            spnPort = new JSpinner();
            spnPort.setModel(new SpinnerNumberModel(9898, 0, 65535, 1));
            spnPort.setBounds(213, 27, 111, 20);
            frmRdtReceiver.getContentPane().add(spnPort);

            spnMyPort = new JSpinner();
            spnMyPort.setModel(new SpinnerNumberModel(9897, 0, 65535, 1));
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

            JLabel lblAddr = new JLabel("Sender IP address:");
            lblAddr.setHorizontalAlignment(SwingConstants.LEFT);
            lblAddr.setBounds(10, 11, 186, 14);
            frmRdtReceiver.getContentPane().add(lblAddr);

            JLabel lblPort = new JLabel("Sender port:");
            lblPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblPort.setBounds(213, 11, 111, 14);
            frmRdtReceiver.getContentPane().add(lblPort);

            JLabel lblColon = new JLabel(":");
            lblColon.setHorizontalAlignment(SwingConstants.CENTER);
            lblColon.setBounds(201, 30, 13, 14);
            frmRdtReceiver.getContentPane().add(lblColon);

            JLabel lblMyPort = new JLabel("Receiver port:");
            lblMyPort.setHorizontalAlignment(SwingConstants.LEFT);
            lblMyPort.setBounds(334, 11, 111, 14);
            frmRdtReceiver.getContentPane().add(lblMyPort);
        }

        private void registerListeners() {
            btnReceive.addActionListener(new ButtonListener());
        }

    }

    private static class ReceiverThread extends Thread {

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

        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        static final int DROP_AT = 10;

        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private InetAddress addr;
        private int port;
        private int mds;
        private Boolean reliable;
        private int dropCounter;
        private int numPackets;
        private int seq;

        private String filePath;

        public ReceiverThread(InetAddress addr, int port, int myPort, Boolean reliable, String filePath)
                throws SocketException, IOException {

            this.filePath = filePath;

            // Ready output socket.
            this.addr = addr;
            this.port = port;
            this.outSocket = new DatagramSocket();

            // Ready input socket.
            this.inSocket = new DatagramSocket(myPort);

            // Set initial handshake MDS.
            this.mds = 3;

            // Set seq to 0.
            this.seq = 0;

            // Unreliable transfer information.
            this.reliable = reliable;
            this.dropCounter = 0;

        }

        public void run() {
            try {
                byte[] fileByteArr = receiveFile();
                FileOutputStream fos = new FileOutputStream(filePath, false);
                fos.write(fileByteArr);
                fos.close();
                this.inSocket.close();
                this.outSocket.close();
                this.pcs.firePropertyChange("FIN", false, true);
            } catch (Exception e) {
            }
        }

        private byte[] receiveFile() throws IOException {

            List<Byte> fileBytes = new ArrayList<Byte>();

            numPackets = 0;

            byte[] inBuffer = new byte[3];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            Header inHeader;

            // Do handshake until handshake complete.
            do {
                // Get packet.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inPacket.getData()[0]);
                dropCounter = ++dropCounter % DROP_AT;

                // Send ACK and set MDS appropriately.
                if (inHeader.isHandshake() && (dropCounter != 0 || reliable)) {
                    mds = (int) ByteBuffer.wrap(extractData(inPacket)).getShort();
                    outSocket.send(makeDatagramPacket(new Header(true, false, true, inHeader.getSeq()), new byte[0]));
                    this.pcs.firePropertyChange("Packets", numPackets, ++numPackets);
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
                    seq = ++seq % 2;
                }

                // Send ACK for last packet received.
                if (dropCounter != 0 || reliable) {
                    outSocket.send(makeDatagramPacket(new Header(false, false, true, inHeader.getSeq()), new byte[0]));
                    this.pcs.firePropertyChange("Packets", numPackets, ++numPackets);
                }

                // Get next packet.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inPacket.getData()[0]);
                dropCounter = ++dropCounter % DROP_AT;

            } while (!inHeader.isFin());

            // Send ACK of FIN + own FIN.
            if (dropCounter != 0 || reliable) {
                outSocket.send(makeDatagramPacket(new Header(false, true, true, inHeader.getSeq()), new byte[0]));
                this.pcs.firePropertyChange("Packets", numPackets, ++numPackets);
            } else {
                dropCounter = ++dropCounter % DROP_AT;
            }

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
            return new DatagramPacket(contents, contents.length, this.addr, this.port);
        }

        private byte[] extractData(DatagramPacket packet) {
            byte[] contents = packet.getData();
            byte[] data = new byte[contents.length - 1];
            System.arraycopy(contents, 1, data, 0, contents.length - 1);
            return data;
        }
    }
}