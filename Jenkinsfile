node {
   properties([pipelineTriggers([]), [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/difi/move-integrasjonspunkt/']])

   // Mark the code checkout 'stage'....
   stage 'Checkout'

   // Get some code from a GitHub repository
   checkout scm


   // Get the maven tool.
   // ** NOTE: This 'M3' maven tool must be configured
   // **       in the global configuration.           
   def mvnHome = tool 'M3'

   // Mark the code build 'stage'....
   stage 'Build'
   // Run the maven build
   sh "${mvnHome}/bin/mvn clean package"
   step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

   dir('integrasjonspunkt') {
      // Run the maven build
      sh "${mvnHome}/bin/mvn docker:build -DpushImage"
      
      
    }

    step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'build'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'Start testing', state: 'PENDING']]]])
    input id: 'Testing', message: 'Start'
    
    stage name: "itest", concurrency: 1
    dir('integrasjonspunkt') {
      sh "${mvnHome}/bin/mvn docker:push -DdockerImageTag=itest"
      step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'build'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'Deployed to itest.', state: 'PENDING']]]])
      input id: 'Testing', message: 'Continue to systest'
    }
    
    stage name: "systest", concurrency: 1
    dir('integrasjonspunkt') {
      sh "${mvnHome}/bin/mvn docker:push -DdockerImageTag=systest"
      step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'build'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'Deployed to systest.', state: 'PENDING']]]])
      input id: 'Testing', message: 'Continue to staging'
    }

    stage name: "staging", concurrency: 1
    dir('integrasjonspunkt') {
      sh "${mvnHome}/bin/mvn docker:push -DdockerImageTag=staging"
      step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'build'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'Deployed to staging.', state: 'PENDING']]]])
      input id: 'Testing', message: 'Continue to production'
    }

    stage name: "production", concurrency: 1
    sh "${mvnHome}/bin/mvn -B release:clean release:prepare -DautoVersionSubmodules=true -Darguments=\"-Dmaven.deploy.skip=true -Dmaven.javadoc.skip=true\""
    dir('integrasjonspunkt') {
      sh "${mvnHome}/bin/mvn docker:build -DpushImage -DdockerImageTag=latest"
    }
    sh "${mvnHome}/bin/mvn -B release:perform -DautoVersionSubmodules=true -Darguments=\"-Dmaven.deploy.skip=true -Dmaven.javadoc.skip=true\""

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {

        sh "git push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/difi/move-integrasjonspunkt.git HEAD:master --follow-tags"
    }

    step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'build'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'Pushed to production', state: 'SUCCESS']]]])

    

}
