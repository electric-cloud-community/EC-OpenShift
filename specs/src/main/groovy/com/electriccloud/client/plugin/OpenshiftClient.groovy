package com.electriccloud.client.plugin

import com.electriccloud.client.commander.CommanderClient
import com.electriccloud.models.enums.LogLevels
import com.electriccloud.models.enums.ServiceTypes
import io.qameta.allure.Step

import static com.electriccloud.models.config.ConfigHelper.*
import static com.electriccloud.models.enums.LogLevels.LogLevel.*
import static com.electriccloud.models.enums.ServiceTypes.*
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*


class OpenshiftClient extends CommanderClient {


    OpenshiftClient() {
        this.timeout = 350
        this.plugin = 'openshift'
    }


    @Step("Create configuration: {configurationName}, {clusterEndpoint}")
    def createConfiguration(configurationName, clusterEndpoint, username, secretToken, clusterVersion, testConnection = true, logLevel = DEBUG) {
        message("creating openshift config")
        def response = client.dslFileMap dslPath(plugin, 'config'), [params: [
                configName: configurationName,
                endpoint: clusterEndpoint,
                logLevel: logLevel.getValue(),
                userName: username,
                token: secretToken,
                version: clusterVersion,
                testConnection: testConnection]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configurationName} with endpoint ${clusterEndpoint} is successfully created.")
        return response
    }

    @Step("Delete configuration: {confName}")
    def deleteConfiguration(confName) {
        message("removing configuration")
        def response = client.dslFileMap(dslPath(plugin, 'deleteConfig'), [params: [configName: confName]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${confName} is successfully deleted.")
        return response
    }



    @Step("Create environment: {configurationName}")
    def createEnvironment(configurationName, openshiftProject) {
        message("environment creation")
        def response = client.dslFileMap(dslPath("flow", 'environment'), [params: [
                configName: configurationName,
                osProject: openshiftProject ]])
        client.log.info("Environment for project: ${response.json.project.projectName} is created")
        return response
    }

    @Step
    def createService(replicaNum, volumes = [source: null, target: null ], canaryDeploy, routerHost, serviceType = LOAD_BALANCER) {
        message("service creation")
        def response = client.dslFileMap dslPath("flow", 'service'),[params: [
                replicas: replicaNum,
                sourceVolume: volumes.source,
                targetVolume: volumes.target,
                isCanary: canaryDeploy.toString(),
                routeHost: routerHost,
                servType: serviceType.getValue() ]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def createApplication(replicaNum, volumes = [source: null, target: null ], canaryDeploy, routerHost, serviceType = LOAD_BALANCER) {
        message("application creation")
        def response = client.dslFileMap dslPath("flow", 'application'), [params: [
                replicas: replicaNum,
                sourceVolume: volumes.source,
                targetVolume: volumes.target,
                isCanary: canaryDeploy.toString(),
                routeHost: routerHost,
                servType: serviceType.getValue() ]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def cleanUpCluster(config, namespace = 'flowqe-test-project') {
        message("cleaning up cluster")
        def response = client.dslFileMap(dslPath(plugin, 'cleanUp'), [params: [ configName: config, projectNamespace: namespace]])
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Cluster is successfully cleaned-up.")
        return response
    }


    @Step
    def importService(yamlfile, parameters, projectName, envProject, envName, clusterName,  importApp = false, applicationName = null) {
        message("service import")
        File yaml = new File("./${yamlPath("yaml", yamlfile)}")
        def yamlFileText = yaml.text.readLines().join('\\n')
        client.log.info("Importing YAML: \n ${yaml.text}")
        def appScoped
        if (importApp){
            appScoped = "1"
        } else {
            appScoped = null
        }
        def response = client.dslFileMap dslPath(plugin, 'import'),  [ params: [
                templateYaml: "${yamlFileText}",
                yamlPrameters: "${parameters}",
                projectName: projectName,
                applicationScoped: appScoped,
                applicationName: applicationName,
                envProjectName: envProject,
                environmentName: envName,
                clusterName: clusterName ]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Import is successfully completed.")
        response
    }



    @Step("Discover {cluster} on {endpoint}")
    def discoverService(project, envProject, envName, cluster, osProject, endpoint, token, importApp = false, appName = null) {
        message("service discovery")
        def response = client.dslFileMap dslPath(plugin, 'discover'), [params: [
                projectName: project,
                envProjectName: envProject,
                environmentName: envName,
                openshiftProject: osProject,
                clusterName: cluster,
                clusterEndpoint: endpoint,
                clusterApiToken: token,
                applicationScoped: importApp.toString(),
                applicationName: appName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Service is discovered successfully.")
        return response
    }




}
