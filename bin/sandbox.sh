#!/usr/bin/env bash

# program : sandbox
#  author : luanjia@taobao.com
#    date : 2018-03-12
# version : 0.0.0.3

# define sandbox's home
## will be replace by install-local.sh
typeset SANDBOX_HOME_DIR
[[ -z ${SANDBOX_HOME_DIR} ]] && SANDBOX_HOME_DIR=${PWD}/..

# define current SANDBOX_USER
typeset SANDBOX_USER=${USER}
[[ -z ${SANDBOX_USER} ]] && SANDBOX_USER=$(whoami)

# define sandbox's network
typeset SANDBOX_SERVER_NETWORK

# define sandbox's lib
typeset SANDBOX_LIB_DIR=${SANDBOX_HOME_DIR}/lib

# define sandbox attach token file
typeset SANDBOX_TOKEN_FILE="${HOME}/.sandbox.token"

# define JVM OPS
typeset SANDBOX_JVM_OPS="-Xms128M -Xmx128M -Xnoclassgc -ea"

# define target JVM Process ID
typeset TARGET_JVM_PID

# define target SERVER network interface
typeset TARGET_SERVER_IP
typeset DEFAULT_TARGET_SERVER_IP="0.0.0.0"

# define target SERVER network port
typeset TARGET_SERVER_PORT

# define target NAMESPACE
typeset TARGET_NAMESPACE
typeset DEFAULT_NAMESPACE="default"

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err() {
  [[ -n "${2}" ]] && echo "${2}" 1>&2
  exit "${1}"
}

