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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * L'iterfaccia deve essere implementata da un servent che voglia offrire ad un bootstrap server
 * la possibilità di segnalare in modo asincrono la presenza di nuovi servent affidabili.
 * @author Michele Comignano
 */
public interface ServentRemote extends Remote {
  /**
   * Segnala la presenza di un nuovo servent nella rete.
   * @param e il descrittore del servent segnalato.
   * @throws RemoteException
   */
  public void signalReliableServent(ServentDescriptor e) throws RemoteException;
}
