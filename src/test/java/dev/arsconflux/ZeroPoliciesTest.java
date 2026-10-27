package dev.arsconflux;

import dev.arsconflux.internal.zero.PreviousPhaseResults;
import dev.arsconflux.internal.zero.ZeroSourceBudget;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZeroPoliciesTest {
    @Test void everyExecutionReadsTheLastOutputAndRejectsLateCallbacks() {
        var history=new PreviousPhaseResults<String>();
        var begin=history.advance(); assertTrue(begin.input().isEmpty());
        history.append(begin,"begin target");
        var tick=history.advance(); assertEquals(List.of("begin target"),tick.input());
        assertFalse(history.append(begin,"late target"));
        history.append(tick,"new target");
        assertEquals(List.of("begin target"),tick.input());
        assertEquals(List.of("new target"),history.advance().input());
    }
    @Test void anEmptyExecutedPhaseDoesNotReviveTheBeginSelection() {
        var history=new PreviousPhaseResults<String>();
        history.append(history.advance(),"begin");
        assertEquals(List.of("begin"),history.advance().input());
        assertTrue(history.advance().input().isEmpty());
    }
    @Test void noTickHandsBeginToEndAndRepeatedTicksStayBounded() {
        var history=new PreviousPhaseResults<Integer>();
        history.append(history.advance(),42);
        var step=history.advance(); assertEquals(List.of(42),step.input());
        for(int i=0;i<10000;i++) { history.append(step,i); step=history.advance(); assertEquals(1,step.input().size()); }
        history.reset(); assertFalse(history.append(step,99)); assertTrue(history.advance().input().isEmpty());
    }
    @Test void splitBranchesShareOneManaConversionAllowance() {
        var budget=ZeroSourceBudget.manaPowered(40);
        assertEquals(20,budget.claim(20)); assertEquals(20,budget.claim(20)); assertEquals(0,budget.claim(20));
    }
    @Test void discountedSourceCastCannotRefundMoreThanItActuallyPaid() {
        var budget=ZeroSourceBudget.sourcePowered(60,100,10);
        assertEquals(2,budget.claim(20)); assertEquals(4,budget.claim(40)); assertEquals(0,budget.claim(100));
        var full=ZeroSourceBudget.sourcePowered(200,100,10);
        assertEquals(10,full.claim(200)); assertEquals(0,full.claim(200));
    }
    @Test void unknownSourceQuotesDoNotMintSourceAndScalingDoesNotOverflow() {
        assertEquals(0,ZeroSourceBudget.sourcePowered(50,50,0).claim(50));
        assertEquals(0,ZeroSourceBudget.sourcePowered(50,0,10).claim(50));
        assertEquals(Integer.MAX_VALUE,ZeroSourceBudget.sourcePowered(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE).claim(Integer.MAX_VALUE));
    }
}
