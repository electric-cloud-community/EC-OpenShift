import spock.lang.*
import com.electriccloud.spec.*
import groovyx.net.http.RESTClient
import groovy.json.JsonBuilder
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PATCH
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT


class OpenShiftHelper extends ContainerHelper {

    static def pluginName = 'EC-OpenShift'

    def promoteKubernetesPlugin() {
        def plugins = dsl "getPlugins()"
        def kubernetes = plugins.plugin.find { it.pluginKey == 'EC-Kubernetes' }
        assert kubernetes : "Kubernetes plugin is not found"
        def pluginName = kubernetes.pluginName
        if (kubernetes.promoted != "1") {
            dsl "promotePlugin $pluginName"
            println "Promoted pluign $pluginName"
        }
        else {
            println "Plugin EC-Kubernetes is promoted"
        }
    }

    def createCluster(projectName, envName, clusterName, configName) {
        createConfig(configName)
        dsl """
            project '$projectName', {
                environment '$envName', {
                    cluster '$clusterName', {
                        pluginKey = '$pluginName'
                        provisionParameter = [
                            config: '$configName',
                            project: 'flowqe-test-project'
                        ]
                        provisionProcedure = 'Check Cluster'
                    }
                }
            }
        """
    }


    def deleteConfig(configName) {
        deleteConfiguration(pluginName, configName)
    }

    def createConfig(configName) {
        def token = System.getenv('OPENSHIFT_TOKEN')
        assert token
        def endpoint = System.getenv('OPENSHIFT_CLUSTER')
        assert endpoint
        def pluginConfig = [
            kubernetesVersion: '1.7',
            clusterEndpoint  : endpoint,
            testConnection   : 'false',
            logLevel         : '2'
        ]
        def props = [:]
        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }
        createPluginConfiguration(
            pluginName,
            configName,
            pluginConfig,
            'test',
            token,
            props
        )
    }

    def cleanupCluster(configName) {
        assert configName
        def procName = 'Cleanup Cluster - Experimental'
        def result = dsl """
            runProcedure(
                projectName: '/plugins/$pluginName/project',
                procedureName: "$procName",
                actualParameter: [
                    namespace: 'default',
                    config: '$configName'
                ]
            )
        """
        assert result.jobId

        def time = 0
        def timeout = 300
        def delay = 50
        while (jobStatus(result.jobId).status != 'completed' && time < timeout) {
            sleep(delay * 1000)
            time += delay
        }

        jobCompleted(result)
    }


    static def deploy(service, deployment) {
        createDeployment(endpoint, token, deployment)
        createService(endpoint, token, service)
    }

    static def cleanupService(name) {
        try {
            deleteDeployment(name)
            deleteService(name)
        } catch (Throwable e) {
            logger.debug(e.getMessage())
        }
    }

    static def createDeployment(endpoint, token, payload) {
        def namespace = 'default'
        def uri = "/apis/extensions/v1beta1/namespaces/${namespace}/deployments"
        request(getEndpoint(),
            uri, POST, null,
            ["Authorization": "Bearer ${getToken()}"],
            new JsonBuilder(payload).toString()
        )
    }

    static def createService(endpoint, token, payload) {
        def namespace = 'default'
        def uri = "/api/v1/namespaces/${namespace}/services"
        request(getEndpoint(), uri, POST, null, ["Authorization": "Bearer ${getToken()}"], new JsonBuilder(payload).toPrettyString())
    }


    static def getService(name) {
        def uri = "/api/v1/namespaces/default/services/${name}"
        request(
            getEndpoint(), uri, GET,
            null, ["Authorization": "Bearer ${getToken()}"], null).data
    }

    static def getDeployment(name) {
        def uri = "/apis/apps/v1beta1/namespaces/default/deployments/${name}"
        request(
            getEndpoint(), uri, GET,
            null, ["Authorization": "Bearer ${getToken()}"], null).data
    }

    static def createSecret(name, url, username, password) {
        def encodedCreds = (username + ":" + password).bytes.encodeBase64().toString()


        def dockerCfgData = ["${url}": [username: username,
                                        password: password,
                                        email   : "none",
                                        auth    : encodedCreds]
        ]
        def dockerCfgJson = new JsonBuilder(dockerCfgData)
        def dockerCfgEnoded = dockerCfgJson.toString().bytes.encodeBase64().toString()
        def secret = [apiVersion: "v1",
                      kind      : "Secret",
                      metadata  : [name: name],
                      data      : [".dockercfg": dockerCfgEnoded],
                      type      : "kubernetes.io/dockercfg"]

        def uri = "/api/v1/namespaces/default/secrets"
        request(getEndpoint(),
            uri, POST, null,
            ["Authorization": "Bearer ${getToken()}"],
            new JsonBuilder(secret).toString()
        )
    }

    static def deleteSecret(name) {
        def uri = "/api/v1/namespaces/default/secrets/$name"
        request(getEndpoint(),
            uri,
            DELETE,
            null,
            ["Authorization": "Bearer ${getToken()}"],
            null
        )
    }

    static def request(requestUrl, requestUri, method, queryArgs, requestHeaders, requestBody) {
        def http = new RESTClient(requestUrl)
        http.ignoreSSLIssues()
        logger.debug(requestBody)

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            logger.debug(uri.path)
            logger.debug(method.toString())
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody

            response.success = { resp, json ->
                [statusLine: resp.statusLine,
                 status    : resp.status,
                 data      : json]
            }

            response.failure = { resp, reader ->
                println resp
                println reader
                throw new RuntimeException("Request failed")
            }

        }
    }

    static def deleteService(serviceName) {
        def uri = "/api/v1/namespaces/default/services/$serviceName"
        request(getEndpoint(), uri, DELETE, null, ["Authorization": "Bearer ${getToken()}"], null)
    }

    static def deleteDeployment(serviceName) {
        def namespace = 'default'
        def headers = ["Authorization": "Bearer ${getToken()}"]
        def uri = "/apis/extensions/v1beta1/namespaces/${namespace}/deployments/$serviceName"
        request(getEndpoint(), uri, DELETE, null,  headers, null)

//        RS


        def res = request(getEndpoint(),
            "/apis/extensions/v1beta1/namespaces/${namespace}/replicasets",
            GET, null, headers, null)

        res.data.items.each {rs ->
            def matcher = rs.metadata.name =~ /(.*)-([^-]+)$/
            try {
                def rsName = matcher[0][1]
                if (rsName == serviceName) {
                    logger.debug("Deleting RS $rsName")
                    request(getEndpoint(), "/apis/extensions/v1beta1/namespaces/${namespace}/replicasets/${rs.metadata.name}", DELETE, null, headers, null)
                }
            } catch (IndexOutOfBoundsException e) {

            }
        }



        res = request(getEndpoint(),
            "/api/v1/namespaces/${namespace}/pods",
            GET,
            null, headers, null)

        res.data.items.each { pod ->
            def matcher = pod.metadata.name =~ /(.*)-([^-]+)-([^-]+)$/
            try {
                def podName = matcher[0][1]
                if (podName == serviceName) {
                    request(getEndpoint(),
                        "/api/v1/namespaces/${namespace}/pods/${pod.metadata.name}",
                        DELETE, null, headers, null
                    )
                }
            } catch (IndexOutOfBoundsException e) {

            }
        }


    }

    static def getToken() {
        def token = System.getenv('OPENSHIFT_TOKEN')
        assert token
        token
    }

    static def getEndpoint() {
        def endpoint = System.getenv('OPENSHIFT_CLUSTER')
        assert endpoint
        endpoint
    }
}
