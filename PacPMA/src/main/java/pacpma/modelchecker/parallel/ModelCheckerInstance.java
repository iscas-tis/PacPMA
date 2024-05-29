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

package pacpma.modelchecker.parallel;

import java.util.Map;

import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;

/**
 * @author Andrea Turrini
 *
 */
public class ModelCheckerInstance extends Thread {
    
    private final ModelChecker modelChecker;
    private Map<Integer, ModelCheckerResult> results = null;
    private Range range = null;
    
    public ModelCheckerInstance(ModelChecker modelChecker) {
        this.modelChecker = modelChecker;
    }

    @Override
    public void run() {
        results = modelChecker.check();
        range = modelChecker.getRange();
    }
    
    public Map<Integer, ModelCheckerResult> getResults() {
        return results;
    }
    
    public Range getRange() {
        return range;
    }
}
