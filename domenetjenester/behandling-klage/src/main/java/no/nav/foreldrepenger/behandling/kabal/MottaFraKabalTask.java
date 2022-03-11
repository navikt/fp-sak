package no.nav.foreldrepenger.behandling.kabal;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kabal.mottafrakabal", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaFraKabalTask extends BehandlingProsessTask {

    public static final String HENDELSETYPE_KEY = "hendelse";
    public static final String UTFALL_KEY = "utfall";

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private KabalTjeneste kabalTjeneste;

    MottaFraKabalTask() {
        // for CDI proxy
    }

    @Inject
    public MottaFraKabalTask(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             KabalTjeneste kabalTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kabalTjeneste = kabalTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {

        var hendelsetype = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSETYPE_KEY))
            .map(KabalHendelse.BehandlingEventType::valueOf).orElse(null);
        if (!KabalHendelse.BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET.equals(hendelsetype)) {
            throw new IllegalStateException("Utviklerfeil: Mottatt ikke-støtte kabalisme");
        }
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSETYPE_KEY))
            .map(KabalUtfall::valueOf).orElse(null);
        if (utfall == null) {
            throw new IllegalStateException("Utviklerfeil: Kabal-klage avsluttet men mangler utfall");
        }
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (KabalUtfall.TRUKKET.equals(utfall)) {
            behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
        } else {
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
            if (KabalUtfall.RETUR.equals(utfall)) {
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.KLAGE_NFP);
            }
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
    }
}
