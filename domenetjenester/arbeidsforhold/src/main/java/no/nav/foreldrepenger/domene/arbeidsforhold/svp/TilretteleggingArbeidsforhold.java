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
    private boolean arbeidsforholdetErSplittet;

    public TilretteleggingArbeidsforhold(Arbeidsgiver arbeidsgiver,
                                         InternArbeidsforholdRef internArbeidsforholdRef,
                                         UttakArbeidType uttakArbeidType,
                                         boolean arbeidsforholdetErSplittet) {
        this.arbeidsgiver = arbeidsgiver;
        this.internArbeidsforholdRef = Objects.requireNonNull(internArbeidsforholdRef, "internArbeidsforholdRef");
        this.uttakArbeidType = uttakArbeidType;
        this.arbeidsforholdetErSplittet = arbeidsforholdetErSplittet;
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

    public boolean getArbeidsforholdetErSplittet() {
        return arbeidsforholdetErSplittet;
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
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(internArbeidsforholdRef, that.internArbeidsforholdRef)
            && Objects.equals(uttakArbeidType, that.uttakArbeidType) && Objects.equals(arbeidsforholdetErSplittet, that.arbeidsforholdetErSplittet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, internArbeidsforholdRef, uttakArbeidType, arbeidsforholdetErSplittet);
    }

}
