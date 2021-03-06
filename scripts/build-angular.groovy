import groovy.json.*
import groovy.transform.Field

def label = "pod-angular-${UUID.randomUUID().toString()}"
def templateImage = "172.30.1.1:5000/ci/nodejs8-slave"

@Field
def jsonMap = null

podTemplate(label: label, cloud: 'openshift', containers: [    
    containerTemplate(image: '172.30.1.1:5000/ci/nodejs8-slave', ttyEnabled: false, name: 'jnlp',args: '${computer.jnlpmac} ${computer.name}')
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
                        $(npm bin)/ng test --single-run --code-coverage --browsers Chrome_no_sandbox
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

                    def npmPublico = "registry=${NEXUS_NPM_PUBLICO}"
                    def npmPrivado = "//${NEXUS_NPM_PRIVADO}:_authToken=${NEXUS_NPM_TOKEN}"
                    
                    sh "echo -e '$npmPublico\n$npmPrivado\nstrict-ssl=false\nalways-auth=true' > $HOME/.npmrc"
                    
                    sh '''
                        cp package.json dist/
                        cd dist && npm publish
                    '''            
                }

                stage ('build/deploy dev') {
                    
                    sh "oc project ${PROJETO}"
                    sh "oc start-build ${NOME_APLICACAO} -e VERSAO_APLICACAO=${VERSAO} --build-arg URL_ARTEFATO_DOWNLOAD=http://${NEXUS_NPM_PRIVADO}${NOME_APLICACAO}/-/${NOME_APLICACAO}-${VERSAO}.tgz --build-arg ARTEFATO=${NOME_APLICACAO}-${VERSAO}.tgz --follow=true"
                    sh "oc tag ${NOME_APLICACAO}:latest ${NOME_APLICACAO}:${VERSAO}"
                    sh "oc tag ${NOME_APLICACAO}:${VERSAO} ${NOME_APLICACAO}:dev"
                    
                }
                        
            }
            
        }

        stage('promover homologação'){
            input('Promover para Homologação?')
        }

        node(label) {
            container('jnlp') {
                stage ('deploy homologação') {

                    sh "oc project ${PROJETO}"
                    sh "oc tag ${NOME_APLICACAO}:${VERSAO} ${NOME_APLICACAO}:hom"
                }
            }
        }
}


  
