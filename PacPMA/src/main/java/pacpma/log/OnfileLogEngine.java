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

package pacpma.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import pacpma.options.OptionsPacPMA;

/**
 * On-file log engine.
 * 
 * Saves immediately all messages sent to this log engine on file.
 * 
 * @author Andrea Turrini
 *
 */
public class OnfileLogEngine implements LogEngine {
    private static final long startTime = System.currentTimeMillis();
    
    private static int logLevel;
    private static boolean isClosed;
    
    private static BufferedWriter logbw = null;
    
    @Override
    public void setup(int level, String filepath) {
        if (!OptionsPacPMA.useLogging()) {
            return;
        }
       
        logLevel = level;
        isClosed = false;
        
        try {
            logbw = new BufferedWriter(new FileWriter(filepath));
        } catch (IOException ioe) {
            logbw = null;
            System.err.println("OnfileLogEngine: failed to open the log file " + filepath);
        }
        log(LEVEL_INFO, "OnfileLogEngine initialized");
    }
    
    @Override
    public synchronized void log(int level, String message) {
        if (isClosed) {
            return;
        }
        if (logbw == null) {
            throw new IllegalStateException("OnfileLogEngine not initialized");
        }
        if (level <= logLevel) {
            try {
                logbw.write("L" + level + " " + (System.currentTimeMillis() - startTime) + ": " + message + "\n");
                logbw.flush();
            } catch (IOException ioe) {
            }
        }
    }
    
    @Override
    public void flush() {
        if (isClosed) {
            return;
        }
        if (logbw == null) {
            throw new IllegalStateException("OnfileLogEngine not initialized");
        }
        try {
            logbw.flush();
        } catch (Exception e) {
        }
    }
    
    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        if (logbw == null) {
            throw new IllegalStateException("OnfileLogEngine not initialized");
        }
        log(LEVEL_INFO, "OnfileLogEngine: closing the log");
        try {
            logbw.close();
            isClosed = true;
        } catch (IOException ioe) {
        }
    }
}
