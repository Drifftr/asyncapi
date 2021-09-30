/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.application;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.controller.Controller;
import io.ballerina.asyncapi.codegenerator.controller.ListenerController;
import io.ballerina.asyncapi.codegenerator.controller.DispatcherController;
import io.ballerina.asyncapi.codegenerator.controller.SchemaController;
import io.ballerina.asyncapi.codegenerator.controller.ServiceTypesController;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;

public class CodeGenerator implements Application {
    private final String specPath;
    private final String outputPath;

    public CodeGenerator(String specPath, String outputPath) {
        this.specPath = specPath;
        this.outputPath = outputPath;
    }

    @Override
    public void generate() throws BallerinaAsyncApiException {
        FileRepository fileRepository = new FileRepositoryImpl();
        String asyncApiSpecYaml = fileRepository.getFileContent(specPath);
        String asyncApiSpecJson;
        if (specPath.endsWith(".json")) {
            asyncApiSpecJson = asyncApiSpecYaml;
        } else if (specPath.endsWith("yaml") || specPath.endsWith("yml")) {
            asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecYaml);
        } else {
            throw new BallerinaAsyncApiException("Unknown file type: ".concat(specPath));
        }

        Controller schemaController = new SchemaController();
        String dataTypesBalContent = schemaController.generateBalCode(asyncApiSpecJson, "");

        Controller serviceTypesController = new ServiceTypesController();
        String serviceTypesBalContent = serviceTypesController.generateBalCode(asyncApiSpecJson, "");

        String listenerTemplate = fileRepository.getFileContentFromResources(Constants.LISTENER_BAL_FILE_NAME);
        Controller listenerController = new ListenerController();
        String listenerBalContent = listenerController.generateBalCode(asyncApiSpecJson, listenerTemplate);

        String dispatcherTemplate = fileRepository.getFileContentFromResources(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME);
        Controller dispatcherController = new DispatcherController();
        String dispatcherContent = dispatcherController.generateBalCode(asyncApiSpecJson, dispatcherTemplate);

        String outputDirectory = getOutputDirectory(outputPath);
        fileRepository.writeToFile(outputDirectory.concat(Constants.DATA_TYPES_BAL_FILE_NAME), dataTypesBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.SERVICE_TYPES_BAL_FILE_NAME), serviceTypesBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.LISTENER_BAL_FILE_NAME), listenerBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME), dispatcherContent);
    }

    private String getOutputDirectory(String outputPath) {
        if (outputPath.endsWith("/")) return outputPath;
        return outputPath.concat("/");
    }
}
