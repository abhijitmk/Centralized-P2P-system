
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {

	static ConcurrentHashMap<Integer, String> rfcs = new ConcurrentHashMap<Integer, String>();
	
	// map of rfcids and names

	static String clientHostname;

	static String folderLocation;

	Socket peerSocketEntry;

	static final int TIME = 200; // this is for adding delay - necessary so that data gets transmitted across different machines properly

	public Client(Socket peerSocketEntry) {

		this.peerSocketEntry = peerSocketEntry;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		BufferedReader is = null;
		Socket clientSocket = null;
		DataOutputStream os = null;

		try {
			clientHostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.out.println(e);
		}

		Scanner sc = null;
		int choice = 0;

		try {
			clientSocket = new Socket(args[1], 7734); //args[1] is the IP address of hostname of the server

			folderLocation = args[0];
			
			is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			os = new DataOutputStream(clientSocket.getOutputStream());
		}

		catch (UnknownHostException e) {
			System.out.println(e);
		}

		if (clientSocket != null) {
			Boolean joinedFlag = false;
			while (true) {
				PrintStream pstream = new PrintStream(clientSocket.getOutputStream());

				if (!joinedFlag) {
					String hostname = InetAddress.getLocalHost().getHostName();
					int port = clientSocket.getLocalPort() + 1;

					String join = "JOIN " + clientHostname + " 7734 P2P-CI/1.0";
					pstream.println(join);
					Thread.sleep(TIME);

					join = "Host: " + clientHostname;
					pstream.println(join);
					Thread.sleep(TIME);

					join = "Port: " + port;
					pstream.println(join);
					Thread.sleep(TIME);

					join = "OS: " + System.getProperty("os.name");
					pstream.println(join);
					Thread.sleep(TIME);

					String checkstr = null;
					sc = new Scanner(clientSocket.getInputStream());

					checkstr = sc.nextLine();

					if (checkstr.equals("OK")) {
						System.out.println("Connected to " + args[1]);
						int uploadPort = clientSocket.getLocalPort() + 1;
						System.out.println("Client upload port : " + uploadPort);

						folderLocation = args[0];

						File directory = new File(folderLocation);
						File[] fList = directory.listFiles();
						ArrayList<String> filenames = new ArrayList<String>();

						// getting all the filenames

						System.out.println("The Own RFC list is : ");

						for (File file : fList) {
							if (file.isFile()) {
								System.out.println(file.getName());
								filenames.add(file.getName());
							}
						}

						// parsing all the filenames and putting in rfc map

						int temprfcid = 0;
						String temprfctitle = new String();
						int numrfcs = 0;

						for (String temp : filenames) {
							temprfcid = Integer.parseInt(temp.substring("RFC".length(), temp.indexOf('-')));
							temprfctitle = temp.substring(temp.indexOf('-') + 1, temp.indexOf('.'));

							rfcs.put(temprfcid, temprfctitle);
							numrfcs++;
						}

						pstream.println(numrfcs);// sending number of rfcs
						Thread.sleep(TIME);

						for (Entry<Integer, String> rfcentry : rfcs.entrySet()) {
							sendRFCdata(rfcentry.getKey(), clientSocket);
						}

						joinedFlag = true;

						// for allowing other clients to connect

						new Thread(new Client(clientSocket)).start();

					}
				}

				System.out.println("What do you want to do ? ");
				System.out.println("1. Upload an RFC entry");
				System.out.println("2. Get the list of RFCs");
				System.out.println("3. Download an RFC");
				System.out.println("4. Exit");

				sc = new Scanner(System.in);
				if (sc.hasNextLine())
					choice = Integer.parseInt(sc.nextLine());

				switch (choice) {
				case 1: {

					int rfcno = 0;
					String addrfc = null;
					System.out.println("Enter the RFC number");
					sc = new Scanner(System.in);
					if (sc.hasNextLine())
						rfcno = Integer.parseInt(sc.nextLine());

					// check if RFC exists at the user provided location

					folderLocation = args[0];

					File directory = new File(folderLocation);
					File[] fList = directory.listFiles();

					String rfcFileStarting = "RFC" + rfcno;
					Boolean rfcfileFound = false;

					String rfcFileName = null;

					for (File file : fList) {
						if (file.isFile()) {

							rfcFileName = file.getName();
							if (rfcFileName.startsWith(rfcFileStarting)) {
								rfcfileFound = true;
								break;
							}
						}
					}

					String rfctitle = null;

					if (rfcfileFound) {
						pstream.println(choice);

						Thread.sleep(TIME);

						rfctitle = rfcFileName.substring(rfcFileName.indexOf('-') + 1, rfcFileName.indexOf('.'));

						rfcs.put(rfcno, rfctitle);

						sendRFCdata(rfcno, clientSocket);

						// checking the reply from server

						System.out.println("The reply from server is");

						String temp = is.readLine();
						System.out.println(temp);

						temp = is.readLine();
						System.out.println(temp);
					}

					else {
						System.out.println("RFC does not exist at specified location");
					}

					break;
				}

				case 2: {

					pstream.println(choice);
					Thread.sleep(TIME);

					int port = clientSocket.getLocalPort() + 1;

					pstream.println("LIST ALL P2P-CI/1.0");
					Thread.sleep(TIME);
					pstream.println("Host: " + clientHostname);
					Thread.sleep(TIME);

					pstream.println("Port: " + port);
					Thread.sleep(TIME);

					int count = Integer.parseInt(is.readLine());

					for (int i = 0; i < count; i++)

					{
						System.out.println(is.readLine());
					}

					break;
				}

				case 3: {

					int rfcno = 0;
					pstream.println(choice);
					Thread.sleep(TIME);

					System.out.println("Enter the RFC number");
					sc = new Scanner(System.in);
					if (sc.hasNextLine())
						rfcno = Integer.parseInt(sc.nextLine());

					String temp = "LOOKUP RFC " + rfcno + " P2P-CI/1.0";
					pstream.println(temp);
					Thread.sleep(TIME);

					temp = "Host: " + clientHostname;
					pstream.println(temp);
					Thread.sleep(TIME);

					int tempPort = clientSocket.getLocalPort() + 1;
					temp = "Port: " + tempPort;
					pstream.println(temp);
					Thread.sleep(TIME);

					String checkReply = is.readLine();

					if ("P2P-CI/1.0 404 Not Found".equals(checkReply))
						System.out.println(checkReply);

					else {

						int num_entries = Integer.parseInt(checkReply);

						System.out.println("Number of peers having RFC " + rfcno + " : " + num_entries);

						String[] peers = new String[num_entries];

						for (int i = 0; i < num_entries; i++) {
							peers[i] = is.readLine();
							System.out.println(peers[i]);
						}

						String[] peerComp = new String[4];

						peerComp[0] = peers[0].substring("RFC number ".length(), peers[0].indexOf("RFC title")); 
						// extracted RFC number
																													
						peerComp[1] = peers[0].substring(peers[0].indexOf("RFC title") + "RFC title ".length(), peers[0].indexOf("hostname") - 1); 
						// extracted RFC title
																																					
						peerComp[2] = peers[0].substring(peers[0].indexOf("hostname ") + "hostname ".length(),
								peers[0].indexOf("upload port number") - 1); // extracted hostname
																				 

						peerComp[3] = peers[0].substring(peers[0].indexOf("upload port number ") + "upload port number ".length(), peers[0].length()); 
						// extracted port number
																																				
						Socket connectToPeer = new Socket(peerComp[2], Integer.parseInt(peerComp[3]));

						BufferedReader peerReader = new BufferedReader(new InputStreamReader(connectToPeer.getInputStream()));

						PrintStream clientPStream = new PrintStream(connectToPeer.getOutputStream());

						String response;

						clientPStream.println("GET RFC " + rfcno + " P2P-CI/1.0");
						Thread.sleep(TIME);
						clientPStream.println("Host: " + clientHostname);
						Thread.sleep(TIME);
						clientPStream.println("OS: " + System.getProperty("os.name"));
						Thread.sleep(TIME);

						while (!(response = peerReader.readLine()).equals("")) {
							System.out.println(response);
						}

						String filename = peerReader.readLine();
						System.out.println("Receiving " + filename + " from " + peerComp[2]);

						int filesize = 2022386;
						int bytesRead;
						int currentTot = 0;
						File newFile = new File(folderLocation + "/" + filename);
						newFile.createNewFile();

						byte[] bytearray = new byte[filesize];
						InputStream peerIS = connectToPeer.getInputStream();
						FileOutputStream fos = new FileOutputStream(newFile);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						bytesRead = peerIS.read(bytearray, 0, bytearray.length);

						currentTot = bytesRead;

						do {
							bytesRead = peerIS.read(bytearray, currentTot, (bytearray.length - currentTot));
							if (bytesRead >= 0)
								currentTot += bytesRead;
						} while (bytesRead > -1);

						bos.write(bytearray, 0, currentTot);
						bos.flush();
						bos.close();

						System.out.println("Received " + filename + " from " + peerComp[2]);
						connectToPeer.close();
					}
					break;

				}

				case 4: {
					System.out.println("Exiting...");
					pstream.println(choice);
					Thread.sleep(TIME);

					// need to send message to server that it is exiting so
					// server can remove entries

					sc.close();
					System.exit(0);
				}

				default: {
					System.out.println("Invalid choice");
					break;
				}

				}
			}

		}

	}

	@Override
	
	/*
	 *  For allowing other clients/peers to connect to download RFC(non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	
	public void run() {

		ServerSocket sSock = null;
		try {
			sSock = new ServerSocket(peerSocketEntry.getLocalPort() + 1);
			// System.out.println("Upload port "+sSock.getLocalPort());
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		Socket newPeerSocket = null;

		while (true) {

			try {
				newPeerSocket = sSock.accept();
			} catch (IOException e2) {

				e2.printStackTrace();
			}
			System.out.println("Port no: " + newPeerSocket.getLocalPort());
			System.out.println("Connected");

			BufferedReader peerReader = null;
			try {
				peerReader = new BufferedReader(new InputStreamReader(newPeerSocket.getInputStream()));

			} catch (IOException e1) {
				e1.printStackTrace();
			}

			try {
				
				PrintStream peerStream = new PrintStream(newPeerSocket.getOutputStream());
				
				String[] request = new String[3];

				for (int i = 0; i < 3; i++) {
					request[i] = peerReader.readLine();
					System.out.println(request[i]);
				}

				int rfcno = Integer.parseInt(request[0].substring("GET RFC ".length(), request[0].indexOf("P2P-CI") - 1));
				String rfctitle = rfcs.get(rfcno);

				// System.out.println("RFC title is "+rfctitle);

				File directory = new File(folderLocation);
				File[] fList = directory.listFiles();
				String filename = new String();

				for (File file : fList) {
					if (file.isFile()) {
						filename = file.getName();
						if (filename.startsWith("RFC" + rfcno))
							break;

					}
				}

				File myFile = new File(folderLocation + "/" + filename);
				byte[] bytearray = new byte[(int) myFile.length()];
				FileInputStream fis = new FileInputStream(myFile);
				BufferedInputStream bis = new BufferedInputStream(fis);
				int count;

				String statusSuccess = "P2P-CI/1.0 200 OK";

				DateFormat dateFormat = new SimpleDateFormat("E, d MMM y HH:mm:ss z");
				Date date = new Date();

				String res = "P2P-CI/1.0 " + statusSuccess + "\n" + "Date: " + dateFormat.format(date) + " " + "\n" + "OS: "
						+ System.getProperty("os.name") + "\n" + "Last-Modified: " + dateFormat.format(myFile.lastModified()) + " " + "\n"
						+ "Content-Length: " + myFile.length() + "\n" + "Content-Type: text/text" + "\n";

				peerStream.println(res);

				try {
					Thread.sleep(TIME);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				// System.out.println("Filename is "+filename);

				peerStream.println(filename);

				try {
					Thread.sleep(TIME);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				bis.read(bytearray, 0, bytearray.length);

				OutputStream os = newPeerSocket.getOutputStream();

				os.write(bytearray, 0, bytearray.length);
				os.flush();

				
				newPeerSocket.close();

			} catch (IOException e) {

				e.printStackTrace();
			}
		}

	}

	/*
	 * Method to send the RFC data in the simplified HTTP message format
	 */
	
	public static void sendRFCdata(int rfcno, Socket clientSocket) throws IOException, InterruptedException {
		String[] addrfc = new String[4];

		PrintStream pstream = new PrintStream(clientSocket.getOutputStream());

		addrfc[0] = "ADD RFC " + rfcno + " P2P-CI/1.0";
		pstream.println(addrfc[0]);

		Thread.sleep(TIME);

		addrfc[1] = "Host " + clientHostname;
		pstream.println(addrfc[1]);

		Thread.sleep(TIME);

		int tempPort = clientSocket.getLocalPort() + 1;

		addrfc[2] = "Port: " + tempPort;
		pstream.println(addrfc[2]);

		Thread.sleep(TIME);

		addrfc[3] = "Title: " + rfcs.get(rfcno);
		pstream.println(addrfc[3]);

		Thread.sleep(TIME);
	}

}
