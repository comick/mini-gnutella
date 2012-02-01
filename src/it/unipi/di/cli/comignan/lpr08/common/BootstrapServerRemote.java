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
import java.util.Vector;

/**
 * L'interfaccia remota che deve essere implementate da un server di bootstrap.
 * @author Michele Comignano
 */
public interface BootstrapServerRemote extends Remote {
  /**
   * Aggiunge il servent chiamante all'elenco dei servents affidabili.
   * @param port la porta pubblica su cui il servent affidabile acetta connessioni.
   * @throws RemoteException
   */
  public void addReliable(int port) throws RemoteException;
  /**
   * Aggiunge il servizio remoto offerto dal servent all'elenco dei beneficiari delle
   * notifiche di nuovi servents affidabili.
   * @param e il riferimento all'oggetto remoto da aggiungere.
   * @throws RemoteException
   */
  public void subscribe(ServentRemote e) throws RemoteException;
  /**
   * Restituisce un elenco di descrittori di servent considerati affidabili.
   * @param max il numero massimo di elementi da restituire.
   * @return un array di descrittori di servent affidabili.
   * @throws RemoteException
   */
  public Vector<ServentDescriptor> getReliableServents(int max) throws RemoteException;
}
