package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOpphørAvYtelser;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.testfeilpraksisutsettelse", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseTask implements ProsessTaskHandler {

    private static final String FRA_FAGSAK_ID = "fagsakId";
    private final HåndterOpphørAvYtelser håndterOpphørAvYtelser;
    private final FagsakRepository fagsakRepository;

    @Inject
    public FeilPraksisUtsettelseTask(HåndterOpphørAvYtelser håndterOpphørAvYtelser,
                                     FagsakRepository fagsakRepository) {
        this.håndterOpphørAvYtelser = håndterOpphørAvYtelser;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var sak = fagsakRepository.finnEksaktFagsak(Long.parseLong(fagsakIdProperty));
        håndterOpphørAvYtelser.oppdaterEllerOpprettRevurdering(sak, null, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, false);
    }

}
