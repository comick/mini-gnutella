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

import it.unipi.di.cli.comignan.lpr08.common.ServentDescriptor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Una connessione implementa il flusso di comunicazione tra due servents connessi.
 * @author Michele Comignano
 */
public class Connection extends Forwarder {
  /**
   * Prepara una richiesta di connessione al peer specificato.
   * @param servent
   * @param peer il peer verso cui tentare la richiesta di connessione.
   */
  protected Connection(Servent servent, ServentDescriptor peer) {
    this(servent, (Socket) null);
    this.peer = peer;
  }
  /**
   * Crea una nuova connessione che una volta avviata processerà una richiesta di
   * connessione da parte di un peer su un socket già accettato in precedenza.
   * @param servent
   * @param socket il socket su cui è stata accettata la connessione.
   */
  public Connection(Servent servent, Socket socket) {
    super();
    this.socket = socket;
    this.servent = servent;
    mustClose = false;
  }
  /**
   * Si occupa di processare una connessione in ingresso.
   * @param socket il socket su cui è stata accettata la richiesta.
   * @throws java.io.IOException in caso di problemi di comuniazione o richieste malformate.
   */
  private void processRequest(Socket socket) throws IOException {
    socket.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
    String request = new DataInputStream(socket.getInputStream()).readUTF();
    if (!request.startsWith(REQUEST + " ")) {
      throw new IOException();
    }
    String[] ports = request.substring(REQUEST.length() + 1).split(",");
    if (ports.length != 2) {
      throw new IOException();
    }
    int peerConnectPort;
    int peerExplorePort;
    try {
      peerConnectPort = Integer.parseInt(ports[0]);
      peerExplorePort = Integer.parseInt(ports[1]);
    } catch (NumberFormatException e) {
      throw new IOException();
    }
    peer = new ServentDescriptor(socket.getInetAddress(), peerConnectPort, peerExplorePort);
    synchronized (servent.cache) {
      int i = servent.cache.indexOf(peer);
      if (i < 0) {
        servent.cache.add(peer);
      } else {
        peer = servent.cache.get(i);
        if (peer.isUsed()) {
          throw new IOException();
        }
      }
      DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
      if (servent.connections.size() < servent.connectionsLimit) {
        dataOut.writeUTF(ACCEPTED + " " + servent.explorePort);
        peer.setUsed(true);
        servent.logger.appendMessage("Accettata richiesta di connessione da " + peer);
      } else {
        dataOut.writeUTF(REFUSED);
        mustClose = true;
      }
      dataOut.flush();
    }
  }
  /**
   * Apre una connessione e fa una richiesta.
   * @throws java.io.IOException
   */
  private void makeRequest() throws IOException {
    if (peer.isUsed()) {
      throw new IOException();
    }
    socket = new Socket(peer.getInetAddress(), peer.getConnectPort());
    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
    dataOut.writeUTF(REQUEST + " " + servent.connectPort + "," + servent.explorePort);
    dataOut.flush();
    socket.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
    String response = new DataInputStream(socket.getInputStream()).readUTF();
    if (!response.startsWith(ACCEPTED + " ")) {
      throw new IOException();
    }
    try {
      peer.setExplorePort(Integer.parseInt(response.substring(ACCEPTED.length() + 1)));
    } catch (NumberFormatException e) {
      throw new IOException();
    }
    peer.setUsed(true);
    servent.logger.appendMessage("Connessione accettata da " + peer);
  }
  /**
   * Il flusso principale di una connessione consiste nel ricevere messaggio di query
   * e query hit e agire a seconda del loro contenuto, inoltrando o prendendo opportunamente
   * atto dell'arrivo di un risultato richiesto.
   */
  @Override
  public void run() {
    try {
      if (socket != null) {
        processRequest(socket);
      } else {
        makeRequest();
      }
    } catch (IOException e) {
      return;
    }
    ObjectInputStream objIn = null;
    try {
      objOut = new ObjectOutputStream(socket.getOutputStream());
      objOut.flush();
      objIn = new ObjectInputStream(socket.getInputStream());
    } catch (IOException e) {
      servent.cache.remove(peer);
      return;
    }
    Sender sender;
    (sender = new Sender()).start();
    Message msg;
    servent.connections.add(this);
    servent.indexedConnections.put(peer, this);
    while (!servent.mustHalt && !mustClose) {
      try {
        msg = (Message) objIn.readObject();
      } catch (SocketTimeoutException e) {
        continue;
      } catch (IOException e) {
        servent.cache.remove(peer);
        break;
      } catch (ClassNotFoundException e) {
        continue;
      }
      switch (msg.messageType) {
        case PushMessage.TYPE_ID:
          Connection tmpConn = servent.seenQueryHit.get(msg);
          if (tmpConn == null) {
            break;
          }
          PushMessage push = (PushMessage) msg;
          if (push.target == null) {
            push.target = peer.getInetAddress();
          }
          if (tmpConn == servent.fakeConnection && servent.firewalled) {
            servent.fileServer.push((PushMessage) msg);
          } else {
            tmpConn.forward(msg, tmpConn);
          }
          break;
        case QueryMessage.TYPE_ID:
          QueryMessage query = (QueryMessage) msg;
          if (servent.haveSeen(query)) {
            break;
          } else {
          }
          servent.setSeen(query, this);
          try {
            query.prepareForward();
            servent.forward(msg, this);
          } catch (DeadMessageException e) {
          }
          String[] matches = servent.fileServer.getMatches(query.keyWords);
          if (matches.length > 0) {
            QueryHitMessage hit = new QueryHitMessage(query.messageId, matches,
                    servent.firewalled ? servent.connectPort : servent.fileServerPort);
            hit.firewalled = servent.firewalled;
            forward(hit, this);
            if (servent.firewalled) {
              servent.seenQueryHit.put(hit, servent.fakeConnection);
            }
          }
          break;
        case QueryHitMessage.TYPE_ID:
          Connection conn = servent.getDestination(msg);
          if (conn == null) {
            break;
          }
          QueryHitMessage hit = (QueryHitMessage) msg;
          if (hit.address == null) {
            hit.address = peer.getInetAddress();
          }
          if (conn == servent.fakeConnection) {
            if (!(hit.firewalled && servent.firewalled)) {
              servent.searches.get(hit).add(hit);
              // Se ricevo una hit mia, la metto in quelle viste
              // cos' se devo mandare una push la mando su questa connessione
              servent.seenQueryHit.put(hit, this);
            }
            break;
          }
          if (!conn.mustClose) {
            try {
              // se mi arriva una hit da forwardare,
              // dico che se arriva una push spedirla col mio socket
              if (hit.firewalled) {
                servent.seenQueryHit.put(hit, this);
              }
              msg.prepareForward();
              conn.forward(msg, conn);
            } catch (DeadMessageException e) {
            }
            break;
          }
          break;
        default:
          continue;
      }
    }
    servent.connections.remove(this);
    servent.indexedConnections.remove(this.peer);
    peer.setUsed(false);
    Util.waitHelper(sender);
    try {
      socket.close();
    } catch (IOException e) {
    }
    servent.logger.appendMessage("Chiusa la connessione con \"" + peer + "\"");
    if (!servent.mustHalt) {
      servent.cacheConnect();
    }
  }
  /**
   * Invia la query specificata nel messaggio al peer servent di questa connessione.
   * @param msg il messaggio da inviare.
   */
  void sendQueryMessage(QueryMessage msg) {
    try {
      objOut.writeObject(msg);
      objOut.flush();
    } catch (IOException e) {
      mustClose = true;
      interrupt();
    }
  }
  @Override
  protected void send(Message msg, Connection conn) {
    try {
      objOut.writeObject(msg);
      objOut.flush();
    } catch (IOException e) {
      mustClose = true;
      interrupt();
    }
  }
  /**
   * Il flusso dati in uscita della connessione.
   */
  protected ObjectOutputStream objOut = null;
  /**
   * Indica se la connessione deve terminare.
   */
  protected boolean mustClose;
  /**
   * Il socket utilizzato per la connessione.
   */
  private Socket socket;
  /**
   * Il descrittore del peer servent a cui siamo connessi.
   */
  protected ServentDescriptor peer;
  /**
   * Il servent a cui questa connessione appartiene.
   */
  private Servent servent;
  /**
   * La stringa di richiesta di connessione con un altro servent.
   */
  public static final String REQUEST = "MINI-GNUTELLA CONNECT";
  /**
   * La stringa di connessione accettata.
   */
  public static final String ACCEPTED = "MINI-GNUTELLA ACCEPTED";
  /**
   * La stringa di connessione rifiutata.
   */
  public static final String REFUSED = "MINI-GNUTELLA REFUSED";
}
