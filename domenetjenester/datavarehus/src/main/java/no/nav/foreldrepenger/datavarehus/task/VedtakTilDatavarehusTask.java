package no.nav.foreldrepenger.datavarehus.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.vedtakTilDatavarehus", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakTilDatavarehusTask extends GenerellProsessTask {

    private DatavarehusTjeneste datavarehusTjeneste;

    VedtakTilDatavarehusTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakTilDatavarehusTask(DatavarehusTjeneste datavarehusTjeneste) {
        super();
        this.datavarehusTjeneste = datavarehusTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        datavarehusTjeneste.opprettOgLagreVedtakXml(behandlingId);
    }

}
