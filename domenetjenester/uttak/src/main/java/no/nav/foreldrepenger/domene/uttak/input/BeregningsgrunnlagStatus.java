package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktørId;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;

public class BeregningsgrunnlagStatus {

    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final Arbeidsgiver arbeidsgiver;
    private final AktivitetStatus aktivitetStatus;

    public BeregningsgrunnlagStatus(AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.arbeidsgiver = arbeidsgiver;
        this.aktivitetStatus = aktivitetStatus;
    }

    /**
     * Andel uten arbeidsgiver. (eks frilanser, selvstendig næringsdrivende).
     */
    public BeregningsgrunnlagStatus(AktivitetStatus aktivitetStatus) {
        this(aktivitetStatus, null, null);
    }

    public AktivitetIdentifikator toUttakAktivitetIdentifikator() {
        if (erArbeidstaker()) {
            var arbeidsgiver = getArbeidsgiver();
            var arbeidsgiverIdentifikator = arbeidsgiver.map(
                a -> a.getErVirksomhet() ? new Orgnummer(a.getIdentifikator()) : new AktørId(a.getIdentifikator())).orElse(null);
            var arbeidsforholdId = getArbeidsforholdRef().map(InternArbeidsforholdRef::getReferanse).orElse(null);
            return AktivitetIdentifikator.forArbeid(arbeidsgiverIdentifikator, arbeidsforholdId);
        }
        if (erFrilanser()) {
            return AktivitetIdentifikator.forFrilans();
        }
        if (erSelvstendigNæringsdrivende()) {
            return AktivitetIdentifikator.forSelvstendigNæringsdrivende();
        }
        return AktivitetIdentifikator.annenAktivitet();
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdRef);
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public boolean erSelvstendigNæringsdrivende() {
        return aktivitetStatus.erSelvstendigNæringsdrivende();
    }

    public boolean erFrilanser() {
        return aktivitetStatus.erFrilanser();
    }

    public boolean erArbeidstaker() {
        return aktivitetStatus.erArbeidstaker();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + "aktivitetStatus=" + aktivitetStatus + (
            arbeidsgiver == null ? "" : ", arbeidsgiver=" + arbeidsgiver) + (
            arbeidsforholdRef == null ? "" : ", arbeidsforholdRef=" + arbeidsforholdRef) + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        var other = (BeregningsgrunnlagStatus) obj;
        return Objects.equals(this.arbeidsforholdRef, other.arbeidsforholdRef) && Objects.equals(this.arbeidsgiver, other.arbeidsgiver)
            && Objects.equals(this.aktivitetStatus, other.aktivitetStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef, aktivitetStatus);
    }
}