# display usage
function usage() {
  echo "
usage: ${0} [h] [<p:> [vlRFfu:a:A:d:m:I:P:C:X]]

    -h : help
         Prints the ${0} help

    -X : debug
         Prints debug message

    -p : PID
         Select target JVM process ID


    -v : version
         Prints sandbox\`s version


    -l : list loaded module
         Prints loaded module list


    -F : force flush
         Force flush the sandbox\`s user module library.

         flush reload user module library\`s module jar file.

         if module froze & unload occur error, ignore this error force froze & unload.
         if module reload occur error, ignore this module.

         MODULE LIB:
            - ${SANDBOX_HOME_DIR}/sandbox-module/


    -f : soft flush
         Soft flush the sandbox\`s user module library.

         flush user module library\`s module which module jar file was changed.
         if module jar file was append, load the newest module.
         if module jar file was changed, reload the newest module.
         if module jar file was removed. remove the modules.

         if module froze & unload occur error, ignore this error force froze & unload.
         if module reload occur error, ignore this module.

         MODULE LIB:
            - ${SANDBOX_HOME_DIR}/sandbox-module/


    -R : restart sandbox
         Force flush the sandbox\` sys & user module library.

         MODULE LIB:
            - ${SANDBOX_HOME_DIR}/module/
            - ${SANDBOX_HOME_DIR}/sandbox-module/


    -u : unload
         Unload compliance module, support pattern expression.

         EXAMPLE:
             ${0} -p <PID> -u *debug*


    -a : active module
         Active compliance module, support pattern expression.
         module will receive event when state was activated.

         EXAMPLE:
             ${0} -p <PID> -a *debug*


    -A : frozen
         Frozen compliance module, support pattern expression.
         when module state change on frozen, it will not receive event anymore.

         EXAMPLE:
             ${0} -p <PID> -A *debug*


    -m : module detail
         Print module detail

         EXAMPLE:
             ${0} -p <PID> -m debug


    -I : IP address
         Appoint the network interface (bind ip address)
         when default, use \"${DEFAULT_TARGET_SERVER_IP}\"

         EXAMPLE:
            ${0} -p <PID> -I 192.168.0.1 -v


    -P : port
         Appoint the sandbox\` network port
         when default, use random port

         EXAMPLE:
            ${0} -p <PID> -P 3658 -v


    -C : connect server only
         No attach target JVM, just connect server with appoint IP:PORT only.

         EXAMPLE:
             ${0} -C -I 192.168.0.1 -P 3658 -m debug

    -S : shutdown server
         Shutdown jvm-sandbox\` server

    -n : namespace
         Appoint the jvm-sandbox\` namespace
         when default, use \"${DEFAULT_NAMESPACE}\"

    -d : data
         Send the command & data to module's command handle method.
         <MODULE-ID>/<COMMAND-NAME>[?<PARAM1=VALUE1>[&PARAM2=VALUE2]]

         EXAMPLE:
            ${0} -d 'sandbox-info/version'
            ${0} -d 'sandbox-module-mgr/detail?id=sandbox-info'

"
}

# check sandbox permission
check_permission() {

  # check PID existed
  pgrep java | grep "${TARGET_JVM_PID}" > /dev/null ||
    exit_on_err 1 "permission denied, java process ${TARGET_JVM_PID} is not existed."

  # check attach
  pgrep -U "${SANDBOX_USER}" | grep "${TARGET_JVM_PID}" > /dev/null ||
    exit_on_err 1 "permission denied, ${SANDBOX_USER} is not allow attach to ${TARGET_JVM_PID}."

  # check $HOME is writeable
  [[ ! -w ${HOME} ]] &&
    exit_on_err 1 "permission denied, ${HOME} is not writable."

  # check SANDBOX-LIB is readable
  [[ ! -r ${SANDBOX_LIB_DIR} ]] &&
    exit_on_err 1 "permission denied, ${SANDBOX_LIB_DIR} is not readable."

  # touch attach token file is writable
  touch "${SANDBOX_TOKEN_FILE}" ||
    exit_on_err 1 "permission denied, ${SANDBOX_TOKEN_FILE} is not writable."

  # check JAVA_HOME is accessible
  [[ ! -x "${SANDBOX_JAVA_HOME}" ]] &&
    exit_on_err 1 "permission denied, ${SANDBOX_JAVA_HOME} is not accessible! please set JAVA_HOME"

  # check java command is executeable
  [[ ! -x "${SANDBOX_JAVA_HOME}/bin/java" ]] &&
    exit_on_err 1 "permission denied, ${SANDBOX_JAVA_HOME}/bin/java is not executable!"

  # check the jvm version, we need 6+
  "${SANDBOX_JAVA_HOME}"/bin/java -version 2>&1 | awk -F '"' '/version/&&$2<="1.5"{exit 1}' ||
    exit_on_err 1 "permission denied, please make sure target java process: ${TARGET_JVM_PID} run in JDK[6,11]"

}

# reset sandbox work environment
# reset some options for env
reset_for_env() {

  # use the env JAVA_HOME for default
  [[ -n "${JAVA_HOME}" ]] &&
    SANDBOX_JAVA_HOME="${JAVA_HOME}"

  # use the target JVM for SANDBOX_JAVA_HOME
  [[ -z "${SANDBOX_JAVA_HOME}" ]] &&
    SANDBOX_JAVA_HOME="$(
      lsof -p "${TARGET_JVM_PID}" |
        grep "/bin/java" |
        awk '{print $9}' |
        xargs ls -l |
        awk '{if($1~/^l/){print $11}else{print $9}}' |
        xargs ls -l |
        awk '{if($1~/^l/){print $11}else{print $9}}' |
        sed 's/\/bin\/java//g'
    )"

  # append toos.jar to JVM_OPT
  [[ -f "${SANDBOX_JAVA_HOME}"/lib/tools.jar ]] &&
    SANDBOX_JVM_OPS="${SANDBOX_JVM_OPS} -Xbootclasspath/a:${SANDBOX_JAVA_HOME}/lib/tools.jar"

  #fix for windows  shell $HOME diff with user.home
  test -n "${USERPROFILE}" -a -z "$(cat "${SANDBOX_TOKEN_FILE}")" && SANDBOX_TOKEN_FILE=${USERPROFILE}/.sandbox.token

}

# attach sandbox to target JVM
# return : attach jvm local info
function attach_jvm() {

  # got an token
  local token
  token="$(date | head | cksum | sed 's/ //g')"

  # attach target jvm
  "${SANDBOX_JAVA_HOME}/bin/java" \
    ${SANDBOX_JVM_OPS} \
    -jar "${SANDBOX_LIB_DIR}/sandbox-core.jar" \
    "${TARGET_JVM_PID}" \
    "${SANDBOX_LIB_DIR}/sandbox-agent.jar" \
    "home=${SANDBOX_HOME_DIR};token=${token};server.ip=${TARGET_SERVER_IP};server.port=${TARGET_SERVER_PORT};namespace=${TARGET_NAMESPACE}" ||
    exit_on_err 1 "attach JVM ${TARGET_JVM_PID} fail."

  # get network from attach result
  SANDBOX_SERVER_NETWORK=$(grep "${token}" "${SANDBOX_TOKEN_FILE}" | grep "${TARGET_NAMESPACE}" | tail -1 | awk -F ";" '{print $3";"$4}')
  [[ -z ${SANDBOX_SERVER_NETWORK} ]] &&
    exit_on_err 1 "attach JVM ${TARGET_JVM_PID} fail, attach lose response."

}

# execute sandbox command
# $1 : path
# $2 : params, eg: &name=dukun&age=31
function sandbox_curl() {
  sandbox_debug_curl "module/http/${1}?1=1${2}"
}

# execute sandbox command & exit
function sandbox_curl_with_exit() {
  sandbox_curl "${@}"
  exit
}

function sandbox_debug_curl() {
  #local host=$(echo "${SANDBOX_SERVER_NETWORK}" | awk -F ";" '{print $1}')
  #local port=$(echo "${SANDBOX_SERVER_NETWORK}" | awk -F ";" '{print $2}')
  local host=${SANDBOX_SERVER_NETWORK%;**}
  local port=${SANDBOX_SERVER_NETWORK#**;}
  if [[ "$host" == "0.0.0.0" ]]; then
    host="127.0.0.1"
  fi
  curl -N -s "http://${host}:${port}/sandbox/${TARGET_NAMESPACE}/${1}" ||
    exit_on_err 1 "target JVM ${TARGET_JVM_PID} lose response."
}

# the sandbox main function
function main() {

  while getopts "hp:vFfRu:a:A:d:m:I:P:ClSn:X" ARG; do
    case ${ARG} in
    h)
      usage
      exit
      ;;
    p) TARGET_JVM_PID=${OPTARG} ;;
    v) OP_VERSION=1 ;;
    l) OP_MODULE_LIST=1 ;;
    R) OP_MODULE_RESET=1 ;;
    F) OP_MODULE_FORCE_FLUSH=1 ;;
    f) OP_MODULE_FLUSH=1 ;;
    u)
      OP_MODULE_UNLOAD=1
      ARG_MODULE_UNLOAD=${OPTARG}
      ;;
    a)
      OP_MODULE_ACTIVE=1
      ARG_MODULE_ACTIVE=${OPTARG}
      ;;
    A)
      OP_MODULE_FROZEN=1
      ARG_MODULE_FROZEN=${OPTARG}
      ;;
    d)
      OP_DEBUG=1
      ARG_DEBUG=${OPTARG}
      ;;
    m)
      OP_MODULE_DETAIL=1
      ARG_MODULE_DETAIL=${OPTARG}
      ;;
    I) TARGET_SERVER_IP=${OPTARG} ;;
    P) TARGET_SERVER_PORT=${OPTARG} ;;
    C) OP_CONNECT_ONLY=1 ;;
    S) OP_SHUTDOWN=1 ;;
    n)
      OP_NAMESPACE=1
      ARG_NAMESPACE=${OPTARG}
      ;;
    X) set -x ;;
    ?)
      usage
      exit_on_err 1
      ;;
    esac
  done

  reset_for_env
  check_permission

  # reset IP
  [ -z "${TARGET_SERVER_IP}" ] && TARGET_SERVER_IP="${DEFAULT_TARGET_SERVER_IP}"

  # reset PORT
  [ -z "${TARGET_SERVER_PORT}" ] && TARGET_SERVER_PORT=0

  # reset NAMESPACE
  [[ ${OP_NAMESPACE} ]] &&
    TARGET_NAMESPACE=${ARG_NAMESPACE}
  [[ -z ${TARGET_NAMESPACE} ]] &&
    TARGET_NAMESPACE=${DEFAULT_NAMESPACE}

  if [[ ${OP_CONNECT_ONLY} ]]; then
    [[ 0 -eq ${TARGET_SERVER_PORT} ]] &&
      exit_on_err 1 "server appoint PORT (-P) was missing"
    SANDBOX_SERVER_NETWORK="${TARGET_SERVER_IP};${TARGET_SERVER_PORT}"
  else
    # -p was missing
    [[ -z ${TARGET_JVM_PID} ]] &&
      exit_on_err 1 "PID (-p) was missing."
    attach_jvm
  fi

  # -v show version
  [[ -n ${OP_VERSION} ]] &&
    sandbox_curl_with_exit "sandbox-info/version"

  # -l list loaded modules
  [[ -n ${OP_MODULE_LIST} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/list"

  # -F force flush module
  [[ -n ${OP_MODULE_FORCE_FLUSH} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/flush" "&force=true"

  # -f flush module
  [[ -n ${OP_MODULE_FLUSH} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/flush" "&force=false"

  # -R reset sandbox
  [[ -n ${OP_MODULE_RESET} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/reset"

  # -u unload module
  [[ -n ${OP_MODULE_UNLOAD} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/unload" "&action=unload&ids=${ARG_MODULE_UNLOAD}"

  # -a active module
  [[ -n ${OP_MODULE_ACTIVE} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/active" "&ids=${ARG_MODULE_ACTIVE}"

  # -A frozen module
  [[ -n ${OP_MODULE_FROZEN} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/frozen" "&ids=${ARG_MODULE_FROZEN}"

  # -m module detail
  [[ -n ${OP_MODULE_DETAIL} ]] &&
    sandbox_curl_with_exit "sandbox-module-mgr/detail" "&id=${ARG_MODULE_DETAIL}"

  # -S shutdown
  [[ -n ${OP_SHUTDOWN} ]] &&
    sandbox_curl_with_exit "sandbox-control/shutdown"

  # -d debug
  if [[ -n ${OP_DEBUG} ]]; then
    sandbox_debug_curl "module/http/${ARG_DEBUG}"
    exit
  fi

  # default
  sandbox_curl "sandbox-info/version"
  exit

}

main "${@}"
