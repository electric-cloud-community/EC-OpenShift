package com.electriccloud.procedures.deployment


import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.Feature
import io.qameta.allure.Flaky
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test


@Feature("Deployment")
class PluginUpdateDeploymentTests extends OpenshiftTestBase {


    @BeforeClass
    void setUpTests(){
        def legacyPlugin = ectoolApi.deletePlugin(pluginName, pluginVersion).installPlugin('EC-OpenShift-legacy').plugin
        ectoolApi.promotePlugin(legacyPlugin.projectName)
        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion)
        osClient.createEnvironment(configName, osProject)
    }

    @AfterClass
    void tearDownTests(){
        def latestPlugin = ectoolApi.deletePlugin(pluginName, pluginLegacyVersion).installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(latestPlugin.pluginName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }


    @AfterMethod
    void tearDownTest(){
        osClient.cleanUpCluster(configName)
        osClient.undeployService(projectName, serviceName)
    }




    @Test(groups = 'pluginUpdate')
    @Flaky
    @TmsLink("324598")
    @Story('Deploy service after Plugin version update')
    void pluginUpdateDeployment(){

        osClient.createService(2, false, getHost(clusterEndpoint))
        osClient.deployService(projectName, serviceName)
        def plugin = ectoolApi.installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(plugin.projectName)
        osClient.deleteConfiguration(configName)
        osClient.createConfiguration(configName, clusterEndpoint, username, clusterToken, clusterVersion)
        osClient.createService(3, false, getHost(clusterEndpoint))
        osClient.deployService(projectName, serviceName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 3
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == "LoadBalancer"
        assert service.spec.ports.first().port == 81
        assert route.metadata.name == "nginx-route"
        assert route.spec.to.name == serviceName
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == osProject
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
    }





}
