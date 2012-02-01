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
 * Un messaggio di push è quello che un servent invia tramite rotta inversa ad una
 * questy hit di un servent coperto da un firewall che però possiede il file voluto
 * e non può offrirlo pubblicamente.
 * @author Michele Comignano
 */
public class PushMessage extends Message {
  protected PushMessage(byte[] messageId, String fileName, int fileServerPort) {
    super(messageId, TYPE_ID);
    this.fileName = fileName;
    target = null;
    this.fileServerPort = fileServerPort;
  }
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  /**
   * Il codice che identifica un messaggio di push.
   */
  protected static final byte TYPE_ID = 0x40;
  /**
   * L'indirizzo di rete del servent che genera il messaggio, quello verso
   * cui il ricevitore ultimo dovrà pushare il file richiesto.
   */
  protected InetAddress target;
  protected int fileServerPort;
  protected String fileName;
}
