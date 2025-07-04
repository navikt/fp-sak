package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.fagsakRelasjonAvsluttningsdato", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettFagsakRelasjonAvslutningsdatoTask extends FagsakRelasjonProsessTask {

    private OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon;

    SettFagsakRelasjonAvslutningsdatoTask() {
        // for CDI proxy
    }

    @Inject
    public SettFagsakRelasjonAvslutningsdatoTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonRepository fagsakRelasjonRepository,
                                                 OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon) {
        super(fagsakLåsRepository, fagsakRelasjonRepository);
        this.oppdaterAvslutningsdatoFagsakRelasjon = oppdaterAvslutningsdatoFagsakRelasjon;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData,
                          Optional<FagsakRelasjon> relasjon,
                          Optional<FagsakLås> fagsak1Lås,
                          Optional<FagsakLås> fagsak2Lås) {
        var fagsakId = prosessTaskData.getFagsakId();
        relasjon.ifPresent(
            fagsakRelasjon -> oppdaterAvslutningsdatoFagsakRelasjon.oppdaterFagsakRelasjonAvslutningsdato(fagsakRelasjon, fagsakId, fagsak1Lås,
                fagsak2Lås, fagsakRelasjon.getFagsakNrEn().getYtelseType()));
    }
}
