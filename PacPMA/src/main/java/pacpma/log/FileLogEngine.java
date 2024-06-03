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
 * @author Andrea Turrini
 *
 */
public class FileLogEngine implements LogEngine {
    private static final long startTime = System.currentTimeMillis();
    
    private static int logLevel;
    private static boolean isClosed;
    
    private static BufferedWriter logbw = null;
    
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
            System.err.println("FileLogEngine: failed to open the log file " + filepath);
        }
        log(LEVEL_INFO, "FileLogEngine initialized");
    }
    
    public synchronized void log(int level, String message) {
        if (isClosed) {
            return;
        }

        if (!OptionsPacPMA.useLogging()) {
            return;
        }
        if (logbw == null) {
            throw new IllegalStateException("FileLogEngine not initialized");
        }
        if (level <= logLevel) {
            try {
                logbw.append("L" + level + " " + (System.currentTimeMillis() - startTime) + ": " + message + "\n");
            } catch (IOException ioe) {
            }
        }
    }
    
    public boolean saveToFile() {
        if (isClosed) {
            return false;
        }
        log(LEVEL_INFO, "Writing log to file");
        if (logbw == null) {
            throw new IllegalStateException("FileLogEngine not initialized");
        }
        try {
            logbw.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() {
        if (isClosed) {
            return;
        }
        try {
            logbw.close();
            isClosed = true;
        } catch (IOException ioe) {
        }
    }
}
