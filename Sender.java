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

class Sender {

    public static class RDTFileTransfer {

        final static int HEADER_SIZE = 1;

        public static void fileSend(InetAddress addr, int port, int timeoutMicro, int mds, File file)
                throws SocketException, IOException {
            int seq = 0;
            DatagramSocket socket = new DatagramSocket(port, addr);

            handshake(socket);

            socket.setSoTimeout((int) (timeoutMicro / 1000));
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
            // TODO handshake.
        }

        private static byte[] makeHeader(int isAck, int ackNum, int seqNum) {
            byte[] header = new byte[HEADER_SIZE];
            char[] bitArr = {isAck != 0 ? '1' : '0', ackNum != 0 ? '1' : '0', seqNum != 0 ? '1' : '0', '0', '0', '0', '0', '0' };
            header[0] = (byte) Byte.parseByte(String.valueOf(bitArr), 2);
            return header;
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

        private static String byteArrToString(byte[] bytes) {
            return new String(bytes);
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