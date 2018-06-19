import spock.lang.*
import com.electriccloud.spec.*

class ImportFromYAML extends OpenShiftHelper {
    static def kubeYAMLFile
    static def projectName = 'EC-OpenShift Specs Import'
    static def applicationScoped = true
    static def applicationName = 'OpenShift Spec App'
    static def envName = 'OpenShift Spec Env'
    static def serviceName = 'openshift-spec-import-test'
    static def clusterName = 'OpenShift Spec Cluster'
    static def configName

    def doSetupSpec() {
        configName = 'OpenShift Spec Config'

        dsl """
            deleteProject(projectName: '$projectName')
        """
        promoteKubernetesPlugin()
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/ImportFromYAML.dsl', [
            projectName: projectName,
            params: [
                osTemplateYaml:       '',
                templateParamValues:  '',
                projName:             '',
                application_scoped:   '',
                application_name:   '',
                envProjectName:     '',
                envName:            '',
                clusterName:       '',
            ]
        ]

    }

    def doCleanupSpec() {

    }

    @Ignore('copy draft')
    def "top level service"() {
        given:
            def sampleName = 'my-service-nginx-deployment'
            cleanupService(sampleName)
            kubeYAMLFile =
'''
kind: Service
apiVersion: v1
metadata:
  name: my-service
spec:
  selector:
    app: nginx
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        kubeYAMLFile: '''$kubeYAMLFile''',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                                            ]
                )
            """
        then:
            logger.debug(result.logs)
            def service = getService(
                projectName,
                sampleName,
                clusterName,
                envName
            )
            assert result.outcome != 'error'
            assert service.service
            assert service.service.defaultCapacity == '3'
            def containers = service.service.container
            assert containers.size() == 1
            assert containers[0].containerName == 'nginx'
            assert containers[0].imageName == 'nginx'
            assert containers[0].imageVersion == '1.7.9'
            def port = containers[0].port[0]
            assert port
            assert port.containerPort == '80'
    }

    @Ignore('copy draft')
    def "application-scoped-service"() {
        given:
        def sampleName = 'my-service-nginx-deployment'
        cleanupService(sampleName)
        kubeYAMLFile =
                '''
kind: Service
apiVersion: v1
metadata:
  name: my-service
spec:
  selector:
    app: nginx
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 256
            cpu: 0.5
          requests:
            memory: 128
            cpu: 0.25
        ports:
        - containerPort: 80
'''.trim()
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        kubeYAMLFile: '''$kubeYAMLFile''',
                        projName: '$projectName',
                        application_scoped: '$applicationScoped',
                        application_name:   '$applicationName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                                            ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getAppScopedService(
                projectName,
                sampleName,
                applicationName,
                clusterName,
                envName
        )
        assert result.outcome != 'error'
        assert service.service
        assert service.service.defaultCapacity == '3'
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == '1.7.9'
        def port = containers[0].port[0]
        assert port
        assert port.containerPort == '80'
    }

    @Ignore('copy')
    def "many top-level services"() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        cleanupService(sampleOneName)
        cleanupService(sampleTwoName)
        kubeYAMLFile =
                '''
kind: Service
apiVersion: v1
metadata:
  name: my-service1
spec:
  selector:
    app: nginx1
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment1
  labels:
    app: nginx1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx1
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 20480k
            cpu: 0.7
          requests:
            memory: 0.5g
            cpu: 0.5
        ports:
        - containerPort: 80
      - name: nginx2
        image: nginx:1.7.10
        resources:
          limits:
            memory: 0.01t
            cpu: 0.7
          requests:
            memory: 0.001p
            cpu: 0.5
        ports:
        - containerPort: 90

---

kind: Service
apiVersion: v1
metadata:
  name: my-service2
spec:
  selector:
    app: nginx2
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment2
  labels:
    app: nginx2
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx2
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 20480k
            cpu: 0.7
          requests:
            memory: 0.5g
            cpu: 0.5
        ports:
        - containerPort: 80
'''.trim()
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        kubeYAMLFile: '''$kubeYAMLFile''',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def serviceOne = getService(
                projectName,
                sampleOneName,
                clusterName,
                envName
        )
        def serviceTwo = getService(
                projectName,
                sampleTwoName,
                clusterName,
                envName
        )


        assert result.outcome != 'error'
        assert serviceOne.service
        assert serviceOne.service.defaultCapacity == '3'
        assert serviceTwo.service
        assert serviceTwo.service.defaultCapacity == '3'
        def containersOne = serviceOne.service.container
        assert containersOne.size() == 2
        assert containersOne[0].containerName == 'nginx'
        assert containersOne[0].imageName == 'nginx'
        assert containersOne[0].imageVersion == '1.7.9'
        assert containersOne[1].containerName == 'nginx2'
        assert containersOne[1].imageName == 'nginx'
        assert containersOne[1].imageVersion == '1.7.10'
        def portOne1 = containersOne[0].port[0]
        assert portOne1
        assert portOne1.containerPort == '80'
        def portOne2 = containersOne[1].port[0]
        assert portOne2
        assert portOne2.containerPort == '90'
        def containersTwo = serviceTwo.service.container
        assert containersTwo.size() == 1
        assert containersTwo[0].containerName == 'nginx'
        assert containersTwo[0].imageName == 'nginx'
        assert containersTwo[0].imageVersion == '1.7.9'
        def portTwo = containersTwo[0].port[0]
        assert portTwo
        assert portTwo.containerPort == '80'

    }

    @Ignore('old copy')
    def "many app-scoped services"() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        cleanupService(sampleOneName)
        cleanupService(sampleTwoName)
        kubeYAMLFile =
                '''
kind: Service
apiVersion: v1
metadata:
  name: my-service1
spec:
  selector:
    app: nginx1
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

kind: Service
apiVersion: v1
metadata:
  name: my-service2
spec:
  selector:
    app: nginx2
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376

    ---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment1
  labels:
    app: nginx1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx1
  template:
    metadata:
      labels:
        app: nginx1
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 20480k
            cpu: 0.7
          requests:
            memory: 0.5g
            cpu: 0.5
        ports:
        - containerPort: 80
      - name: nginx2
        image: nginx:1.7.10
        resources:
          limits:
            memory: 0.01t
            cpu: 0.7
          requests:
            memory: 0.001p
            cpu: 0.5
        ports:
        - containerPort: 90
---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment2
  labels:
    app: nginx2
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx2
  template:
    metadata:
      labels:
        app: nginx2
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        resources:
          limits:
            memory: 20480k
            cpu: 0.7
          requests:
            memory: 0.5g
            cpu: 0.5
        ports:
        - containerPort: 80
'''.trim()
        when:

        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        kubeYAMLFile: '''$kubeYAMLFile''',
                        projName: '$projectName',
                        application_scoped: '$applicationScoped',
                        application_name:   '$applicationName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def serviceOne = getAppScopedService(
                projectName,
                sampleOneName,
                applicationName,
                clusterName,
                envName
        )
        def serviceTwo = getAppScopedService(
                projectName,
                sampleTwoName,
                applicationName,
                clusterName,
                envName
        )


        assert result.outcome != 'error'
        assert serviceOne.service
        assert serviceOne.service.defaultCapacity == '3'
        assert serviceTwo.service
        assert serviceTwo.service.defaultCapacity == '3'
        def containersOne = serviceOne.service.container
        assert containersOne.size() == 2
        assert containersOne[0].containerName == 'nginx'
        assert containersOne[0].imageName == 'nginx'
        assert containersOne[0].imageVersion == '1.7.9'
        assert containersOne[1].containerName == 'nginx2'
        assert containersOne[1].imageName == 'nginx'
        assert containersOne[1].imageVersion == '1.7.10'
        def portOne1 = containersOne[0].port[0]
        assert portOne1
        assert portOne1.containerPort == '80'
        def portOne2 = containersOne[1].port[0]
        assert portOne2
        assert portOne2.containerPort == '90'
        def containersTwo = serviceTwo.service.container
        assert containersTwo.size() == 1
        assert containersTwo[0].containerName == 'nginx'
        assert containersTwo[0].imageName == 'nginx'
        assert containersTwo[0].imageVersion == '1.7.9'
        def portTwo = containersTwo[0].port[0]
        assert portTwo
        assert portTwo.containerPort == '80'

    }

    @IgnoreRest
    def 'import routes'() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        def serviceName = 'my-service-with-routes'
        def routeHostname = 'routeHostname'

        def yamlContent =
                """
apiVersion: v1
kind: Template
metadata:
  name: nginx-app
objects:
- apiVersion: v1
  kind: Service
  metadata:
    name: $serviceName
    namespace: flowqe-test-project
    selfLink: /api/v1/namespaces/flowqe-test-project/services/nginx-service
  spec:
    clusterIP: 172.30.134.16
    externalTrafficPolicy: Cluster
    ports:
      - name: servicehttpnginx-container01523616023078
        nodePort: 31985
        port: 81
        protocol: TCP
        targetPort: http
    selector:
      ec-svc: $serviceName
    sessionAffinity: None
    type: LoadBalancer
- apiVersion: route.openshift.io/v1
  kind: Route
  metadata:
    name: nginx-route
    namespace: flowqe-test-project
    selfLink: /apis/route.openshift.io/v1/namespaces/flowqe-test-project/routes/nginx-route
  spec:
    host: $routeHostname
    path: /
    port:
      targetPort: servicehttpnginx-container01523616023078
    to:
      kind: Service
      name: $serviceName
      weight: 100
    wildcardPolicy: None
  status:
    ingress:
      - conditions:
        host: $routeHostname
        routerName: router
- apiVersion: apps/v1
  kind: Deployment
  metadata:
    annotations:
      deployment.kubernetes.io/revision: '1'
    generation: 1
    labels:
      ec-svc: $serviceName
      ec-track: stable
    name: $serviceName
    namespace: flowqe-test-project
    selfLink: /apis/apps/v1/namespaces/flowqe-test-project/deployments/nginx-service
  spec:
    progressDeadlineSeconds: 120
    replicas: 2
    revisionHistoryLimit: 10
    selector:
      matchLabels:
        ec-svc: $serviceName
        ec-track: stable
    strategy:
      rollingUpdate:
        maxSurge: 1
        maxUnavailable: 1
      type: RollingUpdate
    template:
      metadata:
        creationTimestamp: null
        labels:
          ec-svc: $serviceName
          ec-track: stable
        name: $serviceName
      spec:
        containers:
          - env:
              - name: NGINX_PORT
                value: '8080'
            image: 'tomaskral/nonroot-nginx:latest'
            imagePullPolicy: Always
            name: nginx-container
            ports:
              - containerPort: 8080
                name: http
                protocol: TCP
            resources:
              limits:
                cpu: '2'
                memory: 255M
              requests:
                cpu: 100m
                memory: 128M
            terminationMessagePath: /dev/termination-log
            terminationMessagePolicy: File
            volumeMounts:
              - mountPath: /usr/share/nginx/html
                name: html-content
        volumes:
          - hostPath:
              path: /var/html
              type: ''
            name: html-content

""".trim()
        when:

        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$yamlContent''',
                        projName: '$projectName',
                        application_scoped: '0',
                        application_name:   '',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(projectName, serviceName, clusterName, envName)
        logger.debug(objectToJson(service))
        assert getMappingDetail(service, 'routeHostname') == routeHostname
        assert getMappingDetail(service, 'routeName') == 'nginx-route'
        assert getMappingDetail(service, 'routePath') == '/'
        assert getMappingDetail(service, 'routeTargetPort')
        cleanup:
        deleteService(projectName, serviceName)
    }

    @IgnoreRest
    def 'import routes with warning (two routes per service)'() {
        given:
        def serviceName = 'my-service-with-routes'
        def routeHostname = 'routeHostname'

        def yamlContent =
                """
apiVersion: v1
kind: Template
metadata:
  name: $serviceName
objects:
- apiVersion: v1
  kind: Service
  metadata:
    creationTimestamp: null
    labels:
      ec-svc-id: 1285c29f-4182-11e8-beaf-b29bcf5f67b5
    name: $serviceName
  spec:
    deprecatedPublicIPs:
    - 172.29.100.131
    externalIPs:
    - 172.29.100.131
    ports:
    - name: servicehttptomcat01519748102941
      nodePort: 30270
      port: 8081
      protocol: TCP
      targetPort: http
    selector:
      ec-svc: $serviceName
    sessionAffinity: None
    type: LoadBalancer
  status:
    loadBalancer: {}
- apiVersion: v1
  kind: Route
  metadata:
    creationTimestamp: null
    name: test1
  spec:
    host: 10.200.1.101
    port:
      targetPort: servicehttptomcat01519748102941
    to:
      kind: Service
      name: $serviceName
      weight: 100
    wildcardPolicy: None
  status:
    ingress:
    - conditions:
      - lastTransitionTime: 2018-06-18T13:04:05Z
        status: "True"
        type: Admitted
      host: 10.200.1.101
      routerName: router
      wildcardPolicy: None
- apiVersion: v1
  kind: Route
  metadata:
    creationTimestamp: null
    name: test2
  spec:
    host: 10.200.1.102
    port:
      targetPort: servicehttptomcat01519748102941
    to:
      kind: Service
      name: $serviceName
      weight: 100
    wildcardPolicy: None
  status:
    ingress:
    - conditions:
      - lastTransitionTime: 2018-06-18T13:04:18Z
        status: "True"
        type: Admitted
      host: 10.200.1.102
      routerName: router
      wildcardPolicy: None
- apiVersion: extensions/v1beta1
  kind: Deployment
  metadata:
    annotations:
      deployment.kubernetes.io/revision: "1"
    creationTimestamp: null
    generation: 1
    labels:
      ec-svc: $serviceName
      ec-track: stable
    name: $serviceName
  spec:
    progressDeadlineSeconds: 120
    replicas: 1
    selector:
      matchLabels:
        ec-svc: $serviceName
        ec-track: stable
    strategy:
      rollingUpdate:
        maxSurge: 1
        maxUnavailable: 1
      type: RollingUpdate
    template:
      metadata:
        creationTimestamp: null
        labels:
          ec-svc: $serviceName
          ec-track: stable
        name: $serviceName
      spec:
        containers:
        - image: tomcat:7.0
          imagePullPolicy: IfNotPresent
          name: tomcat
          ports:
          - containerPort: 8080
            name: http
            protocol: TCP
          resources:
            limits:
              cpu: "2"
              memory: 255M
            requests:
              cpu: 100m
              memory: 128M
          terminationMessagePath: /dev/termination-log
        dnsPolicy: ClusterFirst
        restartPolicy: Always
        securityContext: {}
        terminationGracePeriodSeconds: 30
  status:
    availableReplicas: 1
    conditions:
    - lastTransitionTime: 2018-06-18T13:03:16Z
      lastUpdateTime: 2018-06-18T13:03:16Z
      message: Deployment has minimum availability.
      reason: MinimumReplicasAvailable
      status: "True"
      type: Available
    - lastTransitionTime: 2018-06-18T13:03:16Z
      lastUpdateTime: 2018-06-18T13:03:16Z
      message: Replica set "microservice-canary-2132197782" has successfully progressed.
      reason: NewReplicaSetAvailable
      status: "True"
      type: Progressing
    observedGeneration: 1
    replicas: 1
    updatedReplicas: 1
kind: List
metadata: {}
""".trim()
        when:

        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$yamlContent''',
                        projName: '$projectName',
                        application_scoped: '0',
                        application_name:   '',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(projectName, serviceName, clusterName, envName)
        logger.debug(objectToJson(service))
        assert result.logs =~ /Only one route per service is allowed in ElectricFlow. The route/
        cleanup:
        deleteService(projectName, serviceName)
    }

    def getMappingDetail(service, name) {
      def parameterDetail = service.service?.parameterDetail.find {
        it.parameterName == name
      }.parameterValue
      return parameterDetail
    }


}
