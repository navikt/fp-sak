package no.nav.foreldrepenger.web.app.tjenester.los;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.Optional;

@ApplicationScoped
@ProsessTask(value = "los.sendbehandling.behandling", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerBehandlingTilLosTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrerBehandlingTilLosTask.class);

    static final String SEND_BEHANDLING_ID = "sendBehandlingId";

    private BehandlingRepository behandlingRepository;
    private LosBehandlingDtoTjeneste losBehandlingDtoTjeneste;
    private FplosKlient fplosKlient;

    public MigrerBehandlingTilLosTask() {
        // For CDI
    }

    @Inject
    public MigrerBehandlingTilLosTask(BehandlingRepository behandlingRepository,
                                      LosBehandlingDtoTjeneste losBehandlingDtoTjeneste,
                                      FplosKlient fplosKlient) {
        this.behandlingRepository = behandlingRepository;
        this.losBehandlingDtoTjeneste = losBehandlingDtoTjeneste;
        this.fplosKlient = fplosKlient;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = Optional.ofNullable(prosessTaskData.getPropertyValue(SEND_BEHANDLING_ID)).map(Long::valueOf).orElse(null);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var dto = losBehandlingDtoTjeneste.lagLosBehandlingDto(behandling, behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId()).isPresent());
        LOG.info("BehandlingTilLOS - sender behandling {} saksnummer {} til FPLos", behandling.getId(), behandling.getSaksnummer().getVerdi());
        fplosKlient.sendBehandlingTilLos(dto);
    }

}
