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
 * Costanti e procedure di utilità comune.
 * @author Michele Comignano
 */
class Util {
  /**
   * Procedure di utilità per evitare ridondanza nel codice.
   * @param t
   */
  protected static void waitHelper(Thread t) {
    if (t != null) {
      t.interrupt();
      try {
        t.join();
      } catch (InterruptedException e) {
      }
    }
  }
  /**
   * L'intervallo minimo in millisecondi che deve intercorrere tra l'invio di
   * un messaggio e l'altro relativamente al singolo thread che invia messaggi.
   */
  protected static final long PING_SENDING_INTERVAL = 10000;
  protected static final long NEW_MESSAGES_CHECK_INTERVAL = 1000;
  /**
   * Il tempo massiche che il servent deve attendere sul proprio socket TCP.
   */
  protected static final int SOCKET_SO_TIMEOUT = 5000;
}
