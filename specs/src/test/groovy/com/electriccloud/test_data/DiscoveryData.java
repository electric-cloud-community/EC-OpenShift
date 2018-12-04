package com.electriccloud.test_data;

import com.electriccloud.procedures.OpenshiftTestBase;
import org.testng.annotations.DataProvider;

public class DiscoveryData extends OpenshiftTestBase {



    @DataProvider(name = "invalidDiscoveryData")
    public Object[][] getDiscoveryData() {
        return new Object[][]{
                {
                    "", projectName, environmentName, clusterName, osProject, clusterEndpoint, clusterToken,
                        "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, "", environmentName, clusterName, osProject, clusterEndpoint, clusterToken,
                        "One or more arguments are missing. Please provide the following arguments: projectName"
                },
                {
                    projectName, projectName, "", clusterName, osProject, clusterEndpoint, clusterToken,
                        "\'environmentName\' must be between 1 and 255 characters"
                },
                {
                    projectName, projectName, environmentName, "", osProject, clusterEndpoint, clusterToken,
                        "Please provide the following arguments: clusterName"
                },
                {
                    "MyTestProject", projectName, environmentName, clusterName, osProject, clusterEndpoint, clusterToken,
                        "Project \'MyTestProject\' does not exist"
                }

        };
    }

}



