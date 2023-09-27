package no.nav.foreldrepenger.mottak.publiserer.task;


import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
@ProsessTask("mottak.publiserPersistertDokument")
public class PubliserPersistertDokumentHendelseTask extends GenerellProsessTask {

    @Override
    public void prosesser(ProsessTaskData data, Long fagsakId, Long behandlingId) {
        //TODO slett etter alle tasks er ferdig
    }
}
