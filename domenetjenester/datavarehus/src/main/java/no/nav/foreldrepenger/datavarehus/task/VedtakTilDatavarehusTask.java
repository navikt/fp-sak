package no.nav.foreldrepenger.datavarehus.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("iverksetteVedtak.vedtakTilDatavarehus")
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
