package no.nav.foreldrepenger.domene.arbeidsforhold;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.Ambasade;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@ApplicationScoped
public class InntektsmeldingTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;

    InntektsmeldingTjeneste() {
        // CDI-runner
    }

    @Inject
    public InntektsmeldingTjeneste(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
    }

    /**
     * Henter alle inntektsmeldinger
     * Tar hensyn til inaktive arbeidsforhold, dvs. fjerner de
     * inntektsmeldingene som er koblet til inaktivte arbeidsforhold
     *
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunktForOpptjening datoen arbeidsforhold må inkludere eller starte etter for å bli regnet som aktive
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentInntektsmeldinger(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening) {
        Long behandlingId = ref.getBehandlingId();
        AktørId aktørId = ref.getAktørId();
        return hentInntektsmeldinger(behandlingId, aktørId, skjæringstidspunktForOpptjening);
    }

    public List<Inntektsmelding> hentInntektsmeldinger(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunktForOpptjening) {
        return iayTjeneste.finnGrunnlag(behandlingId).map(g -> hentInntektsmeldinger(aktørId, skjæringstidspunktForOpptjening, g)).orElse(Collections.emptyList());
    }

    public List<Inntektsmelding> hentInntektsmeldinger(AktørId aktørId, LocalDate skjæringstidspunktForOpptjening, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<Inntektsmelding> inntektsmeldinger = iayGrunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        // kan ikke filtrere når det ikke finnes yrkesaktiviteter
        if (yrkesaktiviteter.isEmpty()) {
            return inntektsmeldinger;
        }
        return filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(filter, yrkesaktiviteter, inntektsmeldinger, skjæringstidspunktForOpptjening);
    }

    /**
     * Henter ut alle inntektsmeldinger mottatt etter gjeldende vedtak
     * Denne metoden benyttes <b>BARE</b> for revurderinger
     *
     * @param ref referanse til behandlingen
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerMottattEtterGjeldendeVedtak(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Long originalBehandlingId = ref.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Denne metoden benyttes bare for revurderinger"));

        Map<String, Inntektsmelding> revurderingIM = hentIMMedIndexKey(behandlingId);
        Map<String, Inntektsmelding> origIM = hentIMMedIndexKey(originalBehandlingId);
        return revurderingIM.entrySet().stream()
            .filter(imRevurderingEntry -> !origIM.containsKey(imRevurderingEntry.getKey())
                || !Objects.equals(origIM.get(imRevurderingEntry.getKey()).getJournalpostId(), imRevurderingEntry.getValue().getJournalpostId()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Henter alle inntektsmeldinger
     * Tar hensyn til inaktive arbeidsforhold, dvs. fjerner de
     * inntektsmeldingene som er koblet til inaktivte arbeidsforhold
     * Spesial håndtering i forbindelse med beregning
     *
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunktForOpptjening datoen arbeidsforhold må inkludere eller starte etter for å bli regnet som aktive
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentInntektsmeldingerBeregning(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening) {
        AktørId aktørId = ref.getAktørId();
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag = iayTjeneste.finnGrunnlag(ref.getBehandlingId());
        if (iayGrunnlag.isPresent()) {
            return hentInntektsmeldingerBeregning(aktørId, skjæringstidspunktForOpptjening, iayGrunnlag.get());
        }
        return emptyList();
    }

    private List<Inntektsmelding> hentInntektsmeldingerBeregning(AktørId aktørId, LocalDate skjæringstidspunktForOpptjening, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        LocalDate skjæringstidspunktMinusEnDag = skjæringstidspunktForOpptjening.minusDays(1);
        List<Inntektsmelding> inntektsmeldinger = iayGrunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();


        // kan ikke filtrere når det ikke finnes yrkesaktiviteter
        if (yrkesaktiviteter.isEmpty()) {
            return inntektsmeldinger;
        }
        return filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(filter, yrkesaktiviteter, inntektsmeldinger, skjæringstidspunktMinusEnDag);
    }

    public Optional<Inntektsmelding> hentInntektsMeldingFor(Long behandlingId, JournalpostId journalpostId) {
        var grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var inntektsmelding = grunnlag.getInntektsmeldinger().stream().flatMap(imagg -> imagg.getAlleInntektsmeldinger().stream())
            .filter(im -> Objects.equals(im.getJournalpostId(), journalpostId)).findFirst();
        return inntektsmelding;
    }

    /**
     * @deprecated fjern når RykkTilbakeTilStart#hoppTilbakeTil5080OgSlettInntektsmelding fjernes
     */
    @Deprecated
    public void fjernInntektsmelding(Long behandlingId, Set<JournalpostId> fjernInntektsmeldinger) {
        iayTjeneste.dropInntektsmeldinger(behandlingId, fjernInntektsmeldinger);
    }

    /**
     * Henter kombinasjon av arbeidsgiver + arbeidsforholdRef
     * på de det ikke vil komme inn inntektsmelding for.
     *
     * @param behandlingId iden til behandlingen
     * @return Liste med inntektsmelding som ikke kommer {@link InntektsmeldingSomIkkeKommer}
     */
    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(Long behandlingId) {
        List<InntektsmeldingSomIkkeKommer> result = new ArrayList<>();
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        inntektArbeidYtelseGrunnlag.ifPresent(iayg -> result.addAll(iayg.getInntektsmeldingerSomIkkeKommer()));
        return result;
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til angitte behandlinger
     * <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen er knyttet til et inaktivt arbeidsforhold
     *
     * @param behandlingIder
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<Inntektsmelding> hentAlleInntektsmeldingerForAngitteBehandlinger(Set<Long> behandlingIder) {
        return hentUtAlleInntektsmeldingeneFraBehandlingene(behandlingIder);
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til fagsaken på alle behandlinger, uavhengig av status
     * <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen er knyttet til et inaktivt arbeidsforhold
     *
     * @param saksnummer som gjelder fagsaken
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    private List<Inntektsmelding> hentAlleInntektsmeldingerForFagsak(Saksnummer saksnummer) {
        return List.copyOf(iayTjeneste.hentUnikeInntektsmeldingerForSak(saksnummer));
    }

    /**
     * Henter ut alle datoer for innsending av refusjonskrav og første gyldige refusjonskrav for alle inntektsmeldinger koblet til fagsaken på alle behandlinger, uavhengig av status
     * <br>
     * <b>NB!</b> Tar ikke hensyn til om inntektsmeldingen for det aktuelle refusjonskravet er knyttet til et inaktivt arbeidsforhold
     *
     * @param saksnummer som gjelder fagsaken
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    public List<RefusjonskravDato> hentAlleRefusjonskravDatoerForFagsak(Saksnummer saksnummer) {
        return List.copyOf(iayTjeneste.hentRefusjonskravDatoerForSak(saksnummer));
    }

    /**
     * Henter ut alle datoer for refusjon og innsendelse av refusjonskrav koblet til fagsaken, også de på inaktive grunnlag.
     *
     * @param saksnummer Saksnummer til fagsak
     * @return Map med refusjonskravdatoer per arbeidsgiver
     */
    public Map<Arbeidsgiver, List<RefusjonskravDato>> hentAlleRefusjonskravdatoerForFagsakInkludertInaktive(Saksnummer saksnummer) {
        List<RefusjonskravDato> alleRefusjonskravDatoer = hentAlleRefusjonskravDatoerForFagsak(saksnummer);

        return alleRefusjonskravDatoer.stream()
            .collect(Collectors.groupingBy(RefusjonskravDato::getArbeidsgiver));
    }

    /**
     * Henter ut alle inntektsmeldinger koblet til fagsaken, også de på inaktive grunnlag.
     *
     * @param saksnummer Saksnummer til fagsak
     * @return Map med inntektsmeldinger per arbeidsgiver
     */
    public Map<Arbeidsgiver, List<Inntektsmelding>> hentAlleInntektsmeldingerForFagsakInkludertInaktive(Saksnummer saksnummer) {
        List<Inntektsmelding> alleInntektsmeldinger = hentAlleInntektsmeldingerForFagsak(saksnummer);

        return alleInntektsmeldinger.stream()
            .collect(Collectors.groupingBy(Inntektsmelding::getArbeidsgiver));
    }

    public void lagreInntektsmelding(Saksnummer saksnummer, Long behandlingId, InntektsmeldingBuilder im) {
        iayTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, List.of(im));
    }

    /**
     * Filtrer vekk inntektsmeldinger som er knyttet til et arbeidsforhold som har en tom dato som slutter før STP.
     */
    private static List<Inntektsmelding> filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                     Collection<Inntektsmelding> inntektsmeldinger,
                                                                                     LocalDate skjæringstidspunktet) {
        ArrayList<Inntektsmelding> kladd = new ArrayList<>(inntektsmeldinger);
        List<Inntektsmelding> fjernes = new ArrayList<>();

        kladd.forEach(im -> {
            boolean arbeidsgiverHarVærtRegistrertIOpplysningsperioden = yrkesaktiviteter.stream()
                .anyMatch(y -> y.gjelderFor(im.getArbeidsgiver(), InternArbeidsforholdRef.nullRef()));
            boolean skalFjernes = yrkesaktiviteter.stream()
                .noneMatch(y -> {
                    boolean gjelderFor = y.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef());
                    var ansettelsesPerioder = filter.getAnsettelsesPerioder(y);
                    return gjelderFor && ansettelsesPerioder.stream()
                        .anyMatch(ap -> ap.getPeriode().inkluderer(skjæringstidspunktet) || ap.getPeriode().getTomDato().isAfter(skjæringstidspunktet));
                });
            if (skalFjernes && !erAmbasade(im) && arbeidsgiverHarVærtRegistrertIOpplysningsperioden) {
                fjernes.add(im);
            }
        });
        kladd.removeAll(fjernes);
        return List.copyOf(kladd);
    }

    private static boolean erAmbasade(Inntektsmelding im) {
        return im.getArbeidsgiver().getErVirksomhet() && Ambasade.erAmbasade(im.getArbeidsgiver().getOrgnr());
    }

    private Map<String, Inntektsmelding> hentIMMedIndexKey(Long behandlingId) {
        List<Inntektsmelding> inntektsmeldinger = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(Collections.emptyList());

        return inntektsmeldinger.stream()
            .collect(Collectors.toMap(im -> im.getIndexKey(), im -> im));
    }

    private List<Inntektsmelding> hentUtAlleInntektsmeldingeneFraBehandlingene(Collection<Long> behandlingIder) {
        // FIXME (FC) denne burde gått rett på datalagret istd. å iterere over åpne behandlinger
        List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();
        for (Long behandlingId : behandlingIder) {
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
}
