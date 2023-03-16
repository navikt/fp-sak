package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class InntektsmeldingDto {
    private String arbeidsgiverReferanse;
    private String arbeidsgiver;
    private String arbeidsgiverOrgnr;
    private LocalDate arbeidsgiverStartdato;
    private LocalDateTime innsendingstidspunkt;

    private List<UtsettelsePeriodeDto> utsettelsePerioder = new ArrayList<>();
    private List<GraderingPeriodeDto> graderingPerioder = new ArrayList<>();
    private Beløp getRefusjonBeløpPerMnd;

    InntektsmeldingDto() {
        // trengs for deserialisering av JSON
    }

    public InntektsmeldingDto(Inntektsmelding inntektsmelding, Optional<Virksomhet> virksomhet) {
        this.arbeidsgiverReferanse = inntektsmelding.getArbeidsgiver().getIdentifikator();
        var arb = inntektsmelding.getArbeidsgiver();
        this.arbeidsgiver = arb.getErVirksomhet()
                ? virksomhet.orElseThrow(() -> {
                    return new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + arb.getOrgnr());
                }).getNavn()
                : "Privatperson"; // TODO skal navn på privatperson som arbeidsgiver hentes fra et register?
        this.arbeidsgiverOrgnr = arb.getIdentifikator();
        this.arbeidsgiverStartdato = inntektsmelding.getStartDatoPermisjon().orElse(null);
        this.innsendingstidspunkt = inntektsmelding.getInnsendingstidspunkt();

        var utsettelser = inntektsmelding.getUtsettelsePerioder();
        if (utsettelser != null) {
            this.utsettelsePerioder.addAll(utsettelser
                    .stream()
                    .map(UtsettelsePeriodeDto::new)
                    .toList());
        }

        var graderinger = inntektsmelding.getGraderinger();
        if (graderinger != null) {
            this.graderingPerioder.addAll(graderinger
                    .stream()
                    .map(GraderingPeriodeDto::new)
                    .toList());
        }

        getRefusjonBeløpPerMnd = inntektsmelding.getRefusjonBeløpPerMnd();
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public void setArbeidsgiverOrgnr(String arbeidsgiverOrgnr) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
    }

    public LocalDate getArbeidsgiverStartdato() {
        return arbeidsgiverStartdato;
    }

    public void setArbeidsgiverStartdato(LocalDate arbeidsgiverStartdato) {
        this.arbeidsgiverStartdato = arbeidsgiverStartdato;
    }

    public List<UtsettelsePeriodeDto> getUtsettelsePerioder() {
        return utsettelsePerioder;
    }

    public List<GraderingPeriodeDto> getGraderingPerioder() {
        return graderingPerioder;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public Beløp getGetRefusjonBeløpPerMnd() {
        return getRefusjonBeløpPerMnd;
    }

    public void setGetRefusjonBeløpPerMnd(Beløp getRefusjonBeløpPerMnd) {
        this.getRefusjonBeløpPerMnd = getRefusjonBeløpPerMnd;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }
}
