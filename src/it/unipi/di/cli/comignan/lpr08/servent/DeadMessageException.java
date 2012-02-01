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
 * Un eccezione da sollevare quando un messaggio non è più inoltrabile.
 * @author Michele Comignano
 */
class DeadMessageException extends Exception {
  /**
   * Serial Version ID 
   */
  private static final long serialVersionUID = 1L;
  /**
   * Crea una nuova eccezione.
   */
  protected DeadMessageException() {
    super();
  }
}
