use ElectricCommander;
my $ec = ElectricCommander->new();
my $batch = $ec->newBatch('parallel');
my $esx_properties; 
my $esx_vmname;

my $num_of_masters;
my $num_of_etcd;
my $num_of_lbs;
my $num_of_nodes = '$[no_openshift_nodes]';

if ('$[topology]' eq 'minimal') {
	$num_of_masters = 1;
	$num_of_etcd = 0;
	$num_of_lbs = 0;

} elsif ('$[topology]' eq 'multipleEtcd') {
	$num_of_masters = 1;
	$num_of_etcd = 3;
	$num_of_lbs = 0;
} elsif ('$[topology]' eq 'haproxy') {
	$num_of_masters = 3;
	$num_of_etcd = 3;
	$num_of_lbs = 1;
}

my @reqIds = ();

print "Importing " . $num_of_masters . " master(s)";

for (my $i=1; $i <= $num_of_masters; $i++) {

    if ($num_of_masters == 1){
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "master";
   	} else {
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "master" . $i;
   	}

   	if ($num_of_masters == 1){
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "master";
   	} else {
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "master" . $i;
   	}
   
    push @reqIds, $batch->createJobStep(
            {
                jobStepName => "Import Master " . $i,
                parallel => 1,
                subprocedure => 'Import',
                subproject=> '/plugins/EC-ESX/project',
                timeLimit => '5',
                timeLimitUnits => 'minutes',
                actualParameter => [
                    {
                        actualParameterName => 'connection_config',
                        value => '$[esx_config]'
                    },
                    {
                        actualParameterName => 'esx_datastore',
                        value => '$[esx_datastore]'
                    },
                    {
                        actualParameterName => 'esx_host',
                        value => '$[esx_host]'
                    },
                    {
                        actualParameterName => 'esx_number_of_vms',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties',
                        value =>  $esx_properties
                    },
                    {
                        actualParameterName => 'esx_vm_memory',
                        value => '$[master_memory]'
                    },
                    {
                        actualParameterName => 'esx_vm_num_cpus',
                        value => '$[master_cpu]'
                    },
                    {
                        actualParameterName => 'esx_source_directory',
                        value => '$[ovf_package_path]'
                    },
                    {
                        actualParameterName => 'esx_vmname',
                        value => $esx_vmname
                    },
                    {
                        actualParameterName => 'ovftool_path',
                        value => '$[ovftool_path]'
                    },
                    {
                        actualParameterName => 'esx_vm_poweron',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties_location',
                        value => '/myJob/ESX/master' . $i
                    }
                ]
            }
        )
}

print "Importing " . $num_of_etcd . " etcd";

for (my $i=1; $i <= $num_of_etcd; $i++) {

    if ($num_of_etcd == 1){
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "etcd";
   	} else {
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "etcd" . $i;
   	}

   	if ($num_of_etcd == 1){
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "etcd";
   	} else {
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "etcd" . $i;
   	}
   
    push @reqIds, $batch->createJobStep(
            {
                jobStepName => "Import Etcd " . $i,
                parallel => 1,
                subprocedure => 'Import',
                subproject=> '/plugins/EC-ESX/project',
                timeLimit => '5',
                timeLimitUnits => 'minutes',
                actualParameter => [
                    {
                        actualParameterName => 'connection_config',
                        value => '$[esx_config]'
                    },
                    {
                        actualParameterName => 'esx_datastore',
                        value => '$[esx_datastore]'
                    },
                    {
                        actualParameterName => 'esx_host',
                        value => '$[esx_host]'
                    },
                    {
                        actualParameterName => 'esx_number_of_vms',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties',
                        value =>  $esx_properties
                    },
                    {
                        actualParameterName => 'esx_vm_memory',
                        value => '$[master_memory]'
                    },
                    {
                        actualParameterName => 'esx_vm_num_cpus',
                        value => '$[master_cpu]'
                    },
                    {
                        actualParameterName => 'esx_source_directory',
                        value => '$[ovf_package_path]'
                    },
                    {
                        actualParameterName => 'esx_vmname',
                        value => $esx_vmname
                    },
                    {
                        actualParameterName => 'ovftool_path',
                        value => '$[ovftool_path]'
                    },
                    {
                        actualParameterName => 'esx_vm_poweron',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties_location',
                        value => '/myJob/ESX/etcd' . $i
                    }
                ]
            }
        )
}

