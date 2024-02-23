package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse.single", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseSingleTask implements ProsessTaskHandler {

    static final String FAGSAK_ID = "fagsakId";
    private final FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste;
    private final FagsakRepository fagsakRepository;

    @Inject
    public FeilPraksisUtsettelseSingleTask(FeilPraksisOpprettBehandlingTjeneste feilPraksisOpprettBehandlingTjeneste,
                                           FagsakRepository fagsakRepository) {
        this.feilPraksisOpprettBehandlingTjeneste = feilPraksisOpprettBehandlingTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var sak = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID))
            .map(fid -> fagsakRepository.finnEksaktFagsak(Long.parseLong(fid)))
            .orElseThrow();
        feilPraksisOpprettBehandlingTjeneste.opprettBehandling(sak);
    }

}
