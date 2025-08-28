package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static java.util.Collections.emptyList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.AktørId;

@Dependent
class StartpunktUtlederInntektsmelding {

    private static final Set<ArbeidsforholdHandlingType> HANDLING_SOM_IKKE_VENTER_IM = Set.of(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE,
        ArbeidsforholdHandlingType.IKKE_BRUK, ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING, ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private String klassenavn = this.getClass().getSimpleName();

    StartpunktUtlederInntektsmelding() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektsmelding(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                     ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    public StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        var fersktGrunnlag =  inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId());
        var eldsteGrunnlag = finnIayGrunnlagForOrigBehandling(fersktGrunnlag, grunnlag1, grunnlag2);
        var gamleIm = hentInntektsmeldingerFraGittGrunnlag(eldsteGrunnlag);
        var nyim = hentInntektsmeldingerFraGittGrunnlag(fersktGrunnlag);
        var deltaIM = nyim.stream()
            .filter(im -> !gamleIm.contains(im))
            .toList();

        var saksbehandlersArbeidsforholdvalg = arbeidsforholdInntektsmeldingMangelTjeneste.hentArbeidsforholdValgForSak(ref);
        if (ref.behandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            return finnStartpunktFørstegang(ref, fersktGrunnlag, deltaIM, gamleIm, saksbehandlersArbeidsforholdvalg);
        }

        return deltaIM.stream()
            .map(nyIm -> finnStartpunktForNyIm(ref, fersktGrunnlag, nyIm, gamleIm, saksbehandlersArbeidsforholdvalg))
            .min(Comparator.comparingInt(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<Inntektsmelding> hentInntektsmeldingerFraGittGrunnlag(Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        return grunnlag.flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());
    }

    private StartpunktType finnStartpunktFørstegang(BehandlingReferanse ref,
                                                    Optional<InntektArbeidYtelseGrunnlag> grunnlag,
                                                    List<Inntektsmelding> nyeIm,
                                                    List<Inntektsmelding> gamleIm,
                                                    List<ArbeidsforholdValg> saksbehandlersArbeidsforholdvalg) {
        var erImForOverstyrtUtenIM =  nyeIm.stream()
            .anyMatch(i -> erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(grunnlag, i, saksbehandlersArbeidsforholdvalg));
        if (erImForOverstyrtUtenIM) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "overstyring", ref.behandlingId(), "en av arbeidsgivere");
            return StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        }

        if ( FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType()) && (nyeIm.stream().anyMatch(nyIm -> nyttArbForholdForEksisterendeArbgiver(nyIm, gamleIm))
            || nyeIm.stream().anyMatch(nyIm -> endringIArbeidsforholdIdForSammeArbgiver(nyIm, gamleIm)))) {
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
        }

