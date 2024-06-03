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

import pacpma.options.OptionsPacPMA;

/**
 * In-memory log engine.
 * 
 * Keeps all messages sent to this log engine in memory until a {@link #flush()} or {@link #close()} call occurs.
 * 
 * @author Andrea Turrini
 *
 */
public class InmemoryLogEngine implements LogEngine {
    private static final long startTime = System.currentTimeMillis();
    
    private static int logLevel;
    private static String filePath;
    private static boolean isClosed;
    
    private static final StringBuilder log = new StringBuilder();
    
    @Override
    public void setup(int level, String filepath) {
        logLevel = level;
        filePath = filepath;
        isClosed = false;
        log(LEVEL_INFO, "InmemoryLogEngine initialized");
    }
    
    @Override
    public synchronized void log(int level, String message) {
        if (isClosed) {
            return;
        }
        if (!OptionsPacPMA.useLogging()) {
            return;
        }
        if (filePath == null) {
            throw new IllegalStateException("InmemoryLogEngine not initialized");
        }
        if (level <= logLevel) {
            log.append("L")
                .append(level)
                .append(" ")
                .append(System.currentTimeMillis() - startTime)
                .append(": ")
                .append(message)
                .append("\n");
        }
    }
    
    @Override
    public void flush() {
        if (isClosed) {
            return;
        }
        if (filePath == null) {
            throw new IllegalStateException("InmemoryLogEngine not initialized");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(log.toString());
        } catch (Exception e) {
        }
    }
    
    @Override
    public void close() {
        log(LEVEL_INFO, "InmemoryLogEngine: closing the log");
        flush();
        isClosed = true;
        log.setLength(0);
    }
}
