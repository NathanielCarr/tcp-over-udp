import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

class Receiver {

    @SuppressWarnings("serial")
    public static class ReceiverView extends JPanel {
        private ReceiverModel model;

        private JFrame frmRdtReceiver;
        private JTextField txtAddr;
        private JSpinner spnPort;
        private JSpinner spnMyPort;
        private JTextField txtFile;
        private JCheckBox chkUnreliable;
        private JButton btnReceive;
        private JLabel lblReceived;

        /**
         * Updates the attributes of the model in the view.
         */
        private class AttributesListener implements PropertyChangeListener {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                int numP = ReceiverView.this.model.getNumPackets();
                ReceiverView.this.lblReceived.setText(Integer.toString(numP));
                // if (evt.getPropertyName().equals("SenderReceiverStatus")) {
                // use to display whether sender is sending, receiver is receiving, both, etc
                // }
            }
        }

        private class ReceiveListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ReceiverView.this.model = new ReceiverModel(ReceiverView.this.chkUnreliable.isSelected(),
                            ReceiverView.this.txtFile.getText(),
                            (int) ReceiverView.this.spnPort.getValue(),
                            ReceiverView.this.txtAddr.getText(),
                            (int) ReceiverView.this.spnMyPort.getValue());
                    ReceiverView.this.setEnabledAll(false);
                    ReceiverView.this.model.addPropertyChangeListener(new AttributesListener());
                } catch (SocketException sEx) {
                    JOptionPane.showMessageDialog(null, sEx.getMessage() + "\n", "Socket Exception",
                            JOptionPane.ERROR_MESSAGE);
                } catch (UnknownHostException uhEx) {
                    JOptionPane.showMessageDialog(null, uhEx.getMessage() + "\n", "Unknown Host Exception",
                            JOptionPane.ERROR_MESSAGE);
                } catch (IOException IOEx) {
                    JOptionPane.showMessageDialog(null, IOEx.getMessage() + "\n", "I/O Exception",
                            JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage() + "\n", "Unknown Error",
                            JOptionPane.ERROR_MESSAGE);
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

        /**
         * Registers listeners where necessary
         */
        private void registerListeners() {
            this.btnReceive.addActionListener(new ReceiveListener());

        }
    }

    private static class UDPThread extends Thread {
        private final DatagramSocket socket;

        public UDPThread(DatagramSocket s) {
            this.socket = s;
        }

        @Override
        public void interrupt() {
            super.interrupt();
            this.socket.close();
        }
    }

    public static class ReceiverModel {// implements Runnable(?) {
        private final UDPThread receiveThread;
        private final UDPThread sendThread;
        private final File writeFile;
        private final Path path;
        private final Boolean reliability;
        private int tenth;
        private int datagramSize;
        private final int HANDSHAKE_SIZE = 3;

        public enum sendingStatus {
            FINISHED("Finished"), SENDING("Sending");
            private final String statusString;

            sendingStatus(final String statusString) {
                this.statusString = statusString;
            }

            @Override
            public String toString() {
                return this.statusString;
            }
        }

        public enum receivingStatus {
            RECEIVING("Receiving"), FINISHED("Finished");
            private final String statusString;

            receivingStatus(final String statusString) {
                this.statusString = statusString;
            }

            @Override
            public String toString() {
                return this.statusString;
            }
        }

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
                this.ack = ((header >> 6) & 1) == 1;
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
                return (byte) Integer.parseInt(new String(bitArr), 2);
            }

        }

        private sendingStatus sStatus;
        private receivingStatus rStatus;
        private int receivedPackets;
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        private int seqNum;

        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(propertyName, listener);
        }

        public ReceiverModel(Boolean reliability, String fileName, int rPort, String sServer, int sPort)
                throws SocketException, UnknownHostException, IOException {
            this.receiveThread = new UDPThread(new DatagramSocket(rPort));
            this.sendThread = new UDPThread(new DatagramSocket(sPort, InetAddress.getByName(sServer)));
            this.reliability = reliability;

            this.sStatus = sendingStatus.FINISHED;
            this.rStatus = receivingStatus.RECEIVING;
            this.receivedPackets = 0;
            this.tenth = 0;

            this.seqNum = 0;
            this.datagramSize = this.HANDSHAKE_SIZE;

            this.writeFile = new File(fileName);
            this.path = Paths.get(writeFile.getAbsolutePath());
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
            return new DatagramPacket(contents, contents.length, this.sendThread.socket.getLocalSocketAddress());
        }

        public void receivePacket(DatagramPacket packet) {// packet received successfully

            // check to make sure it's the right sequence number, fin, ack, etc
            int oldValue = this.receivedPackets;
            this.receivedPackets++;
            this.pcs.firePropertyChange("PacketNum", oldValue, this.receivedPackets);

            this.seqNum = seqNum == 0 ? 1 : 0;
            // write to file
            // send ack

            // if handshake get the max packet size and store in datagramSize
            // else get rid of any excess info in packet
            // read packet to file
            // if the packet says sender is done, do something w sStatus and send ack
            // if he packet acknowledges receiver is done, do something else w rStatus and
            // stop receiving
        }

        public void run() {
            try {
                while ((rStatus == receivingStatus.RECEIVING) && (!Thread.interrupted())) {
                    DatagramPacket packet = makeDatagramPacket(new Header(false, false, false, this.seqNum),
                            new byte[this.datagramSize]);
                    try {
                        this.receiveThread.socket.receive(packet);
                    } catch (Exception e) {
                    }
                    if (this.reliability || this.tenth != 9) {
                        receivePacket(packet);
                        if (!this.reliability) {
                            this.tenth++;
                        }
                    } else {
                        this.tenth = 0;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {

            }
            this.pcs.firePropertyChange(null, true, false); // used to deal with sender finished or sending and receiver
                                                            // finished or receiving
            this.sendThread.interrupt();
            this.receiveThread.interrupt();
            return;

        }
    }

    public static void main(String[] args) {
        new ReceiverView().frmRdtReceiver.setVisible(true);
    }

}