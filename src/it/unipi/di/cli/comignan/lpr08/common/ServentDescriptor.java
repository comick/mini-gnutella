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

package it.unipi.di.cli.comignan.lpr08.common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

/**
 * Rappresenta il riferimento ad un servent di cui contiene i dati per la connessione.
 * @author Michele Comignano
 */
public class ServentDescriptor implements Cloneable, Serializable {
  /**
   * Numero di versione per la serializzazione.
   */
  private static final long serialVersionUID = 1L;
  /**
   * L'indirizzo di rete.
   */
  private InetAddress address;
  /**
   * Indica se l'elemento è in uso. Questo è utile quando il descrittore è parte di un
   * gruppo di descrittori di un servent.
   */
  private boolean used;
  /**
   * Indica l'ultima data in cui abbiamo appurato l'esistenza di un peer. Utile nel
   * server di bootstrap per sapere l'istante dell'ultima segnalazione di affidabilità.
   */
  private Date lastSeen;
  /**
   * La porta tcp su cui il servent è in ascolto di connessioni da altri servents.
   */
  private int connectPort;
  /**
   * La porta udp su cui il servent attende messaggi di esplorazione.
   */
  private int explorePort;
  /**
   * Crea un nuovo descrittore di servent.
   * @param address l'indirizzo di rete.
   * @param connectPort
   * @param explorePort
   */
  public ServentDescriptor(InetAddress address, int connectPort, int explorePort) {
    this.address = address;
    this.connectPort = connectPort;
    this.explorePort = explorePort;
    lastSeen = new Date();
  }
  /**
   * Restituisce l'indirizzo di rete del servent.
   * @return l'indirizzo di rete del servent.
   */
  public InetAddress getInetAddress() {
    return address;
  }
  public int getExplorePort() {
    return explorePort;
  }
  public void setExplorePort(int explorePort) {
    this.explorePort = explorePort;
  }
  /**
   * Segna lelemento come in uso.
   * @param used true se è in uso, false altrimenti.
   */
  public synchronized void setUsed(boolean used) {
    this.used = used;
  }
  public synchronized boolean isUsed() {
    return used;
  }
  /**
   * Cambia l'istante di ultima visione a quello della chiamata.
   */
  public void touch() {
    lastSeen = new Date();
  }
  public int getConnectPort() {
    return connectPort;
  }
  @Override
  public synchronized Object clone() {
    ServentDescriptor elem = new ServentDescriptor(address, connectPort, explorePort);
    if (used) {
      elem.setUsed(true);
    }
    return elem;
  }
  @Override
  public String toString() {
    return address.getHostAddress() + ":" + connectPort;
  }
  public Date getLastSeen() {
    return lastSeen;
  }
  /**
   * Nel controllo di ugualianza non viene considerata la porta per l'esplorazione perchè
   * per taluni usi può essere omessa.
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    return o.getClass() == getClass() && o.toString().equals(toString());
  }
  /**
   * Anche questo non considera la porta di esplorazione nel calcolo.
   * @return
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 47 * hash + (this.address != null ? this.address.hashCode() : 0);
    hash = 47 * hash + this.connectPort;
    return hash;
  }
}
