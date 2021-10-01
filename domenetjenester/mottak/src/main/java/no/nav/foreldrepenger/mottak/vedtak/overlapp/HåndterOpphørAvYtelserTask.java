package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterOpphørAvYtelser", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterOpphørAvYtelserTask extends GenerellProsessTask {

    public static final String BESKRIVELSE_KEY = "beskrivelse";
    public static final String BEHANDLING_ÅRSAK_KEY = "behandlingAarsak";

    private HåndterOpphørAvYtelser tjeneste;
    private FagsakRepository fagsakRepository;

    HåndterOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterOpphørAvYtelserTask(HåndterOpphørAvYtelser tjeneste, FagsakRepository fagsakRepository) {
        super();
        this.tjeneste = tjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var beskrivelse = prosessTaskData.getPropertyValue(BESKRIVELSE_KEY);
        var behandlingÅrsak = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK_KEY));
        tjeneste.oppdaterEllerOpprettRevurdering(fagsak, beskrivelse, behandlingÅrsak);
    }
}
