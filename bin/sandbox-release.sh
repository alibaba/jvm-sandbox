#!/usr/bin/env bash

## define the GPG_TTY for input password
export GPG_TTY=$(tty)

## maven clean & deploy to maven repo
mvn -f ../pom.xml clean deploy