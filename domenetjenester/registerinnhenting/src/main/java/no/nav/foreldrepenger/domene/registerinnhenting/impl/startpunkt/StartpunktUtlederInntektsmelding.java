package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static java.util.Collections.emptyList;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.vedtak.konfig.Tid;

@Dependent
class StartpunktUtlederInntektsmelding {

    private static final Set<ArbeidsforholdHandlingType> HANDLING_SOM_IKKE_VENTER_IM = Set.of(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE,
        ArbeidsforholdHandlingType.IKKE_BRUK, ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING, ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private String klassenavn = this.getClass().getSimpleName();

    StartpunktUtlederInntektsmelding() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektsmelding(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        Optional<InntektArbeidYtelseGrunnlag> fersktGrunnlag =  inntektArbeidYtelseTjeneste.finnGrunnlag(ref.getBehandlingId());
        Optional<InntektArbeidYtelseGrunnlag> eldsteGrunnlag = finnIayGrunnlagForOrigBehandling(fersktGrunnlag, grunnlag1, grunnlag2);
        List<Inntektsmelding> gamle = hentInntektsmeldingerFraGittGrunnlag(eldsteGrunnlag);
        List<Inntektsmelding> nyim = hentInntektsmeldingerFraGittGrunnlag(fersktGrunnlag);
        List<Inntektsmelding> deltaIM = nyim.stream()
            .filter(im -> !gamle.contains(im))
            .collect(Collectors.toList());

        if (ref.getBehandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            return finnStartpunktFørstegang(ref, fersktGrunnlag, deltaIM);
        }

        List<Inntektsmelding> origIm = gamle.stream()
            .sorted(Comparator.comparing(Inntektsmelding::getInnsendingstidspunkt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        return deltaIM.stream()
            .map(nyIm -> finnStartpunktForNyIm(ref, fersktGrunnlag, nyIm, origIm))
            .min(Comparator.comparingInt(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<Inntektsmelding> hentInntektsmeldingerFraGittGrunnlag(Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        return grunnlag.flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());
    }

    private StartpunktType finnStartpunktFørstegang(BehandlingReferanse ref, Optional<InntektArbeidYtelseGrunnlag> grunnlag, List<Inntektsmelding> nyeIm) {
        var erImForOverstyrtUtenIM =  nyeIm.stream()
            .anyMatch(i -> erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(grunnlag, i)) ;
        if (erImForOverstyrtUtenIM) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "overstyring", ref.getBehandlingId(), "en av arbeidsgivere");
            return StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        }
        return nyeIm.stream()
            .anyMatch(i -> erStartdatoUlikFørsteUttaksdato(ref, i)) ? StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP : StartpunktType.BEREGNING;
    }

    private StartpunktType finnStartpunktForNyIm(BehandlingReferanse ref, Optional<InntektArbeidYtelseGrunnlag> grunnlag, Inntektsmelding nyIm, List<Inntektsmelding> origIm) {
        if (erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(grunnlag, nyIm)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "overstyring", ref.getBehandlingId(), nyIm.getArbeidsgiver().getIdentifikator());
            return StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        }
        if (erStartpunktForNyImBeregning(nyIm, origIm, ref)) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.UTTAKSVILKÅR;
    }

    private boolean erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(Optional<InntektArbeidYtelseGrunnlag> grunnlag, Inntektsmelding nyIm) {
        var agIM = nyIm.getArbeidsgiver();
        return grunnlag.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElse(emptyList()).stream()
            .filter(o -> Objects.equals(o.getArbeidsgiver(), agIM))
            .map(ArbeidsforholdOverstyring::getHandling)
            .anyMatch(HANDLING_SOM_IKKE_VENTER_IM::contains);
    }

    private boolean erStartpunktForNyImBeregning(Inntektsmelding nyIm, List<Inntektsmelding> origIm, BehandlingReferanse ref) {
        Inntektsmelding origIM = sisteInntektsmeldingForArbeidsforhold(nyIm, origIm).orElse(null);
        if (origIM == null) { // Finnes ikke tidligere IM fra denne AG
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "første", ref.getBehandlingId(), nyIm.getArbeidsgiver().getIdentifikator());
            return true;
        }

        if (nyIm.getInntektBeløp().compareTo(origIM.getInntektBeløp()) != 0) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "beløp", ref.getBehandlingId(), nyIm.getArbeidsgiver().getIdentifikator());
            return true;
        }
        if (erEndringPåNaturalYtelser(nyIm, origIM)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "natural", ref.getBehandlingId(), nyIm.getArbeidsgiver().getIdentifikator());
            return true;
        }
        if (erEndringPåRefusjon(nyIm, origIM)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "refusjon", ref.getBehandlingId(), nyIm.getArbeidsgiver().getIdentifikator());
            return true;
        }
        return false;
    }


    private Optional<InntektArbeidYtelseGrunnlag> finnIayGrunnlagForOrigBehandling(Optional<InntektArbeidYtelseGrunnlag> grunnlagForBehandling, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        InntektArbeidYtelseGrunnlag gjeldendeGrunnlag = grunnlagForBehandling.orElse(null);
        if (gjeldendeGrunnlag == null) {
            return Optional.empty();
        }

        if (Objects.equals(gjeldendeGrunnlag, grunnlag1)) {
            return Optional.of(grunnlag2);
        }
        if (Objects.equals(gjeldendeGrunnlag, grunnlag2)) {
            return Optional.of(grunnlag1);
        }
        return Optional.empty();

    }

    private Optional<Inntektsmelding> sisteInntektsmeldingForArbeidsforhold(Inntektsmelding ny, List<Inntektsmelding> origIM) {
        return origIM.stream()
            .filter(ny::gjelderSammeArbeidsforhold)
            .findFirst();
    }

    private boolean erStartdatoUlikFørsteUttaksdato(BehandlingReferanse ref, Inntektsmelding nyIm) {
        // Samme logikk som 5045 AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden
        LocalDate førsteUttaksDato = endreDatoHvisLørdagEllerSøndag(ref.getFørsteUttaksdato());
        LocalDate startDatoIm = endreDatoHvisLørdagEllerSøndag(nyIm.getStartDatoPermisjon().orElse(Tid.TIDENES_BEGYNNELSE));
        return !førsteUttaksDato.equals(startDatoIm);
    }

    LocalDate endreDatoHvisLørdagEllerSøndag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return dato.plusDays(2L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
    }

    private boolean erEndringPåNaturalYtelser(Inntektsmelding nyInntektsmelding, Inntektsmelding opprinneligInntektsmelding) {
        Set<NaturalYtelse> nyeNaturalYtelser = new HashSet<>(nyInntektsmelding.getNaturalYtelser());
        Set<NaturalYtelse> opprNaturalYtelser = new HashSet<>(opprinneligInntektsmelding.getNaturalYtelser());
        return !nyeNaturalYtelser.equals(opprNaturalYtelser);
    }

    private boolean erEndringPåRefusjon(Inntektsmelding nyInntektsmelding, Inntektsmelding opprinneligInntektsmelding) {
        boolean erEndringPåBeløp = !Objects.equals(nyInntektsmelding.getRefusjonBeløpPerMnd(), opprinneligInntektsmelding.getRefusjonBeløpPerMnd())
            || !Objects.equals(nyInntektsmelding.getRefusjonOpphører(), opprinneligInntektsmelding.getRefusjonOpphører());

        boolean erEndringerPåEndringerRefusjon = erEndringerPåEndringerRefusjon(nyInntektsmelding.getEndringerRefusjon(), opprinneligInntektsmelding.getEndringerRefusjon());
        return erEndringPåBeløp || erEndringerPåEndringerRefusjon;
    }

    private boolean erEndringerPåEndringerRefusjon(List<Refusjon> nyInntektsmeldingEndringerRefusjon,
                                                   List<Refusjon> opprinneligInntektsmeldingEndringerRefusjon) {
        Set<Refusjon> nyttSett = new HashSet<>(nyInntektsmeldingEndringerRefusjon);
        Set<Refusjon> opprinneligSett = new HashSet<>(opprinneligInntektsmeldingEndringerRefusjon);

        return !nyttSett.equals(opprinneligSett);
    }

}


