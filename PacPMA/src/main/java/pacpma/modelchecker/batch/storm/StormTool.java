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

package pacpma.modelchecker.batch.storm;

import static pacpma.util.Util.appendConstant;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.algebra.Constant;
import pacpma.externaltool.ToolRunner;
import pacpma.log.LogEngine;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;
import pacpma.modelchecker.batch.BatchModelChecker;
import pacpma.options.OptionsPacPMA;

/**
 * Wrapper for the Storm model checker, interacting as an external tool.
 * 
 * @author Andrea Turrini
 *
 */
public class StormTool implements BatchModelChecker {
    private final static LogEngine logEngine = OptionsPacPMA.getLogEngineInstance();
    
    private final static String RESULT = "Result (for initial states):";
    
    private String filePath = null;
    private String modelType = null;
    private String propertyFormula = null;
    private List<Constant> constants = null;
    private Map<Integer, List<Constant>> parameterValues = null;
    private List<String> options = null;
    private final boolean computeRange = OptionsPacPMA.showRange();
    private Range range = null;

    public StormTool() {}
    
    @Override
    public BatchModelChecker setModelFile(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public BatchModelChecker setModelType(String modelType) {
        this.modelType = modelType;
        return this;
    }

    @Override
    public BatchModelChecker setPropertyFormula(String propertyFormula) {
        this.propertyFormula = propertyFormula;
        return this;
    }

    @Override
    public BatchModelChecker setConstants(List<Constant> constants) {
        this.constants = constants;
        return this;
    }

    @Override
    public BatchModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    @Override
    public BatchModelChecker setOptions(List<String> options) {
        this.options = options;
        return this;
    }

    @Override
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException {
        logEngine.log(LogEngine.LEVEL_INFO, "StormTool: starting check procedure");
        if (filePath == null) {
            throw new IllegalStateException("Model file not specified");
        }
        if (!(new File(filePath).canRead())) {
            throw new IllegalStateException("The model file " + filePath + " cannot be read");
        }
        if (modelType == null) {
            throw new IllegalStateException("Model type not specified");
        }
        if (propertyFormula == null) {
            throw new IllegalStateException("Property formula not specified");
        }

        Map<Integer, ModelCheckerResult> results = new HashMap<>();
        
        for (Integer identifier : parameterValues.keySet()) {
            List<Constant> singleParameters = parameterValues.get(identifier);
            
            List<String> command = new LinkedList<>();
            String program = OptionsPacPMA.getModelCheckerPath();
            if (program == null) {
                command.add("storm");
            } else {
                command.add(program);
            }
            command.add("--" + modelType);
            command.add(filePath);
            command.add("--prop");
            command.add(propertyFormula);
            if (modelType.equals(OptionsPacPMA.MODELTYPE_PRISM)) {
                command.add("--prismcompat");
            }
            StringBuilder sb = new StringBuilder();
            constants.forEach(c -> appendConstant(sb,c));
            singleParameters.forEach(c -> appendConstant(sb,c));
            if (sb.length() > 0) {
                command.add("--constants");
                command.add(sb.toString());
            }
            logEngine.log(LogEngine.LEVEL_DEBUG, "StormTool: sample " + sb.toString());
            if (options != null) {
                options.forEach((o) -> command.add(o));
            }
            
            logEngine.log(LogEngine.LEVEL_INFO, "StormTool: calling actual solver");
            ToolRunner toolRunner = new ToolRunner(command); 
            List<String> output = toolRunner.run();
            logEngine.log(LogEngine.LEVEL_INFO, "StormTool: calling actual solver done");
            logEngine.log(LogEngine.LEVEL_INFO, "StormTool: exit value: " + toolRunner.getExitValue());
            if (output == null) {
                throw new RuntimeException("no output returned; exit value: " + toolRunner.getExitValue());
            }
            logEngine.log(LogEngine.LEVEL_INFO, "StormTool: extracting result");
            boolean hasFailed = true;
            for (String message : output) {
                logEngine.log(LogEngine.LEVEL_DEBUG, "StormTool: raw result: " + message);
                if (message.startsWith(RESULT)) {
                    String result = message.substring(RESULT.length()).trim();
                    ModelCheckerResult modelCheckerResult;
                    if (result.equals("inf")) {
                        modelCheckerResult = new ModelCheckerResult();
                    } else {
                        modelCheckerResult = new ModelCheckerResult(new BigDecimal(result));
                    }
                    if (computeRange) {
                        if (range == null) {
                            range = new Range(modelCheckerResult, singleParameters);
                        } else {
                            range.updateRange(modelCheckerResult, singleParameters);
                        }
                    }
                    results.put(identifier, modelCheckerResult);
                    hasFailed = false;
                }
            }
            if (hasFailed) {
                throw new RuntimeException("Failed execution; raw output:\n" + output);            
            }
            logEngine.log(LogEngine.LEVEL_INFO, "StormTool: extracting result done");
        }
        logEngine.log(LogEngine.LEVEL_INFO, "StormTool: check procedure done");
        return results;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
