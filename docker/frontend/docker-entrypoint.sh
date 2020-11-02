#!/bin/bash
# Inspired from: https://github.com/docker-library/wordpress/blob/master/php7.1/apache/docker-entrypoint.sh
set -euo pipefail

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
    local var="$1"
    local fileVar="${var}_FILE"
    local def="${2:-}"
    if [ "${!var:-}" ] && [ "${!fileVar:-}" ]; then
        echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
        exit 1
    fi
    local val="$def"
    if [ "${!var:-}" ]; then
        val="${!var}"
    elif [ "${!fileVar:-}" ]; then
        val="$(< "${!fileVar}")"
    fi
    export "$var"="$val"
    unset "$fileVar"
}

if [[ "$1" == catalina* ]]; then
    configProps=(
        CODEDEF_CFG_DATA_DIR
        CODEDEF_CFG_ANT_HOME
        CODEDEF_CFG_DB_URL
        CODEDEF_CFG_DB_USERNAME
        CODEDEF_CFG_DB_PASSWORD
        CODEDEF_CFG_CLUSTER_MODE
        CODEDEF_CFG_CLUSTER_JAVA_HOME
        CODEDEF_CFG_CLUSTER_TIMEOUT
        CODEDEF_CFG_CLUSTER_RESERVATION_NAME
        CODEDEF_CFG_PARALLELIZE
        CODEDEF_CFG_FORCE_LOCAL_EXECUTION
        CODEDEF_CFG_MUTANT_COVERAGE
        CODEDEF_CFG_BLOCK_ATTACKER
	)
    envs=(
        CODEDEF_MANAGER_USERNAME
        CODEDEF_MANAGER_PASSWORD
        CODEDEF_MANAGER_ROLES
        CODEDEF_MANAGER_ALLOWED_REMOTE_ADDR
        CODEDEF_ADMIN_USERNAME
        CODEDEF_ADMIN_PASSWORD
        CODEDEF_ADMIN_ROLES
        CODEDEF_LOAD_BALANCER_IP
        "${configProps[@]}"
    )
    haveConfig=
    for e in "${envs[@]}"; do
        file_env "$e"
        if [ -z "$haveConfig" ] && [ -n "${!e}" ]; then
            haveConfig=1
        fi
    done

    if [ "$haveConfig" ]; then
        MAX_WAITS=10
        get_loadbalancer_ip() {
            waits=0
            status="ko"
            loadbalancer_ip=""
            while [[ "$status" != "ok" ]]; do
                loadbalancer_ip=$(getent hosts load-balancer | cut -d ' ' -f1 ; test ${PIPESTATUS[0]} -eq 0)
                if [ $? == 0 ]; then
                    status="ok"
                else
                    sleep 1
                fi
                (( waits++ )) && (( waits == MAX_WAITS )) && break
            done
            if [[ "$status" != "ok" ]]; then
                echo ""
                exit 1
            fi
            echo $loadbalancer_ip
            exit 0
        }

        : "${CODEDEF_MANAGER_USERNAME:=user}"
        : "${CODEDEF_MANAGER_PASSWORD:=password}"
        : "${CODEDEF_MANAGER_ROLES:=manager-gui}"
        : "${CODEDEF_MANAGER_ALLOWED_REMOTE_ADDR:=127\\\\.\\\\d+\\\\.\\\\d+\\\\.\\\\d+|::1|0:0:0:0:0:0:0:1}"
        : "${CODEDEF_ADMIN_USERNAME:=admin}"
        : "${CODEDEF_ADMIN_PASSWORD:=password}"
        : "${CODEDEF_ADMIN_ROLES:=manager-gui}"
        : "${CODEDEF_LOAD_BALANCER_IP:=$(get_loadbalancer_ip)}"
        : "${CODEDEF_CFG_DATA_DIR:=/codedefenders}"
        : "${CODEDEF_CFG_ANT_HOME:=/usr}"
        : "${CODEDEF_CFG_DB_URL:=jdbc:mysql://db:3306/defender}"
        : "${CODEDEF_CFG_DB_USERNAME:=defender}"
        : "${CODEDEF_CFG_DB_PASSWORD:=defender}"
        : "${CODEDEF_CFG_CLUSTER_MODE:=disabled}"
        : "${CODEDEF_CFG_CLUSTER_JAVA_HOME:=}"
        : "${CODEDEF_CFG_CLUSTER_TIMEOUT:=}"
        : "${CODEDEF_CFG_CLUSTER_RESERVATION_NAME:=}"
        : "${CODEDEF_CFG_PARALLELIZE:=enabled}"
        : "${CODEDEF_CFG_FORCE_LOCAL_EXECUTION:=enabled}"
        : "${CODEDEF_CFG_MUTANT_COVERAGE:=enabled}"
        : "${CODEDEF_CFG_BLOCK_ATTACKER:=enabled}"

        add_tomcat_user() {
            local username="$1"
            local password="$2"
            local roles="$3"
            sed -i "/<\/tomcat-users>/i <user username=\"${username}\" password=\"${password}\" roles=\"${roles}\" />" /usr/local/tomcat/conf/tomcat-users.xml
        }

        add_remoteIp_valve() {
            local trusted_proxy_ip="$1"
            local internal_proxy_ip="$2"
            local context_file_path="$3"
            sed -i "/<\/Context>/i<Valve className=\"org.apache.catalina.valves.RemoteIpValve\" remoteIpHeader=\"X-Real-IP\" internalProxies=\"${internal_proxy_ip}\" trustedProxies=\"${trusted_proxy_ip}\" portHeader=\"X-Forwarded-Port\" hostHeader=\"X-Forwarded-Server\" />" "${context_file_path}"
        }

        add_remoteAddr_valve() {
            local allowed_regexp="$1"
            local context_file_path="$2"
            sed -i "/<\/Context>/i<Valve className=\"org.apache.catalina.valves.RemoteAddrValve\" allow=\"${allowed_regexp}\" />" "${context_file_path}"
        }

        add_accessLog_valve() {
            local host_name="$1"
            local context_file_path="$2"
            sed -i "/<\/Context>/i<Valve className=\"org.apache.catalina.valves.AccessLogValve\" directory=\"logs\" prefix=\"${host_name}_access_log\" suffix=\".txt\" requestAttributesEnabled=\"true\" pattern=\"%h %l %u %t &quot;%r&quot; %s %b\" />" "${context_file_path}"
        }

        cp "/usr/local/tomcat/templates/tomcat-users.xml" "/usr/local/tomcat/conf/tomcat-users.xml"
        add_tomcat_user "${CODEDEF_MANAGER_USERNAME}" "${CODEDEF_MANAGER_PASSWORD}" "${CODEDEF_MANAGER_ROLES}"
        add_tomcat_user "${CODEDEF_ADMIN_USERNAME}" "${CODEDEF_ADMIN_PASSWORD}" "${CODEDEF_ADMIN_ROLES}"

        # Configure tomcat manager if app is enabled
        if [[ -d "/usr/local/tomcat/webapps/manager" ]]; then
            cp "/usr/local/tomcat/templates/manager-context.xml" "/usr/local/tomcat/webapps/manager/META-INF/context.xml"

            if [[ "$CODEDEF_LOAD_BALANCER_IP" != "" ]]; then
                add_remoteIp_valve "" "${CODEDEF_LOAD_BALANCER_IP}" "/usr/local/tomcat/webapps/manager/META-INF/context.xml"
            fi
            add_remoteAddr_valve "${CODEDEF_MANAGER_ALLOWED_REMOTE_ADDR}" "/usr/local/tomcat/webapps/manager/META-INF/context.xml"
            add_accessLog_valve "manager" "/usr/local/tomcat/webapps/manager/META-INF/context.xml"
        fi

        cp "/usr/local/tomcat/templates/codedefenders-context.xml" "/tmp/context.xml"
        if [[ "$CODEDEF_LOAD_BALANCER_IP" != "" ]]; then
            add_remoteIp_valve "" "${CODEDEF_LOAD_BALANCER_IP}" "/tmp/context.xml"
        fi
        add_accessLog_valve "codedefenders" "/tmp/context.xml"
        replaceProperties.py "/tmp/context.xml" > "/usr/local/tomcat/webapps/codedefenders/META-INF/context.xml"
        rm "/tmp/context.xml"

    fi

    # now that we're definitely done writing configuration, let's clear out the relevant environment variables (don't leak secrets)
    for e in "${envs[@]}"; do
        unset "$e"
    done
fi

exec "$@"
