package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static java.util.stream.Collectors.flatMapping;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

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
                                                                                                    Skjæringstidspunkt skjæringstidspunkt) {
        Objects.requireNonNull(referanse, VALID_REF);

        var dato = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var påkrevdeInntektsmeldinger = abakusArbeidsforholdTjeneste.finnArbeidsforholdForIdentPåDag(referanse.aktørId(), dato, referanse.fagsakYtelseType());

        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return Collections.emptyMap();
        }

        return utledManglendeInntektsmeldinger(referanse, skjæringstidspunkt, påkrevdeInntektsmeldinger);

    }

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraAAregVurderPermisjon(BehandlingReferanse referanse,
                                                                                                                   Skjæringstidspunkt skjæringstidspunkt) {
        Objects.requireNonNull(referanse, VALID_REF);

        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var arbeidsforholdInfo = abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(referanse.aktørId(), stp, stp,
            referanse.fagsakYtelseType());

        if (arbeidsforholdInfo.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ArbeidsforholdMedPermisjon> påkrevdeArbeidsgivere = new ArrayList<>();
        var mapAvOrgnrOgAvtaler = arbeidsforholdInfo.stream().collect(Collectors.groupingBy(this::arbeidsgiverNøkkel));

        mapAvOrgnrOgAvtaler.forEach((orgnummerOgArbeidsfoholdsId, arbeidsforholdListe) -> {
            var harPermisjon = harHundreProsentPermisjonOgDenErRelevant(arbeidsforholdListe, stp);
            if (!harPermisjon) {
                påkrevdeArbeidsgivere.addAll(arbeidsforholdListe);
            }
        });

        var påkrevdeInntektsmeldinger = påkrevdeArbeidsgivere.stream()
            .collect(Collectors.groupingBy(InntektsmeldingRegisterTjeneste::mapTilArbeidsgiver, flatMapping(arbeidsforholdMedPermisjon -> Stream.of(
                    EksternArbeidsforholdRef.ref(
                        arbeidsforholdMedPermisjon.arbeidsforholdId() != null ? arbeidsforholdMedPermisjon.arbeidsforholdId().getReferanse() : null)),
                Collectors.toSet())));

        return utledManglendeInntektsmeldinger(referanse, skjæringstidspunkt, påkrevdeInntektsmeldinger);
    }

    private static Arbeidsgiver mapTilArbeidsgiver(ArbeidsforholdMedPermisjon arbeidsforholdMedPermisjon) {
        var arbeidsgiver = arbeidsforholdMedPermisjon.arbeidsgiver();
        if (arbeidsgiver.getErVirksomhet()) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr());
        }
        if (arbeidsgiver.erAktørId()) {
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdentifikator()));
        }
        throw new IllegalArgumentException("Arbeidsgiver er verken person eller organisasjon");
    }

    private boolean harHundreProsentPermisjonOgDenErRelevant(List<ArbeidsforholdMedPermisjon> arbeidsforholdListe, LocalDate stp) {
        var stillingsprosentPåStp = arbeidsforholdListe.stream()
            .map(ArbeidsforholdMedPermisjon::aktivitetsavtaler)
            .flatMap(Collection::stream)
            .filter(aktivitetAvtale -> aktivitetAvtale.periode().inkluderer(stp))
            .map(ArbeidsforholdTjeneste.AktivitetAvtale::stillingsprosent)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        var relevantPermisjonsprosentPåStp = arbeidsforholdListe.stream().map(ArbeidsforholdMedPermisjon::permisjoner)
            .flatMap(Collection::stream)
            .filter(this::erPermisjonRelevant)
            .filter(permisjon -> permisjon.periode().inkluderer(stp))
            .map(ArbeidsforholdTjeneste.Permisjon::prosent)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        return stillingsprosentPåStp.compareTo(BigDecimal.ZERO) > 0 && relevantPermisjonsprosentPåStp.compareTo(BigDecimal.valueOf(100)) == 0;
    }

    private boolean erPermisjonRelevant(ArbeidsforholdTjeneste.Permisjon permisjon) {
        return permisjon.type().erRelevantForBeregningEllerArbeidsforhold();
    }

    private String arbeidsgiverNøkkel(ArbeidsforholdMedPermisjon arbeidsforholdInfo) {
        return arbeidsforholdInfo.arbeidsforholdId() != null
            ? arbeidsforholdInfo.arbeidsgiver().getOrgnr() + "-" + arbeidsforholdInfo.arbeidsforholdId().getReferanse()
            : arbeidsforholdInfo.arbeidsgiver().getOrgnr();
    }

    private Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledManglendeInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp,
            Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> påkrevdeInntektsmeldinger) {
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

        filtrerUtMottatteInntektsmeldinger(referanse, stp, påkrevdeInntektsmeldinger, new FinnEksternReferanse());

        return søknadsFilter(referanse, påkrevdeInntektsmeldinger);
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
     * Liste av alle påkrevde inntektsmeldinger
     * inntektsmelding. Filtrert ut åpenbart passive arbeidsforhold
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentAllePåkrevdeInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        Objects.requireNonNull(referanse, VALID_REF);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        var påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, stp, inntektArbeidYtelseGrunnlag);
        var filtrertHvisSvp = søknadsFilter(referanse, påkrevdeInntektsmeldinger);
        return aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, filtrertHvisSvp);
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledPåkrevdeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
                                                                                                      Skjæringstidspunkt skjæringstidspunkt, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger = new HashMap<>();

        inntektArbeidYtelseGrunnlag.ifPresent(grunnlag -> {

            var filterFør = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(referanse.aktørId()))
                .før(skjæringstidspunkt.getUtledetSkjæringstidspunkt());

            var relevanteYrkesaktiviteter = filterFør.getYrkesaktiviteter().stream()
                .filter(ya -> AA_REG_TYPER.contains(ya.getArbeidType()))
                .filter(ya -> harRelevantAnsettelsesperiodeSomDekkerAngittDato(filterFør, ya, skjæringstidspunkt.getUtledetSkjæringstidspunkt()))
                .toList();
            relevanteYrkesaktiviteter.forEach(relevantYrkesaktivitet -> {
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
    /**
     * Liste av arbeidsforhold per arbeidsgiver (ident) som må sende
     * inntektsmelding. Filtrert ut åpenbart passive arbeidsforhold
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
        Skjæringstidspunkt stp) {
        Objects.requireNonNull(referanse, VALID_REF);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        var påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, stp, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "UFILTRERT");

        var påkrevdListeSøkteArbeidsforhold = søknadsFilter(referanse, påkrevdeInntektsmeldinger);
        logInntektsmeldinger(referanse, påkrevdListeSøkteArbeidsforhold, "FILTRERT bort arbeidsforhold det ikke er søkt(svp) for");

        var påkrevdListeAktiveArbeidsforhold = aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, påkrevdListeSøkteArbeidsforhold);
        filtrerUtMottatteInntektsmeldinger(referanse, stp, påkrevdListeAktiveArbeidsforhold, (a, i) -> i);
        logInntektsmeldinger(referanse, påkrevdListeAktiveArbeidsforhold, "FILTRERT bort inaktive arbeidsforhold, og arbeidsforhold vi har mottatt inntektsmelding på");

        return påkrevdListeAktiveArbeidsforhold;
    }

    // Vent med å ta i bruk denne til vi ikke lenger venter på andel i beregning
    private <V> void filtrerUtMottatteInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp,
                                                        Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger, BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        // modder påkrevdeInntektsmeldinger for hvert kall
        if (!påkrevdeInntektsmeldinger.isEmpty()) {
            inntektsmeldingerSomHarKommet(referanse, stp, påkrevdeInntektsmeldinger, tilnternArbeidsforhold);
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

    private <V> void inntektsmeldingerSomHarKommet(BehandlingReferanse referanse, Skjæringstidspunkt stp,
            Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger, BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return; // quick exit
        }

        List<Inntektsmelding> inntektsmeldinger;
        inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, stp.getUtledetSkjæringstidspunkt());

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
    private <V> Map<Arbeidsgiver, Set<V>> søknadsFilter(BehandlingReferanse referanse,
                                                        Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.søknadsFilter(referanse, påkrevdeInntektsmeldinger);
    }

    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                                                      Skjæringstidspunkt stp,
                                                                                      Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }
}
