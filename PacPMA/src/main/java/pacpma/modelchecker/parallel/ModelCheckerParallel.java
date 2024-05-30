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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pacpma.log.Logger;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;
import pacpma.options.OptionsPacPMA;

/**
 * @author Andrea Turrini
 *
 */
public class ModelCheckerParallel {
    
    private final Collection<ModelCheckerInstance> modelCheckerInstances;
    private final Range range;

    public ModelCheckerParallel(Collection<ModelCheckerInstance> modelCheckerInstances) {
        this.modelCheckerInstances = modelCheckerInstances;
        if (OptionsPacPMA.showRange()) {
            range = new Range();
        } else {
            range = null;
        }
    }
    
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException {
        Logger.log(Logger.LEVEL_INFO, "ModelCheckerParallel: starting check procedure");
        
        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: starting threads");
        modelCheckerInstances.forEach(mci -> mci.start());
        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: starting threads done");

        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: waiting for threads' termination");
        modelCheckerInstances.forEach(mci -> {
            while (true) {
                try {
                    mci.join();
                } catch (InterruptedException ie) {
                    continue;
                }
                break;
            }
        });
        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: waiting for threads' termination done");

        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: collecting threads's outcome");
        Map<Integer, ModelCheckerResult> results = new HashMap<>();
        modelCheckerInstances.forEach(mci -> {if (mci.getResults() != null) results.putAll(mci.getResults());});
        if (range != null) {
            modelCheckerInstances.forEach(mci -> {if (mci.getRange() != null) range.updateRange(mci.getRange());});
        }
        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: collecting threads's outcome done");

        Logger.log(Logger.LEVEL_INFO, "ModelCheckerWrapper: check procedure done");
        return results;
    }
    
    public Range getRange() {
        return range;
    }
}
