import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class MultiThreadServer implements Runnable {
	Socket csocket;
	static ConcurrentHashMap<String, Integer> hosts = new ConcurrentHashMap<String, Integer>(); 
	// host and upload port number map
	
	static ConcurrentHashMap<Integer, ArrayList<String>> rfchosts = new ConcurrentHashMap<Integer, ArrayList<String>>(); 
	// map of rfcid and arraylist of hosts having that rfcid
	
	static ConcurrentHashMap<Integer, String> rfctitles = new ConcurrentHashMap<Integer, String>(); 
	// map of rfcid and rfc title
	
	static final int TIME = 200;

	MultiThreadServer(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) throws Exception {

		ServerSocket ssock = new ServerSocket(7734); // starting server at port 7734
		System.out.println("Listening");

		while (true) {
			
			// using multithreading for client requests
			
			Socket sock = ssock.accept();

			new Thread(new MultiThreadServer(sock)).start();
		}
	}

	public void run() {

		BufferedReader is;
		Boolean joinedFlag = false;
		String currentHostName = new String();
		while (true) {

			try {

				try {
					Thread.sleep(1000);

				} catch (InterruptedException ie) {

				}

				// When client is joining
				
				if (!joinedFlag) {
					int num_lines = 4;
					String peer[] = new String[num_lines];

					is = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
					peer[0] = is.readLine();

					peer[1] = is.readLine();

					peer[2] = is.readLine();

					peer[3] = is.readLine();

					System.out.println(peer[0]);
					System.out.println(peer[1]);
					System.out.println(peer[2]);
					System.out.println(peer[3]);

					String peer_host = peer[1].substring("Host: ".length(), peer[1].length());
					int port = Integer.parseInt(peer[2].substring("Port: ".length(), peer[2].length()));

					hosts.put(peer_host, port);

					currentHostName = peer_host;

					System.out.println(currentHostName + " connected");

					PrintStream pstream = new PrintStream(csocket.getOutputStream());
					pstream.println("OK");
					try {
						Thread.sleep(TIME);
					} catch (InterruptedException e) {

						e.printStackTrace();
					}
					joinedFlag = true;

					int num_rfcs = Integer.parseInt(is.readLine());
					for (int i = 0; i < num_rfcs; i++) {
						addRFC(currentHostName, false);
					}
				}

				int choice = 0;

				Scanner sc = new Scanner(csocket.getInputStream());
				choice = sc.nextInt();

				// processing client choices
				
				switch (choice) {

				case 1:
					addRFC(currentHostName, true);
					break;
				case 2:
					listAllrfcs();
					break;
				case 3:
					is = new BufferedReader(new InputStreamReader(csocket.getInputStream()));

					String[] temp = new String[4];

					for (int k = 0; k < 3; k++) {
						temp[k] = is.readLine();
						System.out.println(temp[k]);
					}

					int rfcno = Integer.parseInt(temp[0].substring("LOOKUP RFC ".length(), temp[0].length() - "P2P-CI/1.0".length() - 1));

					returnRFClookup(rfcno, csocket);
					break;
				case 4: {

					sc.close();
					clientExit(currentHostName);
					System.out.println("Client " + currentHostName + " exiting ...");
					csocket.close();

					Thread.currentThread().stop();
				}

				}

			} catch (IOException e) {
				System.out.println(e);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

	}

	public void addRFC(String currentHostName, Boolean manual) throws IOException, InterruptedException {
		BufferedReader is;
		String rfcdetails[] = new String[4];

		is = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
		rfcdetails[0] = is.readLine(); // RFC number
		rfcdetails[1] = is.readLine(); // Host
		rfcdetails[2] = is.readLine(); // Port
		rfcdetails[3] = is.readLine(); // Title

		if (manual) {
			for (int i = 0; i < 4; i++) {
				System.out.println(rfcdetails[i]);
			}
		}

		rfcdetails[0] = rfcdetails[0].substring("ADD RFC ".length(), rfcdetails[0].indexOf("P2P-CI") - 1); 
		// rfc id
																											
		rfcdetails[1] = currentHostName;
		rfcdetails[2] = rfcdetails[2].substring("Port: ".length(), rfcdetails[2].length());
		rfcdetails[3] = rfcdetails[3].substring("Title: ".length(), rfcdetails[3].length()); 
		// rfc title
																								

		System.out.println("RFC added: " + rfcdetails[0]);

		int rfcid = Integer.parseInt(rfcdetails[0]);
		ArrayList<String> tempList = new ArrayList<String>();
		if (rfchosts != null && rfchosts.get(rfcid) != null)
			tempList = rfchosts.get(rfcid);

		rfcdetails[1] = rfcdetails[1].trim();
		rfcdetails[2] = rfcdetails[2].trim();
		rfcdetails[3] = rfcdetails[3].trim();

		tempList.add(rfcdetails[1]);

		rfchosts.put(rfcid, tempList);
		rfctitles.put(rfcid, rfcdetails[3]);

		if (manual) {

			PrintStream pstream = new PrintStream(csocket.getOutputStream());

			String temp = "P2P-CI/1.0 200 OK";

			pstream.println(temp);
			Thread.sleep(TIME);

			temp = "RFC " + rfcid + " " + rfcdetails[3] + " " + rfcdetails[1] + " " + rfcdetails[2];

			pstream.println(temp);
			Thread.sleep(TIME);
		}

	}

	
	/*
	 * Shows all the rfcs present in the system
	 */
	
	public void listAllrfcs() throws IOException, InterruptedException {

		BufferedReader is = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
		String[] list = new String[3];

		for (int i = 0; i < 3; i++) {
			list[i] = is.readLine();
			// System.out.println(list[i]);
		}

		for (int i = 0; i < 3; i++) {

			System.out.println(list[i]);
		}

		PrintStream pstream = new PrintStream(csocket.getOutputStream());
		int totalLines = 0;
		ArrayList<String> rfclineformatted = new ArrayList<String>();
		for (Entry<Integer, ArrayList<String>> rfcentry : rfchosts.entrySet())

		{

			int rfcid = rfcentry.getKey();
			String rfctitle = rfctitles.get(rfcid);

			for (String rfchost : rfcentry.getValue()) {
				// System.out.println(rfchost);
				int port = hosts.get(rfchost);
				rfclineformatted.add(rfcid + " " + rfctitle + " " + rfchost + " " + port);
				// System.out.println(rfclineformatted);

				totalLines++;
			}
		}

		System.out.println(rfclineformatted);
		pstream.println(totalLines);
		Thread.sleep(TIME);

		for (String str : rfclineformatted) {
			pstream.println(str);
			Thread.sleep(TIME);
		}

	}
	
	// returning details of hosts/peers having requested RFC

	public void returnRFClookup(int rfcid, Socket csocket) throws IOException, InterruptedException {

		String rfctitle = rfctitles.get(rfcid);

		ArrayList<String> hostnames = new ArrayList<String>();

		hostnames = rfchosts.get(rfcid);

		ArrayList<Integer> ports = new ArrayList<Integer>();

		PrintStream pstream = new PrintStream(csocket.getOutputStream());

		if (hostnames == null) {
			pstream.println("P2P-CI/1.0 404 Not Found");
			Thread.sleep(TIME);
		}

		else

		{
			pstream.println(hostnames.size());
			Thread.sleep(TIME);

			for (String temp : hostnames) {
				ports.add(hosts.get(temp));
			}

			String rfclineformatted[] = new String[hostnames.size()];

			for (int i = 0; i < hostnames.size(); i++) {

				rfclineformatted[i] = "RFC number " + rfcid + " " + "RFC title " + rfctitle + " " + "hostname " + hostnames.get(i) + " "
						+ "upload port number " + ports.get(i);

				System.out.println(rfclineformatted[i]);

			}

			for (int i = 0; i < hostnames.size(); i++) {

				pstream.println(rfclineformatted[i]);
				Thread.sleep(TIME);

			}
		}

	}

	/*
	 * When client exits, first remove all the rfc entries of the client and
	 * then remove the client entry
	 */

	public void clientExit(String currentHostName) {
		for (ArrayList<String> rfcentry : rfchosts.values())

		{
			rfcentry.remove(currentHostName);
		}

		hosts.remove(currentHostName);
	}
}