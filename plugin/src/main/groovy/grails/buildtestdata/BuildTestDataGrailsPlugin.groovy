package grails.buildtestdata

import grails.plugins.Plugin
import groovy.transform.CompileDynamic

//@CompileStatic
@SuppressWarnings("GroovyUnusedDeclaration")
class BuildTestDataGrailsPlugin extends Plugin {
    def grailsVersion = "3.3.0 > *"

    def title = "Build Test Data Plugin"
    def description = 'Enables the easy creation of test data by automatically satisfying most constraints.'
    def license = "APACHE"
    def documentation = "https://jdaugherty.github.io/build-test-data"

    def developers = [
        [name: "Aaron Long", email: "aaron@aaronlong.me"],
        [name: "Ted Naleid", email: "contact@naleid.com"],
    ]

    def issueManagement = [system: 'github', url: 'https://github.com/jdaugherty/build-test-data/issues']
    def scm = [url: 'https://github.com/jdaugherty/build-test-data/']

    @Override
    void doWithApplicationContext() {
        Class[] domainClasses = grailsApplication.domainClasses*.clazz
        addBuildMetaMethods(domainClasses)
    }

    @CompileDynamic
    void addBuildMetaMethods(Class<?>... entityClasses){
        entityClasses.each { ec ->
            def mc = ec.metaClass
            //println("adding gradmeta for $ec")
            mc.static.build = {
                return TestData.build(ec)
            }
            mc.static.build = { Map args ->
                return TestData.build(args, ec)
            }
            mc.static.build = { Map args, Map data ->
                return TestData.build(args, ec, data)
            }
            mc.static.findOrBuild = {
                return TestData.findOrBuild( ec, [:])
            }
            mc.static.findOrBuild = { Map data ->
                return TestData.findOrBuild( ec, data)
            }
        }
    }

}
