package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class InntektsmeldingRegisterTjeneste {

    private static final String VALID_REF = "behandlingReferanse";
    private static final Set<ArbeidType> AA_REG_TYPER = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD,
        ArbeidType.FORENKLET_OPPGJØRSORDNING);
    private static final Logger LOGGER = LoggerFactory.getLogger(InntektsmeldingRegisterTjeneste.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;
    private Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere;


    InntektsmeldingRegisterTjeneste() {
        // CDI-runner
    }

    @Inject
    public InntektsmeldingRegisterTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste,
                                           @Any Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.abakusArbeidsforholdTjeneste = abakusArbeidsforholdTjeneste;
        this.inntektsmeldingFiltere = inntektsmeldingFiltere;
    }

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraAAreg(BehandlingReferanse referanse, boolean erEndringssøknad) {
        Objects.requireNonNull(referanse, VALID_REF);

        Skjæringstidspunkt skjæringstidspunkt = referanse.getSkjæringstidspunkt();
        LocalDate dato = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> påkrevdeInntektsmeldinger = abakusArbeidsforholdTjeneste.finnArbeidsforholdForIdentPåDag(referanse.getAktørId(), dato);

        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return Collections.emptyMap();
        }

        return utledManglendeInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad);

    }

    private Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledManglendeInntektsmeldinger(BehandlingReferanse referanse,
                                                                                             Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                                                             boolean erEndringssøknad) {
        class FinnEksternReferanse implements BiFunction<Arbeidsgiver, InternArbeidsforholdRef, EksternArbeidsforholdRef> {
            ArbeidsforholdInformasjon arbInfo;

            @Override
            public EksternArbeidsforholdRef apply(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
                if (arbInfo == null) {
                    var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());
                    arbInfo = grunnlag.getArbeidsforholdInformasjon().orElseThrow(
                        () -> new IllegalStateException("Utvikler-feil: mangler IAYG.ArbeidsforholdInformasjon, kan ikke slå opp ekstern referanse"));
                }
                return arbInfo.finnEkstern(arbeidsgiver, internReferanse);
            }
        }


        filtrerUtMottatteInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, new FinnEksternReferanse());

        return filtrerInntektsmeldingerForYtelse(referanse, Optional.empty(), påkrevdeInntektsmeldinger);
    }

    private void logInntektsmeldinger(BehandlingReferanse referanse, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger, String filtrert) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            LOGGER.info("{} påkrevdeInntektsmeldinger[{}]: TOM LISTE", filtrert, referanse.getBehandlingId());
            return;
        }

        påkrevdeInntektsmeldinger.forEach((key, value) -> {
            String arbeidsforholdReferanser = value.stream().map(InternArbeidsforholdRef::toString).collect(Collectors.joining(","));
            LOGGER.info("{} påkrevdeInntektsmeldinger[{}]: identifikator: {}, arbeidsforholdRef: {}", filtrert, referanse.getBehandlingId(), key.getIdentifikator(),
                arbeidsforholdReferanser);
        });
    }

    /**
     * Liste av arbeidsforhold per arbeidsgiver (ident) som må sende inntektsmelding for.
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse, boolean erEndringssøknad) {
        Objects.requireNonNull(referanse, VALID_REF);
        final Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(
            referanse.getBehandlingId());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "UFILTRERT");

        filtrerUtMottatteInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, (a, i) -> i);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "FILTRERT");

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> filtrert = filtrerInntektsmeldingerForYtelse(referanse, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
        return filtrerInntektsmeldingerForYtelseUtvidet(referanse, inntektArbeidYtelseGrunnlag, filtrert);
    }

    /**
     * Liste av arbeidsforhold per arbeidsgiver (ident) som må sende inntektsmelding for.
     */
    protected Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraGrunnlagForVurdering(BehandlingReferanse referanse, boolean erEndringssøknad) {
        Objects.requireNonNull(referanse, VALID_REF);
        final Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(
            referanse.getBehandlingId());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "UFILTRERT");

        filtrerUtMottatteInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, (a, i) -> i);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "FILTRERT");

        return filtrerInntektsmeldingerForYtelse(referanse, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }


    private <V> void filtrerUtMottatteInntektsmeldinger(BehandlingReferanse referanse, Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
                                                        boolean erEndringssøknad,
                                                        BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        // modder påkrevdeInntektsmeldinger for hvert kall
        if (!påkrevdeInntektsmeldinger.isEmpty()) {
            inntektsmeldingerSomHarKommet(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, tilnternArbeidsforhold);
            if (!påkrevdeInntektsmeldinger.isEmpty()) {
                fjernInntektsmeldingerSomAltErAvklart(referanse, påkrevdeInntektsmeldinger, tilnternArbeidsforhold);
            }
        }
    }

    private <V> void fjernInntektsmeldingerSomAltErAvklart(BehandlingReferanse ref, Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
                                                           BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        final Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.getBehandlingId())
            .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        if (arbeidsforholdInformasjon.isPresent()) {
            final ArbeidsforholdInformasjon informasjon = arbeidsforholdInformasjon.get();
            final List<ArbeidsforholdOverstyring> inntektsmeldingSomIkkeKommer = informasjon.getOverstyringer()
                .stream()
                .filter(ArbeidsforholdOverstyring::kreverIkkeInntektsmelding)
                .collect(Collectors.toList());

            fjernInntektsmeldinger(påkrevdeInntektsmeldinger, inntektsmeldingSomIkkeKommer, tilnternArbeidsforhold);
        }
    }

    private <V> void fjernInntektsmeldinger(Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger, List<ArbeidsforholdOverstyring> inntektsmeldingSomIkkeKommer,
                                            BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        for (ArbeidsforholdOverstyring im : inntektsmeldingSomIkkeKommer) {
            if (påkrevdeInntektsmeldinger.containsKey(im.getArbeidsgiver())) {
                final Set<V> arbeidsforhold = påkrevdeInntektsmeldinger.get(im.getArbeidsgiver());
                if (im.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()) {
                    V matchKey = tilnternArbeidsforhold.apply(im.getArbeidsgiver(), im.getArbeidsforholdRef());
                    arbeidsforhold.remove(matchKey);
                } else {
                    arbeidsforhold.clear();
                }
                if (arbeidsforhold.isEmpty()) {
                    påkrevdeInntektsmeldinger.remove(im.getArbeidsgiver());
                }
            }
        }
    }

    private <V> void inntektsmeldingerSomHarKommet(BehandlingReferanse referanse,
                                                   Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
                                                   boolean erEndringssøknad,
                                                   BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return; // quick exit
        }

        List<Inntektsmelding> inntektsmeldinger;
        if (erEndringssøknad && referanse.erRevurdering()) {
            inntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerMottattEtterGjeldendeVedtak(referanse);
        } else {
            var skjæringstidspunkt = referanse.getSkjæringstidspunkt();
            inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt());
        }

        for (Inntektsmelding inntektsmelding : inntektsmeldinger) {
            if (påkrevdeInntektsmeldinger.containsKey(inntektsmelding.getArbeidsgiver())) {
                final Set<V> arbeidsforhold = påkrevdeInntektsmeldinger.get(inntektsmelding.getArbeidsgiver());
                if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
                    V matchKey = tilnternArbeidsforhold.apply(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef());
                    arbeidsforhold.remove(matchKey);
                } else {
                    arbeidsforhold.clear();
                }
                if (arbeidsforhold.isEmpty()) {
                    påkrevdeInntektsmeldinger.remove(inntektsmelding.getArbeidsgiver());
                }
            }
        }

    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledPåkrevdeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
                                                                                                      Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger = new HashMap<>();

        inntektArbeidYtelseGrunnlag.ifPresent(grunnlag -> {

            Skjæringstidspunkt skjæringstidspunkt = referanse.getSkjæringstidspunkt();
            var filterFør = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(referanse.getAktørId()))
                .før(skjæringstidspunkt.getUtledetSkjæringstidspunkt());

            filterFør.getYrkesaktiviteter().stream()
                .filter(ya -> AA_REG_TYPER.contains(ya.getArbeidType()))
                .filter(ya -> harRelevantAnsettelsesperiodeSomDekkerAngittDato(filterFør, ya, skjæringstidspunkt.getUtledetSkjæringstidspunkt()))
                .forEach(relevantYrkesaktivitet -> {
                    var identifikator = relevantYrkesaktivitet.getArbeidsgiver();
                    var arbeidsforholdRef = InternArbeidsforholdRef.ref(relevantYrkesaktivitet.getArbeidsforholdRef().getReferanse());

                    if (påkrevdeInntektsmeldinger.containsKey(identifikator)) {
                        påkrevdeInntektsmeldinger.get(identifikator).add(arbeidsforholdRef);
                    } else {
                        final Set<InternArbeidsforholdRef> arbeidsforholdSet = new LinkedHashSet<>();
                        arbeidsforholdSet.add(arbeidsforholdRef);
                        påkrevdeInntektsmeldinger.put(identifikator, arbeidsforholdSet);
                    }
                });
        });
        return påkrevdeInntektsmeldinger;
    }

    private boolean harRelevantAnsettelsesperiodeSomDekkerAngittDato(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate dato) {
        if (yrkesaktivitet.erArbeidsforhold()) {
            List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
            return ansettelsesPerioder.stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(dato));
        }
        return false;
    }

    /**
     * Utleder påkrevde inntektsmeldinger fra grunnlaget basert på informasjonen som har blitt innhentet fra aa-reg
     * (under INNREG-steget)
     * <p>
     * Sjekker opp mot mottatt dato, og melder påkrevde på de som har gjeldende(bruker var ansatt) på mottatt-dato.
     * <p>
     * Skal ikke benytte sjekk mot arkivet slik som gjøres i utledManglendeInntektsmeldingerFraAAreg da
     * disse verdiene skal ikke påvirkes av endringer i arkivet.
     */
    private <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        InntektsmeldingFilterYtelse filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.getFagsakYtelseType().getKode()));
        return filter.filtrerInntektsmeldingerForYtelse(referanse, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }

    private <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        InntektsmeldingFilterYtelse filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.getFagsakYtelseType().getKode()));
        return filter.filtrerInntektsmeldingerForYtelseUtvidet(referanse, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }
}
