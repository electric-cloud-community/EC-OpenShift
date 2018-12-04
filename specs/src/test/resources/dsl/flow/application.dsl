package dsl.flow

def names = args.params,
        replicas = names.replicas.toString(),
        sourceVolume = names.sourceVolume,
        targetVolume = names.targetVolume,
        isCanary = names.isCanary.toString(),
        routeHost = names.routeHost,
        serviceType = names.serviceType

project 'openshiftProj', {

    application 'nginx-application', {
        description = ''
        projectName = 'openshiftProj'

        service 'nginx-service', {
            applicationName = 'nginx-application'
            defaultCapacity = replicas.toString()
            maxCapacity = (replicas + 1).toString()
            minCapacity = '1'
            volume = sourceVolume

            container 'nginx-container', {
                description = ''
                applicationName = 'nginx-application'
                cpuCount = '0.1'
                cpuLimit = '2.0'
                imageName = 'tomaskral/nonroot-nginx'
                imageVersion = 'latest'
                memoryLimit = '255'
                memorySize = '128'
                serviceName = 'nginx-service'
                volumeMount = targetVolume

                environmentVariable 'NGINX_PORT', {
                    type = 'string'
                    value = '8080'
                }

                port 'http', {
                    applicationName = 'nginx-application'
                    containerName = 'nginx-container'
                    containerPort = '8080'
                    serviceName = 'nginx-service'
                }
            }

            port '_servicehttpnginx-container01530626345623', {
                applicationName = 'nginx-application'
                listenerPort = '81'
                serviceName = 'nginx-service'
                subcontainer = 'nginx-container'
                subport = 'http'
            }

            process 'Deploy', {
                processType = 'DEPLOY'
                serviceName = 'nginx-service'


                processStep 'deployService', {
                    alwaysRun = '0'
                    errorHandling = 'failProcedure'
                    processStepType = 'service'
                    useUtilityResource = '0'
                }
            }

            process 'Undeploy', {
                processType = 'UNDEPLOY'
                serviceName = 'nginx-service'

                processStep 'Undeploy', {
                    alwaysRun = '0'
                    dependencyJoinType = 'and'
                    errorHandling = 'abortJob'
                    processStepType = 'service'
                    subservice = 'nginx-service'
                    useUtilityResource = '0'
                }
            }
        }

        process 'Deploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Deploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Deploy'
                useUtilityResource = '0'

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Undeploy'
                useUtilityResource = '0'

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }

        tierMap '6892d6bd-7ec9-11e8-bce4-00505696e27a', {
            applicationName = 'nginx-application'
            environmentName = 'openshift-environment'
            environmentProjectName = 'openshiftProj'

            serviceClusterMapping '692504a6-7ec9-11e8-bce4-00505696e27a', {
                actualParameter = [
                        'canaryDeployment': isCanary,
                        'createOrUpdateResource': '0',
                        'deploymentTimeoutInSec': '120',
                        'numberOfCanaryReplicas': replicas.toString(),
                        'requestType': 'update',
                        'routeHostname': routeHost,
                        'routeName': 'nginx-route',
                        'routePath': '/',
                        'routeTargetPort': 'servicehttpnginx-container01530626345623',
                        'serviceType': serviceType,
                        'sessionAffinity': 'None'
                ]
                clusterName = 'kube-cluster'
                serviceName = 'nginx-service'
                tierMapName = '6892d6bd-7ec9-11e8-bce4-00505696e27a'
                volume = null

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = '80579643-7ec9-11e8-bce4-00505696e27a'
                    serviceClusterMappingName = '692504a6-7ec9-11e8-bce4-00505696e27a'
                    volumeMount = null
                }
            }
        }

        property 'ec_deploy', {
            ec_notifierStatus = '0'
        }
        jobCounter = '2'
    }

}