package com.electriccloud.plugin.spec

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
    static final def namespace = 'flowqe-test-project'

    def createCluster(projectName, envName, clusterName, configName, project = 'flowqe-test-project') {
        createConfig(configName)
        dsl """
            project '$projectName', {
                environment '$envName', {
                    cluster '$clusterName', {
                        pluginKey = '$pluginName'
                        provisionParameter = [
                            config: '$configName',
                            project: '$project'
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
            kubernetesVersion: getClusterVersion(),
            clusterEndpoint  : endpoint,
            testConnection   : 'false',
            logLevel         : '4'
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

    static def deployConfig(service, deployment) {
        createDeploymentConfig(deployment)
        createService(endpoint, token, service)
    }

    static def cleanupService(name) {
        try {
            deleteDeployment(name)
            deleteService(name)
            deleteRoute(name)
        } catch (Throwable e) {
            logger.debug(e.getMessage())
        }
    }

    static def cleanupDeploymentConfig(name) {
        try {
            deleteDeploymentConfig(name)
            deleteService(name)
        } catch (Throwable e) {
            logger.debug(e.message)
        }
    }

    static def cleanupRoute(name) {
        try {
            deleteRoute(name)
        } catch (Throwable e) {
            logger.debug(e.message)
        }
    }

    static def createDeployment(endpoint, token, payload) {
        def uri = "/apis/extensions/v1beta1/namespaces/${namespace}/deployments"
        request(getEndpoint(),
            uri, POST, null,
            ["Authorization": "Bearer ${getToken()}"],
            new JsonBuilder(payload).toString()
        )
    }


    static def createDeploymentConfig(payload) {
        def uri = "/oapi/v1/namespaces/${namespace}/deploymentconfigs"
        request(getEndpoint(), uri, POST, null, ["Authorization": "Bearer ${getToken()}"], new JsonBuilder(payload).toString())
    }

    static def deleteDeploymentConfig(name) {
        def uri = "/oapi/v1/namespaces/${namespace}/deploymentconfigs/${name}"
        request(getEndpoint(), uri, DELETE, null, ["Authorization": "Bearer ${getToken()}"], null)
    }

    static def createService(endpoint, token, payload) {
        def uri = "/api/v1/namespaces/${namespace}/services"
        request(getEndpoint(), uri, POST, null, ["Authorization": "Bearer ${getToken()}"], new JsonBuilder(payload).toPrettyString())
    }

    static def createRoute(payload) {
        def uri = "/oapi/v1/namespaces/${namespace}/routes"
        request(getEndpoint(), uri, POST, null, ["Authorization": "Bearer ${getToken()}"], new JsonBuilder(payload).toPrettyString())
    }

    static def getService(name) {
        def uri = "/api/v1/namespaces/${namespace}/services/${name}"
        request(
            getEndpoint(), uri, GET,
            null, ["Authorization": "Bearer ${getToken()}"], null).data
    }

    static def getDeployment(name) {
        def uri = "/apis/apps/v1beta1/namespaces/${namespace}/deployments/${name}"
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

        def uri = "/api/v1/namespaces/${namespace}/secrets"
        request(getEndpoint(),
            uri, POST, null,
            ["Authorization": "Bearer ${getToken()}"],
            new JsonBuilder(secret).toString()
        )
    }

    static def deleteSecret(name) {
        def uri = "/api/v1/namespaces/${namespace}/secrets/$name"
        request(getEndpoint(),
            uri,
            DELETE,
            null,
            ["Authorization": "Bearer ${getToken()}"],
            null
        )
    }

    static def deleteRoute(routeName) {
        def uri = "/oapi/v1/namespaces/${namespace}/routes/${routeName}"
        request(getEndpoint(),
            uri,
            DELETE,
            null,
            ["Authorization" :"Bearer ${getToken()}"],
            null
        )
    }

    static def request(requestUrl, requestUri, method, queryArgs, requestHeaders, requestBody) {
        def http = new RESTClient(requestUrl)
        http.ignoreSSLIssues()

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody
            logger.debug("Method: $method, URI: $uri, Body: $requestBody")

            response.success = { resp, json ->
                [statusLine: resp.statusLine,
                 status    : resp.status,
                 data      : json]
            }

            response.failure = { resp, reader ->
                println "Failute: ${resp}"
                println "Failure: ${reader}"
                throw new RuntimeException("Request failed ${resp} ${reader}")
            }

        }
    }

    static def deleteService(serviceName) {
        def uri = "/api/v1/namespaces/${namespace}/services/$serviceName"
        request(getEndpoint(), uri, DELETE, null, ["Authorization": "Bearer ${getToken()}"], null)
    }

    static def deleteDeployment(serviceName) {
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

    static def getClusterVersion() {
        def version = System.getenv('OPENSHIFT_CLUSTER_VERSION') ?: '1.9'
        assert version
        return version
    }

    def deploySample(serviceName) {
        def deployment = [
            kind    : 'Deployment',
            metadata: [
                name: serviceName,
            ],
            spec    : [
                replicas: 1,
                template: [
                    spec    : [
                        containers: [
                            [
                                name        : 'nginx',
                                image       : 'nginx:1.10',
                                ports       : [[containerPort: 80]],
                                env         : [
                                    [name: "TEST_ENV", "value": "TEST"]
                                ],
                                volumeMounts: [
                                    [name: 'my-volume', mountPath: '/tmp/path_in_container']
                                ]
                            ]
                        ],
                        volumes   : [
                            [hostPath: [path: '/tmp/path'], name: 'my-volume']
                        ]
                    ],
                    metadata: [labels: [app: 'nginx_test_spec']],

                ]
            ]
        ]

        def service = [
            kind      : 'Service',
            apiVersion: 'v1',
            metadata  : [name: serviceName],
            spec      : [
                selector: [app: 'nginx_test_spec'],
                ports   : [[protocol: 'TCP', port: 80, targetPort: 80]],
            ]
        ]
        deploy(service, deployment)
    }

}
