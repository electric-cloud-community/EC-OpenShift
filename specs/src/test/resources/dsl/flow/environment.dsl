package dsl.flow

def names = args.params,
        configName = names.configName,
        osProject = names.osProject

project 'openshiftProj', {

        environment 'openshift-environment', {
                environmentEnabled = '1'
                projectName = 'openshiftProj'
                reservationRequired = '0'
                rollingDeployEnabled = null
                rollingDeployType = null

                cluster 'kube-cluster', {
                        environmentName = 'openshift-environment'
                        pluginKey = 'EC-OpenShift'
                        pluginProjectName = null
                        providerClusterName = null
                        providerProjectName = null
                        provisionParameter = [
                                'config': configName,
                                'project': osProject,
                        ]
                        provisionProcedure = 'Check Cluster'

                        // Custom properties

                        property 'ec_provision_parameter', {
                                config = configName
                                property 'project', value: osProject, {
                                        expandable = '1'
                                }
                        }
                }
        }
}