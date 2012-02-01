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

/**
 * Il messaggio di ricerca per inoltrare la ricerca di una stringa attraverso la rete.
 * @author Michele Comignano
 */
class QueryMessage extends Message {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  /**
   * L'identificatore del messaggio di pong.
   */
  public static final byte TYPE_ID = (byte) 0x80;
  /**
   * La stringa di ricerca per cui cercare corrispondenze.
   */
  protected String[] keyWords;
  /**
   * Crea un nuovo messaggio di query con ttl di dafault.
   * @param keyWords la chiave di ricerca.
   */
  public QueryMessage(String[] keyWords) {
    super(TYPE_ID);
    this.keyWords = keyWords;
  }
}
