package no.nav.foreldrepenger.datavarehus.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(RegenererVedtaksXmlDatavarehusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RegenererVedtaksXmlDatavarehusTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "iverksetteVedtak.regenererVedtakXmlTilDatavarehus";
    private DatavarehusTjeneste datavarehusTjeneste;

    public RegenererVedtaksXmlDatavarehusTask() {
    }

    @Inject
    public RegenererVedtaksXmlDatavarehusTask(DatavarehusTjeneste datavarehusTjeneste) {
        this.datavarehusTjeneste = datavarehusTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        datavarehusTjeneste.oppdaterVedtakXml(behandlingId);
    }
}
