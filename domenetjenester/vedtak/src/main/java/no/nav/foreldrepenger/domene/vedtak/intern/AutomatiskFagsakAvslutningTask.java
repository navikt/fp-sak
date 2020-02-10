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
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask(AutomatiskFagsakAvslutningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskFagsakAvslutningTask extends FagsakRelasjonProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.fagsakAvslutning";

    private BehandlingRepository behandlingRepository;
    private Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser;
    private FagsakRelasjonAvsluttningsdatoOppdaterer fagsakRelasjonAvsluttningsdatoOppdaterer;

    AutomatiskFagsakAvslutningTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskFagsakAvslutningTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, BehandlingRepositoryProvider repositoryProvider,
                                          @Any Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser,
                                          FagsakRelasjonAvsluttningsdatoOppdaterer fagsakRelasjonAvsluttningsdatoOppdaterer) {
        super(fagsakLåsRepository, relasjonLåsRepository, repositoryProvider.getFagsakRelasjonRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdaterFagsakStatuser = oppdaterFagsakStatuser;
        this.fagsakRelasjonAvsluttningsdatoOppdaterer = fagsakRelasjonAvsluttningsdatoOppdaterer;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        Long fagsakId = prosessTaskData.getFagsakId();

        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        if (behandling.isPresent()) {
            OppdaterFagsakStatus oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, behandling.get().getFagsakYtelseType())
                .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.get().getFagsakYtelseType().getKode()));
            var resultat = oppdaterFagsakStatus.oppdaterFagsakNårBehandlingEndret(behandling.get());
            if (resultat != FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET) {
                relasjon.ifPresent(fr -> fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fr, fagsakId, relasjonLås, fagsak1Lås, fagsak2Lås));
            }
        }
    }
}