print "Importing " . $num_of_lbs . " Load balancer";

for (my $i=1; $i <= $num_of_lbs; $i++) { 
    $esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "lb";  
    $esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "lb";
    	
    push @reqIds, $batch->createJobStep(
            {
                jobStepName => "Import Load Balancer",
                parallel => 1,
                subprocedure => 'Import',
                subproject=> '/plugins/EC-ESX/project',
                timeLimit => '5',
                timeLimitUnits => 'minutes',
                actualParameter => [
                    {
                        actualParameterName => 'connection_config',
                        value => '$[esx_config]'
                    },
                    {
                        actualParameterName => 'esx_datastore',
                        value => '$[esx_datastore]'
                    },
                    {
                        actualParameterName => 'esx_host',
                        value => '$[esx_host]'
                    },
                    {
                        actualParameterName => 'esx_number_of_vms',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties',
                        value =>  $esx_properties
                    },
                    {
                        actualParameterName => 'esx_vm_memory',
                        value => '$[master_memory]'
                    },
                    {
                        actualParameterName => 'esx_vm_num_cpus',
                        value => '$[master_cpu]'
                    },
                    {
                        actualParameterName => 'esx_source_directory',
                        value => '$[ovf_package_path]'
                    },
                    {
                        actualParameterName => 'esx_vmname',
                        value => $esx_vmname
                    },
                    {
                        actualParameterName => 'ovftool_path',
                        value => '$[ovftool_path]'
                    },
                    {
                        actualParameterName => 'esx_vm_poweron',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties_location',
                        value => '/myJob/ESX/lb'
                    }
                ]
            }
        );
}
   
print "Importing " . $num_of_nodes . " node(s)";

for (my $i=1; $i <= $num_of_nodes; $i++) {

    if ($num_of_nodes == 1){
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "node";
   	} else {
   		$esx_properties = 'hostname=' . '$[openshift_hostname_prefix]' . "node" . $i;
   	}

   	if ($num_of_nodes == 1){
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "node";
   	} else {
   		$esx_vmname = 'OpenShift-' . '$[openshift_hostname_prefix]' . "node" . $i;
   	}
   
    push @reqIds, $batch->createJobStep(
            {
                jobStepName => "Import node " . $i,
                parallel => 1,
                subprocedure => 'Import',
                subproject=> '/plugins/EC-ESX/project',
                timeLimit => '5',
                timeLimitUnits => 'minutes',
                actualParameter => [
                    {
                        actualParameterName => 'connection_config',
                        value => '$[esx_config]'
                    },
                    {
                        actualParameterName => 'esx_datastore',
                        value => '$[esx_datastore]'
                    },
                    {
                        actualParameterName => 'esx_host',
                        value => '$[esx_host]'
                    },
                    {
                        actualParameterName => 'esx_number_of_vms',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties',
                        value =>  $esx_properties
                    },
                    {
                        actualParameterName => 'esx_vm_memory',
                        value => '$[node_memory]'
                    },
                    {
                        actualParameterName => 'esx_vm_num_cpus',
                        value => '$[node_cpu]'
                    },
                    {
                        actualParameterName => 'esx_source_directory',
                        value => '$[ovf_package_path]'
                    },
                    {
                        actualParameterName => 'esx_vmname',
                        value => $esx_vmname
                    },
                    {
                        actualParameterName => 'ovftool_path',
                        value => '$[ovftool_path]'
                    },
                    {
                        actualParameterName => 'esx_vm_poweron',
                        value => '1'
                    },
                    {
                        actualParameterName => 'esx_properties_location',
                        value => '/myJob/ESX/node' . $i
                    }
                ]
            }
        )
}

$batch->submit();
my $result = $batch->findvalue( $reqIds[0], 'property/value' )->value;
print $result