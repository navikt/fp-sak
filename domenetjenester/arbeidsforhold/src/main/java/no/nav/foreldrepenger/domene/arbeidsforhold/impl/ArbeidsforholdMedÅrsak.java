package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class ArbeidsforholdMedÅrsak {

    private InternArbeidsforholdRef ref;
    private Set<AksjonspunktÅrsak> årsaker = new HashSet<>();

    ArbeidsforholdMedÅrsak(){
        // default ctor
    }

    ArbeidsforholdMedÅrsak(InternArbeidsforholdRef ref) {
        this.ref = ref;
    }

    public InternArbeidsforholdRef getRef() {
        return ref;
    }

    public Set<AksjonspunktÅrsak> getÅrsaker() {
        return årsaker;
    }

    ArbeidsforholdMedÅrsak leggTilÅrsak(AksjonspunktÅrsak årsak) {
        årsaker.add(årsak);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsforholdMedÅrsak that = (ArbeidsforholdMedÅrsak) o;
        return Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref);
    }

}
