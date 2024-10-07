package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.jboss.weld.exceptions.IllegalArgumentException;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingBuilder {
    private final Inntektsmelding kladd;
    private EksternArbeidsforholdRef eksternArbeidsforholdId;
    private boolean erBygget;

    public static final String NAV_NO = "NAV_NO";
    private static final String OVERSTYRING_FPSAK = "OVERSTYRING_FPSAK";

    InntektsmeldingBuilder(Inntektsmelding kladd) {
        this.kladd = kladd;
    }

    public static InntektsmeldingBuilder builder() {
        return new InntektsmeldingBuilder(new Inntektsmelding());
    }

    public Inntektsmelding build() {
        return build(false);
    }

    public Inntektsmelding build(boolean ignore) {
        var internRef = getInternArbeidsforholdRef();
        // magic - hvis har ekstern referanse må også intern referanse være spesifikk
        if (internRef.isPresent() && !ignore && eksternArbeidsforholdId != null && eksternArbeidsforholdId.gjelderForSpesifiktArbeidsforhold()
            && internRef.get().getReferanse() == null) {
            throw new IllegalArgumentException(
                "Begge referanser må gjelde spesifikke arbeidsforhold. " + " Ekstern: " + eksternArbeidsforholdId + ", Intern: " + internRef);

        }
        erBygget = true; // Kan ikke bygge mer med samme builder, vil bare returnere samme kladd.
        return kladd;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return kladd.getArbeidsgiver();
    }

    public String getKildesystem() {
        return kladd.getKildesystem();
    }

    public boolean imFraLPSEllerAltinn() {
        return !(NAV_NO.equals(kladd.getKildesystem()) || OVERSTYRING_FPSAK.equals(kladd.getKildesystem()));
    }

    public Optional<EksternArbeidsforholdRef> getEksternArbeidsforholdRef() {
        return Optional.ofNullable(eksternArbeidsforholdId);
    }

    public Optional<InternArbeidsforholdRef> getInternArbeidsforholdRef() {
        return Optional.ofNullable(kladd.getArbeidsforholdRef());
    }

    public InntektsmeldingBuilder leggTil(Gradering gradering) {
        precondition();
        kladd.leggTil(gradering);
        return this;
    }

    public InntektsmeldingBuilder leggTil(NaturalYtelse naturalYtelse) {
        precondition();
        kladd.leggTil(naturalYtelse);
        return this;
    }

    public InntektsmeldingBuilder leggTil(Refusjon refusjon) {
        precondition();
        kladd.leggTil(refusjon);
        return this;
    }

    public InntektsmeldingBuilder leggTil(UtsettelsePeriode utsettelsePeriode) {
        precondition();
        kladd.leggTil(utsettelsePeriode);
        return this;
    }

    public InntektsmeldingBuilder medArbeidsforholdId(EksternArbeidsforholdRef arbeidsforholdId) {
        precondition();
        this.eksternArbeidsforholdId = arbeidsforholdId;
        return this;
    }

    public InntektsmeldingBuilder medArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
        precondition();
        if (arbeidsforholdId != null) {
            // magic - hvis har ekstern referanse må også intern referanse være spesifikk
            if (arbeidsforholdId.getReferanse() == null && eksternArbeidsforholdId != null
                && eksternArbeidsforholdId.gjelderForSpesifiktArbeidsforhold()) {
                throw new IllegalArgumentException(
                    "Begge referanser gjelde spesifikke arbeidsforhold. " + " Ekstern: " + eksternArbeidsforholdId + ", Intern: " + arbeidsforholdId);
            }
            kladd.setArbeidsforholdId(arbeidsforholdId);
        }
        return this;
    }

    public InntektsmeldingBuilder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        precondition();
        kladd.setArbeidsgiver(arbeidsgiver);
        return this;
    }

    public InntektsmeldingBuilder medBeløp(BigDecimal verdi) {
        precondition();
        kladd.setInntektBeløp(verdi == null ? null : new Beløp(verdi));
        return this;
    }

    public InntektsmeldingBuilder medInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        precondition();
        Objects.requireNonNull(innsendingstidspunkt, "innsendingstidspunkt");
        kladd.setInnsendingstidspunkt(innsendingstidspunkt);
        return this;
    }

    public InntektsmeldingBuilder medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak inntektsmeldingInnsendingsårsak) {
        precondition();
        kladd.setInntektsmeldingInnsendingsårsak(inntektsmeldingInnsendingsårsak);
        return this;
    }

    public InntektsmeldingBuilder medJournalpostId(JournalpostId id) {
        precondition();
        kladd.setJournalpostId(id);
        return this;
    }

    public InntektsmeldingBuilder medJournalpostId(String id) {
        precondition();
        return medJournalpostId(new JournalpostId(id));
    }

    public InntektsmeldingBuilder medKanalreferanse(String kanalreferanse) {
        precondition();
        kladd.setKanalreferanse(kanalreferanse);
        return this;
    }

    public InntektsmeldingBuilder medKildesystem(String kildesystem) {
        precondition();
        kladd.setKildesystem(kildesystem);
        return this;
    }

    public InntektsmeldingBuilder medMottattDato(LocalDate mottattDato) {
        precondition();
        kladd.setMottattDato(Objects.requireNonNull(mottattDato, "mottattDato"));
        return this;
    }

    public InntektsmeldingBuilder medNærRelasjon(boolean nærRelasjon) {
        precondition();
        kladd.setNærRelasjon(nærRelasjon);
        return this;
    }

    public InntektsmeldingBuilder medRefusjon(BigDecimal verdi) {
        precondition();
        kladd.setRefusjonBeløpPerMnd(verdi == null ? null : new Beløp(verdi));
        kladd.setRefusjonOpphører(Tid.TIDENES_ENDE);
        return this;
    }

    public InntektsmeldingBuilder medRefusjon(BigDecimal verdi, LocalDate opphører) {
        precondition();
        kladd.setRefusjonBeløpPerMnd(verdi == null ? null : new Beløp(verdi));
        kladd.setRefusjonOpphører(opphører);
        return this;
    }

    public InntektsmeldingBuilder medStartDatoPermisjon(LocalDate startPermisjon) {
        precondition();
        kladd.setStartDatoPermisjon(startPermisjon);
        return this;
    }

    private void precondition() {
        if (erBygget) {
            throw new IllegalStateException("Inntektsmelding objekt er allerede bygget, kan ikke modifisere nå. Returnerer kun : " + kladd);
        }
    }

}
