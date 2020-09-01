#! /bin/bash

# Set HSM IP as an environmental variable
# ENV HSM_IP <insert the IP address of an active CloudHSM instance here>

if [[ -z "$HSM_IP" ]]; then
  #statements
  echo -e "\n* HSM_IP env variable not set: skipping CloudHSM connection \n"
else
  # Configure cloudhms-client
  # COPY customerCA.crt /opt/cloudhsm/etc/
  /opt/cloudhsm/bin/configure -a $HSM_IP

  # start cloudhsm client
  echo -n "* Starting CloudHSM client ... "
  /opt/cloudhsm/bin/cloudhsm_client /opt/cloudhsm/etc/cloudhsm_client.cfg &> /tmp/cloudhsm_client_start.log &

  # wait for startup
  while true
  do
      if grep 'libevmulti_init: Ready !' /tmp/cloudhsm_client_start.log &> /dev/null
      then
    echo "[OK]"
    break
      fi
      sleep 0.5
  done
  echo -e "\n* CloudHSM client started successfully ... \n"
fi

# start application
/opt/ethsigner/bin/ethsigner $@
