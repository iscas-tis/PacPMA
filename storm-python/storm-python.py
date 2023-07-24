#!/usr/bin/python3

import sys
import stormpy
import stormpy.core
import stormpy.pars
import pycarl
import pycarl.core

modelType = sys.argv[1];
modelFile = sys.argv[2];
propertyFormula = sys.argv[3];
constants = sys.argv[4];

if 'prism' == modelType:
    program = stormpy.parse_prism_program(modelFile, True)
    if (len(constants) > 0):
        program = stormpy.preprocess_symbolic_input(program, [], constants)[0].as_prism_program()
    properties = stormpy.parse_properties_for_prism_program(propertyFormula, program)
elif 'jani' == modelType:
    program, properties = stormpy.parse_jani_model(modelFile)
    if (len(constants) > 0):
        program = stormpy.preprocess_symbolic_input(program, [], constants)[0].as_jani_model()
    properties = stormpy.parse_properties_for_jani_model(propertyFormula, program)
else:
    exit()


model = stormpy.build_parametric_model(program, properties)
parameters = model.collect_all_parameters()

if model.model_type == stormpy.ModelType.DTMC:
    instantiator = stormpy.pars.PDtmcInstantiator(model)
elif model.model_type == stormpy.ModelType.MDP:
    instantiator = stormpy.pars.PMdpInstantiator(model)
elif model.model_type == stormpy.ModelType.CTMC:
    instantiator = stormpy.pars.PCtmcInstantiator(model)
else:
    exit()

for entry in sys.stdin:
    entry = entry.rstrip()
    if "EOF" == entry:
        break
    message = entry.split(':')
    identifier = message[0]
    samples = []
    for s in message[1].split(','):
        pair = s.split('=')
        pair[1] = stormpy.RationalRF(pair[1])
        samples.append(pair)
    values = dict(samples)
    instance = dict()
    for p in parameters:
        instance[p] = values.get(p.name)
    instantiated_model = instantiator.instantiate(instance)
    result = stormpy.model_checking(instantiated_model, properties[0]).at(model.initial_states[0])
    print(f'StormPython_RESULT:{identifier}:{result}')
