package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/*
 * Task for
 */
@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterOpphørAvYtelser", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterOpphørAvYtelserTask extends GenerellProsessTask {

    public static final String BESKRIVELSE_KEY = "beskrivelse";

    private HåndterOpphørAvYtelser tjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    public HåndterOpphørAvYtelserTask(HåndterOpphørAvYtelser tjeneste, FagsakRepository fagsakRepository) {
        super();
        this.tjeneste = tjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    HåndterOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var beskrivelse = prosessTaskData.getPropertyValue(BESKRIVELSE_KEY);

        tjeneste.oppdaterEllerOpprettRevurdering(fagsak, beskrivelse, BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN);

    }
}
