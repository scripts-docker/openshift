def label = "pod-angular-${UUID.randomUUID().toString()}"

podTemplate(label: label, cloud: 'openshift', containers: [    
    containerTemplate(image: 'docker.io/petenorth/nodejs8-openshift-slave', ttyEnabled: false, name: 'jnlp',args: '${computer.jnlpmac} ${computer.name}')
  ]) {

  node(label) {   
      
      container('jnlp') {

          stage ('checkout'){
                      
               git 'https://github.com/marcelolucio1982/angular-5-example.git'
                sh 'node -v'
            
          }
          stage ('install modules'){
            
              sh '''
                npm install
              '''
            
          }
          stage ('test'){
            
              sh '''
                $(npm bin)/ng test --single-run --browsers Chrome_no_sandbox
              '''
            
          }
          stage ('code quality'){
            
              sh '$(npm bin)/ng lint'
            
          }
          stage ('build') {
            
              sh '$(npm bin)/ng build --prod --build-optimizer'
            
          }
          stage ('build image') {
            
              sh '''
                rm -rf node_modules
                ls -lha
                oc start-build scripts-openshift --from-dir=. --follow
              '''
            
          }
                  
      }
    
  }
}


  
