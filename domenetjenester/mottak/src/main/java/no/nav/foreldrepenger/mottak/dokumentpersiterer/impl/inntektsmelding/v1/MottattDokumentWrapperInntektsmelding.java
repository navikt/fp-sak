package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20180924.AvtaltFerieListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GraderingIForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GraderingIForeldrepengerListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20180924.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20180924.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20180924.UtsettelseAvForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20180924.UtsettelseAvForeldrepengerListe;

public class MottattDokumentWrapperInntektsmelding extends MottattDokumentWrapper<InntektsmeldingM> {

    public MottattDokumentWrapperInntektsmelding(InntektsmeldingM skjema) {
        super(skjema, InntektsmeldingConstants.NAMESPACE);
    }

    public FagsakYtelseType getYtelse() {
        String ytelse = getSkjema().getSkjemainnhold().getYtelse();
        if (ytelse.toLowerCase().matches("foreldrepenger")) {
            return FagsakYtelseType.FORELDREPENGER;
        }
        if (ytelse.toLowerCase().matches("svangerskapspenger")) {
            return FagsakYtelseType.SVANGERSKAPSPENGER;
        }
        return FagsakYtelseType.UDEFINERT;
    }

    public List<NaturalytelseDetaljer> getGjenopptakelserAvNaturalytelse() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getGjenopptakelseNaturalytelseListe())
            .map(JAXBElement::getValue)
            .map(GjenopptakelseNaturalytelseListe::getNaturalytelseDetaljer)
            .orElse(Collections.emptyList());
    }

    public List<NaturalytelseDetaljer> getOpphørelseAvNaturalytelse() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getOpphoerAvNaturalytelseListe())
            .map(JAXBElement::getValue)
            .map(OpphoerAvNaturalytelseListe::getOpphoerAvNaturalytelse)
            .orElse(Collections.emptyList());
    }

    public String getArbeidstaker() {
        return getSkjema().getSkjemainnhold().getArbeidstakerFnr();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return getSkjema().getSkjemainnhold().getArbeidsgiver();
    }

    public Optional<Arbeidsforhold> getArbeidsforhold() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getArbeidsforhold()).map(JAXBElement::getValue);
    }

    public Optional<String> getArbeidsforholdId() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getArbeidsforhold())
            .map(JAXBElement::getValue)
            .map(Arbeidsforhold::getArbeidsforholdId)
            .map(JAXBElement::getValue);
    }

    public String getVirksomhetsNr() {
        return getArbeidsgiver().getVirksomhetsnummer();
    }

    public boolean getErNærRelasjon() {
        return getSkjema().getSkjemainnhold().isNaerRelasjon();
    }

    public Optional<LocalDate> getStartDatoPermisjon() {
        FagsakYtelseType ytelseType = getYtelse();
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return Optional.ofNullable(getSkjema().getSkjemainnhold().getStartdatoForeldrepengeperiode().getValue());
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            var førsteFraværsdag = getSkjema().getSkjemainnhold().getArbeidsforhold().getValue().getFoersteFravaersdag();
            return Optional.ofNullable(førsteFraværsdag != null ? førsteFraværsdag.getValue() : null);
        }
        return Optional.empty();
    }

    public Optional<Refusjon> getRefusjon() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getRefusjon()).map(JAXBElement::getValue);
    }

    public List<GraderingIForeldrepenger> getGradering() {
        return getArbeidsforhold().map(Arbeidsforhold::getGraderingIForeldrepengerListe)
            .map(JAXBElement::getValue)
            .map(GraderingIForeldrepengerListe::getGraderingIForeldrepenger)
            .orElse(Collections.emptyList());
    }

    public List<Periode> getAvtaltFerie() {
        return getArbeidsforhold().map(Arbeidsforhold::getAvtaltFerieListe)
            .map(JAXBElement::getValue)
            .map(AvtaltFerieListe::getAvtaltFerie)
            .orElse(Collections.emptyList());
    }

    public List<UtsettelseAvForeldrepenger> getUtsettelser() {
        return getArbeidsforhold().map(Arbeidsforhold::getUtsettelseAvForeldrepengerListe)
            .map(JAXBElement::getValue)
            .map(UtsettelseAvForeldrepengerListe::getUtsettelseAvForeldrepenger)
            .orElse(Collections.emptyList());
    }

    /**
     * Innsendingstidspunkt er ikke oppgitt fra Altinn
     */
    public Optional<LocalDateTime> getInnsendingstidspunkt() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getAvsendersystem().getInnsendingstidspunkt())
            .map(JAXBElement::getValue)
            .map(e -> e);
    }

    public String getAvsendersystem() {
        return getSkjema().getSkjemainnhold().getAvsendersystem().getSystemnavn();
    }

}
