import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

class Receiver {

    public static class ReceiverView extends JPanel{
        private ReceiverModel model;


        private JFrame frame;
        private JTextField txtIPAddress;
        private JSpinner spnrDataPort;
        private JSpinner spnrACKPort;
        private JTextField txtFileName;
        private JToggleButton tglReliability;
        private JButton bttnReceive;
        private JButton bttnCancel;
        private JLabel lblNumReceived;

        /**
	 * Updates the attributes of the model in the view.
	 */
	private class AttributesListener implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
            int numP = ReceiverView.this.model.getNumPackets();
            ReceiverView.this.lblNumReceived.setText(Integer.toString(numP));
            if (numP > 0 && ReceiverView.this.bttnCancel.isEnabled()) {
                ReceiverView.this.bttnCancel.setEnabled(false);
            }
            // if (evt.getPropertyName().equals("SenderReceiverStatus")) {
                // use to display whether sender is sending, receiver is receiving, both, etc
            // }
        }
	}
        
        private class ToggleListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tglReliability.isSelected()) {
                    tglReliability.setText("Reliable");
                    //System.out.println("Yay");
                }
                else {
                    tglReliability.setText("Unreliable");
                    //System.out.println("Nay");
                }
            }
        }

        private class ButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String bttn = ((JButton) e.getSource()).getText();
                if (bttn.equals(ReceiverView.this.bttnReceive.getText())) {
                    try {
                        ReceiverView.this.model = new ReceiverModel(ReceiverView.this.tglReliability.isSelected(), (int) ReceiverView.this.spnrDataPort.getValue(), ReceiverView.this.txtIPAddress.getText(), (int) ReceiverView.this.spnrACKPort.getValue());
                        ReceiverView.this.setEnabledAll(false);
                        ReceiverView.this.bttnCancel.setEnabled(true);
                        ReceiverView.this.model.addPropertyChangeListener(new AttributesListener());
                    } catch (SocketException sEx) {
                        JOptionPane.showMessageDialog(null, sEx.getMessage() + "\n", "Socket Exception", JOptionPane.ERROR_MESSAGE);
                    } catch (UnknownHostException uhEx) {
                        JOptionPane.showMessageDialog(null, uhEx.getMessage() + "\n", "Unknown Host Exception", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage() + "\n", "Unknown Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else {
                    ReceiverView.this.model.receiveThread.interrupt();
                    ReceiverView.this.model.sendThread.interrupt();
                }
            }
        }

        private void setEnabledAll(Boolean status) {
            spnrACKPort.setEnabled(status);
            spnrDataPort.setEnabled(status);
            txtFileName.setEnabled(status);
            txtIPAddress.setEnabled(status);
            tglReliability.setEnabled(status);
            bttnReceive.setEnabled(status);
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
            frame = new JFrame();
            frame.setBounds(100, 100, 450, 295);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setLayout(null);
            
            txtIPAddress = new JTextField();
            txtIPAddress.setFont(new Font("Tahoma", Font.PLAIN, 14));
            txtIPAddress.setBounds(187, 10, 239, 20);
            frame.getContentPane().add(txtIPAddress);
            txtIPAddress.setColumns(10);
            
            JLabel lblIPAddress = new JLabel("Sender IP:");
            lblIPAddress.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblIPAddress.setBounds(10, 10, 80, 20);
            frame.getContentPane().add(lblIPAddress);
            
            spnrDataPort = new JSpinner();
            spnrDataPort.setModel(new SpinnerNumberModel(22, 0, 65535, 1));
            spnrDataPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            spnrDataPort.setBounds(187, 90, 239, 20);
            frame.getContentPane().add(spnrDataPort);
            
            spnrACKPort = new JSpinner();
            spnrACKPort.setModel(new SpinnerNumberModel(22, 0, 65535, 1));
            spnrACKPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            spnrACKPort.setBounds(187, 50, 239, 20);
            frame.getContentPane().add(spnrACKPort);
            
            txtFileName = new JTextField();
            txtFileName.setFont(new Font("Tahoma", Font.PLAIN, 14));
            txtFileName.setColumns(10);
            txtFileName.setBounds(187, 130, 239, 20);
            frame.getContentPane().add(txtFileName);
            
            JLabel lblACKPort = new JLabel("Acknowledgement Port#:");
            lblACKPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblACKPort.setBounds(10, 50, 167, 20);
            frame.getContentPane().add(lblACKPort);
            
            JLabel lblDataPort = new JLabel("Data Port#:");
            lblDataPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblDataPort.setBounds(10, 90, 80, 20);
            frame.getContentPane().add(lblDataPort);
            
            JLabel lblFileName = new JLabel("File Name:");
            lblFileName.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblFileName.setBounds(10, 130, 80, 20);
            frame.getContentPane().add(lblFileName);
            
            lblNumReceived = new JLabel("0");
            lblNumReceived.setHorizontalAlignment(SwingConstants.TRAILING);
            lblNumReceived.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblNumReceived.setBounds(380, 210, 46, 20);
            frame.getContentPane().add(lblNumReceived);
            
            JLabel lblNR = new JLabel("Received In-Order Packets:");
            lblNR.setFont(new Font("Tahoma", Font.PLAIN, 14));
            lblNR.setBounds(187, 210, 202, 20);
            frame.getContentPane().add(lblNR);
            
            tglReliability = new JToggleButton("Unreliable");
            tglReliability.setFont(new Font("Tahoma", Font.PLAIN, 14));
            tglReliability.setBounds(10, 170, 118, 20);
            frame.getContentPane().add(tglReliability);
            
            bttnCancel = new JButton("Cancel");
            bttnCancel.setFont(new Font("Tahoma", Font.PLAIN, 14));
            bttnCancel.setBounds(321, 170, 105, 20);
            bttnCancel.setEnabled(false);
            frame.getContentPane().add(bttnCancel);
            
            bttnReceive = new JButton("Receive");
            bttnReceive.setFont(new Font("Tahoma", Font.PLAIN, 14));
            bttnReceive.setBounds(187, 170, 105, 20);
            frame.getContentPane().add(bttnReceive);
        }
        /**
         * Registers listeners where necessary
         */
        private void registerListeners() {
            ButtonListener actionBttn = new ButtonListener();
            this.tglReliability.addActionListener(new ToggleListener());
            this.bttnReceive.addActionListener(actionBttn);
            this.bttnCancel.addActionListener(actionBttn);

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
        private final DatagramSocket receiveSocket;
        private final DatagramSocket sendSocket;
        private final UDPThread receiveThread;
        private final UDPThread sendThread;
        private final Boolean reliability;
        private int tenth;
        private int datagramSize;
        private final int HANDSHAKE_SIZE = 3;

        public enum sendingStatus {
            FINISHED("Finished"), 
            SENDING("Sending");
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
            RECEIVING("Receiving"), 
            FINISHED("Finished");
            private final String statusString;

            receivingStatus(final String statusString) {
                this.statusString = statusString;
            }

            @Override
            public String toString() {
                return this.statusString;
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
        
        public ReceiverModel(Boolean reliability, int rPort, String sServer, int sPort) throws SocketException, UnknownHostException {
            this.receiveSocket = new DatagramSocket(rPort);
            this.sendSocket = new DatagramSocket(sPort, InetAddress.getByName(sServer));
            this.receiveThread = new UDPThread(receiveSocket);
            this.sendThread = new UDPThread(sendSocket);
            this.reliability = reliability;

            this.sStatus = sendingStatus.FINISHED;
            this.rStatus = receivingStatus.RECEIVING;
            this.receivedPackets = 0;
            this.tenth = 0;

            this.seqNum = 0;
            this.datagramSize = this.HANDSHAKE_SIZE;
        }

        
        public int getNumPackets() {
            return this.receivedPackets;
        }

        public void receivePacket(DatagramPacket packet) {// packet received successfully
            int oldValue = this.receivedPackets;
            this.receivedPackets++;
            this.pcs.firePropertyChange("PacketNum", oldValue, this.receivedPackets);
            // if handshake get the max packet size and store in datagramSize
            // else get rid of any excess info in packet
            // read packet to file
            // if the packet says sender is done, do something w sStatus and send ack
            // if he packet acknowledges receiver is done, do something else w rStatus and stop receiving
        }

        @Override
        public void run() {
            try {
                while ((this.rStatus == this.receivingStatus.Receiving) && (!Thread.interrupted())) {
                    DatagramPacket packet = makeDatagramPacket(new Header(false, false, false, this.seqNum), new byte[this.datagramSize]);
                    this.receiveThread.socket.receive(packet);
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
            this.pcs.firePropertyChange(null, true, false); // used to deal with sender finished or sending and receiver finished or receiving
            return;

        }
    }

    public static void main(String[] args) {
		ReceiverView window = new ReceiverView();
        window.frame.setVisible(true);

    }

}