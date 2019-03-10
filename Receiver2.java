import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

class Receiver2 {

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
                        FileOutputStream fos = new FileOutputStream(txtFile.getText(), false);
                        Boolean reliable = !chkUnreliable.isSelected();

                        receiverThread = new ReceiverThread(ReceiverView.this, addr, port, myPort, reliable, fos);
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
                                "The file couldn't be read. Please check the path and make sure it exists.",
                                "Bad File Path", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

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

            txtAddr = new JTextField();
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
            btnReceive.addActionListener(new ButtonListener());
        }

        public void updateReceivedLabel(int numPackets) {
            lblReceived.setText(String.format("Received in-order packets: %d", numPackets));
        }

        public void transferComplete() {
            JOptionPane.showMessageDialog(null, "The file has been received.", "Transfer Complete", JOptionPane.INFORMATION_MESSAGE);
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

        static final int DROP_AT = 10;

        private FileOutputStream fos;
        private ReceiverView view;
        private DatagramSocket outSocket;
        private DatagramSocket inSocket;
        private InetAddress addr;
        private int port;
        private int mds;
        private Boolean reliable;
        private int dropCounter;
        private int seq;

        public ReceiverThread(ReceiverView view, InetAddress addr, int port, int myPort, Boolean reliable,
                FileOutputStream fos) throws SocketException, IOException {

            this.view = view;

            this.fos = fos;

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
                receiveFile();
                fos.close();
                this.inSocket.close();
                this.outSocket.close();
                view.transferComplete();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }

        private void receiveFile() throws IOException {

            int numPackets = 0;

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
                System.out.println(String.format("R: Received packet (in handshake)."));

                // Send ACK and set MDS appropriately.
                if (inHeader.isHandshake() && (dropCounter != 0 || reliable)) {
                    mds = (int) ByteBuffer.wrap(extractData(inPacket)).getShort();
                    outSocket.send(makeDatagramPacket(new Header(true, false, true, inHeader.getSeq()), new byte[0]));
                    view.updateReceivedLabel(++numPackets);
                    System.out.println(String.format("R: Sent packet (in handshake)."));
                } else {
                    System.out.println(String.format("R: Dropped packet (in handshake)."));
                }

            } while (inHeader.isHandshake());

            // Set seq to whatever the first data packet had.
            seq = inHeader.getSeq();

            // Start accepting data packets until FIN received.
            do {
                // Process packet contents if the packet received is appropriate.
                if (inHeader.getSeq() == seq) {
                    fos.write(extractData(inPacket));
                    seq = ++seq % 2;
                }

                // Send ACK for last packet received.
                if (dropCounter != 0 || reliable) {
                    outSocket.send(makeDatagramPacket(new Header(false, false, true, inHeader.getSeq()), new byte[0]));
                    view.updateReceivedLabel(++numPackets);
                    System.out.println(String.format("R: Sent packet (in receiveFile)."));
                } else {
                    System.out.println(String.format("R: Dropped packet (in receiveFile)."));
                }

                // Get next packet.
                inBuffer = new byte[mds];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                inSocket.receive(inPacket);
                inHeader = new Header(inPacket.getData()[0]);
                dropCounter = (dropCounter + 1) % DROP_AT;
                System.out.println(String.format("R: Received packet (in receiveFile)."));

            } while (!inHeader.isFin());

            // Send ACK of FIN + own FIN.
            if (dropCounter != 0 || reliable) {
                outSocket.send(makeDatagramPacket(new Header(false, true, true, inHeader.getSeq()), new byte[0]));
                view.updateReceivedLabel(++numPackets);
                System.out.println(String.format("R: Sent packet (in endConnection)."));
            } else {
                dropCounter = ++dropCounter % DROP_AT;
                System.out.println(String.format("R: Dropped packet (in endConnection)."));
            }

            view.updateReceivedLabel(++numPackets);

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