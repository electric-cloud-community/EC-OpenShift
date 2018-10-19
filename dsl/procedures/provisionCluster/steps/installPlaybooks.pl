use File::Copy::Recursive qw(rcopy);
use File::Path;
use ElectricCommander;

use warnings;
use strict;
$|=1;


sub main() {
    my $ec = ElectricCommander->new();
    $ec->abortOnError(1);

    my $projectName = '$[/myProject/projectName]';
    retrieveDependencies($ec, $projectName);
}


sub retrieveDependencies {
    my ($ec, @projects) = @_;

    my $dep = EC::DependencyManager->new($ec);
    $dep->grabResource();

    my $destinationFolder = $ENV{COMMANDER_WORKSPACE} . '/ansible';
    my $sourceFolder = 'lib/ansible';
    eval {
        $dep->sendDependencies(\@projects, $sourceFolder, $destinationFolder);
    };
    if ($@) {
        my $err = $@;
        print "$err\n";
        $ec->setProperty('/myJobStep/summary', $err);
        exit 1;
    }
}

main();

1;



package EC::DependencyManager;
use strict;
use warnings;

use File::Spec;
use JSON qw(decode_json);
use File::Copy::Recursive qw(rcopy rmove);

our $VERSION = '1.0.0';

sub new {
    my ($class, $ec) = @_;

    my $self = { ec => $ec };
    return bless $self, $class;
}

sub ec {
    return shift->{ec};
}

sub grabResource {
    my ($self) = @_;

    my $resName = '$[/myResource/resourceName]';
    $self->ec->setProperty('/myJob/grabbedResource', $resName);
    print "Grabbed Resource: $resName\n";
}

sub getLocalResource {
    my ($self) = @_;

    my @filterList = ();

    my $propertyPath = '/server/settings/localResourceName';
    my $serverResource = eval {
        $self->ec->getProperty($propertyPath)->findvalue('//value')->string_value
    };

    if ($serverResource) {
        print "Configured Local Resource is $serverResource (taken from $propertyPath)\n";
        return $serverResource;
    }

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "localhost"});

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "127.0.0.1"});

    my $hostname = $self->ec->getProperty('/server/hostName')->findvalue('//value')->string_value;
    print "Hostname is $hostname\n";

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "$hostname"});

    push (@filterList, {"propertyName" => "resourceName",
                            "operator" => "equals",
                            "operand1" => "local"});


    my $result = $self->ec->findObjects('resource',
            {filter => [
         { operator => 'or',
             filter => \@filterList,
        }
      ], numObjects => 1}
    );

    my $resourceName = eval {
        $result->findvalue('//resourceName')->string_value;
    };
    unless($resourceName) {
        die "Cannot find local resource and there is no local resource in the configuration. Please set property $propertyPath to the name of your local resource or resource pool.";
    }
    print "Found local resource: $resourceName\n";
    return $resourceName;
}


sub copyDependencies {
    my ($self, $projectName, $sourceFolder, $destinationFolder) = @_;

    $sourceFolder ||= 'lib';
    my $source = $self->getPluginsFolder() . "/$projectName/" . $sourceFolder;
    $destinationFolder ||= File::Spec->catfile($ENV{COMMANDER_DATA}, 'grape');

    unless(-d $source) {
        die "Folder $source does not exist, please try to reinstall the plugin."
    }

    unless(-d $destinationFolder) {
        mkdir($destinationFolder);
    }

    unless( -w $destinationFolder) {
        die "$destinationFolder is not writable. Please allow agent user to write to this directory."
    }

    my $filesCopied = rcopy($source, $destinationFolder);
    if ($filesCopied == 0) {
        die "Copy failed, no files were copied from $source to $destinationFolder, please check permissions for $destinationFolder";
    }
}


sub getPluginsFolder {
    my ($self) = @_;

    return $self->ec->getProperty('/server/settings/pluginsDirectory')->findvalue('//value')->string_value;
}

