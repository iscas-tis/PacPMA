/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2023

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 *****************************************************************************/

package pacpma.externaltool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * A class that simply collects all lines from an {@link InputStream}, to be
 * used to get the output from a program running in a {@link Thread}.
 * 
 * @author Andrea Turrini
 *
 */
class StreamGobbler extends Thread {
    private final InputStream is;
    private final List<String> lines;

    public StreamGobbler(InputStream is) {
        this.is = is;
        lines = new LinkedList<String>();
    }

    /**
     * While running, collects all lines available from the input stream provided to
     * {@link #StreamGobbler(InputStream)}.
     */
    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ioe) {
        }
    }

    /**
     * Provides the collected lines as collected in {@link #run()}.
     * 
     * @return the collected lines
     */
    public List<String> getLines() {
        return lines;
    }
}