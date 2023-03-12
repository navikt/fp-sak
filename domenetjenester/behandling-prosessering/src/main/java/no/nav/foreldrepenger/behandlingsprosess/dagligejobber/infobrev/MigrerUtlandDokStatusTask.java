package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask("oppgavebehandling.migrer.dokstatus")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MigrerUtlandDokStatusTask implements ProsessTaskHandler {

    public static final String HENDELSE_TYPE = "hendelseType";

    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public MigrerUtlandDokStatusTask(FagsakEgenskapRepository fagsakEgenskapRepository,
                                     InformasjonssakRepository informasjonssakRepository) {
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.informasjonssakRepository = informasjonssakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        informasjonssakRepository.finnBehandlingerMedUtlandsDokumentasjonValg()
            .forEach(udv -> fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(udv.fagsakId(), UtlandDokumentasjonStatus.valueOf(udv.kode())));
    }

}
