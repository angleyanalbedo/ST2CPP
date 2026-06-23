package staticCheckVisitor.register;

import org.reflections.Reflections;
import staticCheckVisitor.factory.Factory;
import staticCheckVisitor.strategy.Strategy;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class Registrant {
    //策略所在的包名
    static public String packageName = "staticCheckVisitor.strategy";

    public Registrant(){
    }

    public void autoRegister() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Reflections f = new Reflections(packageName);
        Factory factory = Factory.getFactory();
        Set<Class<?>> set = f.getTypesAnnotatedWith(StrategyForVisit.class);
        for (Class<?> aClass : set) {
            StrategyForVisit initStrategy = aClass.getAnnotation(StrategyForVisit.class);
            int ruleIndex = initStrategy.ruleIndex();
            int branch = initStrategy.branch();
            Strategy strategy = (Strategy) aClass.getDeclaredConstructor().newInstance();
            factory.register(ruleIndex, branch, strategy);
        }
    }
}
