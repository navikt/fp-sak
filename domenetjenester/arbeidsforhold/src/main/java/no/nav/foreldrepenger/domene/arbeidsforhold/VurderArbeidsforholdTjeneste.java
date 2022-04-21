package no.nav.foreldrepenger.domene.arbeidsforhold;

import static java.util.stream.Collectors.flatMapping;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.FORENKLET_OPPGJØRSORDNING;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.MARITIMT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.EndringIArbeidsforholdId;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.IkkeTattStillingTil;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.LeggTilResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.PåkrevdeInntektsmeldingerTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.VurderPermisjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class VurderArbeidsforholdTjeneste {

    private static final Set<ArbeidType> ARBEIDSFORHOLD_TYPER = Stream.of(ORDINÆRT_ARBEIDSFORHOLD, FORENKLET_OPPGJØRSORDNING, MARITIMT_ARBEIDSFORHOLD)
            .collect(Collectors.toSet());
    private static final Logger LOG = LoggerFactory.getLogger(VurderArbeidsforholdTjeneste.class);

    private PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste;

    VurderArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public VurderArbeidsforholdTjeneste(PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste) {
        this.påkrevdeInntektsmeldingerTjeneste = påkrevdeInntektsmeldingerTjeneste;
    }

    /**
     * Vurderer alle arbeidsforhold innhentet i en behandling.
     * <p>
     * Gjør vurderinger for å se om saksbehandler må ta stilling til enkelte av
     * disse og returener sett med hvilke saksbehandler må ta stilling til.
     * <p>
     *
     * @param behandlingReferanse      behandlingen
     * @param iayGrunnlag              - grunnlag for behandlingen
     * @param SakInntektsmeldinger     - alle inntektsmeldinger for saken
     *                                 behandlingen tilhører
     * @param skalTaStillingTilEndring skal ta stilling til endring i
     *                                 arbeidsforholdRef i inntektsmeldingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            SakInntektsmeldinger sakInntektsmeldinger,
            boolean skalTaStillingTilEndringArbeidsforhold) {
        var arbeidsgiverSetMap = vurderMedÅrsak(behandlingReferanse, iayGrunnlag, sakInntektsmeldinger,
                skalTaStillingTilEndringArbeidsforhold);
        logg(arbeidsgiverSetMap, behandlingReferanse.saksnummer());
        return arbeidsgiverSetMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, VurderArbeidsforholdTjeneste::mapTilArbeidsforholdRef));
    }

    private void logg(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap, Saksnummer saksnummer) {
        Set<ArbeidsforholdMedÅrsak> alleArbeidsforholdMedÅrsaker = arbeidsgiverSetMap.values().stream().flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Set<AksjonspunktÅrsak> alleÅrsaker = alleArbeidsforholdMedÅrsaker.stream()
            .map(ArbeidsforholdMedÅrsak::getÅrsaker)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        alleÅrsaker.forEach(årsak -> LOG.info("FP-729520: Saksnummer {} har fått utledet årsak for avklar arbeidsforhold {}", saksnummer.getVerdi(), årsak));
    }

    private static Set<InternArbeidsforholdRef> mapTilArbeidsforholdRef(Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> entry) {
        return entry.getValue().stream()
                .map(ArbeidsforholdMedÅrsak::getRef)
                .collect(Collectors.toSet());
    }

    /**
     * Vurderer alle arbeidsforhold innhentet i en behandling.
     * <p>
     * Gjør vurderinger for å se om saksbehandler må ta stilling til enkelte av
     * disse og returener sett med hvilke saksbehandler må ta stilling til.
     *
     * Legger også på en årsak for hvorfor arbeidsforholdet har fått et
     * aksjonspunkt.
     * <p>
     *
     * @param ref                      behandlingen
     * @param skalTaStillingTilEndring skal ta stilling til endring i
     *                                 arbeidsforholdRef i inntektsmeldingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> vurderMedÅrsak(BehandlingReferanse ref,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            SakInntektsmeldinger sakInntektsmeldinger,
            boolean skalTaStillingTilEndringArbeidsforhold) {

        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();

        if (skalTaStillingTilEndringArbeidsforhold) {
            Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldinger");
            vurderOmArbeidsforholdKanGjenkjennes(result, sakInntektsmeldinger, iayGrunnlag, ref);
        }

        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(ref, result, iayGrunnlag);
        påkrevdeInntektsmeldingerTjeneste.leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(ref, result);
        erRapportertNormalInntektUtenArbeidsforhold(iayGrunnlag, ref);
        erMottattInntektsmeldingUtenArbeidsforhold(result, iayGrunnlag, ref);

        return result;

    }

    /**
     * Gir forskjellen i inntektsmeldinger mellom to versjoner av inntektsmeldinger.
     * Benyttes for å markere arbeidsforhold det må tas stilling til å hva
     * saksbehandler skal gjøre.
     *
     * @param behandlingReferanse behandlingen
     * @param ytelseType          FagsakYtelseTypen
     * @return Endringene i inntektsmeldinger
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> endringerIInntektsmelding(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            SakInntektsmeldinger sakInntektsmeldinger,
            FagsakYtelseType ytelseType) {
        Objects.requireNonNull(iayGrunnlag, "iayGrunnlag");
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        var yrkesaktiviteterPerArbeidsgiver = mapYrkesaktiviteterPerArbeidsgiver(behandlingReferanse,
                iayGrunnlag);
        var eksisterendeGrunnlag = hentForrigeVersjonAvInntektsmeldingForBehandling(sakInntektsmeldinger,
                behandlingReferanse.behandlingId());
        var nyAggregat = iayGrunnlag.getInntektsmeldinger();

        final var eksisterende = inntektsmeldingerPerArbeidsgiver(eksisterendeGrunnlag
                .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger));
        final var ny = inntektsmeldingerPerArbeidsgiver(nyAggregat);

        if (!eksisterende.equals(ny)) {
            // Klassifiser endringssjekk
            for (var arbeidsgiverSetEntry : ny.entrySet()) {
                EndringIArbeidsforholdId.vurderMedÅrsak(result, arbeidsgiverSetEntry, eksisterende, iayGrunnlag, yrkesaktiviteterPerArbeidsgiver);
            }
        }
        return result;
    }

    private List<Yrkesaktivitet> getAlleArbeidsforhold(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        var yrkesaktiviteterFørStp = filter.før(skjæringstidspunkt).getYrkesaktiviteter();
        var yrkesaktiviteterEtterStp = filter.etter(skjæringstidspunkt).getYrkesaktiviteter();

        return Stream.of(yrkesaktiviteterFørStp, yrkesaktiviteterEtterStp)
                .flatMap(Collection::stream)
                .filter(Yrkesaktivitet::erArbeidsforhold)
                .distinct()
                .collect(Collectors.toList());
    }

    private void erMottattInntektsmeldingUtenArbeidsforhold(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
            InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse) {
        final var inntektsmeldinger = grunnlag.getInntektsmeldinger();
        if (inntektsmeldinger.isPresent()) {
            final var aggregat = inntektsmeldinger.get();
            for (var inntektsmelding : aggregat.getInntektsmeldingerSomSkalBrukes()) {
                if (harInntektsmeldingUtenArbeid(grunnlag, behandlingReferanse, inntektsmelding)) {
                    final var arbeidsgiver = inntektsmelding.getArbeidsgiver();
                    final var arbeidsforholdRefs = trekkUtRef(inntektsmelding);
                    LeggTilResultat.leggTil(result, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD, arbeidsgiver, arbeidsforholdRefs);
                    LOG.info("Inntektsmelding uten kjent arbeidsforhold: arbeidsgiver={}, arbeidsforholdRef={}", arbeidsgiver, arbeidsforholdRefs);
                }
            }
        }
    }

    private boolean harInntektsmeldingUtenArbeid(InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse,
            Inntektsmelding inntektsmelding) {
        var harIngenArbeidsforhold = harIngenArbeidsforhold(grunnlag, behandlingReferanse, inntektsmelding);
        return (harIngenArbeidsforhold || erFiskerUtenAktivtArbeid(grunnlag, behandlingReferanse, inntektsmelding))
                && IkkeTattStillingTil.vurder(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef(), grunnlag);
    }

    private boolean erFiskerUtenAktivtArbeid(InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse,
            Inntektsmelding inntektsmelding) {
        return harOppgittFiske(grunnlag) && harIngenAktiveArbeidsforhold(behandlingReferanse, inntektsmelding, grunnlag);
    }

    private boolean harIngenAktiveArbeidsforhold(BehandlingReferanse behandlingReferanse, Inntektsmelding inntektsmelding,
            InntektArbeidYtelseGrunnlag grunnlag) {
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
                grunnlag.getAktørArbeidFraRegister(behandlingReferanse.aktørId()));
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        return filter.getYrkesaktiviteter().stream()
                .filter(ya -> gjelderInntektsmeldingFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef(), ya))
                .noneMatch(ya -> ya.getAlleAktivitetsAvtaler().stream()
                        .filter(AktivitetsAvtale::erAnsettelsesPeriode).anyMatch(aa -> aa.getPeriode().inkluderer(skjæringstidspunkt)));
    }

    private boolean harIngenArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse,
            Inntektsmelding inntektsmelding) {
        final var antallArbeidsforIArbeidsgiveren = antallArbeidsforHosArbeidsgiveren(behandlingReferanse, grunnlag,
                inntektsmelding.getArbeidsgiver(),
                inntektsmelding.getArbeidsforholdRef());
        return (antallArbeidsforIArbeidsgiveren.før() == 0) && (antallArbeidsforIArbeidsgiveren.etter() == 0);
    }

    /**
     * Finner ut om bruker har oppgitt fiske i søknaden under egne næringer.
     *
     * Fiske kan deles i lott eller hyre. Lott skal rapporteres som
     * næringsvirksomhet mens hyre skal beregnes som arbeidstaker. Disse
     * virksomhetene er ofte unnlatt rapportering i aareg, og det vil derfor ofte
     * komme en inntektsmelding uten arbeidsforhold. Det kan også hende at
     * arbeidsforholdet tidligere har vært registrert i aareg, men ikke er det ved
     * skjæringstidspunktet. I tilfeller der vi har en inntektsmelding uten
     * arbeidsforhold vil vi derfor sjekke om bruker har oppgitt fiske i søknaden.
     * Om søker har oppgitt fiske vil det være mulig å opprette arbeidsforhold
     * basert på denne inntektsmeldingen.
     *
     * @param grunnlag Inntekt-arbeid-ytelse grunnlag
     * @return Har søker oppgitt fiske i søknaden
     */
    private boolean harOppgittFiske(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getOppgittOpptjening().stream().anyMatch(
                oppgittOpptjening -> oppgittOpptjening.getEgenNæring().stream().anyMatch(en -> en.getVirksomhetType().equals(VirksomhetType.FISKE)));
    }

    private Set<InternArbeidsforholdRef> trekkUtRef(Inntektsmelding inntektsmelding) {
        if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
            return Stream.of(inntektsmelding.getArbeidsforholdRef()).collect(Collectors.toSet());
        }
        return Stream.of(InternArbeidsforholdRef.nullRef()).collect(Collectors.toSet());
    }

    private void vurderOmArbeidsforholdKanGjenkjennes(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
            SakInntektsmeldinger sakInntektsmeldinger,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            BehandlingReferanse behandlingReferanse) {
        Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldinger");
        Objects.requireNonNull(iayGrunnlag, "iayGrunnlag");
        var eksisterendeGrunnlag = hentForrigeVersjonAvInntektsmeldingForBehandling(sakInntektsmeldinger, behandlingReferanse.behandlingId());
        var nyAggregat = iayGrunnlag.getInntektsmeldinger();
        var yrkesaktiviteterPerArbeidsgiver = mapYrkesaktiviteterPerArbeidsgiver(behandlingReferanse, iayGrunnlag);

        final var eksisterendeIM = inntektsmeldingerPerArbeidsgiver(eksisterendeGrunnlag
                .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger));
        final var ny = inntektsmeldingerPerArbeidsgiver(nyAggregat);

        if (!eksisterendeIM.isEmpty() && !eksisterendeIM.equals(ny)) {
            // Klassifiser endringssjekk
            for (var nyIM : ny.entrySet()) {
                EndringIArbeidsforholdId.vurderMedÅrsak(result, nyIM, eksisterendeIM, iayGrunnlag, yrkesaktiviteterPerArbeidsgiver);
            }
        }
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> mapYrkesaktiviteterPerArbeidsgiver(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag grunnlag) {
        var yrkesaktiviteter = getAlleArbeidsforhold(behandlingReferanse.aktørId(), grunnlag,
                behandlingReferanse.getUtledetSkjæringstidspunkt());
        return yrkesaktiviteter.stream()
                .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                        flatMapping(ya -> Stream.of(ya.getArbeidsforholdRef()), Collectors.toSet())));
    }

    private static record AntallArbeidsforholdHosAG(Long før, Long etter) {}

    private AntallArbeidsforholdHosAG antallArbeidsforHosArbeidsgiveren(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
                grunnlag.getAktørArbeidFraRegister(behandlingReferanse.aktørId()));

        var antallFør = antallArbeidsfor(arbeidsgiver, arbeidsforholdRef, filter.før(skjæringstidspunkt));
        var antallEtter = antallArbeidsfor(arbeidsgiver, arbeidsforholdRef, filter.etter(skjæringstidspunkt));

        return new AntallArbeidsforholdHosAG(antallFør, antallEtter);
    }

    private long antallArbeidsfor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, YrkesaktivitetFilter filter) {
        long antall = 0;
        antall = filter.getYrkesaktiviteter()
                .stream()
                .filter(yr -> gjelderInntektsmeldingFor(arbeidsgiver, arbeidsforholdRef, yr))
                .count();
        return antall;
    }

    private boolean gjelderInntektsmeldingFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Yrkesaktivitet yr) {
        return ARBEIDSFORHOLD_TYPER.contains(yr.getArbeidType())
                && yr.getArbeidsgiver().equals(arbeidsgiver)
                && yr.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> inntektsmeldingerPerArbeidsgiver(
            Optional<InntektsmeldingAggregat> inntektsmeldingAggregat) {
        if (!inntektsmeldingAggregat.isPresent()) {
            return Collections.emptyMap();
        }
        return inntektsmeldingAggregat.get()
                .getInntektsmeldingerSomSkalBrukes()
                .stream()
                .collect(Collectors.groupingBy(Inntektsmelding::getArbeidsgiver,
                        flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));
    }

    private void erRapportertNormalInntektUtenArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse referanse) {
        var skjæringstidspunkt = referanse.getUtledetSkjæringstidspunkt();
        var filter = grunnlag.getAktørInntektFraRegister(referanse.aktørId()).map(ai -> new InntektFilter(ai).før(skjæringstidspunkt))
                .orElse(InntektFilter.EMPTY);

        var lønnFilter = filter.filterPensjonsgivende().filter(InntektspostType.LØNN);
        var arbeidsforholdInformasjon = grunnlag.getArbeidsforholdInformasjon();
        var filterYrkesaktivitet = new YrkesaktivitetFilter(arbeidsforholdInformasjon, grunnlag.getAktørArbeidFraRegister(referanse.aktørId()));

        lønnFilter.getAlleInntekter()
                .forEach(inntekt -> rapporterHvisHarIkkeArbeidsforhold(grunnlag, inntekt, filterYrkesaktivitet, skjæringstidspunkt));
    }

    private void rapporterHvisHarIkkeArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag,
            Inntekt inntekt,
            YrkesaktivitetFilter filterYrkesaktivitet,
            LocalDate skjæringstidspunkt) {
        var filterFør = filterYrkesaktivitet.før(skjæringstidspunkt);
        var filterEtter = filterYrkesaktivitet.etter(skjæringstidspunkt);

        var ingenFør = true;
        if (!filterFør.getYrkesaktiviteter().isEmpty()) {
            ingenFør = ikkeArbeidsforholdRegisterert(inntekt, filterFør);
        }

        var ingenEtter = true;
        if (!filterEtter.getYrkesaktiviteter().isEmpty()) {
            ingenEtter = ikkeArbeidsforholdRegisterert(inntekt, filterEtter);
        }

        if (ingenFør && ingenEtter) {
            var arbeidsforholdRefs = Stream.of(InternArbeidsforholdRef.nullRef()).collect(Collectors.toSet());
            var inntektsmeldinger = grunnlag.getInntektsmeldinger();
            if (inntektsmeldinger.isPresent()) {
                arbeidsforholdRefs = inntektsmeldinger.get()
                        .getInntektsmeldingerFor(inntekt.getArbeidsgiver())
                        .stream()
                        .map(Inntektsmelding::getArbeidsforholdRef)
                        .collect(Collectors.toSet());
            }
            LOG.info("Inntekter uten kjent arbeidsforhold: arbeidsgiver={}, arbeidsforholdRef={}", inntekt.getArbeidsgiver(), arbeidsforholdRefs);
        }

    }

    private boolean ikkeArbeidsforholdRegisterert(Inntekt inntekt, YrkesaktivitetFilter filter) {
        // må også sjekke mot frilans. Skal ikke be om avklaring av inntektsposter som
        // stammer fra frilansoppdrag
        var yrkesaktiviteter = filter.getFrilansOppdrag();
        if (!yrkesaktiviteter.isEmpty()
                && yrkesaktiviteter.stream().anyMatch(y -> Objects.equals(y.getArbeidsgiver(), inntekt.getArbeidsgiver()))) {
            return false;
        }

        return filter.getYrkesaktiviteter()
                .stream()
                .noneMatch(yr -> ARBEIDSFORHOLD_TYPER.contains(yr.getArbeidType()) && yr.getArbeidsgiver().equals(inntekt.getArbeidsgiver()))
                && filter.getArbeidsforholdOverstyringer()
                        .stream()
                        .noneMatch(it -> Objects.equals(it.getArbeidsgiver(), inntekt.getArbeidsgiver())
                                && Objects.equals(it.getHandling(), ArbeidsforholdHandlingType.IKKE_BRUK));
    }

    /**
     * Henter ut forrige versjon av inntektsmeldinger
     *
     * @param behandlingId iden til behandlingen
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    private Optional<InntektArbeidYtelseGrunnlag> hentForrigeVersjonAvInntektsmeldingForBehandling(SakInntektsmeldinger sakInntektsmeldinger,
            Long behandlingId) {
        Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldiner");
        // litt rar - returnerer forrige grunnlag som har en inntektsmelding annen enn
        // siste benyttede inntektsmelding
        var grunnlagEksternReferanse = sakInntektsmeldinger.getSisteGrunnlagReferanseDerInntektsmeldingerForskjelligFraNyeste(behandlingId);
        return grunnlagEksternReferanse.flatMap(grunnlagUuid -> sakInntektsmeldinger.finnGrunnlag(behandlingId, grunnlagUuid));
    }

}
