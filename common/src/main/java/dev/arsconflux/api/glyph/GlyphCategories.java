package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.api.projectile.ProjectileForm;

/** Presentation semantics; never changes Ars phrase parsing or augment inheritance. */
public final class GlyphCategories {
    public static boolean projectile(AbstractSpellPart part){return part instanceof ProjectileForm;}
    public static int rank(AbstractSpellPart part){
        if(part instanceof AbstractCastMethod)return 0;
        if(projectile(part))return 2;
        if(part instanceof AbstractAugment)return 3;
        return part.getTypeIndex();
    }
    private GlyphCategories(){}
}
