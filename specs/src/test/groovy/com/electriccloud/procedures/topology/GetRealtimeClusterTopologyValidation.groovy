package com.electriccloud.procedures.topology


import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Feature("Topology")
class GetRealtimeClusterTopologyValidation extends OpenshiftTestBase {

    @BeforeClass(alwaysRun = true)
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        ectoolApi.ectoolLogin()
        ecpPodId = clusterEndpoint
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        osClient.cleanUpCluster(configName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }




    @Test(groups = "Negative", dataProvider = "projectNames", enabled = true)
    @TmsLink("")
    @Story("Topology validation")
    @Description("getRealtimeClusterTopology validation (projectName)")
    void getRealtimeClusterTopologyValidationForProjectName(charactersNum, expectedResult) {
        def projectName = characters(charactersNum)
        topologyOutcome = ectoolApi.run("ectool", "getRealtimeClusterTopology", /$projectName/,
                clusterName, "--environmentName", environmentName)

        check(topologyOutcome, expectedResult)
    }


    @Test(groups = "Negative", dataProvider = "clusterNames", enabled = true)
    @Issue("")
    @TmsLink("")
    @Story("Topology validation")
    @Description("getRealtimeClusterTopology validation (clusterName)")
    void getRealtimeClusterTopologyValidationForClusterName(charactersNum, expectedResult) {
        def clusterName     = characters(charactersNum)
        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology",
                projectName, clusterName, "--environmentName", environmentName

        check(topologyOutcome, expectedResult)
    }


    @Test(groups = "Negative", dataProvider = "environmentNames", enabled = true)
    @TmsLink("")
    @Story("Topology validation")
    @Description("getRealtimeClusterTopology validation (environmentName)")
    void getRealtimeClusterTopologyValidationForEnvironmentName(charactersNum, expectedResult) {
        def environmentName = characters(charactersNum)
        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology",
                projectName, clusterName, "--environmentName", environmentName

        check(topologyOutcome, expectedResult)
    }


    @Test(groups = "Negative", enabled = true)
    @TmsLink("")
    @Story("Topology validation")
    @Description("getRealtimeClusterTopology validation (empty names for all fields)")
    void getRealtimeClusterTopologyValidationEmptyNames() {
        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", "", "", "--environmentName", ""
        assert topologyOutcome.contains("'clusterName' is required and must be between 1 and 253 alphanumeric, ' ', '_' or '-' characters")
        assert topologyOutcome.contains("'environmentName' must be between 1 and 255 characters")
        assert topologyOutcome.contains("'projectName' is required and must be between 1 and 255 characters")
    }






    @DataProvider(name = "projectNames")
    Object[][] projectNames() {
        def data = [
                [ 255, "<clusterTopology>" ],
                [ 1,   "<clusterTopology>" ],
                [ 100, "<clusterTopology>" ],
                [ 256, 'ectool error [InvalidProjectName]: \'projectName\' is required and must be between 1 and 255 characters' ],
                [ 0,   'ectool error [InvalidProjectName]: \'projectName\' is required and must be between 1 and 255 characters' ],
        ]
        return data.toArray() as Object[][]
    }



    @DataProvider(name = "clusterNames")
    Object[][] clusterNames() {
        def data = [
                [ 253, "<clusterTopology>" ],
                [ 1,   "<clusterTopology>" ],
                [ 100, "<clusterTopology>" ],
                [ 254, "ectool error [InvalidClusterName]: 'clusterName' is required and must be between 1 and 253 alphanumeric, ' ', '_' or '-' characters" ],
                [ 0,   "ectool error [InvalidClusterName]: 'clusterName' is required and must be between 1 and 253 alphanumeric, ' ', '_' or '-' characters" ],
        ]
        return data.toArray() as Object[][]
    }



    @DataProvider(name = "environmentNames")
    Object[][] environmentNames() {
        def data = [
                [ 255, "<clusterTopology>" ],
                [ 1,   "<clusterTopology>" ],
                [ 100, "<clusterTopology>" ],
                [ 256, 'ectool error [InvalidEnvironmentName]: \'environmentName\' must be between 1 and 255 characters' ],
                [ 0,   'ectool error [InvalidEnvironmentName]: \'environmentName\' must be between 1 and 255 characters' ],
        ]
        return data.toArray() as Object[][]
    }




    def check(actualResult, expectedResult) {
        if(expectedResult == "non-existing") {
            assert actualResult == "ectool error [NoSuchProject]: Project '$projectName' does not exist"
        } else {
            if(expectedResult == "<clusterTopology>") {
//                checkResponseForGetRealtimeClusterTopology(actualResult)
            }
            else {
                assert actualResult == expectedResult
            }
        }
    }
}
