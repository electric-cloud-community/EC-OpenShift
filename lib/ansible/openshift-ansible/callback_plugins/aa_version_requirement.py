#!/usr/bin/python

"""
This callback plugin verifies the required minimum version of Ansible
is installed for proper operation of the OpenShift Ansible Installer.
The plugin is named with leading `aa_` to ensure this plugin is loaded
first (alphanumerically) by Ansible.
"""
import sys
from subprocess import check_output
from ansible import __version__

if __version__ < '2.0':
    # pylint: disable=import-error,no-name-in-module
    # Disabled because pylint warns when Ansible v2 is installed
    from ansible.callbacks import display as pre2_display
    CallbackBase = object

    def display(*args, **kwargs):
        """Set up display function for pre Ansible v2"""
        pre2_display(*args, **kwargs)
else:
    from ansible.plugins.callback import CallbackBase
    from ansible.utils.display import Display

    def display(*args, **kwargs):
        """Set up display function for Ansible v2"""
        display_instance = Display()
        display_instance.display(*args, **kwargs)


# Set to minimum required Ansible version
REQUIRED_VERSION = '2.2.0.0'
DESCRIPTION = "Supported versions: %s or newer (except 2.2.1.0)" % REQUIRED_VERSION
FAIL_ON_2_2_1_0 = "There are known issues with Ansible version 2.2.1.0 which " \
                  "are impacting OpenShift-Ansible. Please use Ansible " \
                  "version 2.2.0.0 or a version greater than 2.2.1.0. " \
                  "See this issue for more details: " \
                  "https://github.com/openshift/openshift-ansible/issues/3111"


def version_requirement(version):
    """Test for minimum required version"""
    return version >= REQUIRED_VERSION


class CallbackModule(CallbackBase):
    """
    Ansible callback plugin
    """

    CALLBACK_VERSION = 1.0
    CALLBACK_NAME = 'version_requirement'

    def __init__(self):
        """
        Version verification is performed in __init__ to catch the
        requirement early in the execution of Ansible and fail gracefully
        """
        super(CallbackModule, self).__init__()

        if not version_requirement(__version__):
            display(
                'FATAL: Current Ansible version (%s) is not supported. %s'
                % (__version__, DESCRIPTION), color='red')
            sys.exit(1)

        if __version__ == '2.2.1.0':
            rpm_ver = str(check_output(["rpm", "-qa", "ansible"]))
            patched_ansible = '2.2.1.0-2'

            if patched_ansible not in rpm_ver:
                display(
                    'FATAL: Current Ansible version (%s) is not supported. %s'
                    % (__version__, FAIL_ON_2_2_1_0), color='red')
                sys.exit(1)
