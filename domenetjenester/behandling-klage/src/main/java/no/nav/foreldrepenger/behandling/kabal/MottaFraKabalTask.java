package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kabal.mottafrakabal", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaFraKabalTask extends BehandlingProsessTask {

    public static final String HENDELSETYPE_KEY = "hendelse";
    public static final String UTFALL_KEY = "utfall";
    public static final String JOURNALPOST_KEY = "journalpostId";
    public static final String KABALREF_KEY = "kabalReferanse";
    public static final String OVERSENDTR_KEY = "oversendtTrygderett";

    private static Set<KabalUtfall> UTEN_VURDERING = Set.of(KabalUtfall.TRUKKET, KabalUtfall.RETUR);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private KabalTjeneste kabalTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    MottaFraKabalTask() {
        // for CDI proxy
    }

    @Inject
    public MottaFraKabalTask(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                             BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                             DokumentArkivTjeneste dokumentArkivTjeneste,
                             KabalTjeneste kabalTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.kabalTjeneste = kabalTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {

        var hendelsetype = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSETYPE_KEY))
            .map(KabalHendelse.BehandlingEventType::valueOf).orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mottatt ikke-støtte kabalisme"));
        var ref = Optional.ofNullable(prosessTaskData.getPropertyValue(KABALREF_KEY))
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler kabalreferanse"));
        switch (hendelsetype) {
            case KLAGEBEHANDLING_AVSLUTTET -> klageAvsluttet(prosessTaskData, behandlingId, ref);
            case ANKEBEHANDLING_OPPRETTET -> ankeOpprettet(behandlingId, ref);
            case ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET -> ankeTrygdrett(prosessTaskData, behandlingId, ref);
            case ANKEBEHANDLING_AVSLUTTET -> ankeAvsluttet(prosessTaskData, behandlingId, ref);
        }
    }

    private void klageAvsluttet(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(UTFALL_KEY))
            .map(KabalUtfall::valueOf).orElseThrow(() -> new IllegalStateException("Utviklerfeil: Kabal-klage avsluttet men mangler utfall"));
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        kabalTjeneste.settKabalReferanse(behandling, ref);
        if (!UTEN_VURDERING.contains(utfall)) {
            kabalTjeneste.lagreKlageUtfallFraKabal(behandling, utfall);
        }
        var journalpost = Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_KEY))
            .map(JournalpostId::new)
            .flatMap(j -> dokumentArkivTjeneste.hentUtgåendeJournalpostForSak(j));
        if (KabalUtfall.TRUKKET.equals(utfall)) {
            if (behandling.isBehandlingPåVent()) {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
            }
            if (!erHenlagt(behandling)) {
                behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
                kabalTjeneste.lagHistorikkinnslagForHenleggelse(behandlingId, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
            }
        } else if (KabalUtfall.RETUR.equals(utfall)) {
            // Knoteri siden behandling tilbakeføres og deretter kanskje skal til Kabal på nytt. Gjennomgå retur-semantikk på nytt.
            if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE)) {
                behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(),
                    behandling.getÅpneAksjonspunkter(List.of(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE)));
            }
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
            kabalTjeneste.fjerneKabalFlagg(behandling);
            behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.KLAGE_NFP);
            endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(behandling);
        } else {
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
        journalpost.ifPresent(j -> kabalTjeneste.lagHistorikkinnslagForBrevSendt(behandling, j));
    }

    private void ankeOpprettet(Long behandlingId, String ref) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        // Anke opprettet av KABAL utenom VL skal normalt ha kildereferanse = påanket klagebehandling
        // Dersom det kommer hendelse pga anke som VL flytter til Kabal skal man ikke reagere
        if (!BehandlingType.KLAGE.equals(behandling.getType())) {
            return;
        }
        var ankeBehandling = behandlingOpprettingTjeneste.opprettBehandlingVedKlageinstans(behandling.getFagsak(), BehandlingType.ANKE);
        kabalTjeneste.opprettNyttAnkeResultat(ankeBehandling, ref, behandling);
        behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(ankeBehandling);
    }

    // Konvensjon: Dersom hendelse har kilderef = ANKE så er det en overført anke, ellers er anken opprettet i/av Kabal
    private void ankeTrygdrett(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        var sendtTrygderetten = Optional.ofNullable(prosessTaskData.getPropertyValue(OVERSENDTR_KEY))
            .map(v -> LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        håndterAnkeAvsluttetEllerTrygderett(prosessTaskData, behandlingId, ref, sendtTrygderetten);
    }

    // Konvensjon: Dersom hendelse har kilderef = ANKE så er det en overført anke, ellers er anken opprettet i/av Kabal
    private void ankeAvsluttet(ProsessTaskData prosessTaskData, Long behandlingId, String ref) {
        håndterAnkeAvsluttetEllerTrygderett(prosessTaskData, behandlingId, ref, null);
    }

    private void håndterAnkeAvsluttetEllerTrygderett(ProsessTaskData prosessTaskData, Long behandlingId, String ref, LocalDate sendtTrygderetten) {
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(UTFALL_KEY))
            .map(KabalUtfall::valueOf).orElse(null);
        if (utfall == null) {
            throw new IllegalStateException("Utviklerfeil: Kabal-klage avsluttet men mangler utfall");
        }
        var ankeBehandling = kabalTjeneste.finnAnkeBehandling(behandlingId, ref)
            .orElseThrow(() -> new IllegalStateException("Mangler ankebehandling for behandling " + behandlingId));
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(ankeBehandling.getId());
        kabalTjeneste.settKabalReferanse(ankeBehandling, ref);
        var journalpost = Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_KEY))
            .map(JournalpostId::new)
            .flatMap(j -> dokumentArkivTjeneste.hentUtgåendeJournalpostForSak(j));
        if (KabalUtfall.TRUKKET.equals(utfall)) {
            if (ankeBehandling.isBehandlingPåVent()) {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(ankeBehandling, kontekst);
            }
            if (!erHenlagt(ankeBehandling)) {
                behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
                kabalTjeneste.lagHistorikkinnslagForHenleggelse(behandlingId, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
            }
        } else if (KabalUtfall.RETUR.equals(utfall)) {
            throw new IllegalStateException("KABAL sender ankeutfall RETUR sak " + ankeBehandling.getFagsak().getSaksnummer().getVerdi());
        } else {
            kabalTjeneste.lagreAnkeUtfallFraKabal(ankeBehandling, utfall, sendtTrygderetten);
            // Skyldes et antall tilfelle som er prematurt avsluttet pga kabal anke-avsluttet men som ble overført Trygderetten
            if (!ankeBehandling.erAvsluttet()) {
                if (ankeBehandling.isBehandlingPåVent()) { // Autopunkt
                    behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(ankeBehandling, kontekst);
                }
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(ankeBehandling);
            }
        }
        journalpost.ifPresent(j -> kabalTjeneste.lagHistorikkinnslagForBrevSendt(ankeBehandling, j));
    }

    private void endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(Behandling behandling) {
        if ((behandling.getBehandlendeEnhet() != null)
            && !BehandlendeEnhetTjeneste.getKlageInstans().enhetId().equals(behandling.getBehandlendeEnhet())) {
            return;
        }
        var tilEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }

    private boolean erHenlagt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).filter(Behandlingsresultat::isBehandlingHenlagt).isPresent();
    }
}
