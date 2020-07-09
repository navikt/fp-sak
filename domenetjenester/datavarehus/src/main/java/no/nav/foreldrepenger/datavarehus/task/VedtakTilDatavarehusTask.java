package no.nav.foreldrepenger.datavarehus.task;

import static no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett.VEDTAK_TIL_DATAVAREHUS_TASK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VedtakTilDatavarehusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakTilDatavarehusTask extends GenerellProsessTask {

    public static final String TASKTYPE = VEDTAK_TIL_DATAVAREHUS_TASK;

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
