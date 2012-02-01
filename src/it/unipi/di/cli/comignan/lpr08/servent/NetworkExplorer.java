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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import it.unipi.di.cli.comignan.lpr08.common.ServentDescriptor;
import java.net.SocketTimeoutException;

/**
 * Implementa la componente attiva (relativamente al servent che
 * la avvia) dell'esplorazione della rete, inviando ping, attendendo pong da parte
 * di altri eventuali servent e catturando, per poi inoltrare, messaggi di ping
 * e pong generati e inoltrati da servents linitrofi (con cui vi sono connessioni attive).
 * @author Michele Comignano
 */
class NetworkExplorer extends Forwarder {
  /**
   * Crea un nuovo esploratore di rete al servizio di un servent.
   * @param servent il riferimento al servent genitore.
   * @param explorePort la porta udp su cui attendere messaggi di ping e pong.
   * @throws SocketException se ci sono problemi di rete.
   */
  protected NetworkExplorer(Servent servent, int explorePort) throws SocketException {
    super();
    receiveSocket = new DatagramSocket(explorePort);
    receiveSocket.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
    sendSocket = new DatagramSocket();
    this.servent = servent;
  }
  /**
   * Il flusso principale riceve messaggi di ping e pong e li gestisce.
   * In particolare se si tratta di un messaggio di ping mai visto, mette un nuovo pong
   * in coda invio, altrimenti lo scarta. Se vede un pong che riferisce un servent
   * sconosciuto, aggiunge alla cache e crea una nuova connessione; in ogni
   * caso lo inoltra sulla rotta che il corrispondente ping seguì all'andata.
   */
  @Override
  public void run() {
    PingGenerator pingGenerator;
    Sender sender;
    (pingGenerator = new PingGenerator()).start();
    (sender = new Sender()).start();
    byte buffer[] = new byte[1024];
    Message msg;
    DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
    servent.logger.appendMessage("Esploratore di rete avviato e in ascolto sulla porta udp " +
            receiveSocket.getLocalPort());
    while (!servent.mustHalt) {
      try {
        receiveSocket.receive(pack);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(pack.getData());
        ObjectInputStream objIn = new ObjectInputStream(byteIn);
        msg = (Message) objIn.readObject();
      } catch (ClassNotFoundException e) {
        continue;
      } catch (SocketTimeoutException e) {
        continue;
      } catch (IOException e) {
        servent.logger.appendError("Errore dell'esploratore di rete", e);
        continue;
      }
      switch (msg.messageType) {
        case PingMessage.TYPE_ID:
          if (haveSeen(msg)) {
            break;
          }
          PingMessage ping = (PingMessage) msg;
          Connection origin = servent.indexedConnections.get(new ServentDescriptor(pack.getAddress(), ping.connectPort,
                  ping.explorePort));
          if (origin == null) {
            break;
          }
          setSeen(ping, origin);
          PongMessage tmpPong = new PongMessage(ping.messageId, servent.connectPort, receiveSocket.
                  getLocalPort());
          forward(tmpPong, origin);
          try {
            ping.prepareForward();
            ping.connectPort = servent.connectPort;
            ping.explorePort = receiveSocket.getLocalPort();
            synchronized (servent.connections) {
              for (int i = 0; i < servent.connections.size(); i++) {
                Connection conn = servent.connections.get(i);
                if (conn != origin) {
                  forward(ping, conn);
                }
              }
            }
          } catch (DeadMessageException e) {
          }
          break;
        case PongMessage.TYPE_ID:
          PongMessage pong = (PongMessage) msg;
          if (pong.address == null) {
            pong.address = pack.getAddress();
          }
          ServentDescriptor peer = new ServentDescriptor(pong.address, pong.connectPort,
                  pong.explorePort);
          synchronized (servent.cache) {
            int i = servent.cache.indexOf(peer);
            if (i < 0) {
              servent.logger.appendMessage("Esplorando ho scoperto \"" + peer + "\", aggiungo");
              servent.cache.add(peer);
            } else {
              peer = servent.cache.get(i);
            }
            synchronized (servent.connections) {
              if (!peer.isUsed() && servent.connections.size() < servent.connectionsLimit) {
                servent.connect(peer);
              }
            }
          }
          Connection dest = getDestination(msg);
          if (dest != null && servent.fakeConnection != dest) {
            try {
              msg.prepareForward();
              forward(msg, dest);
            } catch (DeadMessageException e) {
            }
          }
          break;
        default:
          break;
      }
    }
    Util.waitHelper(pingGenerator);
    Util.waitHelper(sender);
    sendSocket.close();
    receiveSocket.close();
  }

  /**
   * Si occupa di fabbricare nuovi messaggi di ping da inviare a tutti
   * i servents con cui sono stabilite connessioni.
   */
  private class PingGenerator extends Thread {
    @Override
    public void run() {
      PingMessage ping;
      int i;
      while (!servent.mustHalt) {
        synchronized (servent.connections) {
          for (i = 0; i < servent.connections.size() && !servent.mustHalt; i++) {
            ping = new PingMessage(servent.connectPort, receiveSocket.getLocalPort());
            forward(ping, servent.connections.get(i));
            setSeen(ping, servent.fakeConnection);
          }
        }
        try {
          sleep(Util.PING_SENDING_INTERVAL);
        } catch (InterruptedException e) {
          continue;
        }
      }
    }
  }
  @Override
  protected void send(Message msg, Connection target) {
    DatagramPacket pack;
    byte[] data;
    ObjectOutputStream objOut;
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try {
      byteOut.reset();
      objOut = new ObjectOutputStream(byteOut);
      objOut.writeObject(msg);
      objOut.flush();
      data = byteOut.toByteArray();
      pack = new DatagramPacket(data, data.length, target.peer.getInetAddress(),
              target.peer.getExplorePort());
      sendSocket.send(pack);
    } catch (IOException e) {
    }
  }
  /**
   * Il socket su cui ricevere i messaggi.
   */
  private DatagramSocket receiveSocket;
  /**
   * Il socket tramite cui si inviano i messaggi.
   */
  private DatagramSocket sendSocket;
  /**
   * Il riferimento al Servent per cui lavora il NetworkExplorer.
   */
  private Servent servent;
}
