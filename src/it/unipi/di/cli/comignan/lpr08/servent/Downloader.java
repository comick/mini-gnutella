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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Helper del servent per lo scaricamento dei files.
 * @author michele
 */
class Downloader extends Thread {
  protected Downloader(Servent servent, InetAddress address, int port, String fileName) {
    this.servent = servent;
    this.port = port;
    this.fileName = fileName;
    this.address = address;
  }
  @Override
  public void run() {
    Socket socket = null;
    try {
      socket = new Socket(address, port);
      DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
      dataOut.writeUTF(FileServer.GET + fileName);
      dataOut.flush();
      DataInputStream dataIn = new DataInputStream(socket.getInputStream());
      String response = dataIn.readUTF();
      if (!response.equals(FileServer.FOUND)) {
        servent.logger.appendError("File non trovato: \"" + address.getHostAddress() + ":" + port +
                "/" + fileName + "\"");
        socket.close();
        return;
      }
      servent.logger.appendMessage("Avvio lo scaricamento di: \"" + address.getHostAddress() + ":" +
              port + "/" + fileName + "\"");
      File requestedFile = new File(servent.sharedFolder + File.separatorChar + fileName);
      requestedFile.createNewFile();
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(requestedFile));
      byte[] buffer = new byte[1024];
      int read;
      while ((read = dataIn.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      out.flush();
      out.close();
      servent.logger.appendMessage("Terminato lo scaricamento di: \"" + address.getHostAddress() +
              ":" + port + "/" + fileName + "\"");
    } catch (IOException e) {
      servent.logger.appendError("Lo scaricamento di: \"" + address.getHostAddress() + ":" + port +
              "/" + fileName + "\" è stato interrotto", e);
    }
    try {
      socket.close();
    } catch (IOException e) {
    }
  }
  private Servent servent;
  private InetAddress address;
  private int port;
  private String fileName;
}
