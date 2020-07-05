package no.nav.foreldrepenger.datavarehus.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(RegenererVedtaksXmlDatavarehusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RegenererVedtaksXmlDatavarehusTask extends GenerellProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.regenererVedtakXmlTilDatavarehus";
    private DatavarehusTjeneste datavarehusTjeneste;

    public RegenererVedtaksXmlDatavarehusTask() {
    }

    @Inject
    public RegenererVedtaksXmlDatavarehusTask(DatavarehusTjeneste datavarehusTjeneste) {
        super();
        this.datavarehusTjeneste = datavarehusTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        datavarehusTjeneste.oppdaterVedtakXml(behandlingId);
    }
}
