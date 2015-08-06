This is a centralized P2P system where the information about the RFCs ( files ) are maintained in a centralized server, but only the clients/peers have the actual RFCs.


There are 2 files with code

MultiThreadServer.java

Client.java


for server side do :

javac MultiThreadServer.java
If that does not work , because of a deprecated method 

do 

javac -Xlint:deprecation MultiThreadServer.java

then run using:

java MultiThreadServer


for client side do :

javac Client.java

then run using:

java Client <RFC folder location> <IP address or host name of server>

example :

java Client RFCfolder 152.46.18.162

The RFC files in the folder should be in this format :

RFC<rfcno>-<topic>.txt

Example :

RFC1234-Topic3.txt

Here I  have providing 2 folders with RFCs.

You can use the same

Client 1 : 

java Client RFCfolder  <Server IP address>

Client 2 :

java Client RFCfolder2  <Server IP address>