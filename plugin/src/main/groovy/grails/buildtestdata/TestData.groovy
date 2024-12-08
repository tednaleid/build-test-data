package grails.buildtestdata


import grails.buildtestdata.builders.DataBuilder
import grails.buildtestdata.builders.DataBuilderContext
import grails.buildtestdata.builders.DataBuilderFactory
import grails.buildtestdata.builders.PersistentEntityDataBuilder
import grails.buildtestdata.builders.PogoDataBuilder
import grails.buildtestdata.builders.ValidateableDataBuilder
import grails.buildtestdata.propsresolver.InitialPropsResolver
import groovy.transform.CompileStatic
import org.springframework.core.annotation.AnnotationAwareOrderComparator

/**
 * Primary static API to build a domain instance with test data
 */
@CompileStatic
class TestData {

    static InitialPropsResolver initialPropsResolver

    static final Map<Class, DataBuilder> builders = new HashMap<>()
    static final List<DataBuilderFactory> factories = []
    static{
        factories.add(new PersistentEntityDataBuilder.Factory())
        factories.add(new ValidateableDataBuilder.Factory())
        factories.add(new PogoDataBuilder.Factory())
        AnnotationAwareOrderComparator.sort(factories)
    }

    /**
     * calls {@link #build(Map, Class, Map}
     */
    static <T> T build(Class<T> entityClass, Map<String, Object> data = [:]) {
        build([:], entityClass, data)
    }

    /**
     * pulls/parses the args map and calls {@link #build(Map, Class, Map)}
     *
     * @param args optional argument map <br> see desc on {@link #build(Map, Class, Map)} <br>
     *  Adds the following option <br>
     *  - data : a map of data to bind, can also just be in the args. usefull if you have a field named 'save' or 'find' etc..
     * @param entityClass the domain class for the entity that is built
     * @return the built entity.
     */
    static <T> T build(Map args, Class<T> entityClass) {
        Map newArgs = [:]
        Map<String, Object> propValues = [:]
        if (args){
            ['save', 'find', 'includes', 'flush', 'failOnError'].each { key ->
                if (args.containsKey(key)) newArgs[key] = args.remove(key)
            }
            propValues = ((args?.data) ? args.remove('data') : args) as Map<String, Object>
        }
        return build(newArgs, entityClass, propValues)
    }

    /**
     * finds a DataBuilder for the entityClass an calls build. see {@link PersistentEntityDataBuilder#build} for example
     *
     * @param args  optional argument map <br>
     *  - save        : (default: true) whether to call the save method when its a GormEntity <br>
     *  - find        : (default: false) whether to try and find the entity in the datastore first <br>
     *  - flush       : (default: false) passed in the args to the GormEntity save method <br>
     *  - failOnError : (default: true) passed in the args to the GormEntity save method <br>
     *  - include     : a list of the properties to build in addition to the required fields. use `*` to build all <br>
     * @param entityClass   the domain class to use
     * @param data          properties to set on the entity instance before it tries to build tests data
     * @return the built and saved entity instance
     */
    static <T> T build(Map args, Class<T> entityClass, Map<String, Object> data) {

        DataBuilderContext ctx = new DataBuilderContext(data)
        ctx.includes = args['includes']

        (T) findBuilder(entityClass).build(args, ctx)
    }

    /**
     * tried to find the an existing entity in the store, otherwise build its
     *
     * @param entityClass the domain class to use
     * @param data to be used in the finder or if not found used to build it.
     * @return the built and saved entity instance
     */
    static <T> T findOrBuild(Class<T> entityClass, Map<String, Object> data = [:]) {
        build([find: true], entityClass, data)
    }

    static getInitialPropsResolver(){
        if(initialPropsResolver == null){
            initialPropsResolver = new InitialPropsResolver.EmptyInitialPropsResolver()
        }
        initialPropsResolver
    }

    /**
     * Find in cache or create a builder
     * @param clazz
     * @return
     */
    static DataBuilder findBuilder(Class clazz){
        if(!builders.containsKey(clazz)){
            builders.put(clazz,createBuilder(clazz))
        }
        builders.get(clazz)
    }

    /**
     * creates a new DataBuilder by looking through the factories for one that supports the class.
     * @param clazz
     * @return
     */
    static DataBuilder createBuilder(Class clazz){
        for(factory in factories){
            if(factory.supports(clazz)){
                return factory.build(clazz)
            }
        }
    }

    static void clear() {
        builders.clear()
    }
}
