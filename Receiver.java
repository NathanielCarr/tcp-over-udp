import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

class Receiver {

    @SuppressWarnings("serial")
    public static class ReceiverView extends JPanel {
        private ReceiverThread model;

        private JFrame frmRdtReceiver;
        private JTextField txtAddr;
        private JSpinner spnPort;
        private JSpinner spnMyPort;
        private JTextField txtFile;
        private JCheckBox chkUnreliable;
        private JButton btnReceive;
        private JLabel lblReceived;


        private class CloseListener extends WindowAdapter {
            @Override
            public void windowClosing(WindowEvent e){
                if (ReceiverView.this.model != null) {
                    ReceiverView.this.model.stop();
                }
                // e.getWindow().dispose();
                // ReceiverView.this.frmRdtReceiver.dispose();
                // System.exit(0);
            }
        }

        /**
         * Updates the attributes of the model in the view.
         */
        private class AttributesListener implements PropertyChangeListener {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                int numP = ReceiverView.this.model.getNumPackets();
                if (ReceiverView.this.model.getReceiving()) {
                    ReceiverView.this.lblReceived.setText("Received in-order packets: " + Integer.toString(numP));
                } else {
                    ReceiverView.this.lblReceived.setText("Received in-order packets: " + Integer.toString(numP) + "ALL PACKETS RECEIVED");
                }
            }
        }

