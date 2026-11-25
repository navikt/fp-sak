package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.fagsakRelasjonAvsluttningsdato", prioritet = 2)
public class SettFagsakRelasjonAvslutningsdatoTask extends FagsakRelasjonProsessTask {

    private FagsakRepository fagsakRepository;
    private OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon;

    SettFagsakRelasjonAvslutningsdatoTask() {
        // for CDI proxy
    }

    @Inject
    public SettFagsakRelasjonAvslutningsdatoTask(FagsakRelasjonLåsRepository relasjonLåsRepository,
                                                 FagsakRelasjonRepository fagsakRelasjonRepository,
                                                 FagsakLåsRepository fagsakLåsRepository,
                                                 FagsakRepository fagsakRepository,
                                                 OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon) {
        super(fagsakLåsRepository, relasjonLåsRepository, fagsakRelasjonRepository);
        this.fagsakRepository = fagsakRepository;
        this.oppdaterAvslutningsdatoFagsakRelasjon = oppdaterAvslutningsdatoFagsakRelasjon;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        var saksnummer = Saksnummer.fra(prosessTaskData.getSaksnummer());
        // For å sikre at fagsaken hentes opp i cache - ellers dukker den opp via readonly-query og det blir problem.
        var fagsak = fagsakRepository.finnEksaktFagsak(saksnummer);
        relasjon.ifPresent(fagsakRelasjon -> oppdaterAvslutningsdatoFagsakRelasjon.oppdaterFagsakRelasjonAvslutningsdato(fagsakRelasjon, fagsak.getId(), relasjonLås,
            fagsak1Lås, fagsak2Lås, fagsakRelasjon.getFagsakNrEn().getYtelseType()));
    }
}
