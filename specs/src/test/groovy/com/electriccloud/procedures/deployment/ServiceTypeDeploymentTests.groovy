package com.electriccloud.procedures.deployment



import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import static com.electriccloud.models.enums.LogLevels.*
import static com.electriccloud.models.enums.LogLevels.LogLevel.*
import static com.electriccloud.models.enums.ServiceTypes.*
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*


@Feature("Deployment")
class ServiceTypeDeploymentTests extends OpenshiftTestBase {


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
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest() {
        osClient.cleanUpCluster(configName, osProject)
        osClient.client.deleteProject(projectName)
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Deploy service using LoadBalancer service type")
    @Description("Deploy Project-level Microservice with LoadBalancer service type")
    void deployMicroserviceWithLoadBalancer(){
        osClient.createService(2, volumes, false, getHost(clusterEndpoint), LOAD_BALANCER)
        osClient.deployService(projectName, serviceName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 2
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == LOAD_BALANCER.getValue()
        assert service.status.loadBalancer.ingress.size() == 1
        assert service.status.loadBalancer.ingress.first().ip !=  null
        assert service.spec.ports.first().nodePort != null
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
    @TmsLink("")
    @Story("Deploy service using ClusterIP service type")
    @Description("Deploy Project-level Microservice with ClusterIP service type")
    void deployMicroserviceWithClusterIP(){
        osClient.createService(2, volumes, false, getHost(clusterEndpoint), CLUSTER_IP)
        osClient.deployService(projectName, serviceName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 2
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == CLUSTER_IP.getValue()
        assert service.status.loadBalancer.ingress.size() == 0
        assert service.spec.ports.first().nodePort == null
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
    @TmsLink("")
    @Story("Deploy service using NodePort service type")
    @Description("Deploy Project-level Microservice with NodePort service type")
    void deployMicroserviceWithNodePort(){
        osClient.createService(2, volumes, false, getHost(clusterEndpoint), NODE_PORT)
        osClient.deployService(projectName, serviceName)
        def services = osApi.client.services().list().getItems().findAll { it.metadata.namespace == osProject }
        def service = services.sort { it.metadata.name == serviceName }.first()
        def route = osApi.client.routes().list().getItems().findAll { it.metadata.namespace == osProject }.first()
        def pods = osApi.client.pods().list().getItems().findAll { it.metadata.namespace == osProject }
        def resp = req.get("http://${getHost(clusterEndpoint)}/")
        assert services.size() == 1
        assert pods.size() == 2
        assert service.metadata.name == serviceName
        assert service.metadata.namespace == osProject
        assert service.spec.type == NODE_PORT.getValue()
        assert service.status.loadBalancer.ingress.size() == 0
        assert service.spec.ports.first().nodePort != null
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




}
