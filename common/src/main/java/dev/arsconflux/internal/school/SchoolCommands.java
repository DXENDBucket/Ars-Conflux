package dev.arsconflux.internal.school;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import dev.arsconflux.api.school.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

/** Read-only authoring diagnostics; all gameplay data comes from declarations/datapacks. */
public final class SchoolCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("conflux").requires(source->source.hasPermission(2))
            .then(Commands.literal("schools")
                .then(Commands.literal("list").executes(context->{
                    var definitions=SchoolRegistry.all();
                    definitions.forEach((id,def)->context.getSource().sendSuccess(()->Component.literal(id+" — ").append(def.displayName()),false));
                    return definitions.size();
                }))
                .then(Commands.literal("glyph").then(Commands.argument("id",ResourceLocationArgument.id())
                    .suggests((context,builder)->SharedSuggestionProvider.suggest(GlyphSchools.declaredGlyphs().stream().map(Object::toString),builder))
                    .executes(context->{
                        var id=ResourceLocationArgument.getId(context,"id");var text=id.toString();
                        var weights=GlyphSchools.get(id);
                        if(weights.isEmpty())context.getSource().sendSuccess(()->Component.translatable("commands.ars_conflux.schools.undeclared",text),false);
                        weights.shares().forEach((school,share)->context.getSource().sendSuccess(()->Component.literal(school+" — "+String.format(Locale.ROOT,"%.2f%%",share*100)),false));
                        return weights.shares().size();
                    })))));
    }
    private SchoolCommands() {}
}
