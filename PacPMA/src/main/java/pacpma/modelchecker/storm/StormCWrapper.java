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

package pacpma.modelchecker.storm;

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
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;
import pacpma.options.OptionsPacPMA;

/**
 * Wrapper for the Storm model checker, interacting via Python.
 * 
 * @author Andrea Turrini
 *
 */
public class StormCWrapper implements ModelChecker {
    private final static LogEngine logEngine = OptionsPacPMA.getLogEngineInstance();
    
    private final static String RESULT_IDENTIFIER = "StormCWrapper_RESULT";
    
    private final static String FIELD_SEPARATOR = ":";
    
    private String filePath = null;
    private String modelType = null;
    private String propertyFormula = null;
    private List<Constant> constants = null;
    private Map<Integer, List<Constant>> parameterValues = null;
    private final boolean computeRange = OptionsPacPMA.showRange();
    private Range range = null;

    public StormCWrapper() {}
    
    @Override
    public ModelChecker getInstance() {
        return new StormCWrapper();
    }
    
    @Override
    public ModelChecker setModelFile(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public ModelChecker setModelType(String modelType) {
        this.modelType = modelType;
        return this;
    }

    @Override
    public ModelChecker setPropertyFormula(String propertyFormula) {
        this.propertyFormula = propertyFormula;
        return this;
    }

    @Override
    public ModelChecker setConstants(List<Constant> constants) {
        this.constants = constants;
        return this;
    }

    @Override
    public ModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    @Override
    public ModelChecker setOptions(List<String> options) {
        // we don't expect options, for now
        return this;
    }

    @Override
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException {
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: starting check procedure");
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

        List<String> command = new LinkedList<>();
        List<String> messages = new LinkedList<>();
        
        String program = OptionsPacPMA.getModelCheckerPath();
        if (program == null) {
            command.add("storm-c-wrapper");
        } else {
            command.add(program);
        }
        command.add(modelType);
        command.add(filePath);
        command.add(propertyFormula);

        {
            final StringBuilder sbc = new StringBuilder();
            constants.forEach(c -> appendConstant(sbc, c));
            command.add(sbc.toString());
        }
        
        for (Integer identifier : parameterValues.keySet()) {
            final StringBuilder sbp = new StringBuilder();
            parameterValues.get(identifier).forEach(c -> appendConstant(sbp, c));
            sbp.insert(0,identifier + FIELD_SEPARATOR);
            logEngine.log(LogEngine.LEVEL_DEBUG, "StormCWrapper: sample " + sbp.toString());
            messages.add(sbp.toString());
        }
        
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: calling actual solver");
        ToolRunner toolRunner = new ToolRunner(command, messages); 
        List<String> output = toolRunner.run();
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: calling actual solver done");
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: exit value: " + toolRunner.getExitValue());
        if (output == null) {
            throw new RuntimeException("no output returned; exit value: " + toolRunner.getExitValue());
        }
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: extracting results");
        for (String message: output) {
            logEngine.log(LogEngine.LEVEL_DEBUG, "StormCWrapper: raw result: " + message);
            if (message.startsWith(RESULT_IDENTIFIER)) {
                String[] messageSplit = message.split(FIELD_SEPARATOR);
                String result = messageSplit[2];
                ModelCheckerResult modelCheckerResult;
                if (result.equals("inf")) {
                    modelCheckerResult = new ModelCheckerResult();
                } else {
                    modelCheckerResult = new ModelCheckerResult(new BigDecimal(result));
                }
                if (computeRange) {
                    if (range == null) {
                        range = new Range(modelCheckerResult);
                    } else {
                        range.updateRange(modelCheckerResult);
                    }
                }
                results.put(Integer.valueOf(messageSplit[1]), modelCheckerResult);
            }
        }
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: extracting results done");
        if (results.size() != parameterValues.size()) {
            throw new RuntimeException("Incorrect number of results; raw output:\n" + output);            
        }
        logEngine.log(LogEngine.LEVEL_INFO, "StormCWrapper: check procedure done");
        return results;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
