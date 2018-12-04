package com.electriccloud.procedures.topology


import com.electriccloud.models.config.ConfigHelper
import com.electriccloud.models.enums.LogLevels
import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await



@Feature("Topology")
class DoActionOnRealtimeCluster extends OpenshiftTestBase {


    @BeforeClass(alwaysRun = true)
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @BeforeMethod(alwaysRun = true)
    void backendAuthorization(){
        ectoolApi.ectoolLogin()
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        osClient.cleanUpCluster(configName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("View Logs for 'ecp-container' objectType in Topology")
    void viewLogsForEcpContainerObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "viewLogs", "--environmentName", environmentName

        assert topologyOutcome ==~ /.*[\d]{4}\/[\d]{2}\/[\d]{2} [\d]{2}:[\d]{2}:[\d]{2} \[notice\] [\d]+#[\d]+: nginx\/[\d]+.[\d]+.[\d]+.*/
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("Skip actionParameter if it is not implemented for objectType in Topology")
    void getAResponseWithCorrectFieldsForEcpClusterObjectTypeInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "nonExistingAction", "--environmentName", environmentName

        pluginProjectName = ConfigHelper.xml(
                ectoolApi.run("ectool", "getPlugin", pluginName)
        ).'*'.projectName.text()

        assert topologyOutcome == "ectool error [NotImplemented]: Cluster '$clusterName' is configured to use '$pluginProjectName' plugin which does not support action 'nonExistingAction' on object type 'ecp-container'."
    }



    @Test(groups = "Negative")
    @TmsLink("")
    @Story("Do actions on Topology negative")
    @Description("Unable to Get Realtime Cluster Details for non-existing Configuration")
    void unableToGetRealtimeClusterDetailsForNonExistingConfiguration() {

        osClient.deleteConfiguration(configName)

        pluginProjectName = ConfigHelper.xml(
                ectoolApi.run("ectool", "getPlugin", pluginName)
        ).'*'.projectName.text()

        topologyOutcome = ectoolApi.run "ectool", "doActionOnRealtimeCluster",
                projectName, clusterName, ecpContainerId, "ecp-container", "viewLogs", "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration '$configName' " +
                "found at 'ec_plugin_cfgs' for '$pluginProjectName'"

        osClient.createConfiguration(configName, clusterEndpoint, 'qe', clusterToken, clusterVersion)
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Do actions on Topology positive")
    @Description("Perform Action on Realtime Cluster using DSL")
    void performActionOnRealtimeClusterUsingDSL() {

        topologyOutcome = ectoolApi.dsl """doActionOnRealtimeCluster(projectName: '$projectName', clusterName: '$clusterName', objectId: '$ecpContainerId', objectType: 'ecp-container', action: 'viewLogs', environmentName: '$environmentName')"""

        assert topologyOutcome ==~ /.*[\d]{4}\/[\d]{2}\/[\d]{2} [\d]{2}:[\d]{2}:[\d]{2} \[notice\] [\d]+#[\d]+: nginx\/[\d]+.[\d]+.[\d]+.*/
    }


}

