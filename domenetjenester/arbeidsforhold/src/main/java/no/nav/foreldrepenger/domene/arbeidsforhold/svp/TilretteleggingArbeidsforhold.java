package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public final class TilretteleggingArbeidsforhold {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef internArbeidsforholdRef;
    private UttakArbeidType uttakArbeidType;

    public TilretteleggingArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef,
            UttakArbeidType uttakArbeidType) {
        this.arbeidsgiver = arbeidsgiver;
        this.internArbeidsforholdRef = Objects.requireNonNull(internArbeidsforholdRef, "internArbeidsforholdRef");
        this.uttakArbeidType = uttakArbeidType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public InternArbeidsforholdRef getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (TilretteleggingArbeidsforhold) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
                Objects.equals(internArbeidsforholdRef, that.internArbeidsforholdRef) &&
                Objects.equals(uttakArbeidType, that.uttakArbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, internArbeidsforholdRef, uttakArbeidType);
    }

}
