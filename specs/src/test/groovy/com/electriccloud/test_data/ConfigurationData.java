package com.electriccloud.test_data;

import com.electriccloud.models.enums.LogLevels;
import com.electriccloud.procedures.OpenshiftTestBase;
import org.testng.annotations.DataProvider;

import static com.electriccloud.models.enums.LogLevels.*;
import static com.electriccloud.models.enums.LogLevels.LogLevel.*;
import static com.electriccloud.procedures.NamingTestBase.clusterEndpoint;
import static com.electriccloud.procedures.NamingTestBase.serviceaccount;

public class ConfigurationData extends OpenshiftTestBase  {


    @DataProvider(name = "clusterVersions")
    public Object[][] getClusterVersions(){
        return new Object[][]{
                {"1.5"},
                {"1.6"},
                {"1.7"},
                {"1.8"},
                {"1.9"},
                {"1.10"},
                {"1.11"}
        };
    }


    @DataProvider(name = "logLevels")
    public Object[][] getLogLevels(){
        return new Object[][]{
                {DEBUG,"logger DEBUG", "[DEBUG]", "[ERROR]"},
                {INFO, "logger INFO", "[INFO]", "[DEBUG]"},
                {WARNING, "logger WARNING", "[INFO]", "[DEBUG]"},
                {ERROR, "logger ERROR", "[INFO]", "[DEBUG]"},
        };
    }



    @DataProvider(name = "invalidData")
    public Object[][] getInvalidData(){
        return new Object[][]{
                {
                    " ", clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, DEBUG,
                        "configuration credential: \'credentialName\' is required and must be between 1 and 255 characters"
                },
                {
                    configName, " ", serviceaccount, clusterToken, clusterVersion, true, DEBUG,
                        "java.net.URISyntaxException: Illegal character in path at index 0"
                },
                {
                    clusterToken, clusterEndpoint, serviceaccount, clusterToken, clusterVersion, true, DEBUG,
                        "Error creating configuration credential: \'credentialName\' is required and must be between 1 and 255 characters"
                },
                {
                    configName, "https://35.188.101.83", serviceaccount, clusterToken, clusterVersion, true, DEBUG,
                        "java.net.ConnectException: Connection timed out (Connection timed out)"
                },
                {
                    configName, clusterEndpoint, serviceaccount, "test", clusterVersion, true, DEBUG,
                        "Unauthorized\n^"
                }
        };
    }




}
