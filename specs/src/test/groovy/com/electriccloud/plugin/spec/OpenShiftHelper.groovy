package com.electriccloud.plugin.spec

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import spock.lang.*

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

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

    static def deployConfig(service, deployment) {
        createDeploymentConfig(deployment)
        createService(endpoint, token, service)
    }

    static def cleanupService(name) {
        try {
            deleteDeployment(name)
        } catch (Throwable e) {
            logger.debug(e.getMessage())
        }
        try {
            deleteService(name)
        } catch (Throwable e) {
            logger.debug(e.getMessage())
        }
        try {
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
        def apiPath = versionSpecificAPIPath('deployments')
        println apiPath
        def uri = "/apis/${apiPath}/namespaces/${namespace}/deployments"
        request(getEndpoint(),
                uri, POST, null,
                ["Authorization": "Bearer ${getToken()}"],
                new JsonBuilder(payload).toString()
        )
    }

    static boolean isVersionGreaterThan17() {
        return false
//        Different payload
        try {
            float version = Float.parseFloat(getClusterVersion())
            version >= 1.8
        } catch (NumberFormatException ex) {
            true
        }
    }

    static boolean isVersionGreaterThan15() {
        try {
            float version = Float.parseFloat(getClusterVersion())
            version >= 1.6
        } catch (NumberFormatException ex) {
            // default to considering this > 1.5 version
            true
        }
    }


    static String versionSpecificAPIPath(String resource) {
        switch (resource) {
            case 'deployments':
                return isVersionGreaterThan15() ? (isVersionGreaterThan17() ? 'apps/v1beta2' : 'apps/v1beta1') : 'extensions/v1beta1'
            default:
                throw new RuntimeException('unsupported resource')
        }
    }

    def getRoutes(name) {
        def uri = "/oapi/v1/namespaces/${namespace}/routes/${name}"
        def response = request(getEndpoint(), uri, GET, null, ["Authorization": "Bearer ${getToken()}"], null)
        logger.debug("Routes: ${response.logs}")
        return response.data
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
        def apiPath = versionSpecificAPIPath('deployments')
        def uri = "/apis/${apiPath}/namespaces/${namespace}/deployments/${name}"
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
                ["Authorization": "Bearer ${getToken()}"],
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
        def apiPath = versionSpecificAPIPath('deployments')

        def headers = ["Authorization": "Bearer ${getToken()}"]
        def uri = "/apis/${apiPath}/namespaces/${namespace}/deployments/$serviceName"
        request(getEndpoint(), uri, DELETE, null, headers, null)

//        RS


        def res = request(getEndpoint(),
                "/apis/${apiPath}/namespaces/${namespace}/replicasets",
                GET, null, headers, null)

        res.data.items.each { rs ->
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


    def getSelector(serviceName) {
        def selector = "${randomize(serviceName)}"
        selector = selector.replaceAll(/-/, '')
        if (selector.length() > 60)
            selector = selector.substring(0, 60)
        return selector
    }


    def deploySample(serviceName) {
        def selector = getSelector(serviceName)
        selector = 'test-smart-map'
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
                                metadata: [labels: [app: selector]],

                        ]
                ]
        ]

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector: [app: selector],
                        ports   : [[protocol: 'TCP', port: 80, targetPort: 80]],
                ]
        ]
        deploy(service, deployment)
    }


    def deployConfig(serviceName) {
        def selector = getSelector(serviceName)

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        type    : 'LoadBalancer',
                        selector: [app: selector],
                        ports   : [
                                [protocol: 'TCP', port: 80, targetPort: 'first', name: 'first'],
                                [protocol: 'TCP', port: 81, targetPort: 'second', name: 'second']
                        ]
                ]
        ]

        def deployment = [
                kind    : 'DeploymentConfig',
                metadata: [
                        name: serviceName,
                ],
                spec    : [
                        replicas: 1,
                        template: [
                                spec    : [
                                        containers: [
                                                [name: 'hello', image: 'imagostorm/hello-world:1.0', ports: [
                                                        [containerPort: 8080, name: 'first']
                                                ]],
                                                [name: 'hello-2', 'image': 'imagostorm/hello-world:2.0', ports: [
                                                        [containerPort: 8080, name: 'second']
                                                ]]
                                        ]
                                ],
                                metadata: [
                                        labels: [
                                                app: selector
                                        ]
                                ]
                        ]
                ]
        ]

        deployConfig(service, deployment)
    }

    def deployWithRoutes(serviceName) {
        def selector = getSelector(serviceName)
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
                                metadata: [labels: [app: selector]],

                        ]
                ]
        ]

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector: [app: selector],
                        ports   : [[protocol: 'TCP', port: 80, targetPort: 80]],
                ]
        ]

        def route = [
                kind    : 'Route',
                metadata: [name: serviceName],
                spec    : [
                        host: '10.200.1.100', path: '/', port: [targetPort: 'test'], to: [kind: 'Service', name: serviceName]
                ]
        ]
        deploy(service, deployment)
        logger.debug("Created service $serviceName")
        createRoute(route)
        logger.debug("Created route $serviceName")
    }

    def deployTwoContainers(serviceName) {
        def selector = getSelector(serviceName)

        def deployment = [
                kind    : 'Deployment',
                metadata: [
                        name: serviceName,
                ],
                spec    : [
                        replicas: 2,
                        template: [
                                spec    : [
                                        containers: [
                                                [name: 'hello', image: 'imagostorm/hello-world:1.0', ports: [
                                                        [containerPort: 8080, name: 'first']
                                                ]],
                                                [name: 'hello-2', 'image': 'imagostorm/hello-world:2.0', ports: [
                                                        [containerPort: 8080, name: 'second']
                                                ]]
                                        ]
                                ],
                                metadata: [
                                        labels: [
                                                app: selector
                                        ]
                                ]
                        ]
                ]
        ]
        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        type    : 'LoadBalancer',
                        selector: [app: selector],
                        ports   : [
                                [protocol: 'TCP', port: 80, targetPort: 'first', name: 'first'],
                                [protocol: 'TCP', port: 81, targetPort: 'second', name: 'second']
                        ]
                ]
        ]

        deploy(service, deployment)
    }


    def deployWithPercentage(serviceName) {
        def deployment = [
                kind    : 'Deployment',
                metadata: [
                        name: serviceName,
                ],
                spec    : [
                        replicas: 3,
                        strategy: [
                                rollingUpdate: [
                                        maxSurge      : '25%',
                                        maxUnavailable: '25%'
                                ]
                        ],
                        template: [
                                spec    : [
                                        containers: [
                                                [name: 'nginx', image: 'nginx:1.10', ports: [
                                                        [containerPort: 80]
                                                ]]
                                        ],
                                ],
                                metadata: [labels: [app: 'nginx_test_spec']]
                        ]
                ]
        ]

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector: [app: 'nginx_test_spec'],
                        ports   : [[protocol: 'TCP', port: 80, targetPort: 80]]
                ]
        ]
        deploy(service, deployment)
    }


    def deployWithLoadBalancer(serviceName) {
        def selector = getSelector(serviceName)
        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector      : [app: selector],
                        type          : 'LoadBalancer',
                        loadBalancerIP: '35.224.8.81',
                        ports         : [
                                [port: 80, targetPort: 80]
                        ]
                ]
        ]
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
                                metadata: [labels: [app: selector]],

                        ]
                ]
        ]

        deploy(service, deployment)
    }

    def deployWithSecret(serviceName) {
        def secretName = randomize('spec-secret')
        secretName = secretName.replaceAll('_', '-')
        createSecret(secretName, 'registry.hub.docker.com', 'ecplugintest', 'qweqweqwe')
        def deployment = [
                kind    : 'Deployment',
                metadata: [
                        name: serviceName,
                ],
                spec    : [
                        replicas: 1,
                        template: [
                                spec    : [
                                        containers      : [
                                                [name: 'hello', image: 'registry.hub.docker.com/imagostorm/hello-world:1.0', ports: [
                                                        [containerPort: 80]
                                                ]]
                                        ],
                                        imagePullSecrets: [
                                                [name: secretName]
                                        ]
                                ],
                                metadata: [
                                        labels: [
                                                app: 'nginx_test_spec'
                                        ]
                                ]
                        ]
                ]
        ]

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector: [app: 'nginx_test_spec'],
                        ports   : [[protocol: 'TCP', port: 80, targetPort: 80]]
                ]
        ]
        deploy(service, deployment)
        secretName
    }

    def deployLiveness(serviceName) {

        def container = [
                args          : ['/server'],
                image         : 'k8s.gcr.io/liveness',
                livenessProbe : [
                        httpGet            : [
                                path       : '/healthz',
                                port       : 8080,
                                httpHeaders: [
                                        [name: 'X-Custom-Header', value: 'Awesome']
                                ]
                        ],
                        initialDelaySeconds: 15,
                        timeoutSeconds     : 1
                ],
                readinessProbe: [
                        exec               : [
                                command: [
                                        'cat',
                                        '/tmp/healthy'
                                ]
                        ],
                        initialDelaySeconds: 5,
                        periodSeconds      : 5,
                ],
                name          : 'liveness-readiness'
        ]
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
                                                container
                                        ],
                                ],
                                metadata: [
                                        labels: [
                                                app: 'liveness-probe'
                                        ]
                                ]
                        ]
                ]
        ]

        def service = [
                kind      : 'Service',
                apiVersion: 'v1',
                metadata  : [name: serviceName],
                spec      : [
                        selector: [app: 'liveness-probe'],
                        ports   : [[protocol: 'TCP', port: 80, targetPort: 8080]]
                ]
        ]

        deploy(service, deployment)

    }

    def deployRoute(serviceName, routeName) {
        def route = [
                kind    : 'Route',
                metadata: [name: routeName],
                spec    : [
                        host: '10.200.1.100', path: '/', port: [targetPort: 'test'], to: [kind: 'Service', name: serviceName]
                ]
        ]
        createRoute(route)
    }


}
