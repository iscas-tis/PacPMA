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

package pacpma;

import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.options.OptionsPacPMA;

/**
 * PAC model main class
 * 
 * @author Andrea Turrini
 */
public final class PacPMA {
    public static void main(String[] args) {
        if (OptionsPacPMA.parseOptions(args)) {
            LogEngine logEngineInstance = OptionsPacPMA.getLogEngineInstance();
            logEngineInstance.setup(OptionsPacPMA.getLogLevel(), OptionsPacPMA.getLogFile());
            
            {
                StringBuffer clo = new StringBuffer();
                clo.append("PacPMA: command line arguments: [");
                boolean isFirst = true;
                for (String arg : args) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        clo.append(", ");
                    }
                    clo.append(arg);
                }
                clo.append(']');
                logEngineInstance.log(LogEngine.LEVEL_INFO, clo.toString());
            }
            
            Approach approach = OptionsPacPMA.getApproachInstance();
            approach.doAnalysis();
            logEngineInstance.close();
        }
    }
}