sub sendDependencies {
    my ($self, $projects, $sourceFolder, $destinationFolder) = @_;

    my $grapeFolder = File::Spec->catfile($ENV{COMMANDER_DATA}, 'grape');
    $sourceFolder ||= 'lib';
    $destinationFolder ||= $grapeFolder;

    my $serverResource = $self->getLocalResource();
    my $currentResource = '$[/myResource/resourceName]';
    if ($serverResource eq $currentResource) {
        for my $projectName (@$projects) {
            $self->copyDependencies($projectName, $sourceFolder, $destinationFolder);
        }
        return;
    }

    my $windows = $^O =~ /win32/;

    my $channel = int rand 9999999;

    my $pluginsFolder = $self->getPluginsFolder;
    my @folders = map {
        $pluginsFolder . '/' . $_;
    } @$projects;

    my $sendStep = q{
use strict;
use warnings;
use ElectricCommander;
use JSON qw(encode_json);
use Data::Dumper;

my $pluginFolders = '#pluginFolders#';
my @folders = split(';', $pluginFolders);

my $ec = ElectricCommander->new;
my $channel = '#channel#';
print "Channel: $channel\n";

my %mapping = ();
for my $folder (@folders) {
    $folder = File::Spec->catfile($folder, '#sourceFolder#');
    print "Source folder: $folder\n";
    unless(-d $folder) {
        handleError("Folder $folder does not exist");
    }
    my @files = scanFiles($folder);


    for my $file (@files) {
        my $relPath = File::Spec->abs2rel($file, $folder);
        my $destPath = "dependencies/$relPath";
        $mapping{$file} = $destPath;
    }
}

my $response = $ec->putFiles($ENV{COMMANDER_JOBID}, \%mapping, {channel => $channel});
$ec->setProperty('/myJob/ec_dependencies_files', encode_json(\%mapping));

sub handleError {
    my ($error) = @_;

    print 'Error: ' . $error;
    $ec->setProperty('/myJobStep/summary', $error);
    exit 1;
}

sub scanFiles {
    my ($dir) = @_;

    my @files = ();
    opendir my $dh, $dir or handleError("Cannot open folder $dir: $!");
    for my $file (readdir $dh) {
        next if $file =~ /^\./;

        my $fullPath = File::Spec->catfile($dir, $file);
        if (-d $fullPath) {
            push @files, scanFiles($fullPath);
        }
        else {
            push @files, $fullPath;
        }
    }
    return @files;
}

    };

    my $pluginFolders = join(';', @folders);

    $sendStep =~ s/\#pluginFolders\#/$pluginFolders/;
    $sendStep =~ s/\#channel\#/$channel/;
    $sendStep =~ s/\#sourceFolder\#/$sourceFolder/;

    my $xpath = $self->ec->createJobStep({
        jobStepName => 'Grab Dependencies',
        command => $sendStep,
        shell => 'ec-perl',
        resourceName => $serverResource
    });

    my $jobStepId = $xpath->findvalue('//jobStepId')->string_value;
    print "Job Step ID: $jobStepId\n";
    my $completed = 0;
    while(!$completed) {
        my $status = $self->ec->getJobStepStatus($jobStepId)->findvalue('//status')->string_value;
        if ($status eq 'completed') {
            $completed = 1;
        }
    }

    my $err;
    my $timeout = 60;
    $self->ec->getFiles({error => \$err, channel => $channel, timeout => $timeout});
    if ($err) {
        die $err;
    }
    my $files = eval {
        $self->ec->getProperty('/myJob/ec_dependencies_files')->findvalue('//value')->string_value;
    };
    if ($@) {
        die "Cannot get property ec_dependencies_files from the job: $@";
    }

    my $mapping = decode_json($files);
    for my $file (keys %$mapping) {
        my $dest = $mapping->{$file};
        if (-f $dest) {

        }
        else {
            die "The file $dest was not received\n";
        }
    }

    unless(-d $destinationFolder) {
        mkdir $destinationFolder;
    }

    unless( -w $destinationFolder) {
        die "$destinationFolder is not writable. Please allow agent user to write to this directory."
    }
    my $filesCopied = rmove('dependencies', $destinationFolder);
    if ($filesCopied == 0) {
        die "Copy failed, no files were copied to $destinationFolder, please check permissions for the directory $destinationFolder";
    }
}

1;

