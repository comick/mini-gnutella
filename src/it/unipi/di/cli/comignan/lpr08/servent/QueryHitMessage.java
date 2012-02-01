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
 * Il messaggio di risposta ad un messaggio di ricerca.
 * @author Michele Comignano
 */
public class QueryHitMessage extends Message {
  /**
   * Crea un nuovo messaggio con i risultati di una ricerca nel caso il servent che lo
   * emette abbia un file server pubblicamente accessibile.
   * @param messageId
   * @param matches
   * @param fileServerPort
   */
  protected QueryHitMessage(byte[] messageId, String[] matches, int fileServerPort) {
    super(TYPE_ID);
    this.fileServerPort = fileServerPort;
    this.firewalled = false;
    this.matches = matches;
    this.address = null;
    this.messageId = messageId;
  }
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  protected InetAddress address;// TODO rimettere protetti
  protected int fileServerPort;
  protected String[] matches;
  protected int[] sizes;
  protected boolean firewalled;
  /**
   * L'identificatore del messaggio di pong.
   */
  public static final byte TYPE_ID = (byte) 0x81;
}
