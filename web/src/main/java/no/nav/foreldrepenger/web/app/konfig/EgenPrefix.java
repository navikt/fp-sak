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

    private boolean hasPreviouslyResolvedToOtherClass(String name, Class<?> cls) {
        Class<?> prevCls = (Class)this.previouslyResolvedTo.get(name);
        if (cls != null && prevCls != null) {
            return !prevCls.equals(cls);
        } else {
            return false;
        }
    }

    private String strippedName(String fqn, Class<?> cls) {
        String dtoNavn = fqn.replaceAll("^.*\\.", "");

        if (!this.hasPreviouslyResolvedToOtherClass(dtoNavn, cls)) {
            return dtoNavn;
        }

        return fqn;
    }

    protected String getNameOfClass(Class<?> cls) {
        String name = this.strippedName(super.getNameOfClass(cls), cls);
        if (this.hasPreviouslyResolvedToOtherClass(name, cls)) {
            Class<?> otherClass = (Class)this.previouslyResolvedTo.get(name);
            if (otherClass != null) {
                throw new IllegalArgumentException("Type name \"" + name + "\", (for class" + cls.getName() + ") has previously resolved to another class (" + otherClass.getName() + ")");
            } else {
                throw new IllegalArgumentException("Type name \"" + name + "\", (for class" + cls.getName() + ") has previously resolved to another class");
            }
        }

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
//        var antallDtoMedDot = this.previouslyResolvedTo.keySet().stream().filter(k -> k.contains(".")).count();
//        System.out.println(antallDtoMedDot);
        return name;

    }
}
