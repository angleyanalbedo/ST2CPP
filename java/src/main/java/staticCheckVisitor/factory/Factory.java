package staticCheckVisitor.factory;

import java.util.ArrayList;

import staticCheckVisitor.strategy.Strategy;

import java.util.HashMap;

public class Factory {
    private final HashMap<Integer, Strategy> strategyHashMap;
    private final HashMap<Integer, HashMap<Integer, Strategy>> branchStrategyMap;

    static final private Factory factory = new Factory();

    private Factory(){
        strategyHashMap = new HashMap<>();
        branchStrategyMap = new HashMap<>();
    }

    public static Factory getFactory() {
        return factory;
    }

    public void register(int ruleIndex, int branch, Strategy strategy){
        if(branch == 0){
            registerStrategy(ruleIndex, strategy);
        }else {
            registerStrategy(ruleIndex, branch, strategy);
        }
    }

    /**
     * @describe 没有分支/语法的第一个分支的策略注册到strategyHashMap中
     * */
    private void registerStrategy(int ruleIndex, Strategy strategy){
        this.strategyHashMap.put(ruleIndex, strategy);
    }

    /**
     * @describe 分支的策略注册到branchStrategyMap中
     * */
    private void registerStrategy(int ruleIndex, int branch, Strategy strategy){
        HashMap<Integer, Strategy> map;
        if(branchStrategyMap.containsKey(ruleIndex)){
            map = branchStrategyMap.get(ruleIndex);
        }else {
            map = new HashMap<>();
            branchStrategyMap.put(ruleIndex, map);
        }
        map.put(branch, strategy);
    }

    public Strategy getStrategy(int ruleIndex){
        Strategy strategy = this.strategyHashMap.get(ruleIndex);
        if(strategy == null){
            // 无策略的规则跳过语义检查（不影响语法解析和代码生成）
            return (ctx, visitor) -> new ArrayList<>();
        }
        return strategy;
    }

    public Strategy getStrategy(int ruleIndex, int branch){
        Strategy strategy;
        if(branch == 0){
            strategy = this.strategyHashMap.get(ruleIndex);
        }else{
            strategy = this.branchStrategyMap.get(ruleIndex).get(branch);
        }
        if(strategy == null){
            // 无策略的规则跳过语义检查
            return (ctx, visitor) -> new ArrayList<>();
        }
        return strategy;
    }

}
