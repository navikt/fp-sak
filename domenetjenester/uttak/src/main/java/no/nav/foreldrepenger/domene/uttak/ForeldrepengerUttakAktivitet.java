package no.nav.foreldrepenger.domene.uttak;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class ForeldrepengerUttakAktivitet {

    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final UttakArbeidType uttakArbeidType;
    private final Arbeidsgiver arbeidsgiver;

    public ForeldrepengerUttakAktivitet(UttakArbeidType uttakArbeidType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.uttakArbeidType = uttakArbeidType;
        this.arbeidsgiver = arbeidsgiver;
    }

    public ForeldrepengerUttakAktivitet(UttakArbeidType uttakArbeidType) {
        this(uttakArbeidType, null, null);
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ForeldrepengerUttakAktivitet) o;
        return Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) && uttakArbeidType == that.uttakArbeidType && Objects.equals(arbeidsgiver,
            that.arbeidsgiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsforholdRef, uttakArbeidType, arbeidsgiver);
    }

    @Override
    public String toString() {
        return "ForeldrepengerUttakAktivitet{" + "arbeidsforholdRef=" + arbeidsforholdRef + ", uttakArbeidType=" + uttakArbeidType + ", arbeidsgiver="
            + arbeidsgiver + '}';
    }
}
