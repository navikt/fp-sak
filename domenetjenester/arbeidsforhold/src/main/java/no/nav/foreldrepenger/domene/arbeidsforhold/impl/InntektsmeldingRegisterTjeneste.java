package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class InntektsmeldingRegisterTjeneste {

    private static final String VALID_REF = "behandlingReferanse";
    private static final Set<ArbeidType> AA_REG_TYPER = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD,
            ArbeidType.FORENKLET_OPPGJØRSORDNING);
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRegisterTjeneste.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere;

    InntektsmeldingRegisterTjeneste() {
        // CDI-runner
    }

    @Inject
    public InntektsmeldingRegisterTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste, @Any Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingFiltere = inntektsmeldingFiltere;
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
            var arbeidsgivere = relevanteYrkesaktiviteter.stream().map(Yrkesaktivitet::getArbeidsgiver).toList();
            LOG.info("Relevante yrkesaktiviteter for inntektsmelding: {}", arbeidsgivere);
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
        // Sjekk pr arbeidsforhold slik at saksbehandler kan avklare alle arbeidsforhold
        return internUtledManglendeInntektsmeldinger(referanse, stp, true);
    }

    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeInntektsmeldingerForKompletthet(BehandlingReferanse referanse,
                                                                                                         Skjæringstidspunkt stp) {
        // Sjekker pr arbeidsgiver, ikke pr arbeidsforhold, slik at tilfelle med flere arbeidsforhold for samme arbeidsgiver
        // der det har kommet 1 inntektsmelding med arbeidsforhold ikke blir liggende på vent, men går til avklaring
        return internUtledManglendeInntektsmeldinger(referanse, stp, false);
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> internUtledManglendeInntektsmeldinger(BehandlingReferanse referanse,
                                                                                                  Skjæringstidspunkt stp,
                                                                                                  boolean prArbeidsforhold) {
        Objects.requireNonNull(referanse, VALID_REF);
        LOG.info("Utleder manglende inntektsmeldinger på skjæringstidspunkt {} for behandling {}", stp.getUtledetSkjæringstidspunkt(), referanse.behandlingId());
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        var påkrevdeInntektsmeldinger = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, stp, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevdeInntektsmeldinger, "UFILTRERT");

        var påkrevdListeSøkteArbeidsforhold = søknadsFilter(referanse, påkrevdeInntektsmeldinger);
        logInntektsmeldinger(referanse, påkrevdListeSøkteArbeidsforhold, "FILTRERT bort arbeidsforhold det ikke er søkt(svp) for");

        var påkrevdListeAktiveArbeidsforhold = aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, påkrevdListeSøkteArbeidsforhold);
        filtrerUtMottatteInntektsmeldinger(referanse, stp, inntektArbeidYtelseGrunnlag, påkrevdListeAktiveArbeidsforhold, prArbeidsforhold);
        logInntektsmeldinger(referanse, påkrevdListeAktiveArbeidsforhold, "FILTRERT bort inaktive arbeidsforhold, og arbeidsforhold vi har mottatt inntektsmelding på");

        return påkrevdListeAktiveArbeidsforhold;
    }

    // Vent med å ta i bruk denne til vi ikke lenger venter på andel i beregning
    private void filtrerUtMottatteInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp,
                                                    Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                    Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                    boolean prArbeidsforhold) {
        // modder påkrevdeInntektsmeldinger for hvert kall
        if (!påkrevdeInntektsmeldinger.isEmpty()) {
            var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, stp.getUtledetSkjæringstidspunkt());
            for (var inntektsmelding : inntektsmeldinger) {
                fjernArbeidsforholdFraPåkrevde(påkrevdeInntektsmeldinger, inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef(), prArbeidsforhold);
            }
            if (!påkrevdeInntektsmeldinger.isEmpty()) {
                fjernInntektsmeldingerSomAltErAvklart(inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger, prArbeidsforhold);
            }
        }
    }

    private void fjernInntektsmeldingerSomAltErAvklart(Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                       Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                       boolean prArbeidsforhold) {
        var arbeidsforholdInformasjon = inntektArbeidYtelseGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        if (arbeidsforholdInformasjon.isPresent()) {
            var informasjon = arbeidsforholdInformasjon.get();
            var inntektsmeldingSomIkkeKommer = informasjon.getOverstyringer()
                .stream()
                .filter(ArbeidsforholdOverstyring::kreverIkkeInntektsmelding)
                .toList();

            for (var im : inntektsmeldingSomIkkeKommer) {
                fjernArbeidsforholdFraPåkrevde(påkrevdeInntektsmeldinger, im.getArbeidsgiver(), im.getArbeidsforholdRef(), prArbeidsforhold);
            }
        }
    }

    private void fjernArbeidsforholdFraPåkrevde(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef,
                                                boolean prArbeidsforhold) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return; // quick exit
        }

        if (påkrevdeInntektsmeldinger.containsKey(arbeidsgiver)) {
            var arbeidsforhold = påkrevdeInntektsmeldinger.get(arbeidsgiver);
            if (prArbeidsforhold && arbeidsforholdRef != null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
                arbeidsforhold.remove(arbeidsforholdRef);
            } else {
                arbeidsforhold.clear();
            }
            if (arbeidsforhold.isEmpty()) {
                påkrevdeInntektsmeldinger.remove(arbeidsgiver);
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
    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> søknadsFilter(BehandlingReferanse referanse,
                                                                          Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.søknadsFilter(referanse, påkrevdeInntektsmeldinger);
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> aktiveArbeidsforholdFilter(BehandlingReferanse referanse,
                                                                                      Skjæringstidspunkt stp,
                                                                                      Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger) {
        var filter = FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
                .orElseThrow(
                        () -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
        return filter.aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, påkrevdeInntektsmeldinger);
    }
}
