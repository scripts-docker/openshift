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
          
                  
      }
    
  }

stage('dddddd'){
    input('Do you want to proceed?')
}

  node(label) {
      container('jnlp') {

           stage ('deploy hom') {
               sh "echo FOI"
           }
      }
  }
}


  
