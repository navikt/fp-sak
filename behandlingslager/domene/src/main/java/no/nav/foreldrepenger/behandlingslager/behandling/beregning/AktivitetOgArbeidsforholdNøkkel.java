package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.util.Objects;

public class AktivitetOgArbeidsforholdNøkkel {
    private final Arbeidsgiver arbeidsgiver;
    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final AktivitetStatus aktivitetStatus;
    private final Inntektskategori inntektskategori;

    AktivitetOgArbeidsforholdNøkkel(BeregningsresultatAndel andel) {
        this.arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        this.arbeidsforholdRef = andel.getArbeidsforholdRef();
        this.aktivitetStatus = andel.getAktivitetStatus();
        this.inntektskategori = andel.getInntektskategori();
    }

    @Override
    public String toString() {
        return "BeregningsresultatAktivitetsnøkkel{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", aktivitetStatus=" + aktivitetStatus +
            ", inntektskategori=" + inntektskategori +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AktivitetOgArbeidsforholdNøkkel that)){
            return false;
        }

        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef)
            && Objects.equals(aktivitetStatus, that.aktivitetStatus)
            && Objects.equals(inntektskategori, that.inntektskategori);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef, aktivitetStatus, inntektskategori);
    }
}
