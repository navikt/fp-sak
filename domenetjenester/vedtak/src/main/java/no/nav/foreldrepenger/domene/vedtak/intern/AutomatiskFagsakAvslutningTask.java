package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask("behandlingskontroll.fagsakAvslutning")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskFagsakAvslutningTask extends FagsakRelasjonProsessTask {

    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private Instance<FagsakRelasjonAvslutningsdatoOppdaterer> fagsakRelasjonAvslutningsdatoOppdaterer;

    AutomatiskFagsakAvslutningTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskFagsakAvslutningTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, BehandlingRepositoryProvider repositoryProvider,
                                          OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                          @Any Instance<FagsakRelasjonAvslutningsdatoOppdaterer> fagsakRelasjonAvslutningsdatoOppdaterer) {
        super(fagsakLåsRepository, relasjonLåsRepository, repositoryProvider.getFagsakRelasjonRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.fagsakRelasjonAvslutningsdatoOppdaterer = fagsakRelasjonAvslutningsdatoOppdaterer;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        var fagsakId = prosessTaskData.getFagsakId();

        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).ifPresent(behandling -> {
            var ytelseType = behandling.getFagsakYtelseType();
            var resultat = oppdaterFagsakStatus(behandling);
            if (resultat != FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET) {
                oppdaterFagsakRelasjonAvslutningsdato(relasjon, relasjonLås, fagsak1Lås, fagsak2Lås, fagsakId, ytelseType);
            }
        });

    }

    private void oppdaterFagsakRelasjonAvslutningsdato(Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås, Long fagsakId, FagsakYtelseType ytelseType) {
        if(relasjon.isPresent()) {
            var oppdaterer = FagsakYtelseTypeRef.Lookup.find(this.fagsakRelasjonAvslutningsdatoOppdaterer, ytelseType)
                .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner av FagsakRelasjonAvslutningsdatoOppdaterer funnet for ytelse: " + ytelseType.getKode()));
            oppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(relasjon.get(), fagsakId, relasjonLås, fagsak1Lås, fagsak2Lås);
        }
    }

    private FagsakStatusOppdateringResultat oppdaterFagsakStatus(Behandling behandling) {
        return oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAutomatiskAvslBatch(behandling);
    }
}
