package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.models.enums.LogLevels.LogLevel.*
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*
import static org.awaitility.Awaitility.await

@Feature("Deployment")
class ApplicationDeploymentTests extends OpenshiftTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        osClient.deleteConfiguration(configName)
        osClient.createConfiguration(configName, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, DEBUG)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        osClient.deleteConfiguration(configName)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        osClient.createEnvironment(configName, osProject)
        osClient.createApplication(2, volumes, false, getHost(clusterEndpoint), LOAD_BALANCER)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest() {
        osClient.cleanUpCluster(configName, osProject)
        osClient.undeployApplication(projectName, applicationName)
        osClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    @TmsLink("325119")
    @Description("Deploy Application-level Microservice")
    void deployApplicationLevelMicroservice() {
        osClient.deployApplication(projectName, applicationName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 2
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == LOAD_BALANCER.value
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
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
    }



    @Test(groups = "Positive")
    @TmsLink("325120")
    @Description(" Update Application-level Microservice")
    void updateApplicationLevelMicroservice() {
        osClient.deployApplication(projectName, applicationName)
        osClient.createApplication(3, volumes, false, getHost(clusterEndpoint))
        osClient.deployApplication(projectName, applicationName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 3
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == LOAD_BALANCER.value
        assert service.spec.ports.first().port == 81
        assert route.metadata.name == "nginx-route"
        assert route.spec.to.name == serviceName
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
    }



    @Test(groups = "Positive")
    @TmsLink("325121")
    @Description("Canary Deploy for Application-level Microservice")
    void canaryDeployForApplicationLevelMicroservice() {
        osClient.deployApplication(projectName, applicationName)
        osClient.createApplication(2, volumes, true, getHost(clusterEndpoint), LOAD_BALANCER)
        osClient.deployApplication(projectName, applicationName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 4
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == LOAD_BALANCER.value
        assert service.spec.ports.first().port == 81
        assert route.metadata.name == "nginx-route"
        assert route.spec.to.name == serviceName
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == osProject
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.last().metadata.generateName.startsWith('nginx-service-canary-')
        assert pods.last().metadata.namespace == osProject
        assert pods.last().metadata.labels.get('ec-svc') == serviceName
        assert pods.last().metadata.labels.get('ec-track') == "canary"
        pods.each {
            assert it.spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
            assert it.spec.containers.first().ports.first().containerPort == 8080
            assert it.spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
            assert it.spec.containers.first().env.first().value == "8080"
            assert it.spec.containers.first().env.first().name == "NGINX_PORT"
            assert it.status.phase == "Running"
        }
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
    }



    @Test(groups = "Positive")
    @TmsLink("325122")
    @Description("Undeploy Application-level Microservice ")
    void undeployApplicationLevelMicroservice() {
        osClient.deployApplication(projectName, applicationName)
        osClient.undeployApplication(projectName, applicationName)
        await("Wait for Pod size to be: \'0\'").until {
            osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }.size() == 0
        }
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def routes = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        assert services.size() == 0
        assert routes.size() == 0
        assert pods.size() == 0
    }


    @Test(groups = "Positive")
    @TmsLink("325123")
    @Description("Undeploy Application-level Microservice after Canary Deploy")
    void undeployApplicationLevelMicroserviceAfterCanaryDeploy() {
        osClient.deployApplication(projectName, applicationName)
        osClient.createApplication(2, volumes, true, getHost(clusterEndpoint))
        osClient.deployApplication(projectName, applicationName)
        osClient.undeployApplication(projectName, applicationName)
        await("Wait for Pod size to be: \'2\'").until{
            osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }.size() == 2
        }
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def routes = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        assert routes.size() == 0
        assert services.size() == 1
        assert pods.size() == 2
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == LOAD_BALANCER.value
        assert service.spec.ports.first().port == 81
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == osProject
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
    }






}
