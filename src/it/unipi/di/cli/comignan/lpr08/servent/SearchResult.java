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
 * Un messaggio di query hit puòcontenere più risultati. Va quindi esploso e
 * da questa operazione risultano uno o più risultati.
 * @author michele
 */
public class SearchResult {
  protected SearchResult(byte[] searchId, InetAddress address, int fileServerPort, String fileName,
          boolean firewalled) {
    this.address = address;
    this.searchId = searchId;
    this.fileServerPort = fileServerPort;
    this.fileName = fileName;
    this.firewalled = firewalled;
  }
  protected byte[] searchId;
  protected InetAddress address;
  public InetAddress getAddress() {
    return address;
  }
  public String getFileName() {
    return fileName;
  }
  public int getFileServerPort() {
    return fileServerPort;
  }
  protected int fileServerPort;
  public boolean isFirewalled() {
    return firewalled;
  }
  protected String fileName;
  protected boolean firewalled;
}