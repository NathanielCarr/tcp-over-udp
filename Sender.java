import java.io.File;
import java.io.IOException;

import java.nio.file.Files; // Read byte array from file.

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;


class Sender {

    public static class RDTFileTransfer {

        public static void transferFile(InetAddress addr, int port, int timeoutMicro, int mds, File file) throws SocketException, IOException {
            int seq = 0;
            DatagramSocket socket = new DatagramSocket(port, addr);

            handshake(socket);

            socket.setSoTimeout((int) (timeoutMicro / 1000));
            Iterable<DatagramPacket> datagrams = makeDatagrams(file, mds, seq);

            try {
                while (true) {
                    // TODO send datagrams, receive ACKs, timeouts.
                    try {


                    } catch (SocketTimeoutException te) {

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

        private static Iterable<DatagramPacket> makeDatagrams(File file, int mds, int startSeq) throws IOException {
            List<DatagramPacket> datagramList = new ArrayList<DatagramPacket>();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            // TODO append appropriate headers.
            return datagramList;
        }


    }

    public static void main(String[] args) {

    }

}