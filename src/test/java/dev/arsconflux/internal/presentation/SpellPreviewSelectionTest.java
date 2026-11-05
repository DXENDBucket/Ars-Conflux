package dev.arsconflux.internal.presentation;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpellPreviewSelectionTest {
    enum Part { SELF, PROJECTILE, ORBIT, FIREBALL, ICE, HARM, HEAL, AMPLIFY, DELAY }
    private Part select(Part... parts){return SpellPreviewSelection.payload(List.of(parts),
            p->p==Part.FIREBALL||p==Part.ICE||p==Part.AMPLIFY,
            p->p==Part.ORBIT||p==Part.HARM||p==Part.HEAL||p==Part.DELAY,
            p->p==Part.FIREBALL||p==Part.ICE);}
    @Test void directPayloadReplacesAnOtherwiseBlankOrGenericPreview(){
        assertEquals(Part.FIREBALL,select(Part.PROJECTILE,Part.FIREBALL));
        assertEquals(Part.FIREBALL,select(Part.PROJECTILE,Part.AMPLIFY,Part.FIREBALL,Part.HARM));
    }
    @Test void orbitUsesItsOwnPayload(){assertEquals(Part.ICE,select(Part.SELF,Part.ORBIT,Part.AMPLIFY,Part.ICE,Part.HARM));}
    @Test void ordinarySpellsKeepNativeSelection(){
        assertNull(select(Part.SELF,Part.HEAL,Part.AMPLIFY));
        assertNull(select(Part.PROJECTILE,Part.HARM));assertNull(select());
    }
    @Test void laterNestedProjectileCannotReplaceEarlierAction(){
        assertNull(select(Part.SELF,Part.HEAL,Part.DELAY,Part.PROJECTILE,Part.FIREBALL));
        assertNull(select(Part.PROJECTILE,Part.HARM,Part.ORBIT,Part.ICE));
    }
    @Test void implicitMethodTemplatesCanShowTheirPayload(){
        assertEquals(Part.FIREBALL,select(Part.FIREBALL));assertEquals(Part.ICE,select(Part.AMPLIFY,Part.ICE));
    }
}
