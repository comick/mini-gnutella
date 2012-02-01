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

import java.net.InetAddress;

/**
 * Il messaggio di pong che viene inviato da un servent in risposta ad un messaggio di ping.
 * @author Michele Comignano
 */
class PongMessage extends Message {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  /**
   * L'identificatore del messaggio di pong.
   */
  protected static final byte TYPE_ID = (byte) 0x01;
  /**
   * La porta tcp su cui il servent che risponde accetta connessioni entranti.
   */
  protected int connectPort;
  /**
   * La porta udp che il mittente usa per esplorare la rete.
   */
  protected int explorePort;
  /**
   * L'indirizzo dell'host che emette il messaggio.
   */
  protected InetAddress address;
  /**
   * Crea un nuovo messaggio di pong.
   * @param messageId il codice identificativo del messaggio.
   * @param connectPort la porta del servent da segnalare.
   * @param explorePort la porta di ascolto dell'esploratore di rete.
   */
  protected PongMessage(byte[] messageId, int connectPort, int explorePort) {
    super(messageId, TYPE_ID);
    this.connectPort = connectPort;
    this.explorePort = explorePort;
    // Un pong appena generato se dietro un firewall non ha idea del proprio host
    // Questo campo sarà riempito al primo approdo verso l'esterno.
    this.address = null;
  }
}
