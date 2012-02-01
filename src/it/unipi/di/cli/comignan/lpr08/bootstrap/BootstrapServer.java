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

package it.unipi.di.cli.comignan.lpr08.bootstrap;

import it.unipi.di.cli.comignan.lpr08.common.BootstrapServerRemote;
import it.unipi.di.cli.comignan.lpr08.common.ServentGroup;
import it.unipi.di.cli.comignan.lpr08.common.ServentDescriptor;
import it.unipi.di.cli.comignan.lpr08.common.ServentRemote;
import it.unipi.di.cli.comignan.lpr08.common.SimpleLogger;
import it.unipi.di.cli.comignan.lpr08.servent.Servent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

/**
 * Un Bootstrap Server deve essere sempre attivo sulla rete e ha l'unico scopo 
 * di memorizzare riferimenti ad un insieme di servents considerati aﬃdabili
 * presenti sulla rete Mini-Gnutella.
 * @author Michele Comignano
 */
public class BootstrapServer extends Thread implements BootstrapServerRemote {
  /**
   * Crea un server di bootstrap.
   * @param remotePort la porta TCP su cui mettersi in ascolto.
   * @param logger il logger usato per i messaggi.
   * @throws IllegalArgumentException se la porta non è valida.
   */
  public BootstrapServer(int remotePort, SimpleLogger logger) throws
          IllegalArgumentException {
    reliableServents = new ServentGroup();
    this.logger = logger;
    this.registeredServents = new Vector<ServentRemote>();
    this.servicePort = remotePort;
    mustShutdown = false;
    initDone = false;
  }
  /**
   * Il flusso di un server di bootstrap consiste nel controllare periodicamente
   * se servents affidaili hanno cessato di esserlo (smettendo di inviare notifiche).
   */
  @Override
  public void run() {
    try {
      bootstrapServerRemote = (BootstrapServerRemote) UnicastRemoteObject.exportObject(
              (BootstrapServerRemote) this, servicePort);
    } catch (RemoteException e) {
      logger.appendError("Errore pubblicando l'oggetto remoto");
      mustShutdown = true;
    }
    initDone = true;
    while (!mustShutdown) {
      try {
        sleep(Servent.RELIABLE_TRESHOLD / 10);
      } catch (InterruptedException e) {
        continue;
      }
      synchronized (reliableServents) {
        for (int i = 0; i < reliableServents.size(); i++) {
          ServentDescriptor tmp = reliableServents.get(i);
          if (tmp.getLastSeen().getTime() < new Date().getTime() - Servent.RELIABLE_TRESHOLD) {
            reliableServents.remove(i);
            logger.appendMessage(tmp + " non è più affidabile, elimino");
          }
        }
      }
    }
  }
  /**
   * Restituisce il riferimento all'oggetto remoto.
   * @return l'oggetto remoto esportato o null se il server deve ancora partire.
   */
  public BootstrapServerRemote getRemote() {
    return bootstrapServerRemote;
  }
  /**
   * Indica se il server è in esecuzione o meno.
   * @return true se il server è in esecuzione, false altrimenti.
   */
  public boolean isRunning() {
    return !mustShutdown && bootstrapServerRemote != null;
  }
  /**
   * Dice se è stata effettuata la fase di inizializzazione. Serve alle interfacce
   * per attendere l'inizializzazione del server prima di verificare l'eventuale presenza
   * di errori.
   * @return true se è stata effetuata, false altrimenti.
   */
  public boolean getInitDone() {
    return initDone;
  }
  @Override
  public synchronized void addReliable(int connectPort) throws RemoteException {
    try {
      ServentDescriptor elem = new ServentDescriptor(InetAddress.getByName(RemoteServer.
              getClientHost()), connectPort, 0);
      boolean found = false;
      for (int i = 0; i < reliableServents.size(); i++) {
        ServentDescriptor tmp = reliableServents.get(i);
        if (tmp.equals(elem)) {
          found = true;
          tmp.touch();
          break;
        }
      }
      if (!found) {
        logger.appendMessage(elem + " dichiara di essere affidabile, lo aggiungo");
        reliableServents.add(elem);
      }
      for (int i = 0; i < registeredServents.size(); i++) {
        try {
          registeredServents.get(i).signalReliableServent(elem);
        } catch (RemoteException e) {
          registeredServents.remove(i);
        }
      }
    } catch (UnknownHostException e) {
    } catch (ServerNotActiveException e) {
    }
  }
  @Override
  public synchronized void subscribe(ServentRemote e) {
    if (!registeredServents.contains(e)) {
      registeredServents.add(e);
    }
  }
  /**
   * Indica al bootstrap server che deve fermarsi al più presto e ne attende
   * la terminazione.
   */
  public synchronized void shutdown() {
    try {
      UnicastRemoteObject.unexportObject(bootstrapServerRemote, true);
      logger.appendMessage("Oggetto remoto rimosso con successo");
    } catch (NoSuchObjectException e) {
      logger.appendError("Problema durante la rimozione dell'oggetto remoto", e);
    }
    mustShutdown = true;
    interrupt();
    try {
      join();
    } catch (InterruptedException e) {
    }
  }
  /**
   * Fornisce un iteratore con i descrittori di tutti i sevent affidabili. Da usare
   * nelle interfacce.
   * @return un iteratore con i descrittori di tutti i sevent affidabili.
   */
  public Iterator<ServentDescriptor> getReliableServents() {
    return reliableServents.iterator();
  }
  @Override
  public synchronized Vector<ServentDescriptor> getReliableServents(int n) {
    Vector<ServentDescriptor> servents =
            new Vector<ServentDescriptor>(Math.min(n, reliableServents.size()));
    for (int i = 0; i < reliableServents.size(); i++) {
      servents.add(reliableServents.get(i));
    }
    return servents;
  }
  /**
   * Indica se è stata effettuata la fase di inizializzazione.
   */
  private boolean initDone;
  /**
   * La porta su cui l'oggetto remoto è in ascolto.
   */
  private int servicePort;
  /**
   * Riferimento all'oggetto remoto fornito.
   */
  private BootstrapServerRemote bootstrapServerRemote;
  /**
   * Gruppo dei servents considerati affidabili.
   */
  private final ServentGroup reliableServents;
  /**
   * Elenco dei servents registrati per le notifiche.
   */
  private Vector<ServentRemote> registeredServents;
  /**
   * Indica se il bootstrap server deve fermarsi.
   */
  private boolean mustShutdown;
  /**
   * Il logger per i messaggi.
   */
  private SimpleLogger logger;
}
