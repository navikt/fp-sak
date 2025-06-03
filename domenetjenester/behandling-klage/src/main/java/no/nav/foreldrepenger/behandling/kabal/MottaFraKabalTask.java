package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kabal.mottafrakabal", prioritet = 2, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaFraKabalTask extends BehandlingProsessTask {

    public static final String HENDELSETYPE_KEY = "hendelse";
    public static final String UTFALL_KEY = "utfall";
    public static final String JOURNALPOST_KEY = "journalpostId";
    public static final String KABALREF_KEY = "kabalReferanse";
    public static final String OVERSENDTR_KEY = "oversendtTrygderett";
    public static final String FEILOPPRETTET_TYPE_KEY = "feilopprettetType";

    private static final Set<KabalUtfall> UTEN_VURDERING = Set.of(KabalUtfall.TRUKKET, KabalUtfall.HEVET, KabalUtfall.RETUR);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private KabalTjeneste kabalTjeneste;

    MottaFraKabalTask() {
        // for CDI proxy
    }

    @Inject
    public MottaFraKabalTask(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                             BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                             KabalTjeneste kabalTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.kabalTjeneste = kabalTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var hendelsetype = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSETYPE_KEY))
            .map(KabalHendelse.BehandlingEventType::valueOf)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mottatt ikke-støtte kabalisme"));
        var ref = Optional.ofNullable(prosessTaskData.getPropertyValue(KABALREF_KEY))
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler kabalreferanse"));
        switch (hendelsetype) {
            case KLAGEBEHANDLING_AVSLUTTET -> klageAvsluttet(prosessTaskData, behandlingId, ref);
            case ANKEBEHANDLING_OPPRETTET -> ankeOpprettet(behandlingId, ref);
            case ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET -> ankeTrygdrett(prosessTaskData, behandlingId, ref);
            case ANKEBEHANDLING_AVSLUTTET, BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET -> ankeAvsluttet(prosessTaskData, behandlingId, ref);
            case BEHANDLING_FEILREGISTRERT -> henleggFeilopprettet(prosessTaskData, behandlingId, ref);
            case OMGJOERINGSKRAVBEHANDLING_AVSLUTTET -> throw new IllegalArgumentException("Utviklerfeil: hvorfor kom vi hit");
        }
    }

    private void klageAvsluttet(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(UTFALL_KEY))
            .map(KabalUtfall::valueOf).orElseThrow(() -> new IllegalStateException("Utviklerfeil: Kabal-klage avsluttet men mangler utfall"));
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var klageBehandling = behandlingRepository.hentBehandling(behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(klageBehandling, lås);
        kabalTjeneste.settKabalReferanse(klageBehandling, ref);
        if (!UTEN_VURDERING.contains(utfall)) {
            kabalTjeneste.lagreKlageUtfallFraKabal(klageBehandling, lås, utfall);
        }
        if (KabalUtfall.TRUKKET.equals(utfall) || KabalUtfall.HEVET.equals(utfall)) {
            if (klageBehandling.isBehandlingPåVent()) {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(klageBehandling, kontekst);
            }
            if (erIkkeHenlagt(klageBehandling)) {
                behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
                kabalTjeneste.lagHistorikkinnslagForHenleggelse(klageBehandling, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
            }
        } else if (KabalUtfall.RETUR.equals(utfall)) {
            // Knoteri siden behandling tilbakeføres og deretter kanskje skal til Kabal på nytt. Avbrutt er viktig. Gjennomgå retur-semantikk på nytt.
            klageBehandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE)
                .ifPresent(a -> behandlingskontrollTjeneste.settAutopunktTilAvbrutt(kontekst, klageBehandling, a));
            if (klageBehandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(klageBehandling, kontekst);
            }
            kabalTjeneste.fjerneKabalFlagg(klageBehandling);
            behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.KLAGE_NFP);
            endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(klageBehandling);
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(klageBehandling);
        } else {
            if (klageBehandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(klageBehandling, kontekst);
            }
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(klageBehandling);
        }
        Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_KEY))
            .map(JournalpostId::new)
            .ifPresent(j -> kabalTjeneste.lagHistorikkinnslagForBrevSendt(klageBehandling, j));
    }

    private void ankeOpprettet(Long behandlingId, String ref) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        // Anke opprettet av KABAL utenom VL skal normalt ha kildereferanse = påanket klagebehandling
        // Dersom det kommer hendelse pga anke som VL flytter til Kabal skal man ikke reagere
        if (!BehandlingType.KLAGE.equals(behandling.getType())) {
            return;
        }
        var åpneAnkerSammeKlage = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
            .filter(b -> BehandlingType.ANKE.equals(b.getType()))
            .filter(b -> !b.erSaksbehandlingAvsluttet())
            .anyMatch(a -> kabalTjeneste.gjelderÅpenAnkeDenneKlagen(a, behandling));
        if (åpneAnkerSammeKlage) {
            throw new IllegalStateException("Mottatt anke opprettet, men har allerede åpen ankebehandling");
        }
        var ankeBehandling = behandlingOpprettingTjeneste.opprettBehandlingVedKlageinstans(behandling.getFagsak(), BehandlingType.ANKE);
        kabalTjeneste.opprettNyttAnkeResultat(ankeBehandling, ref, behandling);
        behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(ankeBehandling);
    }

    // Konvensjon: Dersom hendelse har kilderef = ANKE så er det en overført anke, ellers er anken opprettet i/av Kabal
    private void ankeTrygdrett(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        var sendtTrygderetten = Optional.ofNullable(prosessTaskData.getPropertyValue(OVERSENDTR_KEY))
            .map(v -> LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE)).orElseThrow();
        håndterAnkeAvsluttetEllerTrygderett(prosessTaskData, behandlingId, ref, sendtTrygderetten);
    }

    // Konvensjon: Dersom hendelse har kilderef = ANKE så er det en overført anke, ellers er anken opprettet i/av Kabal
    private void ankeAvsluttet(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        håndterAnkeAvsluttetEllerTrygderett(prosessTaskData, behandlingId, ref, null);
    }

    private void håndterAnkeAvsluttetEllerTrygderett(ProsessTaskData prosessTaskData, Long behandlingId, String ref, LocalDate sendtTrygderetten) {
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(UTFALL_KEY))
            .map(KabalUtfall::valueOf)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Kabal-anke avsluttet men mangler utfall"));
        var ankeBehandling = kabalTjeneste.finnAnkeBehandling(behandlingId, ref)
            .orElseThrow(() -> new IllegalStateException("Mangler ankebehandling for behandling " + behandlingId));
        var lås = behandlingRepository.taSkriveLås(ankeBehandling);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(ankeBehandling, lås);
        kabalTjeneste.settKabalReferanse(ankeBehandling, ref);
        if (KabalUtfall.TRUKKET.equals(utfall) || KabalUtfall.HEVET.equals(utfall)) {
            if (ankeBehandling.isBehandlingPåVent()) {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(ankeBehandling, kontekst);
            }
            if (erIkkeHenlagt(ankeBehandling)) {
                behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_ANKE_TRUKKET);
                kabalTjeneste.lagHistorikkinnslagForHenleggelse(ankeBehandling, BehandlingResultatType.HENLAGT_ANKE_TRUKKET);
            }
        } else if (KabalUtfall.RETUR.equals(utfall)) {
            throw new IllegalStateException("KABAL sender ankeutfall RETUR sak " + ankeBehandling.getSaksnummer().getVerdi());
        } else {
            kabalTjeneste.lagreAnkeUtfallFraKabal(ankeBehandling, utfall, sendtTrygderetten);
            if (ankeBehandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(ankeBehandling, kontekst);
            }
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ankeBehandling);
        }
        Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_KEY))
            .map(JournalpostId::new)
            .ifPresent(j -> kabalTjeneste.lagHistorikkinnslagForBrevSendt(ankeBehandling, j));
    }

    private void henleggFeilopprettet(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        var kabalBehandlingType = Optional.ofNullable(prosessTaskData.getPropertyValue(FEILOPPRETTET_TYPE_KEY))
            .map(KabalHendelse.BehandlingType::valueOf)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Kabal feilregistrert men mangler type behandling"));
        var behandling = KabalHendelse.BehandlingType.KLAGE.equals(kabalBehandlingType) ? behandlingRepository.hentBehandling(behandlingId) :
            kabalTjeneste.finnAnkeBehandling(behandlingId, ref)
                .orElseThrow(() -> new IllegalStateException("Finner ike ankebehandling for behandling " + behandlingId));
        var lås = behandlingRepository.taSkriveLås(behandling);
        kabalTjeneste.settKabalReferanse(behandling, ref);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        kabalTjeneste.settKabalReferanse(behandling, ref);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }
        if (erIkkeHenlagt(behandling)) {
            behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_FEILOPPRETTET);
            kabalTjeneste.lagHistorikkinnslagForHenleggelse(behandling, BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        }
    }

    private void endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(Behandling behandling) {
        if (behandling.getBehandlendeEnhet() != null && !BehandlendeEnhetTjeneste.getKlageInstans()
            .enhetId()
            .equals(behandling.getBehandlendeEnhet())) {
            return;
        }
        var tilEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }

    private boolean erIkkeHenlagt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).filter(Behandlingsresultat::isBehandlingHenlagt).isEmpty();
    }
}
