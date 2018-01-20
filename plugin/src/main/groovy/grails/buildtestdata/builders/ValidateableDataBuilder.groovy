package grails.buildtestdata.builders

import grails.buildtestdata.MockErrors
import grails.buildtestdata.handler.*
import grails.buildtestdata.utils.ClassUtils
import grails.gorm.validation.Constrained
import grails.gorm.validation.ConstrainedProperty
import grails.gorm.validation.Constraint
import grails.gorm.validation.DefaultConstrainedProperty
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.InvokerHelper

@Slf4j
@CompileStatic
class ValidateableDataBuilder extends PogoTestDataBuilder {

    static List<String> CONSTRAINT_SORT_ORDER = [
        ConstrainedProperty.IN_LIST_CONSTRAINT, // most important
        ConstrainedProperty.NULLABLE_CONSTRAINT,
        ConstrainedProperty.EMAIL_CONSTRAINT,
        ConstrainedProperty.CREDIT_CARD_CONSTRAINT,
        ConstrainedProperty.URL_CONSTRAINT,
        ConstrainedProperty.RANGE_CONSTRAINT,
        ConstrainedProperty.SCALE_CONSTRAINT,
        ConstrainedProperty.SIZE_CONSTRAINT,
        ConstrainedProperty.MAX_CONSTRAINT,
        ConstrainedProperty.MIN_CONSTRAINT,
        ConstrainedProperty.MIN_SIZE_CONSTRAINT,
        ConstrainedProperty.MAX_SIZE_CONSTRAINT,
        ConstrainedProperty.MATCHES_CONSTRAINT,
        ConstrainedProperty.VALIDATOR_CONSTRAINT,   // not implememnted, provide sample data
        ConstrainedProperty.BLANK_CONSTRAINT, // precluded by no '' default value applied in the nullable constraint handling
    ].reverse()
    
    static Map<String, ? extends ConstraintHandler> defaultHandlers = [
        (ConstrainedProperty.MIN_SIZE_CONSTRAINT)   : new MinSizeConstraintHandler(),
        (ConstrainedProperty.MAX_SIZE_CONSTRAINT)   : new MaxSizeConstraintHandler(),
        (ConstrainedProperty.IN_LIST_CONSTRAINT)    : new InListConstraintHandler(),
        (ConstrainedProperty.CREDIT_CARD_CONSTRAINT): new CreditCardConstraintHandler(),
        (ConstrainedProperty.EMAIL_CONSTRAINT)      : new EmailConstraintHandler(),
        (ConstrainedProperty.URL_CONSTRAINT)        : new UrlConstraintHandler(),
        (ConstrainedProperty.RANGE_CONSTRAINT)      : new RangeConstraintHandler(),
        (ConstrainedProperty.SIZE_CONSTRAINT)       : new SizeConstraintHandler(),
        (ConstrainedProperty.MIN_CONSTRAINT)        : new MinConstraintHandler(),
        (ConstrainedProperty.MAX_CONSTRAINT)        : new MaxConstraintHandler(),
        (ConstrainedProperty.NULLABLE_CONSTRAINT)   : new NullableConstraintHandler(),
        (ConstrainedProperty.MATCHES_CONSTRAINT)    : new MatchesConstraintHandler(),
        (ConstrainedProperty.BLANK_CONSTRAINT)      : new BlankConstraintHandler(),
        (ConstrainedProperty.VALIDATOR_CONSTRAINT)  : new ValidatorConstraintHandler()
    ]
    
    // reverse so that when we compare, missing items are -1, then we are orders 0 -> n least to most important

    // TODO: filter to actual list for this class, or possibly each property value?
    Map<String, ? extends ConstraintHandler> handlers

    //Collection<String> requiredPropertyNames
    Set<String> requiredPropertyNames

    ValidateableDataBuilder(Class target) {
        super(target)
        this.requiredPropertyNames = findRequiredPropertyNames()
        this.handlers = new HashMap<String,? extends ConstraintHandler>(defaultHandlers)

    }

    def isRequiredConstrained(Constrained constrained) {
        boolean bindable = isBindable(constrained)
        boolean nullable = constrained.nullable
        !nullable && bindable
    }
    
    boolean isBindable(Constrained constrained){
        //TODO: Check if constraint is bindable 
        return true 
    }

    Map<String, ConstrainedProperty>  getConstraintsMap() {
        //Assume its a grails.validation.Validateable. overrides in GormEntityDataBuilder
        ClassUtils.getStaticPropertyValue(target,'constraintsMap') as Map<String, ConstrainedProperty>
    }

    Set<String>  findRequiredPropertyNames() {
        Map<String,ConstrainedProperty> constraints = constraintsMap
        //println constraintsMap

        if(constraints){
            return constraints.keySet().findAll {
                isRequiredConstrained(constraints.get(it))
            }
        }
        return [] as Set
    }

    
    @Override
    def build(DataBuilderContext ctx) {
        Object instance = (Object) super.build(ctx)
        populateRequiredValues(instance, ctx)
        instance
    }

    void populateRequiredValues(Object instance, DataBuilderContext ctx) {
        for (requiredPropertyName in requiredPropertyNames) {
            ConstrainedProperty constrained = constraintsMap.get(requiredPropertyName)
            if(!isSatisfied(instance,requiredPropertyName,constrained)){
                satisfyConstrained(instance, requiredPropertyName,constrained,ctx)
            }
            else if(!isBasicType(((ConstrainedProperty)constrained).propertyType)){
                ctx.satisfyNested(instance,requiredPropertyName,constrained.propertyType)
            }
            //do examples if it exists
            exampleMetaConstraints(instance, requiredPropertyName, constrained,ctx)
        }
    }

    @CompileDynamic
    void exampleMetaConstraints(Object instance, String propertyName, ConstrainedProperty constrained, DataBuilderContext ctx){
        if(constrained instanceof DefaultConstrainedProperty && constrained.metaConstraints["example"]){
            new ExampleConstraintHandler().handle(instance, propertyName, null, constrained, ctx)
        }
    }
    
    boolean isSatisfied(Object instance,String propertyName,ConstrainedProperty constrainedProperty){
        def errors = new MockErrors(this)

        constrainedProperty.validate(instance, InvokerHelper.getProperty(instance,propertyName), errors)
        return !errors.hasErrors()
    }
    
    Object satisfyConstrained(Object instance, String propertyName, ConstrainedProperty constrained, DataBuilderContext ctx) {
        return sortedConstraints(constrained.appliedConstraints).find { Constraint constraint ->
            log.debug "${target?.name}.${constraint?.name} constraint, field before adjustment: ${InvokerHelper.getProperty(instance,propertyName)}"
            ConstraintHandler handler = handlers[constraint.name]
            if (handler) {
                handler.handle(instance, propertyName, constraint, constrained, ctx)
                log.debug "${target?.name}.$propertyName field after adjustment for ${constraint?.name}: ${InvokerHelper.getProperty(instance,propertyName)}"
            } else {
                log.warn "Unable to find property generator handler for constraint ${constraint?.name}!"
            }

            if (isSatisfied(instance, propertyName,constrained)) {
                return true
            }
        }
    }
    List<Constraint> sortedConstraints(Collection<Constraint> appliedConstraints) {
        return appliedConstraints.sort { a, b ->
            CONSTRAINT_SORT_ORDER.indexOf(b.name) <=> CONSTRAINT_SORT_ORDER.indexOf(a.name)
        }
    }
}
