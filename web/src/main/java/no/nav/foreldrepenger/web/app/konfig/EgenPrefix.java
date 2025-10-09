package no.nav.foreldrepenger.web.app.konfig;


import io.swagger.v3.core.jackson.TypeNameResolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EgenPrefix extends TypeNameResolver {
    private final Map<String, Class<?>> previouslyResolvedTo;

    public EgenPrefix() {
        this.previouslyResolvedTo = new HashMap<>();
        this.setUseFqn(true);
    }

    private String strippedName(String fqn) {
        return fqn.replaceAll("^.*\\.", "");
    }

    protected String getNameOfClass(Class<?> cls) {
        String name = this.strippedName(super.getNameOfClass(cls));

        var dtoNavnErTatt = this.previouslyResolvedTo.get(name) != null;

        if (dtoNavnErTatt) {
            var fqn = super.getNameOfClass(cls);
            var navnDeler = fqn.split("\\.");

            // Prefix på 3 ledd er nok for å sikre unikhet
            String nyttNavnMed3PrefixLedd = String.join(".", Arrays.copyOfRange(navnDeler, navnDeler.length-3, navnDeler.length));
            this.previouslyResolvedTo.put(nyttNavnMed3PrefixLedd, cls);
            return nyttNavnMed3PrefixLedd;
        }

        this.previouslyResolvedTo.put(name, cls);
        return name;
    }
}
