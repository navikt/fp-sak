package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.*;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

@ApplicationScoped
public class InntektsmeldingRegisterTjeneste {

    private static final String VALID_REF = "behandlingReferanse";
    private static final Set<ArbeidType> AA_REG_TYPER = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD,
            ArbeidType.FORENKLET_OPPGJØRSORDNING);
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRegisterTjeneste.class);

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

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraAAreg(BehandlingReferanse referanse,
            boolean erEndringssøknad) {
        Objects.requireNonNull(referanse, VALID_REF);

        var skjæringstidspunkt = referanse.getSkjæringstidspunkt();
        var dato = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var påkrevdeInntektsmeldinger = abakusArbeidsforholdTjeneste
                .finnArbeidsforholdForIdentPåDag(referanse.aktørId(), dato, referanse.fagsakYtelseType());

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
                    var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.behandlingId());
                    arbInfo = grunnlag.getArbeidsforholdInformasjon().orElseThrow(
                            () -> new IllegalStateException(
                                    "Utvikler-feil: mangler IAYG.ArbeidsforholdInformasjon, kan ikke slå opp ekstern referanse"));
                }
                return arbInfo.finnEkstern(arbeidsgiver, internReferanse);
            }
        }

        filtrerUtMottatteInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, new FinnEksternReferanse());

        return filtrerInntektsmeldingerForYtelse(referanse, påkrevdeInntektsmeldinger);
    }

    private void logInntektsmeldinger(BehandlingReferanse referanse, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
            String filtrert) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            LOG.info("{} påkrevdeInntektsmeldinger[{}]: TOM LISTE", filtrert, referanse.behandlingId());
            return;
        }

        påkrevdeInntektsmeldinger.forEach((key, value) -> {
            var arbeidsforholdReferanser = value.stream().map(InternArbeidsforholdRef::toString).collect(Collectors.joining(","));
            LOG.info("{} påkrevdeInntektsmeldinger[{}]: identifikator: {}, arbeidsforholdRef: {}", filtrert, referanse.behandlingId(),
                    tilMaskertNummer(key.getIdentifikator()),
                    arbeidsforholdReferanser);
        });
    }

    /**
     * Liste av arbeidsforhold per arbeidsgiver (ident) som må sende
     * inntektsmelding. Filtrert ut åpenbart passive arbeidsforhold
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
            boolean erEndringssøknad) {
        Objects.requireNonNull(referanse, VALID_REF);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        var påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "UFILTRERT");

        filtrerUtMottatteInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, erEndringssøknad, (a, i) -> i);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "FILTRERT");

        var filtrert = filtrerInntektsmeldingerForYtelse(referanse, påkrevdeInntektsmeldinger);
        return filtrerInntektsmeldingerForYtelseUtvidet(referanse, inntektArbeidYtelseGrunnlag, filtrert);
    }

    // Vent med å ta i bruk denne til vi ikke lenger venter på andel i beregning

    private <V> void filtrerUtMottatteInntektsmeldinger(BehandlingReferanse referanse,
                                                        Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
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
        var arbeidsforholdInformasjon = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId())
            .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        if (arbeidsforholdInformasjon.isPresent()) {
            var informasjon = arbeidsforholdInformasjon.get();
            var inntektsmeldingSomIkkeKommer = informasjon.getOverstyringer()
                .stream()
                .filter(ArbeidsforholdOverstyring::kreverIkkeInntektsmelding)
                .toList();

            fjernInntektsmeldinger(påkrevdeInntektsmeldinger, inntektsmeldingSomIkkeKommer, tilnternArbeidsforhold);
        }
    }

    private <V> void fjernInntektsmeldinger(Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
            List<ArbeidsforholdOverstyring> inntektsmeldingSomIkkeKommer,
            BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        for (var im : inntektsmeldingSomIkkeKommer) {
            if (påkrevdeInntektsmeldinger.containsKey(im.getArbeidsgiver())) {
                var arbeidsforhold = påkrevdeInntektsmeldinger.get(im.getArbeidsgiver());
                if (im.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()) {
                    var matchKey = tilnternArbeidsforhold.apply(im.getArbeidsgiver(), im.getArbeidsforholdRef());
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

        for (var inntektsmelding : inntektsmeldinger) {
            if (påkrevdeInntektsmeldinger.containsKey(inntektsmelding.getArbeidsgiver())) {
                var arbeidsforhold = påkrevdeInntektsmeldinger.get(inntektsmelding.getArbeidsgiver());
                if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
                    var matchKey = tilnternArbeidsforhold.apply(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef());
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

            var skjæringstidspunkt = referanse.getSkjæringstidspunkt();
            var filterFør = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
                    grunnlag.getAktørArbeidFraRegister(referanse.aktørId()))
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
            var ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
            return ansettelsesPerioder.stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(dato));
        }
        return false;
    }

    /**
     * Utleder påkrevde inntektsmeldinger fra grunnlaget basert på informasjonen som
     * har blitt innhentet fra aa-reg (under INNREG-steget)
     * <p>
     * Sjekker opp mot mottatt dato, og melder påkrevde på de som har
     * gjeldende(bruker var ansatt) på mottatt-dato.
     * <p>
     * Skal ikke benytte sjekk mot arkivet slik som gjøres i
     * utledManglendeInntektsmeldingerFraAAreg da disse verdiene skal ikke påvirkes
     * av endringer i arkivet.
     */
    private <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse,
                                                                            Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.filtrerInntektsmeldingerForYtelse(referanse, påkrevdeInntektsmeldinger);
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse,
            Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.filtrerInntektsmeldingerForYtelseUtvidet(referanse, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }
}
