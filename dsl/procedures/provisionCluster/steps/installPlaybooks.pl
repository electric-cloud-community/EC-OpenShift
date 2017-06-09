use File::Copy::Recursive qw(rcopy);
use File::Path;
use ElectricCommander;

use warnings;
use strict;
$|=1;

my $ec = ElectricCommander->new();
$ec->abortOnError(1);


my $xpath = $ec->retrieveArtifactVersions({
        artifactVersionName => 'com.electriccloud:EC-OpenShift-Ansible:1.0.0'
    });

my $dataDir = $ENV{COMMANDER_WORKSPACE};
die "ERROR: Data directory not defined!" unless ($dataDir);

my $ansibleDir = $ENV{COMMANDER_WORKSPACE} . '/ansible';
my $dir = $xpath->findvalue("//artifactVersion/cacheDirectory");

mkpath($ansibleDir);
die "ERROR: Cannot create target directory" unless( -e $ansibleDir );

rcopy( $dir, $ansibleDir) or die "Copy failed: $!";
print "Retrieved and copied ansible playbooks from $dir to $ansibleDir\n";
