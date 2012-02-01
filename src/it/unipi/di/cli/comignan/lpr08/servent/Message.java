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

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Il generico messaggio con le caratteristiche comuni a tutti i messaggi.
 * Ogni messaggio estende questa classe.
 * @author Michele Comignano
 */
public abstract class Message implements Serializable {
  /**
   * Generatore di numeri casuali per la generazione degli identificatori.
   */
  private static Random random = new Random();
  /**
   * Il massimo numero di inoltri per un nuovo messaggio.
   */
  public static final byte DEFAULT_TTL = 7;
  /**
   * Il numero di bytes che compongono l'identificatore univoco.
   */
  private static final int ID_LENGTH = 16;
  /**
   * Un valore che identifica univocamente il messaggio all'interno della rete
   * Gnutella anche dopo inoltri successivi.
   */
  protected byte[] messageId;
  /**
   * Identifica i tipo di messaggio (ping, pong, ...).
   */
  protected byte messageType;
  /**
   * Il numero di volte che il messaggio può ancora essere inoltrato.
   */
  private byte ttl;
  /**
   * Costruisce un nuovo messaggio del tipo dato.
   * @param messageType il tipo del messaggio.
   */
  protected Message(byte messageType) {
    messageId = new byte[ID_LENGTH];
    random.nextBytes(messageId);
    try {
      messageId = MessageDigest.getInstance("SHA-256").digest(messageId);
    } catch (NoSuchAlgorithmException e) {
    }
    this.messageType = messageType;
    this.ttl = DEFAULT_TTL;
  }
  /**
   * Crea un nuovo messaggio di tipo dato che sarà identificato dall'id fornito.
   * Utile quando si riceve un messaggio di ping e si crea un messaggio di pong
   * che dovendo seguire la rotta inversa, deve avere stesso identificatore.
   * @param messageId l'identificatore univoco.
   * @param messageType il tipo del messaggio.
   */
  protected Message(byte[] messageId, byte messageType) {
    this(messageType);
    this.messageId = messageId;
  }
  /**
   * Prepara il messaggio ad essere inoltrato decrementando il ttl e lanciando
   * un opportuna eccezione de il messaggio non può più essere inoltrato.
   * @throws DeadMessageException se il messaggio non è più inoltrabile.
   */
  protected void prepareForward() throws DeadMessageException {
    if (ttl-- <= 0) {
      throw new DeadMessageException();
    }
  }
  /**
   * Ricava un intero dall'id univoco del messaggio. Nella messaggi di tipo diverso possono
   * avere lo stesso id, ad esempio uno di ping e il rispettivo messaggio di pong. In entrambi
   * i casi sarà restituito lo stesso valore e questo è utile perchè questi messaggi sono
   * usati come chiavi di tabelle hash contenenti le connessioni di destinazione o
   * provenienza.
   * @return
   */
  @Override
  public int hashCode() {
    int ret = 0;
    for (int i = 0; i < messageId.length; i++) {
      ret += messageId[i];
    }
    return ret;
  }
  /**
   * Segenedo l'idea della hashCode due messaggi anche di tipo diverso risulteranno uguali
   * se hanno lo stesso identificatore univoco.
   * @param obj
   * @return
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Message)) {
      return false;
    }
    return ((Message) obj).hashCode() == hashCode();
  }
}
