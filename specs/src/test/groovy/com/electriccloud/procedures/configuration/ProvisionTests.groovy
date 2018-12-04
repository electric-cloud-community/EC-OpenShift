package com.electriccloud.procedures.configuration

import com.electriccloud.models.enums.LogLevels
import com.electriccloud.procedures.OpenshiftTestBase
import com.electriccloud.test_data.ProvisionData
import io.qameta.allure.Description
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static com.electriccloud.models.enums.LogLevels.LogLevel.*

class ProvisionTests extends OpenshiftTestBase{

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, DEBUG)
        osClient.createEnvironment(configName, osProject)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(environmentProjectName)
    }


    @Test(groups = "Positive")
    @Story("Provisioning of openshift environment")
    @Description("Provision Openshift cluster")
    void provisionCluster(){
        def jobId = osClient.provisionEnvironment(projectName, environmentName, clusterName).json.jobId
        def jobStatus = osClient.client.getJobStatus(jobId).json
        def jobLogs = osClient.client.getJobLogs(jobId)
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobLogs.contains("OpenShift cluster is reachable at ${clusterEndpoint}")

    }

    @Test(dataProvider = 'invalidData', dataProviderClass = ProvisionData.class, groups = "Negative")
    @Story('Provisioning of openshift environment with invalid data')
    @Description("Provision Openshift cluster with invalid data ")
    void invalidClusterProvisioning(project, environment, cluster, message){
        try {
            osClient.provisionEnvironment(project, environment, cluster).json
        } catch (e){
            assert e.cause.message.contains(message)
        }
    }




}