        private class ReceiveListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!txtAddr.getText().matches("[0-9\\.]+") && !txtAddr.getText().toUpperCase().equals("LOCALHOST")) {
                    JOptionPane.showMessageDialog(null,
                            "An IP address can only contain digits and '.'. Alternatively, write \"localhost\" for your own IP address",
                            "Invalid IP Address", JOptionPane.ERROR_MESSAGE);
                } else if (txtFile.getText() == "") {
                    JOptionPane.showMessageDialog(null, "A file must be specified.", "Missing File",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        ReceiverView.this.model = new ReceiverThread(ReceiverView.this.chkUnreliable.isSelected(),
                                ReceiverView.this.txtFile.getText(),
                                (int) ReceiverView.this.spnPort.getValue(),
                                ReceiverView.this.txtAddr.getText(),
                                (int) ReceiverView.this.spnMyPort.getValue());
                        ReceiverView.this.setEnabledAll(false);
                        ReceiverView.this.model.addPropertyChangeListener(new AttributesListener());
                    } catch (SocketException sEx) {
                        JOptionPane.showMessageDialog(null, "Couldn't connect to the destination. Please check the IP address and port numbers.\n" + sEx.getMessage(), "Socket Exception",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (UnknownHostException uhEx) {
                        JOptionPane.showMessageDialog(null, "The IP address specified cannot be resolved. Please check this address.\n" + uhEx.getMessage(), "Unknown Host Exception",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (IOException IOEx) {
                        JOptionPane.showMessageDialog(null, "The file could not be read. Please check the path.\n" + IOEx.getMessage(), "I/O Exception",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage() + "\n", "Unknown Error",
                                JOptionPane.ERROR_MESSAGE);
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
        }

        /**
         * Create the application.
         * 
         */
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

            JLabel lblFile = new JLabel("File name:");
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

        /**
         * Registers listeners where necessary
         */
        private void registerListeners() {
            this.btnReceive.addActionListener(new ReceiveListener());
            this.frmRdtReceiver.addWindowListener(new CloseListener());
        }
    }

    public static class ReceiverThread implements Runnable {
        private final DatagramSocket receiveSocket;
        private final DatagramSocket sendSocket;
        private final int senderPort;
        private InetAddress senderAddress;

        private final File writeFile;
        // private List<Byte> fileByteList;
        private FileOutputStream packetFOS;

        private final Boolean reliability;
        private final int DROP = 10;
        private int tenth;

        private int datagramSize;
        private final int HANDSHAKE_SIZE = 3;
        private Boolean handshake;
        private Boolean eof;

        private Boolean receiving;

        public static class Header {
            private Boolean handshake;
            private Boolean eof;
            private Boolean ack;
            private int seq;

            public Header(boolean handshake, boolean eof, boolean ack, int seq) {
                this.handshake = handshake;
                this.eof = eof;
                this.ack = ack;
                this.seq = seq == 0 ? 0 : 1;
            }

            public Header(byte header) {
                this.handshake = ((header >> 7) & 1) == 1;
                this.eof = ((header >> 6) & 1) == 1;
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
                return this.eof;
            }

            public int getSeq() {
                return this.seq;
            }

            public byte toByte() {
                char[] bitArr = { this.handshake ? '1' : '0', this.eof ? '1' : '0', this.ack ? '1' : '0',
                        this.seq != 0 ? '1' : '0', '0', '0', '0', '0' };
                return (byte) Integer.parseInt(new String(bitArr), 2);
            }

        }

        private int receivedPackets;
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        private int seqNum;

        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(propertyName, listener);
        }

        public ReceiverThread(Boolean reliability, String fileName, int rPort, String sServer, int sPort)
                throws SocketException, UnknownHostException, IOException {
            this.receiveSocket = new DatagramSocket(rPort);
            this.sendSocket = new DatagramSocket();
            this.senderPort = sPort;
            this.senderAddress = InetAddress.getByName(sServer);
            this.reliability = reliability;

            this.receiving = true;

            this.receivedPackets = 0;
            this.tenth = 1;

            this.seqNum = 0;
            this.datagramSize = this.HANDSHAKE_SIZE;

            this.writeFile = new File(fileName);
        }

        public int getNumPackets() {
            return this.receivedPackets;
        }

        private DatagramPacket makeDatagramPacket(Header header, byte[] data) {
            // add appropriate header to the front of the contents.
            byte[] contents = new byte[1 + data.length];
            byte[] headerByteArr = { header.toByte() };
            System.arraycopy(headerByteArr, 0, contents, 0, 1);
            System.arraycopy(data, 0, contents, 1, data.length);
            return new DatagramPacket(contents, contents.length, this.senderAddress, this.senderPort);
        }

        private byte[] extractData(DatagramPacket packet) {
            byte[] contents = packet.getData();
            byte[] data = new byte[contents.length - 1];
            System.arraycopy(contents, 1, data, 0, contents.length - 1);
            return data;
        }

        public void receivePacket(DatagramPacket packet) throws IOException {// packet received successfully
            Header packetHeader = new Header(packet.getData()[0]); // Header of the packet
            byte[] data; // Data stored in the packet
            DatagramPacket ackPac; // Acknowledgement to 
            if (packetHeader.getSeq() == this.seqNum && !this.eof) { // If the sequence number is correct
                this.handshake = packetHeader.isHandshake(); // update whether handshaking
                this.eof = packetHeader.isFin(); // update whether conversation is over
                data = extractData(packet); // Get the data from the packet
                this.packetFOS.write(data); // write current packet to the file
                if (this.handshake) { // if this is the handshake
                    this.datagramSize = (int) ByteBuffer.wrap(data).getShort(); // update the max datagram size
                }
                else if (this.eof) { // refers to eof in recent packet
                    this.stop();
                }
                // else { // for when adding data to file all at once
                //     for (byte b : data) {
                //         this.fileByteList.add(b);
                //     }
                // }
                int oldValue = this.receivedPackets;
                this.receivedPackets++;
                this.pcs.firePropertyChange("PacketNum", oldValue, this.receivedPackets);
                this.seqNum = seqNum == 0 ? 1 : 0;
            } 
            
            ackPac = makeDatagramPacket(new Header(this.handshake, this.eof, true, this.seqNum), new byte[0]);
            this.sendSocket.send(ackPac);
        
        }

        public void writeToFile(byte[] fileByteArr) throws IOException {
            writeFile.getParentFile().mkdirs();
            writeFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(writeFile, false);
            fos.write(fileByteArr);
            fos.close();
        }

        public void run() {
            byte[] receiveBuff;
            DatagramPacket packet;
            // this.fileByteList = new ArrayList<Byte>();
            while ((receiving) && (!Thread.interrupted())) {
                receiveBuff = new byte[this.datagramSize];
                packet = new DatagramPacket(receiveBuff, this.datagramSize);
                try {
                    this.receiveSocket.receive(packet);
                
                    if (this.reliability || this.tenth != this.DROP) {
                        receivePacket(packet);
                        if (!this.reliability) {
                            this.tenth++;
                        }
                    } else {
                        this.tenth = 1;
                    }
                } catch (IOException IOEx) {
                    JOptionPane.showMessageDialog(null, "Could not parse packet\n" + IOEx.getMessage() + "\n", "I/O Exception",
                            JOptionPane.ERROR_MESSAGE);
                } 
                // byte[] fileByteArr = new byte[this.fileByteList.size()];
                // for (int i = 0; i < fileByteArr.length; i++) {
                //     fileByteArr[i] = fileByteList.get(i);
                // }

                // try {
                //     writeToFile(fileByteArr);
                // } catch (IOException IOEx) {
                //     JOptionPane.showMessageDialog(null, "Could not write to file\n" + IOEx.getMessage() + "\n", "I/O Exception",
                //             JOptionPane.ERROR_MESSAGE);
                // }
            this.pcs.firePropertyChange(null, true, false); // used to deal with receiver eofished
            this.sendSocket.close();
            this.receiveSocket.close();
            return;

        }

        public Boolean getReceiving() {
            return this.receiving;
        }

        public void stop() {
            this.receiving = false;
        }
    }

    public static void main(String[] args) {
        ReceiverView rView = new ReceiverView();
        rView.frmRdtReceiver.setVisible(true);
        rView.model.run();

    }

}