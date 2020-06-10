import java.io.File

procedure 'Discover',
	description: '''<html>Automatically create microservice models in CloudBees CD for the services and the pods discovered within a project on a OpenShift cluster.
<div>
    <ol>
        <li><b>Select your method of discovery from a OpenShift Cluster</b>  There are two options for connecting to OpenShift for discovery
            <ul>
                <li><b>Existing CloudBees CD Environment and Cluster</b>  Use the Cluster configuration details in an existing CloudBees CD environment to connect to OpenShift. Enter details for the existing environment and cluster in the following parameters:
                    <ul>
                        <li>Environment Project Name: The project containing the existing environment</li>
                        <li>Environment Name:  the name of an existing environment that contains the OpenShift backend cluster to be discovered</li>
                        <li>Cluster Name: The name of the CloudBees CD cluster in the environment above that represents the OpenShift cluster</li>
                    </ul></li>
                <li><b>OpenShift Connection Details</b>  Enter OpenShift endpoint and Account details to directly connect to the endpoint and discover the clusters and pods.  Enter the endpoint and account details in the following parameters:
                    <ul>
                        <li>OpenShift Endpoint: The endpoint where the OpenShift endpoint will be reachable</li>
                        <li>Service Account API Token</li>
                        <li><i>If selecting this connection option, you can optionally enter a new values for Environment Name and Cluster Name parameters, to create a new environment and cluster in CloudBees CD based on the discovered services and pods.</i></li>
                    </ul>
                </li>
            </ul></li>
        <li><b>Determine how the discovered microservices will be created in CloudBees CD</b>
            <ul>
                <li><b>Create the microservices individually at the top-level within the project.</b> All discovered microservices will be created at the top-level.  Enter the following parameters:
                    <ul>
                        <li>Project Name: Enter the name of the project where the microservices will be created</li>
                    </ul>
                </li>
                <li><b>Create the Microservices within an application in CloudBees CD.</b> All discovered microservices will be created as services within a new application. Enter the following parameters:
                    <ul>
                        <li>Project Name: Enter the name of the project where the new application will be created</li>
                        <li>Create Microservices within and Application:  Select the checkbox</li>
                        <li>Application Name:  The name of a new application which will be created in CloudBees CD containing the discovered services</li>
                    </ul>
                </li></ul>
        </li>
    </ol>
</div>
</html>''', {

    //Using a simple description for use with the step picker since it cannot handle HTML content
    property 'stepPickerDescription',
        value: 'Automatically create microservice models in CloudBees CD for the services and the pods discovered within a project on a OpenShift cluster.'

    step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

	step 'discover',
    	  command: new File(pluginDir, 'dsl/procedures/discover/steps/discover.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  resourceName: '$[grabbedResource]',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'
}

