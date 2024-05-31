#include <iostream>
#include <string>

#include <boost/algorithm/string.hpp>

#include "storm-config.h"
#include "storm/api/storm.h"
#include "storm/modelchecker/results/ExplicitQuantitativeCheckResult.h"
#include "storm/models/sparse/Dtmc.h"
#include "storm/models/sparse/Mdp.h"
#include "storm/utility/initialize.h"
#include "storm-parsers/api/storm-parsers.h"
#include "storm-parsers/parser/PrismParser.h"


std::map<std::string, std::string> splitParameters(std::string input_string) {
    std::map<std::string, std::string> mapping;

    std::vector<std::string> split_strings;
    boost::split(split_strings, input_string, boost::is_any_of(","));

    for (const std::string & single_string : split_strings) {
        std::vector<std::string> pair_strings;
        boost::split(pair_strings, single_string, boost::is_any_of("="));
        mapping.insert(std::pair<std::string, std::string>(pair_strings[0], pair_strings[1]));
    }

    return mapping;
} 

double checkDtmc(storm::prism::Program instantiatedProgram, std::shared_ptr<storm::logic::Formula const> formula) {
    storm::modelchecker::SparseExplorationModelChecker<storm::models::sparse::Dtmc<double>, uint32_t> checker(instantiatedProgram);
        
    std::unique_ptr<storm::modelchecker::CheckResult> checkerResult = checker.check(storm::modelchecker::CheckTask<>(*formula, true));
        
    storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = checkerResult->asExplicitQuantitativeCheckResult<double>();

    return quantitativeResult[0];
}

double checkMdp(storm::prism::Program instantiatedProgram, std::shared_ptr<storm::logic::Formula const> formula) {
    storm::modelchecker::SparseExplorationModelChecker<storm::models::sparse::Mdp<double>, uint32_t> checker(instantiatedProgram);
        
    std::unique_ptr<storm::modelchecker::CheckResult> checkerResult = checker.check(storm::modelchecker::CheckTask<>(*formula, true));
        
    storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = checkerResult->asExplicitQuantitativeCheckResult<double>();

    return quantitativeResult[0];
}

int main (int argc, char *argv[]) {

    // Init loggers
    storm::utility::setUp();
    storm::utility::setLogLevel(l3pp::LogLevel::OFF);

    // Set some settings objects.
    storm::settings::initializeAll("stormsmc-c-wrapper", "stormsmc-c-wrapper");

    std::vector<std::string> arguments;
    arguments.reserve(argc);
    arguments.assign(argv, argv + argc);

    std::string modelFile = arguments[1];
    std::string propertyFormula = arguments[2];
    std::string constants = arguments[3];
    std::string callOptions = arguments[4];

    storm::prism::Program program = storm::parser::PrismParser::parse(modelFile, true);

    std::shared_ptr<storm::logic::Formula const> formula = storm::api::extractFormulasFromProperties(storm::api::parsePropertiesForPrismProgram(propertyFormula, program)).front();;

    if (constants.length() > 0) {
        program = storm::utility::prism::preprocess(program, constants);
    }

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> instanceValues;
        boost::split(instanceValues, line, boost::is_any_of(":"));

        storm::prism::Program instantiatedProgram = storm::utility::prism::preprocess(program, instanceValues[1]);

        double result;
        if (instantiatedProgram.isDeterministicModel()) {
            result = checkDtmc(instantiatedProgram, formula);
        } else {
            result = checkMdp(instantiatedProgram, formula);
        }
            
        std::cout << "StormsmcCWrapper_RESULT:" << instanceValues[0] << ":" << result << "\n";
    }
    return 0;
}