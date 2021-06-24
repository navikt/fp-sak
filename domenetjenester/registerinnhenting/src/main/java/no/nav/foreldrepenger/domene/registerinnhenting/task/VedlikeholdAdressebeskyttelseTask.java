package no.nav.foreldrepenger.domene.registerinnhenting.task;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VedlikeholdAdressebeskyttelseTask.TASKTYPE)
public class VedlikeholdAdressebeskyttelseTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "vedlikehold.adressebeskyttelse";

    private static final Logger LOG = LoggerFactory.getLogger(VedlikeholdAdressebeskyttelseTask.class);

    private PersoninfoAdapter personinfoAdapter;
    private PersonopplysningRepository personopplysningRepository;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    VedlikeholdAdressebeskyttelseTask() {
        // for CDI proxy
    }

    @Inject
    public VedlikeholdAdressebeskyttelseTask(BehandlingRepositoryProvider repositoryProvider,
                                             PersoninfoAdapter personinfoAdapter,
                                             BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                             FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var aktørId = new AktørId(prosessTaskData.getAktørId());
        /*
         * Placeholder for håndtering av adressebeskyttelsehendelser og øvrige tilfelle av oppdaget adressebeskyttelse
         * - maskere adresser for bruker
         * - flytte åpne behandlinger til Vikafossen
         * - Avklar PO og LagretVedtak med produkteiere
         * - lage endepunkt for fpabonnent
         * - opprette tasks når det oppdages SPSF/SPFO
         */

    }

    private List<Fagsak> hentBrukersRelevanteSaker(AktørId aktørId, LocalDate opprettetEtter, LocalDate familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId);
    }


}
