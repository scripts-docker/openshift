import groovy.json.*
import groovy.transform.Field

def label = "pod-angular-${UUID.randomUUID().toString()}"

@Field
def jsonMap = null

podTemplate(label: label, cloud: 'openshift', containers: [    
    containerTemplate(image: 'docker.io/petenorth/nodejs8-openshift-slave', ttyEnabled: false, name: 'jnlp',args: '${computer.jnlpmac} ${computer.name}')
  ]) {

  node(label) {   
      
      container('jnlp') {

          stage ('checkout'){
            git "${URL_REPOSITORIO_APP}"

            def packageJson = readFile(file:'package.json')
            jsonMap = new JsonSlurperClassic().parseText(packageJson)

            jsonMap.version = "${VERSAO}"
            
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

            def json = JsonOutput.toJson(jsonMap)
            json = JsonOutput.prettyPrint(json)

            writeFile(file:'package.json', text: json)
            
              sh '''
                curl -o $HOME/.npmrc https://raw.githubusercontent.com/scripts-docker/openshift/master/.npmrc
                cat $HOME/.npmrc
                cat package.json
                cp package.json dist/
                cd dist && npm publish
              '''
            
          }

          stage ('build image') {
            
              sh '''                
                oc start-build ${NOME_APLICACAO} --follow=true
                oc tag ${NOME_APLICACAO}:latest app-frontend-ssvida:${VERSAO}
              '''
            
          }
                  
      }
    
  }
}


  
