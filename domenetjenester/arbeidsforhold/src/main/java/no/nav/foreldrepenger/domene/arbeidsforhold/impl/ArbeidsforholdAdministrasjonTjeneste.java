package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde.INNTEKTSKOMPONENTEN;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

/**
 * Håndterer administrasjon(saksbehandlers input) vedrørende arbeidsforhold.
 */
@ApplicationScoped
public class ArbeidsforholdAdministrasjonTjeneste {

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdAdministrasjonTjeneste.class);

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste,
            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    /**
     * Oppretter en builder for å lagre ned overstyringen av arbeidsforhold
     *
     * @param behandlingId behandlingen sin ID
     * @return buildern
     */
    public ArbeidsforholdInformasjonBuilder opprettBuilderFor(Long behandlingId) {
        return ArbeidsforholdInformasjonBuilder.oppdatere(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId));
    }

    /**
     * Lagrer overstyringer i ArbeidsforholdInformasjon
     *
     * @param behandlingId behandlingId
     * @param builder      ArbeidsforholdsOverstyringene som skal lagrers
     */
    public void lagreOverstyring(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder) {
        inntektArbeidYtelseTjeneste.lagreOverstyrtArbeidsforhold(behandlingId, aktørId, builder);
    }

    /**
     * Avsjekk arbeidsforhold mot inntektsmeldinger.
     */
    public Set<ArbeidsforholdWrapper> hentArbeidsforholdFerdigUtledet(BehandlingReferanse ref,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            SakInntektsmeldinger sakInntektsmeldinger,
            UtledArbeidsforholdParametere param) {
        var ytelseType = ref.fagsakYtelseType();
        var aktørId = ref.aktørId();
        var skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();

        var arbeidsgiverSetMap = vurderArbeidsforholdTjeneste.endringerIInntektsmelding(ref, iayGrunnlag,
                sakInntektsmeldinger, ytelseType);

        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunkt, iayGrunnlag, true);

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        var filterFør = filter.før(skjæringstidspunkt);
        var filterEtter = filter.etter(skjæringstidspunkt);

        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();

        var inntektsmeldingerForGrunnlag = iayGrunnlag.getInntektsmeldinger()
                .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
                .orElse(emptyList());

        var alleYrkesaktiviteter = filter.getAlleYrkesaktiviteter();

        Set<ArbeidsforholdWrapper> arbeidsforhold = new LinkedHashSet<>(utledArbeidsforholdFraInntektsmeldinger(
                filter,
                inntektsmeldinger, alleYrkesaktiviteter, overstyringer, arbeidsgiverSetMap, skjæringstidspunkt,
                iayGrunnlag.getArbeidsforholdInformasjon()));

        arbeidsforhold.addAll(utledArbeidsforholdFraYrkesaktivitet(
                filterFør, overstyringer, inntektsmeldinger, skjæringstidspunkt));

        arbeidsforhold.addAll(utledArbeidsforholdFraYrkesaktivitet(
                filterEtter, overstyringer, inntektsmeldinger, skjæringstidspunkt));

        arbeidsforhold.addAll(utledArbeidsforholdFraArbeidsforholdInformasjon(filter,
                overstyringer, alleYrkesaktiviteter, inntektsmeldingerForGrunnlag, skjæringstidspunkt));

        sjekkHarAksjonspunktForVurderArbeidsforhold(ref, arbeidsforhold, iayGrunnlag, sakInntektsmeldinger, param.isVurderArbeidsforhold());

        return arbeidsforhold;
    }

    private void sjekkHarAksjonspunktForVurderArbeidsforhold(BehandlingReferanse ref, Set<ArbeidsforholdWrapper> arbeidsforhold,
            InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger,
            boolean vurderArbeidsforhold) {
        if (vurderArbeidsforhold && !arbeidsforhold.isEmpty()) {
            final var vurder = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, iayGrunnlag,
                    sakInntektsmeldinger,
                    true);
            for (var arbeidsforholdWrapper : arbeidsforhold) {
                for (var arbeidsgiverSetEntry : vurder.entrySet()) {
                    if (erAksjonspunktPå(arbeidsforholdWrapper, arbeidsgiverSetEntry)) {
                        arbeidsforholdWrapper.setHarAksjonspunkt(true);
                        arbeidsforholdWrapper.setBrukArbeidsforholdet(null);
                        arbeidsforholdWrapper.setFortsettBehandlingUtenInntektsmelding(null);
                        arbeidsforholdWrapper.setKanOppretteNyttArbforFraIM(harInntektsmeldingUtenArbeid(arbeidsgiverSetEntry));
                    }
                }
            }
        }
    }

    private boolean harInntektsmeldingUtenArbeid(Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetEntry) {
        return arbeidsgiverSetEntry.getValue().stream()
                .flatMap(arbeidsforholdMedÅrsak -> arbeidsforholdMedÅrsak.getÅrsaker().stream())
                .anyMatch(årsak -> årsak.equals(AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));
    }

    public void fjernOverstyringerGjortAvSaksbehandler(Long behandlingId, AktørId aktørId) {
        var builder = opprettBuilderFor(behandlingId);
        builder.fjernAlleOverstyringer();
        inntektArbeidYtelseTjeneste.lagreOverstyrtArbeidsforhold(behandlingId, aktørId, builder);
    }

    public void fjernOverstyringerGjortAvSaksbehandlerOpptjening(Long behandlingId) {
        inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandlingId);
    }

    private boolean erAksjonspunktPå(ArbeidsforholdWrapper arbeidsforholdWrapper, Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> entry) {
        if (arbeidsforholdWrapper.getKilde() == INNTEKTSKOMPONENTEN) {
            return entry.getKey().getIdentifikator().equals(arbeidsforholdWrapper.getArbeidsgiverReferanse());
        }

        var arbeidsforholdRef = InternArbeidsforholdRef.ref(arbeidsforholdWrapper.getArbeidsforholdId());
        return entry.getKey().getIdentifikator().equals(arbeidsforholdWrapper.getArbeidsgiverReferanse())
                && entry.getValue().stream().map(ArbeidsforholdMedÅrsak::getRef).anyMatch(arbeidsforholdRef::gjelderFor);
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraInntektsmeldinger(YrkesaktivitetFilter filter, List<Inntektsmelding> inntektsmeldinger,
            Collection<Yrkesaktivitet> alleYrkesaktiviteter,
            List<ArbeidsforholdOverstyring> overstyringer,
            Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap,
            LocalDate skjæringstidspunkt,
            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return inntektsmeldinger.stream()
                .map(i -> mapInntektsmeldingTilWrapper(filter, alleYrkesaktiviteter, overstyringer, arbeidsgiverSetMap, skjæringstidspunkt, i,
                        arbeidsforholdInformasjon))
                .collect(Collectors.toList());
    }

    private ArbeidsforholdWrapper mapInntektsmeldingTilWrapper(YrkesaktivitetFilter filter,
            Collection<Yrkesaktivitet> alleYrkesaktiviteter,
            List<ArbeidsforholdOverstyring> overstyringer,
            Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap,
            LocalDate skjæringstidspunkt,
            Inntektsmelding inntektsmelding,
            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var wrapper = new ArbeidsforholdWrapper();
        mapArbeidsgiver(wrapper, inntektsmelding.getArbeidsgiver());
        wrapper.setMottattDatoInntektsmelding(inntektsmelding.getMottattDato());

        var arbeidsforholdRef = inntektsmelding.getArbeidsforholdRef();
        if (arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        }
        var yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, inntektsmelding.getArbeidsgiver(), arbeidsforholdRef);

        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        var overstyring = finnMatchendeOverstyring(inntektsmelding, overstyringer);

        if (overstyring.isPresent()) {
            var os = overstyring.get();
            wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(os.getBekreftetPermisjon()));
            wrapper.setBegrunnelse(os.getBegrunnelse());
            wrapper.setStillingsprosent(os.getStillingsprosent() != null ? os.getStillingsprosent().getVerdi()
                    : UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
            mapDatoForArbeidsforhold(wrapper, filter, yrkesaktiviteter, skjæringstidspunkt, os);
            wrapper.setLagtTilAvSaksbehandler(os.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
            wrapper.setBasertPåInntektsmelding(os.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING));
        } else {
            var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        }
        // setter disse
        wrapper.setBrukArbeidsforholdet(true);
        final var arbeidsgiver = inntektsmelding.getArbeidsgiver();
        final Boolean erNyttArbeidsforhold = erNyttArbeidsforhold(overstyringer, arbeidsgiver, inntektsmelding.getArbeidsforholdRef());
        wrapper.setErNyttArbeidsforhold(erNyttArbeidsforhold);
        wrapper.setFortsettBehandlingUtenInntektsmelding(false);
        wrapper.setIkkeRegistrertIAaRegister(yrkesaktiviteter.isEmpty());
        wrapper.setVurderOmSkalErstattes(
                erNyttOgIkkeTattStillingTil(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef(), arbeidsgiverSetMap));
        wrapper.setHarErsattetEttEllerFlere(!inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());
        wrapper.setErstatterArbeidsforhold(
                harErstattetEtEllerFlereArbeidsforhold(arbeidsgiver, inntektsmelding.getArbeidsforholdRef(), overstyringer));
        wrapper.setErEndret(sjekkOmFinnesIOverstyr(overstyringer, inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()));
        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setKilde(yrkesaktiviteter.stream().anyMatch(ya -> !filter.getAnsettelsesPerioder(ya).isEmpty()) ? ArbeidsforholdKilde.AAREGISTERET
                : ArbeidsforholdKilde.INNTEKTSMELDING);
        var kilde = yrkesaktiviteter.stream().anyMatch(ya -> !filter.getAnsettelsesPerioder(ya).isEmpty())
                ? ArbeidsforholdKilde.AAREGISTERET
                : ArbeidsforholdKilde.INNTEKTSMELDING;
        wrapper.setKilde(kilde);
        wrapper.setKanOppretteNyttArbforFraIM(kilde.equals(ArbeidsforholdKilde.INNTEKTSMELDING));
        if (arbeidsforholdInformasjon.isPresent()) {
            var eksternArbeidsforholdRef = arbeidsforholdInformasjon.get().finnEkstern(inntektsmelding.getArbeidsgiver(), arbeidsforholdRef);
            wrapper.setEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
        }
        return wrapper;
    }

    private void mapDatoForArbeidsforhold(ArbeidsforholdWrapper wrapper, YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter,
            LocalDate skjæringstidspunkt, ArbeidsforholdOverstyring overstyring) {
        var overstyrtAnsettelsesperiode = overstyring.getArbeidsforholdOverstyrtePerioder().stream().findFirst()
                .map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode);
        if (overstyrtAnsettelsesperiode.isPresent()) {
            wrapper.setFomDato(overstyrtAnsettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(overstyrtAnsettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
        } else {
            var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
        }
    }

    private boolean sjekkOmFinnesIOverstyr(List<ArbeidsforholdOverstyring> overstyringer, Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdRef) {
        return overstyringer.stream()
                .anyMatch(overstyring -> overstyring.getArbeidsgiver().equals(arbeidsgiver)
                        && Objects.equals(overstyring.getArbeidsforholdRef(), arbeidsforholdRef)
                        && overstyring.erOverstyrt());
    }

    private boolean erNyttOgIkkeTattStillingTil(Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdRef,
            Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap) {
        return arbeidsgiverSetMap.getOrDefault(arbeidsgiver, Collections.emptySet()).stream()
                .map(ArbeidsforholdMedÅrsak::getRef)
                .anyMatch(arbeidsforholdRef::equals);
    }

    private boolean erNyttArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer, Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdRef) {
        return overstyringer.stream().anyMatch(ov -> ov.getHandling().equals(NYTT_ARBEIDSFORHOLD) && ov.getArbeidsgiver().equals(arbeidsgiver)
                && ov.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef));
    }

    private List<Yrkesaktivitet> finnYrkesAktiviteter(Collection<Yrkesaktivitet> yrkesaktiviteter, Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdRef) {
        return yrkesaktiviteter.stream()
                .filter(yr -> yr.gjelderFor(arbeidsgiver, arbeidsforholdRef))
                .collect(Collectors.toList());
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraArbeidsforholdInformasjon(YrkesaktivitetFilter filter,
            List<ArbeidsforholdOverstyring> overstyringer,
            Collection<Yrkesaktivitet> alleYrkesaktiviteter,
            List<Inntektsmelding> alleInntektsmeldinger,
            LocalDate skjæringstidspunkt) {
        return overstyringer.stream()
                .filter(ArbeidsforholdOverstyring::erOverstyrt)
                .filter(a -> !Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, a.getHandling()))
                .map(a -> mapOverstyringTilWrapper(filter, a, alleYrkesaktiviteter, alleInntektsmeldinger, skjæringstidspunkt))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private Optional<ArbeidsforholdWrapper> mapOverstyringTilWrapper(YrkesaktivitetFilter filter,
                                                      ArbeidsforholdOverstyring overstyring,
                                                      Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                      List<Inntektsmelding> alleInntektsmeldinger,
                                                      LocalDate skjæringstidspunkt) {
        final var arbeidsgiver = overstyring.getArbeidsgiver();
        final var arbeidsforholdRef = overstyring.getArbeidsforholdRef();
        final var yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, arbeidsgiver, arbeidsforholdRef);
        final var mottattDatoInntektsmelding = mottattInntektsmelding(overstyring, alleInntektsmeldinger);
        var wrapper = new ArbeidsforholdWrapper();
        if (!yrkesaktiviteter.isEmpty()) {
            var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setKilde(utledKilde(ansettelsesperiode, mottattDatoInntektsmelding, overstyring.getHandling()));
            if (!overstyring.getArbeidsforholdOverstyrtePerioder().isEmpty()) {
                var overstyrtAnsettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter,
                        skjæringstidspunkt, true);
                wrapper.setOverstyrtTom(overstyrtAnsettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            }
            wrapper.setIkkeRegistrertIAaRegister(false);
            wrapper.setHandlingType(overstyring.getHandling());
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        } else {
            wrapper.setKilde(utledKilde(Optional.empty(), mottattDatoInntektsmelding, overstyring.getHandling()));
            wrapper.setIkkeRegistrertIAaRegister(true);
            var arbeidsforholdOverstyrtePerioder = overstyring.getArbeidsforholdOverstyrtePerioder();
            if (arbeidsforholdOverstyrtePerioder.size() > 1) {
                throw new IllegalStateException("Forventer kun ett innslag i listen");
            }
            if (arbeidsforholdOverstyrtePerioder.isEmpty()) {
               LOG.info("Finner ingen match mot overstyrte perioder for dette arbeidsforholdet:"+ arbeidsgiver.getIdentifikator() + "med denne refen:"+ arbeidsforholdRef.getReferanse());
               return Optional.empty();
            }
            wrapper.setFomDato(arbeidsforholdOverstyrtePerioder.get(0).getOverstyrtePeriode().getFomDato());
            wrapper.setTomDato(arbeidsforholdOverstyrtePerioder.get(0).getOverstyrtePeriode().getTomDato());
            wrapper.setStillingsprosent(overstyring.getStillingsprosent() == null
                    ? UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt)
                    : overstyring.getStillingsprosent().getVerdi());
        }
        mapArbeidsgiverForOverstyring(wrapper, arbeidsgiver);
        mapArbeidsforholdHandling(wrapper, overstyring);
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        wrapper.setBegrunnelse(overstyring.getBegrunnelse());
        wrapper.setMottattDatoInntektsmelding(mottattDatoInntektsmelding);
        wrapper.setErEndret(true);

        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setBrukMedJustertPeriode(Objects.equals(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE, overstyring.getHandling()));
        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(overstyring.getBekreftetPermisjon()));
        return Optional.of(wrapper);
    }

    private void mapArbeidsforholdHandling(ArbeidsforholdWrapper wrapper, ArbeidsforholdOverstyring overstyring) {
        if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.IKKE_BRUK)) {
            wrapper.setBrukArbeidsforholdet(false);
            wrapper.setFortsettBehandlingUtenInntektsmelding(false);
        } else if (overstyring.getHandling().equals(SLÅTT_SAMMEN_MED_ANNET)) {
            wrapper.setErSlettet(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)) {
            wrapper.setLagtTilAvSaksbehandler(true);
            wrapper.setBrukArbeidsforholdet(true);
            wrapper.setFortsettBehandlingUtenInntektsmelding(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING)) {
            wrapper.setBasertPåInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
            wrapper.setKanOppretteNyttArbforFraIM(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG)) {
            wrapper.setInntektMedTilBeregningsgrunnlag(true);
            wrapper.setBasertPåInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
        } else {
            wrapper.setFortsettBehandlingUtenInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
        }
    }

    private ArbeidsforholdKilde utledKilde(Optional<DatoIntervallEntitet> avtale, LocalDate mottattDatoInntektsmelding,
            ArbeidsforholdHandlingType handling) {
        if (Objects.equals(handling, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)) {
            return ArbeidsforholdKilde.SAKSBEHANDLER;
        }
        if (avtale.isPresent()) {
            return ArbeidsforholdKilde.AAREGISTERET;
        }
        if ((mottattDatoInntektsmelding != null) || handling.equals(SLÅTT_SAMMEN_MED_ANNET)) {
            return ArbeidsforholdKilde.INNTEKTSMELDING;
        }
        return INNTEKTSKOMPONENTEN;
    }

    private LocalDate mottattInntektsmelding(ArbeidsforholdOverstyring overstyringEntitet, List<Inntektsmelding> alleInntektsmeldinger) {
        final var mottattDato = alleInntektsmeldinger
                .stream()
                .filter(im -> overstyringEntitet.getArbeidsgiver().equals(im.getArbeidsgiver())
                        && overstyringEntitet.getArbeidsforholdRef().gjelderFor(im.getArbeidsforholdRef()))
                .findFirst()
                .map(Inntektsmelding::getMottattDato);

        return mottattDato.orElse(null);
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraYrkesaktivitet(YrkesaktivitetFilter filter,
            List<ArbeidsforholdOverstyring> overstyringer,
            List<Inntektsmelding> inntektsmeldinger,
            LocalDate skjæringstidspunkt) {
        var stp = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt);
        return filter.getYrkesaktiviteter().stream()
                .filter(yr -> AA_REGISTER_TYPER.contains(yr.getArbeidType()))
                .filter(yr -> harIkkeFåttInntektsmelding(yr, inntektsmeldinger))
                .filter(yr -> filter.getAnsettelsesPerioder(yr).stream().map(AktivitetsAvtale::getPeriode)
                        .anyMatch(periode -> periode.overlapper(stp)) ||
                        filter.getAnsettelsesPerioder(yr).stream().map(AktivitetsAvtale::getPeriode)
                                .anyMatch(periode -> periode.getFomDato().isAfter(skjæringstidspunkt)))
                .filter(yr -> filtreVekkLagtTilAvSaksbehandler(yr, overstyringer))
                .map(yr -> mapYrkesaktivitetAAREG(filter, yr, overstyringer, skjæringstidspunkt))
                .collect(Collectors.toList());
    }

    private boolean filtreVekkLagtTilAvSaksbehandler(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream().noneMatch(o -> yrkesaktivitet.gjelderFor(o.getArbeidsgiver(), o.getArbeidsforholdRef())
                && o.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private ArbeidsforholdWrapper mapYrkesaktivitetAAREG(YrkesaktivitetFilter filter,
            Yrkesaktivitet yrkesaktivitet,
            List<ArbeidsforholdOverstyring> overstyringer,
            LocalDate skjæringstidspunkt) {
        final var arbeidsforholdOverstyringEntitet = finnMatchendeOverstyring(yrkesaktivitet, overstyringer);
        final var arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        final var arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef();
        final var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktivitet, skjæringstidspunkt, false);
        var wrapper = new ArbeidsforholdWrapper();
        wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktivitet, skjæringstidspunkt));
        wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
        wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        wrapper.setKilde(ArbeidsforholdKilde.AAREGISTERET);
        wrapper.setIkkeRegistrertIAaRegister(false);
        wrapper.setBrukArbeidsforholdet(true);
        wrapper.setFortsettBehandlingUtenInntektsmelding(harTattStillingTil(yrkesaktivitet, overstyringer));
        wrapper.setErEndret(sjekkOmFinnesIOverstyr(overstyringer, arbeidsgiver, yrkesaktivitet.getArbeidsforholdRef()));
        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, List.of(yrkesaktivitet), skjæringstidspunkt));
        mapArbeidsgiver(wrapper, arbeidsgiver);
        arbeidsforholdOverstyringEntitet.ifPresent(ov -> {
            wrapper.setHandlingType(ov.getHandling());
            wrapper.setBegrunnelse(ov.getBegrunnelse());
            wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(ov.getBekreftetPermisjon()));
            wrapper
                    .setInntektMedTilBeregningsgrunnlag(
                            Objects.equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG, ov.getHandling()) ? Boolean.FALSE : null);
            wrapper.setBrukMedJustertPeriode(Objects.equals(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE, ov.getHandling()));
            if (!ov.getArbeidsforholdOverstyrtePerioder().isEmpty()) {
                var overstyrtTom = UtledAnsettelsesperiode.utled(filter, yrkesaktivitet, skjæringstidspunkt, true)
                        .map(DatoIntervallEntitet::getTomDato).orElse(null);
                wrapper.setOverstyrtTom(overstyrtTom);
            }
        });
        return wrapper;
    }

    private void mapArbeidsgiver(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver) {
        wrapper.setArbeidsgiverReferanse(arbeidsgiver.getIdentifikator());
    }

    private void mapArbeidsgiverForOverstyring(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver) {
        wrapper.setArbeidsgiverReferanse(arbeidsgiver.getIdentifikator());
    }

    private boolean harTattStillingTil(Yrkesaktivitet yr, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
                .anyMatch(ov -> ov.kreverIkkeInntektsmelding()
                        && yr.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef()));
    }

    private String harErstattetEtEllerFlereArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref,
            List<ArbeidsforholdOverstyring> overstyringer) {
        if ((ref == null) || !ref.gjelderForSpesifiktArbeidsforhold()) {
            return null;
        }
        return overstyringer.stream().filter(ov -> Objects.equals(ov.getHandling(), ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                && Objects.equals(ov.getArbeidsgiver(), arbeidsgiver) && Objects.equals(ov.getNyArbeidsforholdRef(), ref))
                .findFirst()
                .map(ov -> ov.getArbeidsgiver().getIdentifikator() + "-" + ov.getArbeidsforholdRef().getReferanse())
                .orElse(null);
    }

    private boolean harIkkeFåttInntektsmelding(Yrkesaktivitet yr, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream().noneMatch(i -> yr.gjelderFor(i.getArbeidsgiver(), i.getArbeidsforholdRef()));
    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Yrkesaktivitet ya, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
                .filter(os -> ya.gjelderFor(os.getArbeidsgiver(), os.getArbeidsforholdRef()))
                .filter(ArbeidsforholdOverstyring::erOverstyrt)
                .findFirst();
    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Inntektsmelding inntektsmelding,
            List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
                .filter(os -> Objects.equals(inntektsmelding.getArbeidsgiver(), os.getArbeidsgiver()) &&
                        inntektsmelding.getArbeidsforholdRef().gjelderFor(os.getArbeidsforholdRef()))
                .filter(ArbeidsforholdOverstyring::erOverstyrt)
                .findFirst();
    }

    /**
     * Param klasse for å kunne ta inn parametere som styrer utleding av
     * arbeidsforhold.
     */
    public static record UtledArbeidsforholdParametere(boolean isVurderArbeidsforhold) {
    }

}
