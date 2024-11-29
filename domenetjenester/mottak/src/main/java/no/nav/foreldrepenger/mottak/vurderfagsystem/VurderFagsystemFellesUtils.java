package no.nav.foreldrepenger.mottak.vurderfagsystem;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VurderFagsystemFellesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemFellesUtils.class);

    private static final Period MAKS_AVVIK_DAGER_IM_INPUT = Period.of(0,3,1);
    private static final Period OPPLYSNINGSPLIKT_INTERVALL = Period.of(0,2,1);
    private static final Period PERIODE_FOR_AKTUELLE_SAKER = Period.ofMonths(10);
    private static final Period PERIODE_FOR_AKTUELLE_SAKER_IM = Period.ofMonths(10);
    private static final Period PERIODE_FOR_MULIGE_SAKER_IM = Period.ofMonths(14);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public VurderFagsystemFellesUtils(){
        //Injected normal scoped bean is now proxyable
    }

    @Inject
    public VurderFagsystemFellesUtils(BehandlingRepositoryProvider repositoryProvider,
                                      FamilieHendelseTjeneste familieHendelseTjeneste,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                      InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                      SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                      FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public List<Fagsak> filtrerSakerForBehandlingTema(List<Fagsak> saker, BehandlingTema behandlingTema) {
        if (BehandlingTema.ikkeSpesifikkHendelse(behandlingTema)) {
            return saker;
        }
        return saker.stream()
            .filter(s -> behandlingTema.erKompatibelMed(this.getBehandlingsTemaForFagsak(s)))
            .toList();
    }

    public List<Fagsak> finnÅpneSaker(List<Fagsak> saker) {
        return saker.stream()
            .filter(Fagsak::erÅpen)
            .filter(s -> FagsakStatus.LØPENDE.equals(s.getStatus()) || harÅpenYtelsesBehandling(s))
            .toList();
    }

    private boolean harÅpenYtelsesBehandling(Fagsak fagsak) {
        return !behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).isEmpty();
    }

    private boolean harÅpenEllerNyligAvsluttetKlageEllerAnkeBehandlingKlageinstans(Fagsak fagsak) {
        var klageinstansEnhet = BehandlendeEnhetTjeneste.getKlageInstans().enhetId();
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()).stream()
            .filter(b -> BehandlingType.KLAGE.equals(b.getType()) || BehandlingType.ANKE.equals(b.getType()))
            .filter(b -> klageinstansEnhet.equals(b.getBehandlendeEnhet()))
            .anyMatch(b -> !b.erAvsluttet() || b.getAvsluttetDato().isAfter(LocalDateTime.now().minusMonths(2)));
    }

    private List<Fagsak> harSakMedAvslagGrunnetManglendeDok(List<Fagsak> saker) {
        return saker.stream()
            .filter(s -> mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(s) && !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(s))
            .toList();
    }

    public Optional<FamilieHendelseEntitet> finnGjeldendeFamilieHendelseSVP(Fagsak fagsak) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(behandling -> familieHendelseTjeneste.finnAggregat(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon));
    }

    public boolean erBehandlingAvsluttetFørOpplysningspliktIntervall(Behandling behandling) {
        return behandling.getAvsluttetDato() != null && behandling.getAvsluttetDato().isAfter(LocalDateTime.now().minus(OPPLYSNINGSPLIKT_INTERVALL));
    }

    public List<Fagsak> sakerOpprettetInnenIntervall(List<Fagsak> sakerGittYtelseType) {
        var sammenlign = LocalDateTime.now().minus(PERIODE_FOR_AKTUELLE_SAKER);
        return sakerGittYtelseType.stream()
            .filter(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign))
            .toList();
    }

    public List<Saksnummer> sakerOpprettetInnenTvilsintervall(List<Fagsak> sakerGittYtelseType) {
        var sammenlign = LocalDateTime.now().minus(PERIODE_FOR_MULIGE_SAKER_IM);
        return sakerGittYtelseType.stream()
            .filter(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign))
            .map(Fagsak::getSaksnummer)
            .toList();
    }

    public boolean harSakOpprettetInnenIntervallForIM(List<Fagsak> sakerGittYtelseType, LocalDate referanseDato) {
        var sammenlign = referanseDato.atStartOfDay().minus(PERIODE_FOR_AKTUELLE_SAKER_IM);
        return sakerGittYtelseType.stream()
            .anyMatch(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign));
    }

    public boolean harBehandlingTilkjentRundtIM(Behandling behandling, LocalDate referanseDato) {
        var tilkjent = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of());
        var min = tilkjent.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE).minusWeeks(3);
        var max = tilkjent.stream().filter(b -> b.getDagsats() > 0).map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder()).orElse(Tid.TIDENES_BEGYNNELSE);
        return referanseDato.isAfter(min) && referanseDato.isBefore(max);
    }

    public boolean erÅpenBehandlingMedSøknadRundtIM(Behandling behandling, LocalDate referanseDato) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            return false;
        }
        var perioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getOppgittFordeling)
            .map(OppgittFordelingEntitet::getPerioder).orElse(List.of()).stream()
            .filter(p -> !p.isOpphold() && !(p.isUtsettelse() && UtsettelseÅrsak.FRI.equals(p.getÅrsak())))
            .toList();
        var min = perioder.stream().map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE).minusWeeks(3);
        var max = perioder.stream().map(OppgittPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder()).orElse(Tid.TIDENES_BEGYNNELSE);
        return referanseDato.isAfter(min) && referanseDato.isBefore(max);
    }

    public boolean erFagsakMedFamilieHendelsePassendeForSøknadFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Finn behandling
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(this::hentAktuellFamilieHendelse)
            .map(g -> erGrunnlagPassendeFor(g, fagsak.getYtelseType(), vurderFagsystem)).orElse(false);
    }

    public boolean erFagsakPassendeForSøknadFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak, boolean vurderHenlagte) {
        // Vurder omskriving av denne og neste til Predicate<Fagsak> basert på bruksmønster
        // Finn behandling
        var behandling = vurderHenlagte ? behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()) :
            behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId());
        if (behandling.isEmpty()) {
            return true;
        }

        return behandling
            .flatMap(this::hentAktuellFamilieHendelse)
            .map(g -> erGrunnlagPassendeFor(g, fagsak.getYtelseType(), vurderFagsystem))
            // Her har vi en sak m/behandling uten FH - 3 hovedtilfelle uregistrert papirsøknad, im før søknad.
            // Innkommende kan være søknad, IM, eller ustrukturert For ES godtar man alt.
            .orElseGet(() -> kanFagsakUtenGrunnlagBrukesForDokument(vurderFagsystem, behandling.get()));
    }

    /*
     * Har fagsak en inntektsmelding, men ingen søknad eller familiehendelse?
     */
    public boolean erFagsakBasertPåInntektsmeldingUtenSøknad(Fagsak fagsak) {
        // Sjekk om sak er basert på innsendt inntektsmelding, ikke har søknader/familiehendelse.
        var harSøknad = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsak.getId()).stream().anyMatch(MottattDokument::erSøknadsDokument);
        var harFamilieHendelse = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(this::hentAktuellFamilieHendelse)
            .or(() -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).flatMap(this::hentAktuellFamilieHendelse))
            .isPresent();
        var harMottattInntektsmelding = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsak.getId()).stream()
            .anyMatch(d -> DokumentTypeId.INNTEKTSMELDING.equals(d.getDokumentType()));
        return !harSøknad && !harFamilieHendelse && harMottattInntektsmelding;
    }

    /**
     * Scenario: Det finnes en henlagt sak opprettet basert på innsent inntektsmelding - uten søknad i saken
     * Logikk: bruk saken dersom IM er innsendt mindre enn 3 siden (vanlig slingringsmonn) - legg på 3 ekstra måneder søknadsfrist ift startdato
     */
    public boolean kanFagsakBasertPåInntektsmeldingBrukesForSøknad(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        var idagMinusSøknadsfrist = LocalDate.now().minus(MAKS_AVVIK_DAGER_IM_INPUT);
        var referansedato = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding()
            .filter(sd -> sd.isBefore(idagMinusSøknadsfrist)).orElse(idagMinusSøknadsfrist)
            .minus(MAKS_AVVIK_DAGER_IM_INPUT);
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsakInkludertInaktive(fagsak.getSaksnummer()).values().stream()
            .flatMap(Collection::stream)
            .anyMatch(i -> i.getInnsendingstidspunkt().toLocalDate().isAfter(referansedato));
    }

    private boolean erGrunnlagPassendeFor(FamilieHendelseGrunnlagEntitet grunnlag, FagsakYtelseType ytelseType, VurderFagsystem vurderFagsystem) {
        var fhType = grunnlag.getGjeldendeVersjon().getType();
        var bhTemaFagsak = BehandlingTema.fraFagsakHendelse(ytelseType, fhType);
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }
        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderFødsel(fhType)) {
            return familieHendelseTjeneste.matcherFødselsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getBarnTermindato().orElse(null), vurderFagsystem.getBarnFodselsdato().orElse(null));
        }
        if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return familieHendelseTjeneste.matcherOmsorgsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getOmsorgsovertakelsedato().orElse(null), vurderFagsystem.getAdopsjonsbarnFodselsdatoer());
        }
        return false;
    }

    public boolean erFagsakMedAnnenFamilieHendelseEnnSøknadFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Finn behandling
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(this::hentAktuellFamilieHendelse)
            .map(g -> gjelderGrunnlagAnnenFamiliehendelse(g, fagsak.getYtelseType(), vurderFagsystem))
            .orElse(false);
    }

    private boolean gjelderGrunnlagAnnenFamiliehendelse(FamilieHendelseGrunnlagEntitet grunnlag, FagsakYtelseType ytelseType, VurderFagsystem vurderFagsystem) {
        var fhType = grunnlag.getGjeldendeVersjon().getType();
        var bhTemaFagsak = BehandlingTema.fraFagsakHendelse(ytelseType, fhType);
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }
        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderFødsel(fhType)) {
            return !familieHendelseTjeneste.matcherFødselsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getBarnTermindato().orElse(null), vurderFagsystem.getBarnFodselsdato().orElse(null));
        }
        if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return !familieHendelseTjeneste.matcherOmsorgsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getOmsorgsovertakelsedato().orElse(null), vurderFagsystem.getAdopsjonsbarnFodselsdatoer());
        }
        return false;
    }


    public BehandlendeFagsystem vurderAktuelleFagsakerForInntektsmeldingFP(VurderFagsystem vurderFagsystem, List<Fagsak> saker) {
        var startdatoIM = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding().orElse(null);
        if (startdatoIM == null) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        // Sorter saker etter om de har søknad, tidligere IM eller ikke + matching. Evaluer i rekkefølge
        var sorterteSaker = saker.stream()
            .collect(Collectors.groupingBy(f -> sorterFagsakForStartdatoIM(f, startdatoIM)));

        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_DATO_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.GRUNNLAG_DATO_MATCH), SorteringSaker.GRUNNLAG_DATO_MATCH, vurderFagsystem);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_MULIG_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.GRUNNLAG_MULIG_MATCH), SorteringSaker.GRUNNLAG_MULIG_MATCH, vurderFagsystem);
        }
        var tvilssaker = sakerOpprettetInnenTvilsintervall(sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_MISMATCH, List.of()));
        if (!tvilssaker.isEmpty()) {
            LOG.info("VurderFagsystem FP IM {} manuell pga nylige saker av type {} startdatoIM {} saker {}", vurderFagsystem.getJournalpostIdLog(), SorteringSaker.GRUNNLAG_MISMATCH, startdatoIM, tvilssaker);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_DATO_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.INNTEKTSMELDING_DATO_MATCH), SorteringSaker.INNTEKTSMELDING_DATO_MATCH, vurderFagsystem);
        }
        var tvilssakerIM = sakerOpprettetInnenTvilsintervall(sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_MISMATCH, List.of()));
        if (!tvilssakerIM.isEmpty()) {
            LOG.info("VurderFagsystem FP IM {} manuell pga nylige saker av type {} startdatoIM {} im-saker {}", vurderFagsystem.getJournalpostIdLog(), SorteringSaker.INNTEKTSMELDING_MISMATCH, startdatoIM, tvilssakerIM);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return sorterteSaker.getOrDefault(SorteringSaker.TOM_SAK, List.of()).isEmpty() ?  new BehandlendeFagsystem(VEDTAKSLØSNING) :
            new BehandlendeFagsystem(VEDTAKSLØSNING, sorterteSaker.get(SorteringSaker.TOM_SAK).get(0).getSaksnummer());
    }

    private BehandlendeFagsystem behandlendeFagsystemFraFagsaker(List<Fagsak> saker, SorteringSaker sortering, VurderFagsystem vurderFagsystem) {
        if (saker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, saker.get(0).getSaksnummer());
        }
        if (saker.size() > 1) {
            LOG.info("VurderFagsystem FP IM {} manuell pga flere saker av type {}", vurderFagsystem.getJournalpostIdLog(), sortering);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        throw new IllegalArgumentException("Utviklerfeil skal ikke kalles med tom liste");
    }

    private SorteringSaker sorterFagsakForStartdatoIM(Fagsak fagsak, LocalDate startdatoIM) {
        var sisteBehandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).stream().findFirst()
            .orElseGet(() -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null));
        if (sisteBehandling == null) {
            return SorteringSaker.TOM_SAK;
        }
        var fhGrunnlag = familieHendelseTjeneste.finnAggregat(sisteBehandling.getId());
        if (fhGrunnlag.isEmpty()) {
            var alleInntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsakInkludertInaktive(fagsak.getSaksnummer());
            var match = alleInntektsmeldinger.values().stream().flatMap(Collection::stream).map(Inntektsmelding::getStartDatoPermisjon).flatMap(Optional::stream)
                .anyMatch(d -> startdatoIM.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(d) && startdatoIM.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(d));
            if (match) {
                return SorteringSaker.INNTEKTSMELDING_DATO_MATCH;
            }
            return alleInntektsmeldinger.isEmpty() ?  SorteringSaker.TOM_SAK : SorteringSaker.INNTEKTSMELDING_MISMATCH;
        }
        // Her har vi registrert søknad. Vurder å sjekke tilkjent framfor skjæringstidspunkter (som kan gi exception ved totalopphør)
        try {
            var førsteDagBehandling = skjæringstidspunktTjeneste.getSkjæringstidspunkter(sisteBehandling.getId()).getFørsteUttaksdato();
            var referanseDatoForSaker = LocalDate.now().isBefore(startdatoIM) ? LocalDate.now() : startdatoIM;
            if (førsteDagBehandling.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(startdatoIM) && førsteDagBehandling.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(startdatoIM)) {
                return SorteringSaker.GRUNNLAG_DATO_MATCH;
            }
            var innvilgetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
            if (fagsak.erÅpen() && innvilgetBehandling != null && harBehandlingTilkjentRundtIM(innvilgetBehandling, referanseDatoForSaker)) {
                return SorteringSaker.GRUNNLAG_MULIG_MATCH;
            }
            if (erÅpenBehandlingMedSøknadRundtIM(sisteBehandling, referanseDatoForSaker)) {
                return SorteringSaker.GRUNNLAG_MULIG_MATCH;
            }
            if (fagsak.erÅpen() && harSakOpprettetInnenIntervallForIM(List.of(fagsak), referanseDatoForSaker)
                || erBehandlingAvsluttetFørOpplysningspliktIntervall(sisteBehandling)) {
                return SorteringSaker.GRUNNLAG_MULIG_MATCH;
            }
            return SorteringSaker.GRUNNLAG_MISMATCH;
        } catch (Exception e) {
            return SorteringSaker.GRUNNLAG_MISMATCH;
        }
    }

    /**
     * Scenario: Fagsak vurdert mangler FHgrunnlag (papirsøknad, IM, infobrev) mens incoming er søknad, im, ustrukturert
     * Logikk: bruk saken med mindre det allerede ligger IM med helt annen startdato der ift innkommende søknad/im
     */
    private boolean kanFagsakUtenGrunnlagBrukesForDokument(VurderFagsystem vurderFagsystem, Behandling sisteBehandling) {
        var innkommendeReferanseDato = getReferanseDatoFraInnkommendeForVurdering(vurderFagsystem);
        var alleInntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsakInkludertInaktive(sisteBehandling.getSaksnummer());
        if (alleInntektsmeldinger.isEmpty()) {
            // Unngå gjenbruk av flere år gamle saker
            var behandlingRefDato = Optional.ofNullable(sisteBehandling.getAvsluttetDato()).map(LocalDateTime::toLocalDate).orElseGet(LocalDate::now);
            return behandlingRefDato.isAfter(innkommendeReferanseDato.minus(PERIODE_FOR_MULIGE_SAKER_IM));
        }
        var startdato = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding();
        if (startdato.isPresent()) {
            var oppgittFørsteDag = startdato.get();
            return alleInntektsmeldinger.values().stream().flatMap(Collection::stream)
                .map(im -> im.getStartDatoPermisjon().orElseGet(() -> im.getInnsendingstidspunkt().toLocalDate()))
                .anyMatch(d -> oppgittFørsteDag.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(d) && oppgittFørsteDag.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(d));
        } else {
            return alleInntektsmeldinger.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(i -> i.getStartDatoPermisjon().orElseGet(() -> i.getInnsendingstidspunkt().toLocalDate()).isAfter(innkommendeReferanseDato.minus(PERIODE_FOR_MULIGE_SAKER_IM)));
        }
    }

    private LocalDate getReferanseDatoFraInnkommendeForVurdering(VurderFagsystem vurderFagsystem) {
        // getStartDatoForeldrepengerInntektsmelding er satt for innkommende IM og Søknad/FP. Ikke for Søknad/SVP (ennå)
        var startdato = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding();
        if (startdato.isPresent()) {
            return startdato.get();
        }
        var idag = LocalDate.now();
        var referansedato = vurderFagsystem.getBarnFodselsdato()
            .or(vurderFagsystem::getBarnTermindato)
            .or(vurderFagsystem::getOmsorgsovertakelsedato)
            .orElse(idag);
        return  idag.isBefore(referansedato) ? idag : referansedato;
    }

    public Optional<BehandlendeFagsystem> standardUstrukturertDokumentVurdering(List<Fagsak> sakerTilVurdering) {
        var åpneFagsaker = finnÅpneSaker(sakerTilVurdering);
        if (åpneFagsaker.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING, åpneFagsaker.get(0).getSaksnummer()));
        }
        if (åpneFagsaker.size() > 1) {
            LOG.info("VurderFagsystem ustrukturert dokument flere åpne saker {}", åpneFagsaker.stream().map(Fagsak::getSaksnummer).toList());
            return Optional.of(new BehandlendeFagsystem(MANUELL_VURDERING));
        }
        var avslagDokumentasjon = harSakMedAvslagGrunnetManglendeDok(sakerTilVurdering);
        if (avslagDokumentasjon.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING, avslagDokumentasjon.get(0).getSaksnummer()));
        }
        return Optional.empty();
    }

    public Optional<BehandlendeFagsystem> klageinstansUstrukturertDokumentVurdering(List<Fagsak> sakerTilVurdering) {
        var åpneFagsaker = sakerTilVurdering.stream()
            .filter(this::harÅpenEllerNyligAvsluttetKlageEllerAnkeBehandlingKlageinstans)
            .toList();
        return åpneFagsaker.size() != 1 ? Optional.empty() :
            Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING, åpneFagsaker.get(0).getSaksnummer()));
    }

    public Optional<BehandlendeFagsystem> vurderFagsystemKlageAnke(BehandlingTema behandlingTema, List<Fagsak> sakerTilVurdering) {
        // Ruter inn på sak med nyeste vedtaksdato
        var behandlinger = sakerTilVurdering.stream()
            .filter(f -> BehandlingTema.UDEFINERT.equals(behandlingTema) || BehandlingTema.gjelderSammeYtelse(behandlingTema, BehandlingTema.fraFagsak(f, null)))
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(Objects::nonNull)
            .filter(b -> behandlingVedtakRepository.hentForBehandling(b.getId()).getVedtakstidspunkt().isAfter(LocalDateTime.now().minusYears(2)))
            .toList();
        if (behandlinger.isEmpty() && !sakerTilVurdering.isEmpty() && behandlingTema != null && !BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            // Det var oppgitt et behandlingtema men vi fant ingen passende saker som matchet oppgitt behandlingtema. Sjekker derfor alle saker
            behandlinger = sakerTilVurdering.stream()
                .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
                .filter(Objects::nonNull)
                .filter(b -> behandlingVedtakRepository.hentForBehandling(b.getId()).getVedtakstidspunkt().isAfter(LocalDateTime.now().minusYears(2)))
                .toList();
        }

        var sakerMedKjenteNyereKlager = sakerTilVurdering.stream()
            .flatMap(f -> behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(f.getId(), BehandlingType.KLAGE).stream())
            .filter(Objects::nonNull)
            .filter(b -> b.getAvsluttetDato() == null || b.getAvsluttetDato().isAfter(LocalDateTime.now().minusYears(2)))
            .map(b -> b.getSaksnummer())
            .collect(Collectors.toSet());

        // Først sjekk om det finnes saker med diffus klagehistorikk
        if (sakerMedKjenteNyereKlager.size() > 1) {
            LOG.info("VFS-KLAGE flere klager i VL saksnummer {}", sakerMedKjenteNyereKlager);
            return Optional.empty();
        } else if (sakerMedKjenteNyereKlager.size() == 1 && (behandlinger.isEmpty() || behandlinger.stream().noneMatch(b -> sakerMedKjenteNyereKlager.contains(b.getSaksnummer())))) {
            LOG.info("VFS-KLAGE eldre klage i VL saksnummer {}", sakerMedKjenteNyereKlager);
            return Optional.empty();
        }

        if (behandlinger.isEmpty() && sakerTilVurdering.isEmpty()) {
            LOG.info("VFS-KLAGE ingen saker i VL");
        } else if (behandlinger.isEmpty()) {
            var saker = sakerTilVurdering.stream().map(Fagsak::getSaksnummer).toList();
            var sakerMedÅpenBehandling = sakerTilVurdering.stream().filter(f -> !behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(f.getId()).isEmpty()).count();
            LOG.info("VFS-KLAGE ingen vedtak - antall saker {} saker med åpen behandling {} saksnummer {}", sakerTilVurdering.size(), sakerMedÅpenBehandling, saker);
        } else if (behandlinger.size() > 1) {
            var sistOpprettetSakMinusÅr = behandlinger.stream().map(b -> b.getFagsak().getOpprettetTidspunkt())
                .max(Comparator.naturalOrder()).orElseGet(LocalDateTime::now).minusMonths(12);
            var fagsakerSisteÅret = behandlinger.stream().map(Behandling::getFagsak).filter(f -> f.getOpprettetTidspunkt().isAfter(sistOpprettetSakMinusÅr)).toList();
            if (fagsakerSisteÅret.size() == 1) { // Første element i fordelingslogikk klage
                return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING, fagsakerSisteÅret.get(0).getSaksnummer()));
            }
            var saker = sakerTilVurdering.stream().map(Fagsak::getSaksnummer).toList();
            var sakerMedKlage = sakerTilVurdering.stream().filter(f -> behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(f.getId(), BehandlingType.KLAGE).isPresent()).count();
            LOG.info("VFS-KLAGE flere saker - antall saker {} saker med klage {} saksnummer {}", sakerTilVurdering.size(), sakerMedKlage, saker);
        }
        return behandlinger.size() != 1 ? Optional.empty() :
            Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING, behandlinger.get(0).getSaksnummer()));
    }

    public static boolean erSøknad(VurderFagsystem vurderFagsystem) {
        return DokumentTypeId.getSøknadTyper().contains(vurderFagsystem.getDokumentTypeId()) || DokumentKategori.SØKNAD.equals(
            vurderFagsystem.getDokumentKategori());
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        var behandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(s.getId());
        if (behandling.isEmpty()) {
            return BehandlingTema.fraFagsakHendelse(s.getYtelseType(), FamilieHendelseType.UDEFINERT);
        }

        var fhType = behandling.flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType)
            .orElse(FamilieHendelseType.UDEFINERT);
        return BehandlingTema.fraFagsakHendelse(s.getYtelseType(), fhType);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> hentAktuellFamilieHendelse(Behandling behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling.getId())
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                .flatMap(r -> r.getRelatertFagsak(behandling.getFagsak()))
                .flatMap(f -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f.getId()))
                .flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId())));
    }

    public enum SorteringSaker {
        GRUNNLAG_DATO_MATCH,
        GRUNNLAG_MULIG_MATCH,
        GRUNNLAG_MISMATCH,
        INNTEKTSMELDING_DATO_MATCH,
        INNTEKTSMELDING_MISMATCH,
        TOM_SAK
    }

}
