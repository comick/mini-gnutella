/* This file is part of Mini-Gnutella.
 * Copyright (C) 2010  Michele Comignano
 *
 * Foobar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.unipi.di.cli.comignan.lpr08.servent;

import it.unipi.di.cli.comignan.lpr08.common.BootstrapServerRemote;
import it.unipi.di.cli.comignan.lpr08.common.ServentGroup;
import it.unipi.di.cli.comignan.lpr08.common.ServentDescriptor;
import it.unipi.di.cli.comignan.lpr08.common.ServentRemote;
import it.unipi.di.cli.comignan.lpr08.common.SimpleLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * Il Servent rappresenza la principale entità nella rete Mini-Gnutella, esso agisce sia da server 
 * che da client e può entrare dinamicamente a far parte della overlay network. Questa classe
 * impementa la logica di un servent da usare nella costruzione di un interfaccia utente.
 * @author Michele Comignano
 */
public class Servent extends Forwarder implements ServentRemote {
  /**
   * Crea un nuovo Servent con la cache data e avvia i servizi essenziali per il funzionamento del
   * Servent.
   * @param cache la cache con i servents conosciuti dall'ultima connessione.
   * @param explorePort la porta udp per lo scambio di pacchetti di ping e pong.
   * @param connectPort la porta tcp per le connessioni agli altri servents.
   * @param fileServerPort la porta di ascolto del file server.
   * @param sharedFolder il percorso della cartella condivisa.
   * @param connectionsLimit il nassimo numero di connessioni da stabilire.
   * @param logger un logger da usare per mostrare i messaggi.
   * @param firewalled indica se il servent è dietro un firewall.
   * @throws IllegalArgumentException se sharedFolder non è una cartella.
   */
  public Servent(ServentGroup cache, int connectPort, int explorePort, int fileServerPort,
          String sharedFolder, int connectionsLimit, SimpleLogger logger, boolean firewalled) {
    this.sharedFolder = sharedFolder;
    this.cache = cache;
    this.logger = logger;
    this.connectionsLimit = connectionsLimit;
    this.connectPort = connectPort;
    this.explorePort = explorePort;
    this.fileServerPort = fileServerPort;
    seenBSs = new LinkedList<BootstrapServerRemote>();
    exec = Executors.newCachedThreadPool();
    initDone = false;
    mustHalt = true;
    this.firewalled = firewalled;
    connections = new LinkedList<Connection>();
    indexedConnections = new Hashtable<ServentDescriptor, Connection>();
    fakeConnection = new Connection((Servent) null, (ServentDescriptor) null);
    searches = new Hashtable<Message, SearchResults>();
    seenQueryHit = new Hashtable<Message, Connection>();
  }
  @Override
  public void run() {
    mustHalt = false;
    ServerSocket serverSocket = null;
    fileServer = null;
    Thread reliableManager = null, queryMessageForwarder = null, networkExplorer = null;
    try {
      fileServer = new FileServer(sharedFolder, this, fileServerPort);
      if (!firewalled) {
        serverSocket = new ServerSocket(connectPort);
        serverSocket.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
        (networkExplorer = new NetworkExplorer(this, explorePort)).start();
        (reliableManager = new ReliableManager()).start();
        fileServer.start();
      }
      (queryMessageForwarder = new Sender()).start();
    } catch (IOException e) {
      logger.appendError("Impossibile avviare il servent", e);
      mustHalt = true;
    }
    if (!mustHalt) {
      cacheConnect();
      logger.appendMessage("Servent avviato");
    }
    initDone = true;
    while (!mustHalt) {
      try {
        if (!firewalled) {
          Socket dataSocket = serverSocket.accept();
          exec.execute(new Connection(this, dataSocket));
        } else {
          sleep(Util.SOCKET_SO_TIMEOUT);
        }
      } catch (IOException e) {
        continue;
      } catch (InterruptedException e) {
        break;
      }
    }
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException e) {
      }
    }
    Util.waitHelper(reliableManager);
    Util.waitHelper(networkExplorer);
    Util.waitHelper(queryMessageForwarder);
    Util.waitHelper(fileServer);
    for (int i = 0; i < connections.size(); i++) {
      connections.get(i).interrupt();
    }
    exec.shutdown();
    try {
      if (!exec.awaitTermination(10, TimeUnit.SECONDS)) {
        exec.shutdownNow();
      }
    } catch (InterruptedException e) {
    }
  }
  /**
   * Restituisce i descrittori dei servents con cui questo ha delle connessioni in corso.
   * @return i descrittori dei servents con cui questo ha delle connessioni in corso.
   */
  public LinkedList<ServentDescriptor> getConnectedServents() {
    LinkedList<ServentDescriptor> conectedServents = new LinkedList<ServentDescriptor>();
    synchronized (connections) {
      for (int i = 0; i < connections.size(); i++) {
        ServentDescriptor elem = connections.get(i).peer;
        if (elem.isUsed()) {
          conectedServents.add(elem);
        }
      }
    }
    return conectedServents;
  }
  public Vector<String> getSharedFilesNames() {
    return fileServer.getSharedFilesNames();
  }
  /**
   * Indica se dopo la partenza del servent si è conclusa la prima fase.
   * @return true se dopo la partenza del servent si è conclusa la prima fase, false altrimenti.
   */
  public boolean isInitDone() {
    return initDone;
  }
  public void download(SearchResult res) {
    if (res.firewalled) {
      PushMessage push = new PushMessage(res.searchId, res.fileName, fileServerPort);
      Connection conn = seenQueryHit.get(push);
      conn.send(push, conn);
    } else {
      exec.execute(new Downloader(this, res.address, res.fileServerPort, res.fileName));
    }
  }
  /**
   * Scorre la cache e prova a collegarsi a tutti i servent non connessi.
   */
  protected void cacheConnect() {
    synchronized (cache) {
      for (int i = 0; i < cache.size(); i++) {
        ServentDescriptor descr = cache.get(i);
        if (connections.size() >= connectionsLimit) {
          break;
        } else if (!descr.isUsed()) {
          connect(cache.get(i));
        }
      }
    }
  }
  /**
   * Esegue la fase di bootstrap del servent dove si cerca di riempire la cache
   * locale con i riferimenti ai servents restituiti dal bootstrap server stabilendo anche
   * qualche connessione.
   * @param bs il bootstrap server da usare.
   * @throws RemoteException in caso di problemi di comunicazione con il bootstrap server.
   */
  public void bootstrap(BootstrapServerRemote bs) throws RemoteException {
    int received = 0;
    Vector<ServentDescriptor> elems = bs.getReliableServents(connectionsLimit * 2);
    ServentDescriptor peer;
    for (int i = 0; i < elems.size(); i++) {
      synchronized (cache) {
        peer = elems.get(i);
        if (!this.equals(peer) && !cache.contains(peer)) {
          logger.appendMessage("Ricevuto \"" + peer + "\" dal bootstrap server");
          cache.add(peer);
          received++;
        }
      }
    }
    if (!seenBSs.contains(bs)) {
      seenBSs.add(bs);
    }
    logger.appendMessage("Bootstrap ultimato, ricevuti " + received + " nuovi servents");
    cacheConnect();
  }
  /**
   * Indica se il servent corrisponde al descrittore dato.
   * @param descr il descrittore da verificare.
   * @return true se si, false se no.
   */
  protected boolean equals(ServentDescriptor descr) {
    InetAddress localHost = null;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      return false;
    }
    return descr.getInetAddress().equals(localHost) && descr.getConnectPort() == connectPort;
  }
  /**
   * Indica se il servent è in esecuzione o meno.
   * @return true se il servent è in esecuzione, false altrimenti.
   */
  public boolean isRunning() {
    return !mustHalt;
  }
  protected void connect(ServentDescriptor peer) {
    exec.execute(new Connection(this, peer));
  }
  public void connect(String host, int connectPort) {
    InetAddress address;
    try {
      address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      return;
    }
    ServentDescriptor e = new ServentDescriptor(address, connectPort, 0);
    int i = cache.indexOf(e);
    if (i <= 0) {
      cache.add(e);
    } else if ((e = cache.get(i)).isUsed()) {
      logger.appendError("Impossibile connettersi a \"" + e + "\", connessione già attiva");
      return;
    }
    connect(e);
  }
  /**
   * Registra il servent ad un bootstrap server per ricevere notifiche.
   * @param bs il server di bootstrap presso cui registrarsi.
   * @param port la porta su cui pubblicare il servizio per le callback.
   * @throws RemoteException in caso di problemi.
   */
  public void subscribe(BootstrapServerRemote bs, int port) throws RemoteException {
    if (servent == null) {
      servent = (ServentRemote) UnicastRemoteObject.exportObject(this, port);
    }
    logger.appendMessage("Registrato al bootstrap server per ricevere notifiche");
    bs.subscribe(servent);
  }
  @Override
  public void signalReliableServent(ServentDescriptor peer) {
    if (!this.equals(peer) && !cache.contains(peer)) {
      logger.appendMessage("Un bootstrap server segnala in modo asincrono \"" + peer + "\"");
      cache.add(peer);
      if (connections.size() < connectionsLimit) {
        connect(peer);
      }
    }
  }
  /**
   * Invita il servent a terminare con gentilezza.
   */
  public void shutdown() {
    mustHalt = true;
  }
  /**
   * Invia un a richiesta di ricerca su tutte le connessioni attive.
   * @param query
   * @return un vettore di hit che viene riempito man mano che arrivano risultati.
   */
  public SearchResults search(String query) {
    String[] keyWords = query.split(" ");
    QueryMessage msg = new QueryMessage(keyWords);
    SearchResults results = new SearchResults(msg);
    searches.put(msg, results);
    synchronized (connections) {
      int i;
      for (i = 0; i < connections.size(); i++) {
        connections.get(i).sendQueryMessage(msg);
      }
      if (i > 0) {
        setSeen(msg, fakeConnection);
      }
    }
    return results;
  }
  public void stopSearch(SearchResults results) {
    removeSeen(results.id);
    searches.remove(results.id);
  }

  /**
   * Il gestore dell'affidabilità si occupa di segnalare ai bootstrap servers visti dal servent
   * l'affidabilità dello stesso non appena il suo up time superi la soglia per essere considerato
   * tale. La segnalazione viene rifatta in modo che il bootstrap server sappia che il servent è
   * ancora online.
   */
  private class ReliableManager extends Thread {
    @Override
    public void run() {
      try {
        sleep(RELIABLE_TRESHOLD);
      } catch (InterruptedException e) {
      }
      logger.appendMessage("Sono ora affidabile");
      while (!mustHalt) {
        synchronized (seenBSs) {
          for (int i = 0; i < seenBSs.size(); i++) {
            BootstrapServerRemote bs = seenBSs.get(i);
            try {
              bs.addReliable(connectPort);
            } catch (RemoteException e) {
              seenBSs.remove(bs);
            }
          }
        }
        try {
          sleep((long) (RELIABLE_TRESHOLD * 0.6));
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }
  @Override
  void send(Message msg, Connection origin) {
    for (int i = 0; i < connections.size(); i++) {
      Connection conn = connections.get(i);
      if (origin != conn) {
        conn.sendQueryMessage((QueryMessage) msg);
      }
    }
  }
  /**
   * Contiene l'elenco dei bootstrap server visti per segnalare ad ognuno la propria affidabilità.
   */
  private final LinkedList<BootstrapServerRemote> seenBSs;
  /**
   * L'uptime minimo di un servent per essere considerato affidabile (in millisecondi).
   */
  public static final long RELIABLE_TRESHOLD = 30000;
  /**
   * La porta su cui di default si accettano connessioni.
   */
  public static final int DEFAULT_TCP_PORT = 6346;
  private boolean initDone;
  /**
   * Indica se il servent è spento in fase di spegnimento.
   */
  protected boolean mustHalt;
  /**
   * Il numero massimo di connessioni che un servent può effettuare verso gli
   * altri servent. Viene stabilito al primo avvio del servent ed è indicato
   * come MC nella specifica del progetto.
   */
  protected int connectionsLimit;
  /**
   * La porta UDP usata dal server per i messaggi di ping e pong.
   */
  protected int explorePort;
  /**
   * La porta tcp che sarà usata per ricevere richieste di ricerca.
   */
  protected int connectPort;
  /**
   * La porta su cui dovrà mettersi in ascolta il file server.
   */
  protected int fileServerPort;
  /**
   * La cache interna del Servent.
   */
  protected final ServentGroup cache;
  /**
   * Il servizio per eseguire i vari thread del Servent.
   */
  protected ExecutorService exec;
  /**
   * Il logger generale su cui annotare gli eventi salienti.
   */
  protected SimpleLogger logger;
  /**
   * Le connessioni stabilite dal servent da usare quando si vogliono scorrere tutte.
   */
  final LinkedList<Connection> connections;
  /**
   * Elenco ad accesso veloce (sperabilmente costante) delle connessioni attive.
   */
  protected Hashtable<ServentDescriptor, Connection> indexedConnections;
  /**
   * Il servizio per le callback associato al servent.
   */
  private ServentRemote servent;
  /**
   * I files condivisi.
   */
  protected String sharedFolder;
  /**
   * Riferimento al file server al serivizo di questo servent.
   */
  protected FileServer fileServer;
  /**
   * Connessione farlocca usata come origine per i messaggi generati dal questo
   * servent.
   */
  protected Connection fakeConnection;
  /**
   * Tiene traccia di tutte le ricerche attive (i cui risultati vengono ancora raccolti)
   * indicizzate con l'id univoco del messaggio di searchId.
   */
  protected Hashtable<Message, SearchResults> searches;
  protected boolean firewalled;
  /**
   * Questa tabella di hash è utilizzata dal servent e dalle connessioni per
   * l'implementazione della rotta inversa dei messaggi di push.
   */
  protected Hashtable<Message, Connection> seenQueryHit;
}
