package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.fagsak.*;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import java.util.Optional;

@ApplicationScoped
@ProsessTask("iverksetteVedtak.fagsakRelasjonAvsluttningsdato")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettFagsakRelasjonAvslutningsdatoTask extends FagsakRelasjonProsessTask {

    private OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon;

    SettFagsakRelasjonAvslutningsdatoTask() {
        // for CDI proxy
    }

    @Inject
    public SettFagsakRelasjonAvslutningsdatoTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, FagsakRelasjonRepository fagsakRelasjonRepository,
                                                 OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon) {
        super(fagsakLåsRepository, relasjonLåsRepository, fagsakRelasjonRepository);
        this.oppdaterAvslutningsdatoFagsakRelasjon = oppdaterAvslutningsdatoFagsakRelasjon;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        var fagsakId = prosessTaskData.getFagsakId();
        relasjon.ifPresent(fagsakRelasjon -> oppdaterAvslutningsdatoFagsakRelasjon.oppdaterFagsakRelasjonAvslutningsdato(fagsakRelasjon, fagsakId, relasjonLås,
            fagsak1Lås, fagsak2Lås, fagsakRelasjon.getFagsakNrEn().getYtelseType()));
        }
}
