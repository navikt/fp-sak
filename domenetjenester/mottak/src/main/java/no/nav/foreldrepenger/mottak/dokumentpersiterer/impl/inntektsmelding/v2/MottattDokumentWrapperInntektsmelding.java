package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
import no.seres.xsd.nav.inntektsmelding_m._20181211.AvtaltFerieListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GraderingIForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GraderingIForeldrepengerListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.UtsettelseAvForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.UtsettelseAvForeldrepengerListe;

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

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getArbeidsgiver()).map(JAXBElement::getValue);
    }

    public Optional<ArbeidsgiverPrivat> getArbeidsgiverPrivat() {
        return Optional.ofNullable(getSkjema().getSkjemainnhold().getArbeidsgiverPrivat()).map(JAXBElement::getValue);
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

    public Optional<String> getVirksomhetsNr() {
        return getArbeidsgiver().map(Arbeidsgiver::getVirksomhetsnummer);
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
     * Hvis inntektsmeldingen kommer fra Altinn (innsendingstidspunkt ikke oppgitt), bruker vi
     * tilnæringen "LocalDateTime.now()", selv om riktig innsendingstidspunkt er arkiveringstidspunkt i joark.
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
