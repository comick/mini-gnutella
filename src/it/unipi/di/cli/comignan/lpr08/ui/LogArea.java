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

package it.unipi.di.cli.comignan.lpr08.ui;

import it.unipi.di.cli.comignan.lpr08.common.SimpleLogger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.JTextArea;

/**
 * Questa classe implementa un gestore di log grafico per scrivere meno codice
 * nell'interfaccia.
 * @author Michele Comignano
 */
public class LogArea extends JTextArea implements SimpleLogger {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  private static String now() {
    return new SimpleDateFormat("kk:mm:ss").format(
            Calendar.getInstance().getTime());
  }
  @Override
  public void appendError(String err) {
    this.append("ERRORE (" + now() + "): " + err + ".\n");
  }
  @Override
  public void appendError(String err, Exception e) {
    this.append("ERRORE (" + now() + "): " + err + ":\n\t" + e + "\n");
  }
  @Override
  public void appendMessage(String msg) {
    this.append("AVVISO (" + now() + "): " + msg + ".\n");
  }
}
