package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask("oppgavebehandling.utland.til.fagsak")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MigrerUtlandMerkingTask implements ProsessTaskHandler {

    public static final String HENDELSE_TYPE = "hendelseType";

    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public MigrerUtlandMerkingTask(InformasjonssakRepository informasjonssakRepository) {
        this.informasjonssakRepository = informasjonssakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        informasjonssakRepository.slettApForUtlandsmerking();
    }

}
