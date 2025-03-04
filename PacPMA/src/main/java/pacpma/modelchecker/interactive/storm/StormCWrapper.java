/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2025

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

package pacpma.modelchecker.interactive.storm;

import static pacpma.util.Util.appendConstant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import pacpma.algebra.Constant;
import pacpma.log.LogEngine;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;
import pacpma.modelchecker.interactive.InteractiveModelChecker;
import pacpma.options.OptionsPacPMA;

/**
 * @author Andrea Turrini
 *
 */
public class StormCWrapper implements InteractiveModelChecker, ModelChecker {
    private enum Stage {
        INITIALIZE,
        STARTED,
        STOPPED
    }
    
    private final static LogEngine logEngineInstance = OptionsPacPMA.getLogEngineInstance();
    
    private final static String RESULT_IDENTIFIER = "StormCWrapper_RESULT";
    
    private final static String EOF = "EOF";
    
    private final static String FIELD_SEPARATOR = ":";
    
    private Stage stage = Stage.INITIALIZE;
    
    private String filePath = null;
    private String modelType = null;
    private String propertyFormula = null;
    private List<Constant> constants = null;
    private final boolean computeRange = OptionsPacPMA.showRange();
    private Range range = null;
    
    private Process modelCheckerProcess = null;
    private OutputStreamWriter modelCheckerInput = null;
    private BufferedReader modelCheckerOutput = null;
    
    private int identifier = 0;

    public StormCWrapper() {}
    
    @Override
    public InteractiveModelChecker setModelFile(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public InteractiveModelChecker setModelType(String modelType) {
        this.modelType = modelType;
        return this;
    }

    @Override
    public InteractiveModelChecker setPropertyFormula(String propertyFormula) {
        this.propertyFormula = propertyFormula;
        return this;
    }

    @Override
    public InteractiveModelChecker setConstants(List<Constant> constants) {
        this.constants = constants;
        return this;
    }

    @Override
    public Range getRange() {
        return range;
    }

    @Override
    public void startModelChecker() throws IllegalStateException {
        logEngineInstance.log(LogEngine.LEVEL_INFO, "StormCWrapper: starting the model checker");
        
        if (stage != Stage.INITIALIZE) {
            throw new IllegalStateException("The model checker is not in the initialize stage");
        }

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
        
        List<String> command = new LinkedList<>();
        
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
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        try {
            modelCheckerProcess = pb.start();
        } catch (IOException e) {
            logEngineInstance.log(LogEngine.LEVEL_ERROR, "StormCWrapper: failed to start the model checker; error: " + e);
            return;
        }      
        
        modelCheckerInput = new OutputStreamWriter(modelCheckerProcess.getOutputStream());
        modelCheckerOutput = new BufferedReader(new InputStreamReader(modelCheckerProcess.getInputStream()));
        
        stage = Stage.STARTED;
        logEngineInstance.log(LogEngine.LEVEL_INFO, "StormCWrapper: the model checker started");
    }

    @Override
    public ModelCheckerResult check(List<Constant> parameterValues) throws IllegalStateException {
        if (stage != Stage.STARTED) {
            throw new IllegalStateException("The model checker is not started");
        }
        
        final StringBuilder sbp = new StringBuilder();
        parameterValues.forEach(c -> appendConstant(sbp, c));
        sbp.insert(0, identifier + FIELD_SEPARATOR);
        logEngineInstance.log(LogEngine.LEVEL_DEBUG, "StormCWrapper: checking " + sbp.toString());
        sbp.append('\n');
        
        String message;
        try {
            modelCheckerInput.write(sbp.toString());
            modelCheckerInput.flush();
            message = modelCheckerOutput.readLine();
        } catch (IOException ioe) {
            logEngineInstance.log(LogEngine.LEVEL_ERROR, "StormCWrapper: checking failure for point " + sbp.toString() + "; message: " + ioe.toString());
            return null;
        }
        
        ModelCheckerResult modelCheckerResult = null;
        logEngineInstance.log(LogEngine.LEVEL_DEBUG, "StormCWrapper: raw result: " + message);
        if (message.startsWith(RESULT_IDENTIFIER)) {
            String[] messageSplit = message.split(FIELD_SEPARATOR);
            String result = messageSplit[2];
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
        } else { //something wrong happened, probably a std::bad_alloc; just throw it
            throw new IllegalStateException(message);
        }

        identifier++;
        return modelCheckerResult;
    }

    @Override
    public void stopModelChecker() throws IllegalStateException {
        logEngineInstance.log(LogEngine.LEVEL_INFO, "StormCWrapper: stopping the model checker");
        if (stage != Stage.STARTED) {
            throw new IllegalStateException("The model checker is not started");
        }
        
        try {
            modelCheckerInput.write(EOF + "\n");
            modelCheckerInput.flush();
            modelCheckerInput.close();
            while (modelCheckerOutput.readLine() != null);
            modelCheckerOutput.close();
        } catch (IOException ioe) {}

        logEngineInstance.log(LogEngine.LEVEL_INFO, "StormCWrapper: model checker stopped");
    }
    
}
