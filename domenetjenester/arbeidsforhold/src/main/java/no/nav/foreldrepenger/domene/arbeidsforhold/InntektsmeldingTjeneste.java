package no.nav.foreldrepenger.domene.arbeidsforhold;

import static java.util.Collections.emptyList;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.Ambasade;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ApplicationScoped
public class InntektsmeldingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;

    InntektsmeldingTjeneste() {
        // CDI-runner
    }

    @Inject
    public InntektsmeldingTjeneste(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
    }

    /**
     * Henter alle inntektsmeldinger Tar hensyn til inaktive arbeidsforhold, dvs.
     * fjerner de inntektsmeldingene som er koblet til inaktivte arbeidsforhold
     *
     * @param ref                             {@link BehandlingReferanse}
     * @param skjæringstidspunktForOpptjening datoen arbeidsforhold må inkludere
     *                                        eller starte etter for å bli regnet
     *                                        som aktive
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentInntektsmeldinger(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening) {
        return iayTjeneste.finnGrunnlag(ref.behandlingId())
            .map(g -> hentInntektsmeldinger(ref, skjæringstidspunktForOpptjening, g, true))
            .orElse(Collections.emptyList());
    }

    public List<Inntektsmelding> hentInntektsmeldinger(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening,
                                                       InntektArbeidYtelseGrunnlag iayGrunnlag, boolean filtrerForStartdato) {
        var skalIkkeFiltrereStartdato = !filtrerForStartdato ||
            !FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) ||
            skjæringstidspunktForOpptjening == null ||
            UtsettelseCore2021.kreverSammenhengendeUttak(skjæringstidspunktForOpptjening);
        var datoFilterDato = Optional.ofNullable(skjæringstidspunktForOpptjening).orElseGet(LocalDate::now);
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).orElse(emptyList())
            .stream().filter(im -> skalIkkeFiltrereStartdato || kanInntektsmeldingBrukesForSkjæringstidspunkt(im, skjæringstidspunktForOpptjening)).toList();

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(ref.aktørId()));
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        // kan ikke filtrere når det ikke finnes yrkesaktiviteter
        if (yrkesaktiviteter.isEmpty()) {
            return inntektsmeldinger;
        }
        return filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(filter, yrkesaktiviteter, inntektsmeldinger, datoFilterDato, iayGrunnlag.getGjeldendeOppgittOpptjening());
    }

    /**
     * Henter kombinasjon av arbeidsgiver + arbeidsforholdRef på de det ikke vil
     * komme inn inntektsmelding for.
     *
     * @param behandlingId iden til behandlingen
     * @return Liste med inntektsmelding som ikke kommer
     *         {@link InntektsmeldingSomIkkeKommer}
     */
    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(Long behandlingId) {
        List<InntektsmeldingSomIkkeKommer> result = new ArrayList<>();
        var inntektArbeidYtelseGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        inntektArbeidYtelseGrunnlag.ifPresent(iayg -> result.addAll(iayg.getInntektsmeldingerSomIkkeKommer()));
        return result;
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til angitte behandlinger <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen er knyttet til et
     * inaktivt arbeidsforhold
     *
     * @param behandlingIder
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerForAngitteBehandlinger(Set<Long> behandlingIder) {
        return hentUtAlleInntektsmeldingeneFraBehandlingene(behandlingIder);
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til fagsaken på alle behandlinger,
     * uavhengig av status <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen er knyttet til et
     * inaktivt arbeidsforhold
     *
     * @param saksnummer som gjelder fagsaken
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerForFagsak(Saksnummer saksnummer) {
        return List.copyOf(iayTjeneste.hentUnikeInntektsmeldingerForSak(saksnummer));
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til fagsaken, også de på inaktive
     * grunnlag.
     *
     * @param saksnummer Saksnummer til fagsak
     * @return Map med inntektsmeldinger per arbeidsgiver
     */
    public Map<Arbeidsgiver, List<Inntektsmelding>> hentAlleInntektsmeldingerForFagsakInkludertInaktive(Saksnummer saksnummer) {
        var alleInntektsmeldinger = hentAlleInntektsmeldingerForFagsak(saksnummer);

        return alleInntektsmeldinger.stream()
                .collect(Collectors.groupingBy(Inntektsmelding::getArbeidsgiver));
    }

    public void lagreInntektsmelding(InntektsmeldingBuilder im, Behandling behandling) {
        var saksnummer = behandling.getSaksnummer();
        iayTjeneste.lagreInntektsmeldinger(saksnummer, behandling.getId(), List.of(im));
    }

    /**
     * Filtrer vekk inntektsmeldinger som er knyttet til et arbeidsforhold som har
     * en tom dato som slutter før STP.
     */
    private static List<Inntektsmelding> filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(YrkesaktivitetFilter filter,
            Collection<Yrkesaktivitet> yrkesaktiviteter,
            Collection<Inntektsmelding> inntektsmeldinger,
            LocalDate skjæringstidspunktet,
            Optional<OppgittOpptjening> oppgittOpptjening) {
        var kladd = new ArrayList<>(inntektsmeldinger);
        List<Inntektsmelding> fjernes = new ArrayList<>();

        kladd.forEach(im -> {
            var arbeidsgiverHarVærtRegistrertIOpplysningsperioden = yrkesaktiviteter.stream()
                    .anyMatch(y -> y.gjelderFor(im.getArbeidsgiver(), InternArbeidsforholdRef.nullRef()));

            // Hvis det er opprettet et arbeidsforhold basert på inntektsmeldingen skal aldri dette gjøre at inntektsmeldingen filtreres ut
            var finnesManueltArbeidsforholdTilknyttetIM = finnesManueltOpprettetArbeidsforholdForIM(filter.getArbeidsforholdOverstyringer(), im);

            var skalFjernes = yrkesaktiviteter.stream()
                    .noneMatch(y -> {
                        var gjelderFor = y.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef());
                        var ansettelsesPerioder = filter.getAnsettelsesPerioder(y);
                        return gjelderFor && ansettelsesPerioder.stream()
                                .anyMatch(ap -> ap.getPeriode().inkluderer(skjæringstidspunktet)
                                        || ap.getPeriode().getTomDato().isAfter(skjæringstidspunktet));
                    });
            if (skalFjernes && !erAmbasade(im) && !harOppgittFiske(oppgittOpptjening) && arbeidsgiverHarVærtRegistrertIOpplysningsperioden && !finnesManueltArbeidsforholdTilknyttetIM) {
                fjernes.add(im);
            }
        });
        kladd.removeAll(fjernes);
        return List.copyOf(kladd);
    }

    private static boolean finnesManueltOpprettetArbeidsforholdForIM(Collection<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, Inntektsmelding im) {
        return arbeidsforholdOverstyringer.stream()
            .anyMatch(os -> ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING.equals(os.getHandling())
                && os.getArbeidsgiver() != null && os.getArbeidsgiver().equals(im.getArbeidsgiver()) && os.getArbeidsforholdRef().gjelderFor(im.getArbeidsforholdRef()));
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
     * @param oppgittOpptjening Oppgitt opptjening
     * @return Har bruker oppgitt fikse i søknaden
     */
    private static boolean harOppgittFiske(Optional<OppgittOpptjening> oppgittOpptjening) {
        return oppgittOpptjening.stream()
                .anyMatch(oo -> oo.getEgenNæring().stream().anyMatch(en -> en.getVirksomhetType().equals(VirksomhetType.FISKE)));
    }

    private static boolean erAmbasade(Inntektsmelding im) {
        return im.getArbeidsgiver().getErVirksomhet() && Ambasade.erAmbasade(im.getArbeidsgiver().getOrgnr());
    }

    private List<Inntektsmelding> hentUtAlleInntektsmeldingeneFraBehandlingene(Collection<Long> behandlingIder) {
        // FIXME (FC) denne burde gått rett på datalagret istd. å iterere over åpne behandlinger
        List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();
        for (var behandlingId : behandlingIder) {
            inntektsmeldinger.addAll(hentAlleInntektsmeldinger(behandlingId));
        }
        return inntektsmeldinger;
    }

    private List<Inntektsmelding> hentAlleInntektsmeldinger(Long behandlingId) {
        return iayTjeneste.finnGrunnlag(behandlingId)
                .map(iayGrunnlag -> iayGrunnlag.getInntektsmeldinger()
                        .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).orElse(emptyList()))
                .orElse(emptyList());
    }

    private boolean kanInntektsmeldingBrukesForSkjæringstidspunkt(Inntektsmelding inntektsmelding, LocalDate skjæringstidspunkt) {
        if (skjæringstidspunkt == null) {
            return true; // Ikke definert skjæringstidspunkt så alle inntektsmeldinger er kandidater inntil videre
        }
        var tidligsteDato = skjæringstidspunkt.minusWeeks(4).minusDays(1);
        var sisteBeregningMåned = YearMonth.from(skjæringstidspunkt.minusMonths(1));
        var imdato = inntektsmelding.getInnsendingstidspunkt().toLocalDate();
        var imdatoMåned = YearMonth.from(imdato);
        var ferskNok = imdato.isAfter(tidligsteDato) ||
            YearMonth.from(tidligsteDato).equals(imdatoMåned) ||
            sisteBeregningMåned.equals(imdatoMåned);
        if (ferskNok) {
            return true;
        }
        // TODO Er denne nødvendig gitt det over? Sjekke logger etter noen uker
        // Obligatorisk Startdato innfases fram mot sommer 2022. Unntak er hvis begrunnelseForReduksjonEllerIkkeUtbetalt = IkkeFravær
        // Perioder (samme måned eller 2 uker) som godtas bør matche DokumentmottakerFelles . endringSomUtsetterStartdato()
        var startdatoBetraktning = inntektsmelding.getStartDatoPermisjon().isEmpty() ||
            inntektsmelding.getStartDatoPermisjon()
                .filter(s -> s.isAfter(skjæringstidspunkt.minusDays(15)) || YearMonth.from(s).equals(YearMonth.from(skjæringstidspunkt)))
                .isPresent();
        if (startdatoBetraktning) {
            LOG.info("Inntektsmelding: passerte ikke ferskNok, men OK startdatobetraktning");
        }
        return startdatoBetraktning;
    }
}
