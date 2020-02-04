# TCP Over UDP
Written for CP 372 (Computer Networks); enables TCP-like transmission of files from one computer to another over UDP. 

## Authors
Group 059
- Saje Bailey
- Nathaniel Carr

## Datagram Format
The header for each datagram is one byte in length, in the form of: [HS]\[EoT\]\[ACK\]\[seq\]0000.

The first bit of the header (HS) indicates whether the datagram packet is the handshake (see below).

The next bit is the End-of-Transmission bit (EoT); it indicates whether the datagram is an EoT datagram. If the Receiver receives an EoT, it sends an acknowledgement before closing itssockets. If the Sender fails to receive an acknowledgement after re-sending the EoT datagram up to three times, it also closes its sockets.

The next bit is the acknowledgement (ACK) bit. It indicates if the current packet is acknowledgement for a previous datagram packet.

Finally is the sequence (seq) bit. It holds the sequence number of the current datagram packet.

After the header of the datagram is the actual data and metadata of the file as a byte array of the specified maximum datagram size (MDS) minus one byte for the header.

## Handshaking
Each handshake begins with a three-byte message sent from the Sender to Receiver. The first datagram of the handshake is identified identified by a header whose settings are HS=1, EoT=0, ACK=0, seq=0. The next two bytes contain a short Java primitive that represents the MDS. TheReceiver uses this MDS in future transmissions.

Upon receiving this handshake packet, the Receiver sends an ACK with header settings HS=1, EoT=0, ACK=1, seq=0. When the Sender receives this ACK, it considers the connection open. It then begins sending data packets. Upon receiving the first of these data packets, the Receiver considers the connection open.