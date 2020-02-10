package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SettFagsakRelasjonAvsluttningsdatoTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettFagsakRelasjonAvsluttningsdatoTask extends FagsakRelasjonProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.fagsakRelasjonAvsluttningsdato";

    private FagsakRelasjonAvsluttningsdatoOppdaterer fagsakRelasjonAvsluttningsdatoOppdaterer;

    SettFagsakRelasjonAvsluttningsdatoTask() {
        // for CDI proxy
    }

    @Inject
    public SettFagsakRelasjonAvsluttningsdatoTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, FagsakRelasjonRepository fagsakRelasjonRepository,
                                                  FagsakRelasjonAvsluttningsdatoOppdaterer fagsakRelasjonAvsluttningsdatoOppdaterer) {
        super(fagsakLåsRepository, relasjonLåsRepository, fagsakRelasjonRepository);
        this.fagsakRelasjonAvsluttningsdatoOppdaterer = fagsakRelasjonAvsluttningsdatoOppdaterer;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        Long fagsakId = prosessTaskData.getFagsakId();
        relasjon.ifPresent(fr -> fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fr, fagsakId, relasjonLås, fagsak1Lås, fagsak2Lås));
    }
}
