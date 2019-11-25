#!/usr/bin/env groovy

/*
 *  Copyright (c) 2010-2018 Poterion. All rights reserved.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* REQUIRED CREDENTTIALS
 *
 * - poterion-git               [SSH Key]   (global)
 */

def setup() {
    if (env.TEST_PORT) {
        echo "Setup already done."
    } else {
        def major = sh(returnStdout: true, script: 'date +%Y').replaceAll(/\s/, "")
        def minor
        if (BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/) {
            minor = sh(returnStdout: true, script: "git tag | grep -E '^v${major}\\.[0-9]+' | sort -r | head -n 1 | sed -E 's/^v${major}\\.([0-9]+)\$/\\1/'")
            if (minor == "") minor = "0"
            minor = minor.toInteger() + 1
        } else if (BRANCH_NAME ==~ /\w+\/.*/) {
            minor = BRANCH_NAME.replaceAll(/\w+\/(.*)/) { matches -> "${matches[1]}" }
        } else {
            minor = "DEV"
        }
        env.VERSION = major + '.' + minor

        currentBuild.displayName = "${VERSION}.${BUILD_NUMBER}"

        echo "Building version: ${VERSION}"
    }
}

node {
    //agent any
    env.JAVA_HOME = "${tool name: 'JDK_8'}"

    properties([
            //timeout(time: 1, unit: 'HOURS'),
            disableConcurrentBuilds(),
            buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    ])

    timestamps {
        ansiColor('xterm') {
            stage('Checkout') {
                if (BRANCH_NAME ==~ /release\/.*/) cleanWs()

                checkout([
                        $class                           : 'GitSCM',
                        branches                         : [[name: "*/${BRANCH_NAME}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [
                                [
                                        $class      : 'CloneOption',
                                        depth       : 1,
                                        honorRefspec: true,
                                        noTags      : false,
                                        reference   : '',
                                        shallow     : true
                                ],
                                [$class: 'WipeWorkspace']//,
                                //[$class: 'CleanBeforeCheckout']//,
                                //[$class: 'CleanCheckout']
                        ],
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [
                                [
                                        credentialsId: 'poterion-git',
                                        url          : 'ssh://git@bitbucket.intra:22999/monitor/application.git'
                                ]
                        ]
                ])

                setup()
            }

            stage('Build') {
                setup()
                lock(resource: 'sailexpert-build-backend', inversePrecedence: true) {
                    script {
                        withMaven(maven: 'Maven_3') {
                            sh "mvn versions:set -DnewVersion=${VERSION}"
                            sh "mvn clean install"
                        }
                    }
                    milestone(20)
                }
                archiveArtifacts artifacts: "assembly/target/monitor-${VERSION}.jar"
            }
        }
    }
}