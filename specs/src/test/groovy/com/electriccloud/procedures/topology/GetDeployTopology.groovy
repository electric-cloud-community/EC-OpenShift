package com.electriccloud.procedures.topology



import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.models.enums.LogLevels.LogLevel.*


@Feature("Topology")
class GetDeployTopology extends OpenshiftTestBase {


    @BeforeMethod(alwaysRun = true)
    void backendAuthorization(){
        ectoolApi.ectoolLogin()
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTests() {
        osClient.cleanUpCluster(configName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology for Project-level Microservice")
    void getDeployTopologyForProjectLevelMicroservice() {

        createAndDeployService()
        setTopology()

        topologyOutcome = ectoolApi.run "ectool", "getDeployTopology", projectName, "--serviceName", serviceName

        [
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology for Application")
    void getDeployTopologyForApplication() {

        createAndDeployService(true)
        setTopology(true)

        topologyOutcome = ectoolApi.run "ectool", "getDeployTopology", projectName, "--applicationName", applicationName

        [
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
                [ id: applicationId, name: applicationName, type: 'application', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: applicationId, target: environmentId, topologyType: 'deployTopology'  ],
                [ source: applicationId, target: serviceId, topologyType: 'deployTopology'  ],
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology for Environment")
    void getDeployTopologyForEnvironment() {

        createAndDeployService(true)
        setTopology(true)

        topologyOutcome = ectoolApi.run "ectool", "getDeployTopology", projectName, "--environmentName", environmentName

        [
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology for Cluster")
    void getDeployTopologyForCluster() {

        createAndDeployService()
        setTopology()

        topologyOutcome = ectoolApi.run "ectool", "getDeployTopology", projectName,
                "--environmentName", environmentName, "--clusterName", clusterName

        [
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }



    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology for Application with Mapping to Environment Tiers and Resources")
    void getDeployTopologyForEnvTiersAndResources() {

        ectoolApi.run 'ectool', 'deleteProject', projectName

        createAndDeployService(true)
        setTopology(true)

        def appTier1 = 'qe app tier 1',
            appTier2 = 'qe app tier 2',
            envTier1 = 'qe env tier 1',
            envTier2 = 'qe app tier 2'

        ectoolApi.dsl """
applicationTier('$appTier1', projectName: '$projectName', applicationName: '$applicationName');
applicationTier('$appTier2', projectName: '$projectName', applicationName: '$applicationName');
environmentTier('$envTier1', projectName: '$projectName', environmentName: '$environmentName'); 
environmentTier('$envTier2', projectName: '$projectName', environmentName: '$environmentName') {
    resource('qe res 1');
    resource('qe res 2')
}
"""
        ectoolApi.dsl("tierMap('tierMap', projectName: '$projectName', applicationName: '$applicationName', " +
                "environmentProjectName: '$projectName', environmentName: '$environmentName', " +
                "tierMapping: [ '$appTier1':'$appTier2', '$envTier1':'$envTier2' ])")
        def appTierId1 = ectoolApi.run 'ectool', '--valueOf', '//applicationTierId', 'getApplicationTier', projectName, applicationName, appTier1
        def appTierId2 = ectoolApi.run 'ectool', '--valueOf', '//applicationTierId', 'getApplicationTier', projectName, applicationName, appTier2
        def envTierId1 = ectoolApi.run 'ectool', '--valueOf', '//environmentTierId', 'getEnvironmentTier', projectName, environmentName, envTier1
        def envTierId2 = ectoolApi.run 'ectool', '--valueOf', '//environmentTierId', 'getEnvironmentTier', projectName, environmentName, envTier2

        topologyOutcome = ectoolApi.run "ectool", "getDeployTopology", projectName, "--applicationName", applicationName

        [
                [ id: applicationId, name: applicationName, type: 'application', topologyType: 'deployTopology' ],
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
                [ id: envTierId1,    name: envTier1, type: 'environmentTier', topologyType: 'deployTopology' ],
                [ id: envTierId2,    name: envTier2, type: 'environmentTier', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: applicationId, target: environmentId, topologyType: 'deployTopology'  ],
                [ source: applicationId, target: serviceId, topologyType: 'deployTopology'  ],
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }

    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Topology positive")
    @Description("Get Deploy Topology using DSL")
    void getDeployTopologyUsingDSL() {

        createAndDeployService()
        setTopology()

        topologyOutcome = ectoolApi.dsl "getDeployTopology(projectName: '$projectName', serviceName: '$serviceName')"

        [
                [ id: environmentId, name: environmentName, type: 'environment', topologyType: 'deployTopology' ],
                [ id: clusterId,     name: clusterName, type: 'cluster', topologyType: 'deployTopology' ],
                [ id: serviceId,     name: serviceName, type: 'service', topologyType: 'deployTopology' ],
        ].each {
            assert !_node(it).toString().empty
        }

        [
                [ source: serviceId,     target: environmentId, topologyType: 'deployTopology'  ],
                [ source: environmentId, target: clusterId, topologyType: 'deployTopology'  ],
        ].each {
            assert !_link(it).toString().empty
        }
    }



    @Test(groups = "Negative")
    @TmsLink("")
    @Story("Get Topology negative")
    @Description("Unable to Get Realtime Cluster Details for non-existing Configuration")
    void unableToGetRealtimeClusterDetailsForNonExistingConfiguration() {

        createAndDeployService()
        setTopology()

        osClient.deleteConfiguration(configName)

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterDetails",
                projectName, clusterName, ecpContainerId, "ecp-container", "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration \'$configName\' " +
                "found at \'ec_plugin_cfgs\' for \'$pluginProjectName\'"

        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, DEBUG)
    }



}