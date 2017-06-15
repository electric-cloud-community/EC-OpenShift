use ElectricCommander;
my $ec = ElectricCommander->new();

my $clusterEndpoint;

if ('$[topology]' eq 'haproxy') {
    $clusterEndpoint = "https://$[openshift_hostname_prefix]lb.$[domain_name]:8443"
} else {
    $clusterEndpoint = "https://$[openshift_hostname_prefix]master.$[domain_name]:8443"
}

$ec->createJobStep(
            {
                jobStepName => "Create EC-OpenShift Plugin Config",
                parallel => 1,
                subprocedure => 'CreateConfiguration',
                subproject=> '/plugins/EC-OpenShift/project',
                timeLimit => '5',
                timeLimitUnits => 'minutes',
                actualParameter => [
                    {
                        actualParameterName => 'config',
                        value => '$[config]'
                    },
                    {
                        actualParameterName => 'desc',
                        value => 'EC-OpenShift Config'
                    },
                    {
                        actualParameterName => 'logLevel',
                        value => '2'
                    },
                    {
                        actualParameterName => 'clusterEndpoint',
                        value => $clusterEndpoint
                    },
                    {
                        actualParameterName => 'credential',
                        value =>  '$[config]'
                    }
                ],
                credential => [
                    {
                        credentialName => '$[config]',
                        userName => '$[service_account]',
                        password => '$[service_token]'
                    }

                ]
            }
        );