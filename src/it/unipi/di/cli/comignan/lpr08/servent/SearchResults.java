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

import java.util.Vector;

/**
 * Raccoglie uno o più risultati di una query per fornirli esternamente ad un servent.
 * @author Michele Comignano
 */
public class SearchResults extends Vector<SearchResult> {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;
  /**
   * L'identificatore univoco del messaggio di query che ha dato luogo ai riosultati
   * ivi contenuti.
   */
  protected Message id;
  protected SearchResults(Message id) {
    this.id = id;
  }
  protected void add(QueryHitMessage hit) {
    for (int i = 0; i < hit.matches.length; i++) {
      add(new SearchResult(hit.messageId, hit.address, hit.fileServerPort, hit.matches[i],
              hit.firewalled));
    }
  }
}