        return StartpunktType.BEREGNING;
    }

    private StartpunktType finnStartpunktForNyIm(BehandlingReferanse ref,
                                                 Optional<InntektArbeidYtelseGrunnlag> grunnlag,
                                                 Inntektsmelding nyIm,
                                                 List<Inntektsmelding> gamleIm,
                                                 List<ArbeidsforholdValg> saksbehandlersArbeidsforholdvalg) {
        if (erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(grunnlag, nyIm, saksbehandlersArbeidsforholdvalg)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "overstyring", ref.behandlingId(), nyIm.getKanalreferanse());
            return StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType()) && (nyttArbForholdForEksisterendeArbgiver(nyIm, gamleIm) || endringIArbeidsforholdIdForSammeArbgiver(nyIm, gamleIm))){
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
        }
        if (erStartpunktForNyImBeregning(grunnlag, nyIm, gamleIm, ref)) {
            return StartpunktType.BEREGNING;
        }
        if (erStartpunktForNyImBeregningRefusjon(nyIm, gamleIm, ref)) {
            return StartpunktType.BEREGNING_REFUSJON;
        }
        return StartpunktType.UDEFINERT;
    }


    private boolean erInntektsmeldingArbeidsforholdOverstyrtIkkeVenterIM(Optional<InntektArbeidYtelseGrunnlag> grunnlag,
                                                                         Inntektsmelding nyIm,
                                                                         List<ArbeidsforholdValg> saksbehandlersArbeidsforholdvalg) {
        var agIM = nyIm.getArbeidsgiver();
        var erIkkeVentetFraIAY = grunnlag.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer)
            .orElse(emptyList())
            .stream()
            .filter(o -> Objects.equals(o.getArbeidsgiver(), agIM))
            .map(ArbeidsforholdOverstyring::getHandling)
            .anyMatch(HANDLING_SOM_IKKE_VENTER_IM::contains);
        var erIkkeVentetFraSaksbehandler = saksbehandlersArbeidsforholdvalg.stream()
            .filter(valg -> valg.getArbeidsgiver().equals(agIM))
            .anyMatch(valg -> valg.getVurdering().equals(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
                || valg.getVurdering().equals(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING));
        return erIkkeVentetFraIAY || erIkkeVentetFraSaksbehandler;
    }

    private boolean erStartpunktForNyImBeregning(Optional<InntektArbeidYtelseGrunnlag> grunnlag, Inntektsmelding nyIm, List<Inntektsmelding> gamleIm, BehandlingReferanse ref) {
        var origIM = sisteInntektsmeldingForArbeidsforhold(nyIm, gamleIm).orElse(null);
        if (origIM == null) { // Finnes ikke tidligere IM fra denne AG
            // TODO Hvis det er en IM for arbeidsforhold som er tilkommet kan vi starte i refusjon
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "første", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }

        // Endring til/fra angitt arbeidsforholdId og ingen slik id
        if ((origIM.gjelderForEtSpesifiktArbeidsforhold() && !nyIm.gjelderForEtSpesifiktArbeidsforhold()) ||
            (!origIM.gjelderForEtSpesifiktArbeidsforhold() && nyIm.gjelderForEtSpesifiktArbeidsforhold()) &&
                harFlereArbeidsforholdSammeArbeidsgiver(grunnlag, nyIm, ref.aktørId())) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "im-arbeidsforhold", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }

        if (nyIm.getInntektBeløp().compareTo(origIM.getInntektBeløp()) != 0) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "beløp", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }
        if (erEndringPåNaturalYtelser(nyIm, origIM)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "natural", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }
        if (erEndringPåRefusjon(nyIm, origIM)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "refusjon", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }
        return false;
    }

    private boolean erStartpunktForNyImBeregningRefusjon(Inntektsmelding nyIm,
                                                         List<Inntektsmelding> gamleIm,
                                                         BehandlingReferanse ref) {
        var origIM = sisteInntektsmeldingForArbeidsforhold(nyIm, gamleIm).orElseThrow();
        if (erEndringPåRefusjon(nyIm, origIM)) {
            FellesStartpunktUtlederLogger.skrivLoggStartpunktIM(klassenavn, "refusjon", ref.behandlingId(), nyIm.getKanalreferanse());
            return true;
        }
        return false;
    }

    private Optional<InntektArbeidYtelseGrunnlag> finnIayGrunnlagForOrigBehandling(Optional<InntektArbeidYtelseGrunnlag> grunnlagForBehandling, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        var gjeldendeGrunnlag = grunnlagForBehandling.orElse(null);
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

    private Optional<Inntektsmelding> sisteInntektsmeldingForArbeidsforhold(Inntektsmelding ny, List<Inntektsmelding> gamleIm) {
        return gamleIm.stream()
            .filter(ny::gjelderSammeArbeidsforhold)
            .max(COMP_REKKEFØLGE);
    }

    private boolean harFlereArbeidsforholdSammeArbeidsgiver(Optional<InntektArbeidYtelseGrunnlag> grunnlag, Inntektsmelding ny, AktørId aktørId) {
        return grunnlag.flatMap(g -> g.getAktørArbeidFraRegister(aktørId))
            .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of()).stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .filter(ya -> Objects.equals(ya.getArbeidsgiver(), ny.getArbeidsgiver()))
            .map(Yrkesaktivitet::getArbeidsforholdRef)
            .distinct()
            .count() > 1;
    }

    private boolean nyttArbForholdForEksisterendeArbgiver(Inntektsmelding ny, List<Inntektsmelding> origIM) {
        var arbeidsgiverFinnesFraFør = origIM.stream().anyMatch(im -> Objects.equals(im.getArbeidsgiver(), ny.getArbeidsgiver()));
        return arbeidsgiverFinnesFraFør && origIM.stream()
            .noneMatch(ny::gjelderSammeArbeidsforhold);
    }

    private boolean endringIArbeidsforholdIdForSammeArbgiver(Inntektsmelding ny, List<Inntektsmelding> origIM) {
        var arbeidsgiverFinnesFraFør = origIM.stream().anyMatch(im -> Objects.equals(im.getArbeidsgiver(), ny.getArbeidsgiver()));
        return arbeidsgiverFinnesFraFør && origIM.stream()
            .anyMatch(ny::endringIArbeidsforholdsIdForSammeArbGiver);
    }

    private boolean erEndringPåNaturalYtelser(Inntektsmelding nyInntektsmelding, Inntektsmelding opprinneligInntektsmelding) {
        Set<NaturalYtelse> nyeNaturalYtelser = new HashSet<>(nyInntektsmelding.getNaturalYtelser());
        Set<NaturalYtelse> opprNaturalYtelser = new HashSet<>(opprinneligInntektsmelding.getNaturalYtelser());
        return !nyeNaturalYtelser.equals(opprNaturalYtelser);
    }

    private boolean erEndringPåRefusjon(Inntektsmelding nyInntektsmelding, Inntektsmelding opprinneligInntektsmelding) {
        var erEndringPåBeløp = !Objects.equals(nyInntektsmelding.getRefusjonBeløpPerMnd(), opprinneligInntektsmelding.getRefusjonBeløpPerMnd())
            || !Objects.equals(nyInntektsmelding.getRefusjonOpphører(), opprinneligInntektsmelding.getRefusjonOpphører());

        var erEndringerPåEndringerRefusjon = erEndringerPåEndringerRefusjon(nyInntektsmelding.getEndringerRefusjon(), opprinneligInntektsmelding.getEndringerRefusjon());
        return erEndringPåBeløp || erEndringerPåEndringerRefusjon;
    }

    private boolean erEndringerPåEndringerRefusjon(List<Refusjon> nyInntektsmeldingEndringerRefusjon,
                                                   List<Refusjon> opprinneligInntektsmeldingEndringerRefusjon) {
        Set<Refusjon> nyttSett = new HashSet<>(nyInntektsmeldingEndringerRefusjon);
        Set<Refusjon> opprinneligSett = new HashSet<>(opprinneligInntektsmeldingEndringerRefusjon);

        return !nyttSett.equals(opprinneligSett);
    }

    // Lånt fra abakus - just in case
    private static final Comparator<? super Inntektsmelding> COMP_REKKEFØLGE = (Inntektsmelding a, Inntektsmelding b) -> {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
        if (a.getKanalreferanse() != null && b.getKanalreferanse() != null) {
            return a.getKanalreferanse().compareTo(b.getKanalreferanse());
        } else {
            // crazy fallback for manglende kanalreferanser
            return a.getInnsendingstidspunkt().compareTo(b.getInnsendingstidspunkt());
        }
    };

}


