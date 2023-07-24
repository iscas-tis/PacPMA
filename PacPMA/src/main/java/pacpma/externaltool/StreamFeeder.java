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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * A class that simply all strings to an {@link OutputStream}, to be
 * used to feed the input to a program running in a {@link Thread}.
 * 
 * @author Andrea Turrini
 *
 */
class StreamFeeder extends Thread {
    private final OutputStream os;
    private final List<String> lines;
    private final String eof;

    public StreamFeeder(OutputStream os, List<String> lines, String eof) {
        this.os = os;
        this.lines = lines;
        this.eof = eof;
    }

    /**
     * While running, feeds all lines to the output stream provided to
     * {@link #StreamGobbler(OutputStream, List<String>, String)}; as last string,
     * it feeds {@code eof}.
     */
    @Override
    public void run() {
        if (lines != null) {
            try {
                OutputStreamWriter osw = new OutputStreamWriter(os);
                for (String line: lines) {
                    osw.write(line + "\n");
                    osw.flush();
                }
                osw.write(eof + "\n");
                osw.flush();
                
            } catch (IOException ioe) {
            }
        }
    }
}