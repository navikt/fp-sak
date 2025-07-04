package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask(value = "behandlingskontroll.fagsakAvslutning", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskFagsakAvslutningTask extends FagsakRelasjonProsessTask {

    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon;
    private FagsakRepository fagsakRepository;

    AutomatiskFagsakAvslutningTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskFagsakAvslutningTask(FagsakLåsRepository fagsakLåsRepository, BehandlingRepositoryProvider repositoryProvider,
                                          OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                          OppdaterAvslutningsdatoFagsakRelasjon oppdaterAvslutningsdatoFagsakRelasjon,
                                          FagsakRepository fagsakRepository) {
        super(fagsakLåsRepository, repositoryProvider.getFagsakRelasjonRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.oppdaterAvslutningsdatoFagsakRelasjon = oppdaterAvslutningsdatoFagsakRelasjon;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData,
                          Optional<FagsakRelasjon> relasjon,
                          Optional<FagsakLås> fagsak1Lås,
                          Optional<FagsakLås> fagsak2Lås) {
        var fagsakId = prosessTaskData.getFagsakId();
        // For å sikre at fagsaken hentes opp i cache - ellers dukker den opp via readonly-query og det blir problem.
        fagsakRepository.finnEksaktFagsak(fagsakId);

        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).ifPresent(behandling -> {
            var ytelseType = behandling.getFagsakYtelseType();
            var resultat = oppdaterFagsakStatus(behandling);
            if (resultat != FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET) {
                oppdaterFagsakRelasjonAvslutningsdato(relasjon, fagsak1Lås, fagsak2Lås, fagsakId, ytelseType);
            }
        });

    }

    private void oppdaterFagsakRelasjonAvslutningsdato(Optional<FagsakRelasjon> relasjon,
                                                       Optional<FagsakLås> fagsak1Lås,
                                                       Optional<FagsakLås> fagsak2Lås,
                                                       Long fagsakId,
                                                       FagsakYtelseType ytelseType) {
        relasjon.ifPresent(
            fagsakRelasjon -> oppdaterAvslutningsdatoFagsakRelasjon.oppdaterFagsakRelasjonAvslutningsdato(fagsakRelasjon, fagsakId, fagsak1Lås, fagsak2Lås, ytelseType));
    }

    private FagsakStatusOppdateringResultat oppdaterFagsakStatus(Behandling behandling) {
        return oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAutomatiskAvslBatch(behandling);
    }
}
