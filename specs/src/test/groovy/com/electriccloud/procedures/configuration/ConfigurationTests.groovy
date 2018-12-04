package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.OpenshiftTestBase
import com.electriccloud.test_data.ConfigurationData
import io.qameta.allure.Description
import io.qameta.allure.Issue
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class ConfigurationTests extends OpenshiftTestBase {


    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        osClient.deleteConfiguration(configName)
    }


    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }


    @Test(dataProvider = "clusterVersions", dataProviderClass = ConfigurationData.class, groups = "Positive")
    @Story("Cross-version configuration")
    @Description("Create Configuration for all cluster versions")
    void createConfigurationForDifferentVersions(version){
        def job = osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, version)
        String logs = osClient.client.getJobLogs(job.json.jobId)
        def jobStatus = osClient.client.getJobStatus(job.json.jobId).json
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("OpenShift cluster is reachable at ${clusterEndpoint}")
    }


    @Test(groups = "Positive")
    @Story("Create Configuration without test connection")
    @Description("Create configuration without cluster test connection")
    void createConfigurationWithoutTestConnection(){
        def job = osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, false)
        String logs = osClient.client.getJobLogs(job.json.jobId)
        def jobStatus = osClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = osClient.client.getJobSteps(job.json.jobId).json.object
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("")
        assert jobSteps[0].jobStep.combinedStatus.status == "skipped"
        assert jobSteps[1].jobStep.combinedStatus.status == "skipped"
    }


    @Test(groups = "Positive")
    @Story("Create Configuration with test connection")
    @Description("Create Configuration with cluster test connection")
    void createConfigurationWithTestConnection(){
        def job = osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true)
        String logs = osClient.client.getJobLogs(job.json.jobId)
        def jobStatus = osClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = osClient.client.getJobSteps(job.json.jobId).json.object
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("OpenShift cluster is reachable at ${clusterEndpoint}")
        assert jobSteps[0].jobStep.combinedStatus.status == "completed_success"
        assert jobSteps[1].jobStep.combinedStatus.status == "completed_success"
    }


    @Test(dataProvider = "logLevels", dataProviderClass = ConfigurationData.class, groups = "Negative")
    @Issue("ECPOPSHIFT-136")
    @Story("Log Level Configuration")
    @Description("Create Configuration for different log Levels")
    void createConfigurationForDifferentLogLevels(logLevel, message, desiredLog, missingLog){
        def job = osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, logLevel)
        osClient.createEnvironment(configName, osProject)
        def resp = osClient.provisionEnvironment(projectName, environmentName, clusterName)
        def jobStatus = osClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = osClient.client.getJobSteps(job.json.jobId).json.object
        String logs = osClient.client.getJobLogs(resp.json.jobId)
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[2].jobStep.command.contains(message)
        assert logs.contains(desiredLog)
        assert !logs.contains(missingLog)
    }



    @Test(groups = "Negative")
    @Story("Invalid configuration")
    @Description("Unable to create configuration that already exist")
    void unableToCreateExistingConfiguration(){
        try {
            osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion)
            osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion)
        } catch (e){
            def jobId = e.cause.message
            def jobStatus = osClient.client.getJobStatus(jobId).json
            String logs = osClient.client.getJobLogs(jobId)
            assert logs.contains("A configuration named '${configName}' already exists.")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }


    @Test(dataProvider = "invalidData", dataProviderClass = ConfigurationData.class, groups = "Negative")
    @Story("Invalid configuration")
    @Description("Unable to configure with invalid data")
    void unableToConfigureWithInvalidData(cinfigName, endpoint, username, token, version, testConnection, logLevel, errorMessage){
        def jobStatus = null
        String logs = " "
        try {
            osClient.createConfiguration(cinfigName, endpoint, username, token, version, testConnection, logLevel)
        } catch (e){
            def jobId = e.cause.message
            jobStatus = osClient.client.getJobStatus(jobId).json
            logs = osClient.client.getJobLogs(jobId)
        } finally {
            assert logs.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }






}
