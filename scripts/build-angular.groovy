import groovy.json.*
import groovy.transform.Field

def label = "pod-angular-${UUID.randomUUID().toString()}"
def templateImage = "${REGISTRY_DOCKER}/ci/nodejs8-slave"

@Field
def jsonMap = null

sh("echo $templateImage")

podTemplate(label: label, cloud: 'openshift', containers: [    
    containerTemplate(image: templateImage, ttyEnabled: false, name: 'jnlp',args: '${computer.jnlpmac} ${computer.name}')
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

          stage ('sonar') {
              sh("sonar-scanner -Dsonar.tests=src/app -Dsonar.test.inclusions=**/*.spec.ts -Dsonar.ts.tslint.configPath=tslint.json -Dsonar.sources=src/app -Dsonar.exclusions=**/node_modules/**,**/*.spec.ts -Dsonar.host.url=${SONAR} -Dsonar.typescript.lcov.reportPaths=coverage/lcov.info -Dsonar.projectKey=${jsonMap.name} -Dsonar.projectName='${jsonMap.description}' -Dsonar.projectVersion=${VERSAO}")
          }

          stage ('build') {
            
              sh '$(npm bin)/ng build --prod --build-optimizer'
            
          }

          stage ('publica nexus') {

            def json = JsonOutput.toJson(jsonMap)
            json = JsonOutput.prettyPrint(json)

            writeFile(file:'package.json', text: json)
            
              sh "echo \$'registry=${NEXUS_NPM_PUBLICO}\n//${NEXUS_NPM_PRIVADO}:_authToken=${NEXUS_NPM_TOKEN}\nstrict-ssl=false\nalways-auth=true' > $HOME/.npmr"
              sh '''
                cp package.json dist/
                cd dist && npm publish
              '''
            
          }

          stage ('build/deploy image') {
            
              sh "oc start-build ${NOME_APLICACAO}  -e VERSAO-APLICACAO=${VERSAO} --build-arg URL_ARTEFATO_DOWNLOAD=${NEXUS_NPM_PRIVADO}${NOME_APLICACAO}/-/${NOME_APLICACAO}-${VERSAO}.tgz --build-arg ARTEFATO=${NOME_APLICACAO}-${VERSAO}.tgz --follow=true"
              sh "oc tag ${NOME_APLICACAO}:latest ${NOME_APLICACAO}:${VERSAO}"
            
          }
                  
      }
    
  }
}


  
