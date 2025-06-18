package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Utfører innhenting av registerdata.
 */
@ApplicationScoped
@ProsessTask("behandlingskontroll.registerdataOppdaterBehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class RegisterdataOppdatererTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterdataOppdatererTask.class);

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private RegisterdataEndringshåndterer registerdataOppdaterer;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;

    RegisterdataOppdatererTask() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataOppdatererTask(BehandlingRepository behandlingRepository,
                                      BehandlingLåsRepository låsRepository,
                                      BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                      BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                      RegisterdataEndringshåndterer registerdataOppdaterer) {
        super(låsRepository);
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.registerdataOppdaterer = registerdataOppdaterer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingsId) {
        // NB lås før hent behandling
        behandlingRepository.taSkriveLås(behandlingsId);
        var behandling = behandlingRepository.hentBehandling(behandlingsId);
        if (behandling.erSaksbehandlingAvsluttet()) return;

        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        registerdataOppdaterer.utledDiffOgReposisjonerBehandlingVedEndringer(behandling, hentUtSnapshotFraPayload(prosessTaskData), true);
        // I tilfelle tilbakehopp reåpner aksjonspunkt
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandling)
            .ifPresent(enhet -> behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.VEDTAKSLØSNINGEN, ""));
    }

    EndringsresultatSnapshot hentUtSnapshotFraPayload(ProsessTaskData prosessTaskData) {
        var payloadAsString = prosessTaskData.getPayloadAsString();
        if (payloadAsString == null) return null;
        return StandardJsonConfig.fromJson(payloadAsString, EndringsresultatSnapshot.class);
    }
}
