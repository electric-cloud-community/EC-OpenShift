cd \$COMMANDER_WORKSPACE/ansible/openshift-ansible; 
export ANSIBLE_ROLES_PATH=\$COMMANDER_WORKSPACE/ansible/openshift-ansible/roles;
export ANSIBLE_CONFIG=\$COMMANDER_WORKSPACE/ansible/openshift-ansible/ansible.cfg;
export ANSIBLE_FILTER_PLUGINS=\$COMMANDER_WORKSPACE/ansible/openshift-ansible/filter_plugins;
ansible-playbook -vvvv \$COMMANDER_WORKSPACE/ansible/openshift-ansible/playbooks/byo/config.yml -i /tmp/hosts -M \$COMMANDER_WORKSPACE/ansible/openshift-ansible/library
