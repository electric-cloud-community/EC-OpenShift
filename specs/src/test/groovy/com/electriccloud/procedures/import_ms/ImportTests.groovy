package com.electriccloud.procedures.import_ms

import com.electriccloud.procedures.OpenshiftTestBase
import com.electriccloud.test_data.ImportData
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@Feature('Import')
class ImportTests extends OpenshiftTestBase {


    def parameters = "nginxImage=nginx:latest, nginxPort=8080"


    @BeforeClass(alwaysRun = true)
    void setUp(){
        osClient.deleteConfiguration(configName)
        osClient.deleteConfiguration(configName)
        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion)
        osClient.createEnvironment(configName, osProject)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests() {
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        osClient.client.deleteApplication(projectName, applicationName)
        osClient.client.deleteService(projectName, serviceName)
    }


    @AfterMethod(alwaysRun = true)
    void tearDownTest() {
        osClient.client.deleteApplication(projectName, applicationName)
        osClient.client.deleteService(projectName, serviceName)
    }




    @Test(groups = "Positive")
    @Issue('ECPOPSHIFT-113')
    @Story('Microservice import')
    void microserviceImport() {
        osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
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
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName

    }


    @Test(groups = "Positive")
    @Issue('ECPOPSHIFT-127')
    @Story('Import microservice without environment mapping')
    void microserviceImportWithoutEnvironmentMapping() {
        osClient.importService(serviceName, null,
                projectName,
                null,
                null,
                null,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
        def container = osClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = osClient.getServiceMappings(projectName, serviceName)
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '0'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mappings == null
    }




    @Test(groups = "Positive")
    @Story('Application import')
    void applicationImport(){
        osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def apps = osClient.client.getApplications(projectName).json.application
        def app = osClient.client.getApplication(projectName, applicationName).json.application
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = osClient.getAppMappings(projectName, applicationName)
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }

    @Test(groups = "Positive")
    @Story('Application import without environment mapping')
    void applicationImportWithoutEnvironmentMapping(){
        osClient.importService(applicationName, null,
                projectName,
                null,
                null,
                null,
                true, applicationName)
        def apps = osClient.client.getApplications(projectName).json.application
        def app = osClient.client.getApplication(projectName, applicationName).json.application
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = osClient.getAppMappings(projectName, applicationName)
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.envTemplateTierMapCount == '0'
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mappings == null
    }



    @Test(groups = "Positive")
    @Issue('ECPOPSHIFT-116')
    @Story('Microservice import')
    void microserviceImportWithParameters() {
        osClient.importService("nginx-service-parametrized", parameters,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, serviceName).json.service
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
        assert container.imageName == "nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }

    @Test(groups = "Positive")
    @Issue('ECPOPSHIFT-116')
    @Story('Microservice import')
    void applicationImportWithParameters() {
        osClient.importService("nginx-service-parametrized", parameters,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def apps = osClient.client.getApplications(projectName).json.application
        def app = osClient.client.getApplication(projectName, applicationName).json.application
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = osClient.getAppMappings(projectName, applicationName)
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.applicationName == applicationName
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
    }


    @Test(groups = "Negative")
    @Story('Import existing microservice')
    void importServiceThatAllreadyExist(){
        osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def jobId = osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null).json.jobId
        def services = osClient.client.getServices(projectName).json.service
        def service = osClient.client.getService(projectName, 'nginx-service').json.service
        def container = osClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        String jobLogs = osClient.client.getJobLogs(jobId)
        def jobStatus = osClient.client.getJobStatus(jobId).json
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
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "warning"
        assert jobLogs.contains("Service ${serviceName} already exists, skipping")
    }


    @Test(groups = "Negative")
    @Story('Import existing microservice')
    void importApplicationThatAllreadyExist(){
        osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def jobId = osClient.importService(serviceName, null,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName).json.jobId
        def apps = osClient.client.getApplications(projectName).json.application
        def app = osClient.client.getApplication(projectName, applicationName).json.application
        def container = osClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        String jobLogs = osClient.client.getJobLogs(jobId)
        def jobStatus = osClient.client.getJobStatus(jobId).json
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.applicationName == applicationName
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "warning"
        assert jobLogs.contains("Service ${serviceName} already exists, skipping")
    }





    @Test(groups = "Negative", dataProvider = 'importData', dataProviderClass = ImportData.class)
    @Issue("ECPOPSHIFT-164")
    @Story('Import with invalid parameter')
    void invalidServiceImport(yamlFile, params, project, envName, clusterName, isApp, appName, errorMessage){
        try {
            osClient.importService(yamlFile, params,
                    project,
                    project,
                    envName,
                    clusterName,
                    isApp, appName)
        } catch (e){
            def jobId = e.cause.message
            String errorLog = osClient.client.getJobLogs(jobId)
            def jobStatus = osClient.client.getJobStatus(jobId).json
            assert errorLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }






}
