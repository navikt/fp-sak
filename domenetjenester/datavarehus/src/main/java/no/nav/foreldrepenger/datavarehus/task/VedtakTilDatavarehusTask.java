package no.nav.foreldrepenger.datavarehus.task;

import static no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett.VEDTAK_TIL_DATAVAREHUS_TASK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VedtakTilDatavarehusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakTilDatavarehusTask implements ProsessTaskHandler {

    public static final String TASKTYPE = VEDTAK_TIL_DATAVAREHUS_TASK;

    private DatavarehusTjeneste datavarehusTjeneste;

    VedtakTilDatavarehusTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakTilDatavarehusTask(DatavarehusTjeneste datavarehusTjeneste) {
        this.datavarehusTjeneste = datavarehusTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        datavarehusTjeneste.opprettOgLagreVedtakXml(behandlingId);
    }
}
