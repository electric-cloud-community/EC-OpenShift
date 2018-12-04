package com.electriccloud.client.commander


import com.electriccloud.client.APIClient
import static com.electriccloud.models.config.ConfigHelper.message
import static com.electriccloud.models.config.ConfigHelper.dslPath
import static com.electriccloud.models.config.ConfigHelper.yamlPath
import groovy.json.JsonBuilder
import io.qameta.allure.Step


class CommanderClient {

    public APIClient client
    def timeout = 120
    def plugin
    def json

    CommanderClient(){
        this.client = new APIClient()
        this.json = new JsonBuilder()
    }



    @Step
    def runDsl(dslFile) {
        message("running dsl")
        def response = client.dslFile dslPath(plugin, dslFile)
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Procedure successfully finished.")
        return response
    }


    @Step("Deploy project-level service: {serviceName}")
    def deployService(projectName, serviceName) {
        message("service deployment")
        def mapping = getServiceMappings(projectName, serviceName)[0]
        def response = client.dslFileMap dslPath('flow', 'deploy'),
                [params: [project: "${projectName}", environment: "${mapping.environmentName}", envProject: "${mapping.environmentProjectName}", service: "${serviceName}"]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Deployment is successfully completed.")
        return response
    }

    @Step("Undeploy project-level service: {serviceName}")
    def undeployService(projectName, serviceName) {
        message("service undeploy")
        def mapping = getServiceMappings(projectName, serviceName)[0]
        def response = client.dslFileMap dslPath('flow', 'undeploy'),
                [params: [project: "${projectName}", environment: "${mapping.environmentName}", envProject: "${mapping.environmentProjectName}", service: "${serviceName}"]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Undeploy is successfully completed.")
        return response
    }

    @Step("Deploy application-level service: {serviceName}")
    def deployApplication(projectName, applicationName) {
        message("application service deployment")
        def mapping = getAppMappings(projectName, applicationName)[0]
        def response = client.dslFileMap dslPath('flow', 'appDeploy'),
                [params: [project: projectName, appName: mapping.applicationName, tierMapName: mapping.tierMapName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Deployment is successfully completed.")
        return response
    }

    @Step("Undeploy application-level service: {serviceName}")
    def undeployApplication(projectName, applicationName) {
        message("application service undeploy")
        def mapping = getAppMappings(projectName, applicationName)[0]
        def response = client.dslFileMap dslPath('flow', 'appUndeploy'),
                [params: [project: projectName, appName: mapping.applicationName, tierMapName: mapping.tierMapName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Undeploy is successfully completed.")
        response
    }


    @Step
    def provisionEnvironment(projectName, environmentName, clusterName, timeout = 120) {
        message("environment provisioning")
        def response = client.dslFileMap dslPath('flow', 'provision'),[params: [ projectName: projectName, environmentName: environmentName, cluster: clusterName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Cluster provisioning is successfully completed.")
        response
    }


    def getServiceMappings(project, service) {
        //message("got service mappings")
        def map = client.dslFileMap(dslPath('flow', 'envMaps'), [params: [projectName: "${project}", serviceName: "${service}"]]).json.environmentMap
        // returns the collection of mappings
        map
    }

    def getAppMappings(project, application) {
        //message("got application mappings")
        def map = client.tierMappings(project, application).json.tierMap
        // returns the collection of mappings
        map
    }



}
