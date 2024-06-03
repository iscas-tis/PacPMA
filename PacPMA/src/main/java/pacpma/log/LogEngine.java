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

/**
 * @author Andrea Turrini
 *
 */
public interface LogEngine {
    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_ERROR = LEVEL_NONE + 1;
    public static final int LEVEL_WARNING = LEVEL_ERROR + 1;
    public static final int LEVEL_INFO = LEVEL_WARNING + 1;
    public static final int LEVEL_DEBUG = LEVEL_INFO + 1;
    public static final int LEVEL_ALL = LEVEL_DEBUG + 1;

    /**
     * Sets the level of the log messages to keep.
     * 
     * @param level
     *            the logging level
     * @param filepath
     *            the file path where to store the logged messages
     */
    public void setup(int level, String filepath);
    
    /**
     * Stores the message in the log, provided that its level is at least the one
     * set by the last call to {@link #setLevel(int)}.
     * 
     * @param level
     *            the logging level of the message
     * @param message
     *            the message
     */
    public void log(int level, String message);
    
    /**
     * Saves the logged messages into the provided file.
     * 
     * @param filepath
     *            the file path
     * @return whether the log has been saved to file without raising errors
     */
    public boolean saveToFile();
    
    /**
     * Closes the log.
     * 
     * The fate of messages sent to the log after closing it is undefined.
     */
    
    public void close();
}
