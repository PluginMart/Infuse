package com.catadmirer.infuseSMP.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SelectStringArgumentType implements CustomArgumentType<String,String> {
    private static final Dynamic2CommandExceptionType ERROR_INVALID_STRING = new Dynamic2CommandExceptionType((arg,allowed) -> new LiteralMessage("\"" + arg + "\" is not a valid argument here. Use one of: " + allowed));
    
    private final List<String> allowedStrings;

    public SelectStringArgumentType(List<String> allowedStrings) {
        this.allowedStrings = List.copyOf(allowedStrings);
    }

    public SelectStringArgumentType(String... allowedStrings) {
        this.allowedStrings = Arrays.asList(allowedStrings);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String in = reader.readUnquotedString();

        if (allowedStrings.contains(in)) return in;

        throw ERROR_INVALID_STRING.create(in, Arrays.deepToString(allowedStrings.toArray()));
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        allowedStrings.stream()
            .filter(s -> s.contains(builder.getRemaining()))
            .sorted()
            .forEach(builder::suggest);

        return builder.buildFuture();
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}