def label = "pod-angular-${UUID.randomUUID().toString()}"

podTemplate(label: label, cloud: 'openshift', containers: [    
    containerTemplate(image: 'docker.io/petenorth/nodejs8-openshift-slave', ttyEnabled: false, name: 'jnlp',args: '${computer.jnlpmac} ${computer.name}')
  ]) {

  node(label) {   
      
      container('jnlp') {

          stage ('checkout'){
              git "${URL_REPOSITORIO_APP}"
            
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

          stage ('publica nexus') {
            
              sh '''
                curl -o $HOME/.npmrc https://raw.githubusercontent.com/scripts-docker/openshift/master/.npmrc
                cat $HOME/.npmrc
                cp package.json dist/
                cd dist && npm publish
              '''
            
          }

          stage ('build image') {
            
              sh '''                
                oc start-build ${NOME_APLICACAO} --follow
                oc tag ${NOME_APLICACAO}:latest app-frontend-ssvida:${VERSAO}
              '''
            
          }
                  
      }
    
  }
}


  
