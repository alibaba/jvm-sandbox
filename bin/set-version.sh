#!/usr/bin/env bash

while getopts "sr" ARG
    do
        case ${ARG} in
            s)
                OP_SNAPSHOT=1;
                shift $((OPTIND-1))
                ;;
            r)
                OP_RELEASE=1;
                shift $((OPTIND-1))
                ;;
        esac
    done

[[ ${OP_SNAPSHOT} ]] && NEW_VERSION=${1}-"SNAPSHOT"
[[ ${OP_RELEASE} ]] && NEW_VERSION=${1}

echo "set project to new version: ${NEW_VERSION}"
cat ../pom.xml\
    | sed "s/<sandbox.version>[^<]*<\/sandbox.version>/<sandbox.version>${NEW_VERSION}<\/sandbox.version>/1" > ../pom.xml.newVersion\
    && mv ../pom.xml.newVersion ../pom.xml\
    && mvn -f ../pom.xml versions:set -DoldVersion=* -DnewVersion=${NEW_VERSION} -DprocessAllModules=true -DallowSnapshots=true