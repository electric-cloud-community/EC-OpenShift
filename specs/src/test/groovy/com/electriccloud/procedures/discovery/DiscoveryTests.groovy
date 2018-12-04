package com.electriccloud.procedures.discovery


import com.electriccloud.procedures.OpenshiftTestBase
import com.electriccloud.test_data.DiscoveryData
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*


@Feature('Discovery')
class DiscoveryTests extends OpenshiftTestBase {


    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        osClient.deleteConfiguration(configName)
        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion)
        osClient.createEnvironment(configName, osProject)
        osClient.createService(2, volumes, false, getHost(clusterEndpoint), LOAD_BALANCER)
        osClient.deployService(projectName, serviceName)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        osClient.client.deleteService(projectName, serviceName)
        osClient.client.deleteApplication(projectName, applicationName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        osClient.client.deleteService(projectName, serviceName)
        osClient.client.deleteApplication(projectName, applicationName)
        osClient.client.deleteProject('MyProject')
    }


    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        osClient.cleanUpCluster(configName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    @TmsLink("324439")
    @Description("Discover Project-level Microservice")
    void discoverProjectLevelMicroservice() {
        osClient.discoverService(
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                osProject,
                null,
                null,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
        def container = osClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mapping = osClient.getServiceMappings(projectName, serviceName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "routeHostname"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == getHost(clusterEndpoint)
        assert mapping.actualParameters.parameterDetail[1].parameterName == "routeName"
        assert mapping.actualParameters.parameterDetail[1].parameterValue == "nginx-route"
        assert mapping.actualParameters.parameterDetail[2].parameterName == "routePath"
        assert mapping.actualParameters.parameterDetail[2].parameterValue == "/"
        assert mapping.actualParameters.parameterDetail[3].parameterName == "routeTargetPort"
        assert mapping.actualParameters.parameterDetail[3].parameterValue == "servicehttpnginx-container01530626345623"

    }



    @Test(groups = "Positive")
    @TmsLink("324441")
    @Description("Discover Project-level Microservice with Environment generation")
    void discoverProjectLevelMicroserviceWithEnvironmentGeneration() {
        osClient.discoverService(
                projectName,
                environmentProjectName,
                'my-environment',
                clusterName,
                osProject,
                clusterEndpoint,
                clusterToken,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
        def environment = osClient.client.getEnvironment(projectName, 'my-environment').json.environment
        def container = osClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = osClient.getServiceMappings(projectName, serviceName)
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == 'my-environment'
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == 'my-environment'
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Description("Discover Project-level Microservice with Project generation")
    void discoverProjectLevelMicroserviceWithProjectGeneration() {
        osClient.discoverService(projectName,
                "MyProject",
                environmentName,
                clusterName,
                osProject,
                clusterEndpoint,
                clusterToken,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
        def environment = osClient.client.getEnvironment("MyProject", environmentName).json.environment
        def container = osClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = osClient.getServiceMappings(projectName, serviceName)
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == "MyProject"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }



    @Test(groups = "Positive")
    @TmsLink("324440")
    @Description("Discover Application-level Microservice")
    void discoverApplicationLevelMicroservice() {
        osClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                osProject,
                null,
                null,
                true, applicationName)
        def applications = osClient.client.getApplications(projectName).json.application
        def application = osClient.client.getApplication(projectName, applicationName).json.application
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mapping = osClient.getAppMappings(projectName, applicationName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "routeHostname"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == getHost(clusterEndpoint)
        assert mapping.actualParameters.parameterDetail[1].parameterName == "routeName"
        assert mapping.actualParameters.parameterDetail[1].parameterValue == "nginx-route"
        assert mapping.actualParameters.parameterDetail[2].parameterName == "routePath"
        assert mapping.actualParameters.parameterDetail[2].parameterValue == "/"
        assert mapping.actualParameters.parameterDetail[3].parameterName == "routeTargetPort"
        assert mapping.actualParameters.parameterDetail[3].parameterValue == "servicehttpnginx-container01530626345623"
    }




    @Test(groups = "Positive")
    @TmsLink("324442")
    @Description("Discover Application-level Microservice with Environment generation")
    void discoverApplicationLevelMicroserviceAndEnvironmentGeneration() {
        osClient.discoverService(projectName,
                environmentProjectName,
                "my-environment",
                clusterName,
                osProject,
                clusterEndpoint,
                clusterToken,
                true, applicationName)
        def applications = osClient.client.getApplications(projectName).json.application
        def application = osClient.client.getApplication(projectName, applicationName).json.application
        def environment = osClient.client.getEnvironment(projectName, "my-environment").json.environment
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = osClient.getAppMappings(projectName, applicationName)
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == "my-environment"
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == "my-environment"
    }



    @Test(groups = "Positive")
    @TmsLink("324443")
    @Description("Discover Application-level Microservice with Project generation")
    void discoverApplicationLevelMicroserviceWithProjectGeneration() {
        osClient.discoverService(
                projectName,
                'MyProject',
                environmentName,
                clusterName,
                osProject,
                clusterEndpoint,
                clusterToken,
                true, applicationName)
        def applications = osClient.client.getApplications(projectName).json.application
        def application = osClient.client.getApplication(projectName, applicationName).json.application
        def environment = osClient.client.getEnvironment('MyProject', environmentName).json.environment
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == 'MyProject'
    }




    @Test(groups = "Negative")
    @TmsLink("324444")
    @Story("Discover existing microservice")
    @Description("Unable to Discover Existing Project-level Microservice")
    void unableToDiscoverExistingProjectLevelMicroservice() {
        try {
            osClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    osProject,
                    clusterEndpoint,
                    clusterToken,
                    false, null)
            osClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    osProject,
                    clusterEndpoint,
                    clusterToken,
                    false, null)
        } catch (e) {
            def jobId = e.cause.message
            String errorLog = osClient.client.getJobLogs(jobId)
            def jobStatus = osClient.client.getJobStatus(jobId).json
            assert errorLog.contains("Service ${serviceName} already exists")
            assert errorLog.contains("Container ${containerName} already exists")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @Test(groups = "Negative")
    @TmsLink("324444")
    @Story("Discover existing microservice")
    @Description("Unable to Discover Existing Application")
    void unableToDiscoverExistingApplication() {
        try {
            osClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    osProject,
                    clusterEndpoint,
                    clusterToken,
                    true, applicationName)
            osClient.discoverService(projectName, projectName,
                    environmentName,
                    clusterName,
                    osProject,
                    clusterEndpoint,
                    clusterToken,
                    true, applicationName)
        } catch (e) {
            def jobId = e.cause.message
            String errorLog = osClient.client.getJobLogs(jobId)
            def jobStatus = osClient.client.getJobStatus(jobId).json
            assert errorLog.contains("Application ${applicationName} already exists in project ${projectName}")
            assert errorLog.contains("Process \'Deploy\' already exists in application \'${applicationName}\'")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @Test(groups = "Negative")
    @Issue("ECPOPSHIFT-117")
    @TmsLink("324445")
    @Story("Discover with invalid data")
    @Description("Unable to Discover Project-level Microservice with Invalid Openshift Project name")
    void projectLevelMicroserviceDiscoveryWithInvalidNamespace(){
        def resp =  osClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName, "test project",
                clusterEndpoint,
                clusterToken).json
        def jobStatus = osClient.client.getJobStatus(resp.jobId).json
        String jobLog = osClient.client.getJobLogs(resp.jobId)
        assert jobStatus.outcome == "warning"
        assert jobStatus.status == "completed"
        assert jobLog.contains("No services found on the cluster ${clusterEndpoint}")
        assert jobLog.contains("Discovered services: 0")
    }




    @Test(groups = "Negative", dataProvider = "invalidDiscoveryData", dataProviderClass = DiscoveryData.class)
    @TmsLink("324445")
    @Story("Discover with invalid data")
    @Description("Project-level Microservice Discovery with Invalid Input Data")
    void projectLevelMicroserviceDiscoveryWithInvalidInputData(project, envProject, envName, clusterName, osProject, endpoint, token, errorMessage) {
        try {
            osClient.discoverService(project, envProject,
                    envName,
                    clusterName,
                    osProject,
                    endpoint,
                    token,
                    false, null)
        } catch (e){
            def jobId = e.cause.message
            String errorLog = osClient.client.getJobLogs(jobId)
            println errorLog
            def jobStatus = osClient.client.getJobStatus(jobId).json
            assert errorLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



}