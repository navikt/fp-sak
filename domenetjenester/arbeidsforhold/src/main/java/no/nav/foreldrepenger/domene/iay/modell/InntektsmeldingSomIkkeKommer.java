package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class InntektsmeldingSomIkkeKommer {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef internRef;

    public InntektsmeldingSomIkkeKommer(Arbeidsgiver arbeidsgiver,
                                        InternArbeidsforholdRef internRef,
                                        @SuppressWarnings("unused") EksternArbeidsforholdRef eksternRef // NOSONAR
    ) {
        this.arbeidsgiver = arbeidsgiver;
        this.internRef = internRef;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getRef() {
        return internRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InntektsmeldingSomIkkeKommer that = (InntektsmeldingSomIkkeKommer) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(internRef, that.internRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, internRef);
    }

    @Override
    public String toString() {
        return "InntektsmeldingSomIkkeKommer{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", internRef=" + internRef +
            '}';
    }
}
