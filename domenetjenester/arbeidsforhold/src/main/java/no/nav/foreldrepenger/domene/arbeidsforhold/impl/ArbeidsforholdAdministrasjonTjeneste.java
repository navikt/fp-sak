package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
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

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdAdministrasjonTjeneste.class);

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
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
    public Set<ArbeidsforholdWrapper> hentArbeidsforholdFerdigUtledet(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var aktørId = ref.aktørId();
        var skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();

        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunkt, iayGrunnlag, true);

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        var filterFør = filter.før(skjæringstidspunkt);
        var filterEtter = filter.etter(skjæringstidspunkt);

        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();

        var alleYrkesaktiviteter = filter.getAlleYrkesaktiviteter();

        Set<ArbeidsforholdWrapper> arbeidsforhold = new LinkedHashSet<>(utledArbeidsforholdFraInntektsmeldinger(
                filter,
                inntektsmeldinger, alleYrkesaktiviteter, overstyringer, skjæringstidspunkt,
                iayGrunnlag.getArbeidsforholdInformasjon()));

        arbeidsforhold.addAll(utledArbeidsforholdFraYrkesaktivitet(
                filterFør, overstyringer, inntektsmeldinger, skjæringstidspunkt));

        arbeidsforhold.addAll(utledArbeidsforholdFraYrkesaktivitet(
                filterEtter, overstyringer, inntektsmeldinger, skjæringstidspunkt));

        arbeidsforhold.addAll(utledArbeidsforholdFraArbeidsforholdInformasjon(filter,
                overstyringer, alleYrkesaktiviteter, skjæringstidspunkt));

        return arbeidsforhold;
    }


    public void fjernOverstyringerGjortAvSaksbehandler(Long behandlingId, AktørId aktørId) {
        var builder = opprettBuilderFor(behandlingId);
        builder.fjernAlleOverstyringer();
        inntektArbeidYtelseTjeneste.lagreOverstyrtArbeidsforhold(behandlingId, aktørId, builder);
    }

    public void fjernOverstyringerGjortAvSaksbehandlerOpptjening(Long behandlingId) {
        inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandlingId);
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraInntektsmeldinger(YrkesaktivitetFilter filter,
                                                                                List<Inntektsmelding> inntektsmeldinger,
                                                                                Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                                List<ArbeidsforholdOverstyring> overstyringer,
                                                                                LocalDate skjæringstidspunkt,
                                                                                Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return inntektsmeldinger.stream()
                .map(i -> mapInntektsmeldingTilWrapper(filter, alleYrkesaktiviteter, overstyringer, skjæringstidspunkt, i,
                        arbeidsforholdInformasjon))
                .collect(Collectors.toList());
    }

    private ArbeidsforholdWrapper mapInntektsmeldingTilWrapper(YrkesaktivitetFilter filter,
                                                               Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                               List<ArbeidsforholdOverstyring> overstyringer,
                                                               LocalDate skjæringstidspunkt,
                                                               Inntektsmelding inntektsmelding,
                                                               Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var wrapper = new ArbeidsforholdWrapper();
        mapArbeidsgiver(wrapper, inntektsmelding.getArbeidsgiver());

        var arbeidsforholdRef = inntektsmelding.getArbeidsforholdRef();
        if (arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        }
        var yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, inntektsmelding.getArbeidsgiver(), arbeidsforholdRef);

        var overstyring = finnMatchendeOverstyring(inntektsmelding, overstyringer);

        if (overstyring.isPresent()) {
            var os = overstyring.get();
            wrapper.setStillingsprosent(os.getStillingsprosent() != null ? os.getStillingsprosent().getVerdi()
                    : UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
            mapDatoForArbeidsforhold(wrapper, filter, yrkesaktiviteter, skjæringstidspunkt, os);
        } else {
            var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        }
        // setter disse
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

   private List<Yrkesaktivitet> finnYrkesAktiviteter(Collection<Yrkesaktivitet> yrkesaktiviteter, Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdRef) {
        return yrkesaktiviteter.stream()
                .filter(yr -> yr.gjelderFor(arbeidsgiver, arbeidsforholdRef))
                .collect(Collectors.toList());
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraArbeidsforholdInformasjon(YrkesaktivitetFilter filter,
                                                                                        List<ArbeidsforholdOverstyring> overstyringer,
                                                                                        Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                                        LocalDate skjæringstidspunkt) {
        return overstyringer.stream()
                .filter(ArbeidsforholdOverstyring::erOverstyrt)
                .filter(a -> !Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, a.getHandling()))
                .map(a -> mapOverstyringTilWrapper(filter, a, alleYrkesaktiviteter, skjæringstidspunkt))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private Optional<ArbeidsforholdWrapper> mapOverstyringTilWrapper(YrkesaktivitetFilter filter,
                                                                     ArbeidsforholdOverstyring overstyring,
                                                                     Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                     LocalDate skjæringstidspunkt) {
        final var arbeidsgiver = overstyring.getArbeidsgiver();
        final var arbeidsforholdRef = overstyring.getArbeidsforholdRef();
        final var yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, arbeidsgiver, arbeidsforholdRef);
        var wrapper = new ArbeidsforholdWrapper();
        if (!yrkesaktiviteter.isEmpty()) {
            var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        } else {
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
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        return Optional.of(wrapper);
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
                .map(yr -> mapYrkesaktivitetAAREG(filter, yr, skjæringstidspunkt))
                .collect(Collectors.toList());
    }

    private boolean filtreVekkLagtTilAvSaksbehandler(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream().noneMatch(o -> yrkesaktivitet.gjelderFor(o.getArbeidsgiver(), o.getArbeidsforholdRef())
                && o.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private ArbeidsforholdWrapper mapYrkesaktivitetAAREG(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate skjæringstidspunkt) {
        final var arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        final var arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef();
        final var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktivitet, skjæringstidspunkt, false);
        var wrapper = new ArbeidsforholdWrapper();
        wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktivitet, skjæringstidspunkt));
        wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
        wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        mapArbeidsgiver(wrapper, arbeidsgiver);
        return wrapper;
    }

    private void mapArbeidsgiver(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver) {
        wrapper.setArbeidsgiverReferanse(arbeidsgiver.getIdentifikator());
    }

    private void mapArbeidsgiverForOverstyring(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver) {
        wrapper.setArbeidsgiverReferanse(arbeidsgiver.getIdentifikator());
    }

    private boolean harIkkeFåttInntektsmelding(Yrkesaktivitet yr, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream().noneMatch(i -> yr.gjelderFor(i.getArbeidsgiver(), i.getArbeidsforholdRef()));
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
