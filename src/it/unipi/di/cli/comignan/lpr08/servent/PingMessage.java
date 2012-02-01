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
 * Il messaggio di ping per l'esplorazione della rete.
 * @author Michele Comignano
 */
class PingMessage extends Message {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  /**
   * Il codice che identifica un messaggio di ping.
   */
  protected static final byte TYPE_ID = 0x00;
  /**
   * Crea un nuovo messaggio di ping.
   * @throws InvalidMessageException se i valori del messaggio non sono validi.
   */
  protected PingMessage(int connectPort, int explorePort) {
    super(TYPE_ID);
    this.connectPort = connectPort;
    this.explorePort = explorePort;
  }
  /**
   * La porta su cui il mittende attende messaggi di esplorazione.
   * Il protocollo Gnutella 0.4 non prevede un tale campo nei messaggi di ping,
   * poichè anche l'esplorazione avviene sull'unica connessione tcp tra i servents.
   * Qui invece quando ricevo un ping non ho idea della porta su cui ascolta il 
   * mittente, informazione necessaria per restituire un bel pong.
   */
  protected int explorePort;
  /**
   * La porta su cui il mittente accetta connessioni. Questa si è resa necessaria
   * perchè con le informazione prelevate dai messaggi di ping costruisco descrittori
   * temporanei di servent che poi confronto. La "equals" di un descrittore valuta
   * anche l'uguaglianza della porta tcp per le connessioni, da qui la necessità
   * di portare questa informazione nel messaggio di ping. Quanche qui valgono conesiderazioni
   * analoghe alle precedenti (explorePort).
   */
  protected int connectPort;
}
