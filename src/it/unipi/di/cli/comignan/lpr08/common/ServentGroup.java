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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Rappresenta un gruppo di servents da usarsi nei servents e nel bootstrap
 * server dove vi sia necessità di gestire i riferimenti a più servents, come in
 * una cache o in un'elenco di servents affidabili. Si tratta di una normale lista
 * concatenata con aggiunta la possibilità di creazione a partire da un file di cache
 * e il salvataggio.
 * @author Michele Comignano
 */
public class ServentGroup extends LinkedList<ServentDescriptor> implements Serializable {
  /**
   * Servial version UID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Crea un nuovo gruppo di servents leggendo da un file precedentemente
   * generato.
   * @param fileName la posizione del file di cache all'interno del filesystem.
   * @throws IOException in caso di problemi nell'apertura del file.
   * @throws ClassNotFoundException in caso di problemi nella deserializzazione.
   */
  public ServentGroup(String fileName) throws IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
    ServentGroup tmp = (ServentGroup) in.readObject();
    while (!tmp.isEmpty()) {
      ServentDescriptor descr = (ServentDescriptor) tmp.poll().clone();
      descr.setUsed(false);
      this.add(descr);
    }
    in.close();
  }
  /**
   * Inizializza un elenco di servents vuoto.
   */
  public ServentGroup() {
    super();
  }
  /**
   * Salva il gruppo di servents presente al momento dell'invocazione nel file
   * specificato.
   * @param fileName il percorso del file su cui scrivere l'oggetto.
   * @throws java.io.IOException in caso di problemi di I/O.
   * @throws java.lang.ClassNotFoundException in caso di problemi di serializzazione.
   */
  public synchronized void save(String fileName) throws IOException,
          ClassNotFoundException {
    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fileName));
    os.writeObject(this);
    os.close();
  }
}
