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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Implementa il server per consentire lo scaricamento dei files. E' ispirato
 * ad HTTP, ma non compatibile con questo. Si limita a leggere la prima e
 * unica riga della richiesta e a servire l'eventuale file corrispondente se
 * offerto. Attende richieste get e push.
 * @author Michele Comignano
 */
public class FileServer extends Thread {
  String sharedFolder;
  ServerSocket server;
  public static final String GET = "GET ";
  public static final String PUSH = "PUSH ";
  public static final String NOT_FOUND = "NOT FOUND\n\n";
  public static final String FOUND = "FOUND\n\n";
  private Servent servent;
  private int port;
  /**
   * 
   * @param sharedFolder
   * @param servent
   * @param port
   * @throws IOException
   */
  protected FileServer(String sharedFolder, Servent servent, int port) throws IOException {
    this.servent = servent;
    this.port = port;
    this.sharedFolder = sharedFolder;
    if (!servent.firewalled) {
      server = new ServerSocket(port);
      server.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
    }
  }
  @Override
  public void run() {
    servent.logger.appendMessage("File server avviato e in attesa di richieste");
    while (!servent.mustHalt) {
      try {
        Socket client = server.accept();
        DataInputStream in = new DataInputStream(client.getInputStream());
        String request = in.readUTF();
        client.setSoTimeout(Util.SOCKET_SO_TIMEOUT);
        if (request.startsWith(GET)) {
          servent.exec.execute(new GetHandler(client, request));
        } else if (request.startsWith(PUSH)) {
          servent.exec.execute(new PushHandler(client, request));
        } else {
          client.close();
          continue;
        }

      } catch (Exception e) {
        continue;
      }
    }
    try {
      server.close();
    } catch (IOException e) {
    }
  }
  public int getSharedFilesCount() {
    return getSharedFilesNames().size();
  }
  protected void push(PushMessage push) {
    servent.exec.execute(new Pusher(push));
  }

  private class Pusher extends Thread {
    public Pusher(PushMessage msg) {
      fileName = msg.fileName;
      address = msg.target;
      fileServerPort = msg.fileServerPort;
    }
    public void run() {
      File requestedFile = new File(sharedFolder + File.separatorChar + fileName);
      try {
        Socket socket = new Socket(address, fileServerPort);
        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
        dataOut.writeUTF(PUSH + fileName);
        dataOut.flush();
        FileInputStream fileIn = new FileInputStream(requestedFile);
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[1024];
        servent.logger.appendMessage("Comincio il trasferimento di " + requestedFile + " verso " +
                address.getHostAddress() + ":" + fileServerPort);
        for (int i = 0; i != -1; i = fileIn.read(buffer)) {
          out.write(buffer, 0, i);
        }
        out.flush();
        out.close();
        socket.close();
        servent.logger.appendMessage("Trasferimento di \"" + requestedFile + "\" ultimato");
      } catch (IOException e) {
        servent.logger.appendError("Non è stato possibile effettuare il push di " + requestedFile +
                " su " + address.getHostAddress() + ":" + fileServerPort, e);
      }
    }
    String fileName;
    InetAddress address;
    int fileServerPort;
  }
  public Vector<String> getSharedFilesNames() {
    File f = new File(sharedFolder);
    Vector<String> v = new Vector<String>();
    if (f.isDirectory()) {
      File[] childs = f.listFiles();
      for (int i = 0; i < childs.length; i++) {
        if (childs[i].isFile()) {
          v.add(childs[i].getAbsolutePath().substring(servent.sharedFolder.length()));
        }
      }
    }
    return v;
  }
  public String[] getMatches(String[] keyWords) {
    Vector<String> files = getSharedFilesNames();
    Vector<String> results = new Vector<String>();
    for (int i = 0; i < files.size(); i++) {
      for (int j = 0; j < keyWords.length; j++) {
        if (files.get(i).toLowerCase().contains(keyWords[j].toLowerCase().subSequence(0,
                keyWords[j].length()))) {
          results.add(files.get(i));
        }
      }
    }
    return results.toArray(new String[0]);
  }

  /**
   * Si occupa di gestire la richesta di un file.
   */
  private class GetHandler extends Thread {
    private Socket socket;
    private String request;
    public GetHandler(Socket socket, String request) {
      this.socket = socket;
      this.request = request;
    }
    @Override
    public void run() {
      try {
        String fileName = request.substring(GET.length());
        File requestedFile = new File(sharedFolder + fileName);
        DataOutputStream out = (new DataOutputStream(socket.getOutputStream()));
        if (!requestedFile.exists() || !requestedFile.isFile()) {
          out.writeUTF(NOT_FOUND);
          out.flush();
          socket.close();
          return;
        }
        out.writeUTF(FOUND);
        out.flush();
        FileInputStream fileIn = new FileInputStream(requestedFile);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
        byte[] buffer = new byte[1024];
        servent.logger.appendMessage("Comincio il trasferimento di \"" + requestedFile + "\"");
        for (int i = 0; i != -1; i = fileIn.read(buffer)) {
          bufferedOut.write(buffer, 0, i);
          if (servent.mustHalt) {
            throw new IOException();
          }
        }
        out.flush();
        out.close();
        socket.close();
        servent.logger.appendMessage("Trasferimento di \"" + requestedFile + "\" ultimato");
      } catch (IOException e) {
      }
    }
  }

  /**
   * Gestisce una richiesta di tipo push che consiste nel ricevere il file offerto da un
   * servent coperto da firewall.
   */
  private class PushHandler extends Thread {
    private Socket socket;
    private String request;
    /**
     * Crea un nuovo gestore di push.
     * @param socket il socket su cui è stata accettata la richiesta di push.
     * @param request la richiesta precedentemente accettata.
     */
    public PushHandler(Socket socket, String request) {
      this.socket = socket;
      this.request = request;
    }
    /**
     * Il flusso di un gestore di push consiste nel creare localmente il file offerto
     * e riempirlo con i contenuti inviati finchè ne arrivano.
     */
    @Override
    public void run() {
      String fileName = request.substring(PUSH.length());
      File requestedFile = new File(sharedFolder + fileName);
      try {
        requestedFile.createNewFile();
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
                requestedFile));
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] buffer = new byte[1024];
        servent.logger.appendMessage("Comincio la ricezione di \"" + requestedFile + "\"");
        for (int i = 0; i != -1; i = in.read(buffer)) {
          out.write(buffer, 0, i);
          if (servent.mustHalt) {
            throw new IOException();
          }
        }
        out.flush();
        out.close();
        socket.close();
        servent.logger.appendMessage("Ricezione di \"" + requestedFile + "\" ultimato");
      } catch (IOException e) {
        servent.logger.appendError("Ricezione di \"" + fileName + "\" fallito", e);
      }
    }
  }
}