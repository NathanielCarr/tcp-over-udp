import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;
import javax.swing.JButton;

class Receiver {

    public static class ReceiverView {
        private JFrame frame;
        private JTextField txtIPAddress;
        private JTextField txtDataPort;
        private JTextField txtACKPort;
        private JTextField txtFileName;
        private JToggleButton tglReliability;
        private JButton bttnReceive;
        private JButton bttnCancel;
        private JLabel lblNumReceived;
        
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
                    setEnabledAll(false);

                    UDPThread socketThread = new UDPThread();
                }
                else {
                    
                }
            }
        }

        private void setEnabledAll(Boolean status) {
            txtACKPort.setEnabled(status);
            txtDataPort.setEnabled(status);
            txtFileName.setEnabled(status);
            txtIPAddress.setEnabled(status);
            tglReliability.setEnabled(status);
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
            frame.setBounds(100, 100, 450, 235);
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
            
            txtDataPort = new JTextField();
            txtDataPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            txtDataPort.setColumns(10);
            txtDataPort.setBounds(187, 90, 239, 20);
            frame.getContentPane().add(txtDataPort);
            
            txtACKPort = new JTextField();
            txtACKPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
            txtACKPort.setColumns(10);
            txtACKPort.setBounds(187, 50, 239, 20);
            frame.getContentPane().add(txtACKPort);
            
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
            tglReliability.addActionListener(new ToggleListener());
            bttnReceive.addActionListener(actionBttn);
            bttnCancel.addActionListener(actionBttn);
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

    public static class ReceiverModel {
        private final Socket receiveSocket;
        private final Socket sendSocket;

        public ReceiverModel(String rServer, int rPort, String sServer, int sPort) {
            this.receiveSocket = new Socket(rServer, rPort)
        }
    }

    public static void main(String[] args) {
		ReceiverView window = new ReceiverView();
        window.frame.setVisible(true);

    }

}