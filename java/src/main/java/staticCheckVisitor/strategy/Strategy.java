package staticCheckVisitor.strategy;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;

import java.util.ArrayList;

public interface Strategy {
    ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor);
}
