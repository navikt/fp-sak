package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse.single", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseSingleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisSaksmerkingSingleTask.class);

    static final String FAGSAK_ID = "fagsakId";
    private final FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste;
    private final FagsakRepository fagsakRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public FeilPraksisUtsettelseSingleTask(FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste,
                                           FagsakRepository fagsakRepository,
                                           FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.feilPraksisOpprettBehandlingTjeneste = feilPraksisOpprettBehandlingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var sak = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID))
            .map(fid -> fagsakRepository.finnEksaktFagsak(Long.parseLong(fid)))
            .orElseThrow();
        var saksmerking = fagsakEgenskapRepository.finnFagsakMarkering(sak.getId()).orElse(FagsakMarkering.NASJONAL);
        if (FagsakMarkering.PRAKSIS_UTSETTELSE.equals(saksmerking)) {
            feilPraksisOpprettBehandlingTjeneste.opprettBehandling(sak);
        } else {
            LOG.info("FeilPraksisUtsettelse: Mangler saksmerking praksis utsettelse, oppretter ikke revurdering for {} som har merking {}",
                sak.getSaksnummer(), saksmerking);
        }
    }

}
