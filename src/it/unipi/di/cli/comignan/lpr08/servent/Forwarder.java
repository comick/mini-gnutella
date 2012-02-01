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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Il Forwarder è una classe astratta che va implementata da tutte quelle classi
 * che nel loro percorso di vita sono solite ricevere messaggi e instradarli
 * verso una qualche meta. Questo consente di riutilizzare molto codice comune
 * altersì presente nelle implementazioni delle connessioni, del servent e
 * dell'esploratore di rete.
 * @author michele
 */
public abstract class Forwarder extends Thread {
  /**
   * Inizializza le risorse.
   */
  protected Forwarder() {
    sendingQueue = new LinkedList<MessageWithTarget>();
    seen = new Hashtable<Message, Connection>();
  }
  /**
   * Il metoto deve essere implementato per fornire le operazioni specifiche necessarie
   * a inviare realmente il messaggio. Si è scelto di specificare una connessione come
   * secondo parametro perchè questa è pronta per l'invio di messaggi tcp verso il peer
   * e contiene al suo interno un descrittore con tutto ciò che serve per l'invio di
   * messaggi udp al peer. L'altra motiviazione è che sia i messaggi di esplorazione
   * che quelli di query e query hit vengono scambiati solo con i peer connessi!
   * @param msg il messaggio da inviare.
   * @param target la connessione verso il peer a cui il messaggio deve essere inviato.
   */
  abstract void send(Message msg, Connection target);
  /**
   * Inserisce un messaggio tra quelli visti assieme alla descrizione del
   * servent che lo ha inviato.
   * @param msg
   * @param origin
   */
  void setSeen(Message msg, Connection origin) {
    seen.put(msg, origin);
  }
  /**
   * Indica se un messaggio (il suo id univoco) è stato visto da questo forwarder.
   * @param msg il messaggio con l'id da verificare.
   * @return <code>true</code> se il messaggio è stato visto, <code>false</code> altrimenti.
   */
  boolean haveSeen(Message msg) {
    return seen.get(msg) != null;
  }
  void forward(Message msg, Connection dest) {
    sendingQueue.add(new MessageWithTarget(msg, dest));
  }
  Connection getDestination(Message msg) {
    return seen.get(msg);
  }
  void removeSeen(Message msg) {
    seen.remove(msg);
  }

  /**
   * Si occupa di inoltrare i messaggi di ping ricevuti da altri servent che
   * erano stati messi in coda e inviare quelli creati ad hoc.
   */
  protected class Sender extends Thread {
    @Override
    public void run() {
      while (true) {
        if (sendingQueue.size() <= 0) {
          try {
            sleep(Util.NEW_MESSAGES_CHECK_INTERVAL);
            continue;
          } catch (InterruptedException e) {
            break;
          }
        }
        MessageWithTarget msgWithRoute = sendingQueue.poll();
        Message msg = msgWithRoute.message;
        Connection target = msgWithRoute.target;
        send(msg, target);
      }
    }
  }

  /**
   * Classe di comodo per memorizzare messaggi con destinazione da mettere in coda di invio.
   */
  protected class MessageWithTarget {
    Message message;
    Connection target;
    MessageWithTarget(Message message, Connection target) {
      this.message = message;
      this.target = target;
    }
  }
  /**
   * Questa tabella hash tiene traccia dei messaggi di ping inviati o
   * inoltrati dal NetworkExplorer per scartare i messaggi di pong di cui non
   * si è mai avuta traccia. Di fatto questo è parte del sistema di routing.
   */
  private Hashtable<Message, Connection> seen;
  /**
   * La lista dei messaggi di ping da inoltrare in attesa di invio.
   */
  private Queue<MessageWithTarget> sendingQueue;
}
