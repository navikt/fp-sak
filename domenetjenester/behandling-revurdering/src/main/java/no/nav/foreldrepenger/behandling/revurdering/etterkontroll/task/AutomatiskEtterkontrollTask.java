package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "behandlingsprosess.etterkontroll", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskEtterkontrollTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private EtterkontrollRepository etterkontrollRepository;
    private RevurderingHistorikk revurderingHistorikk;

    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,
                                       EtterkontrollRepository etterkontrollRepository,
                                       FamilieHendelseTjeneste familieHendelseTjeneste,
                                       PersoninfoAdapter personinfoAdapter,
                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingHistorikk = new RevurderingHistorikk(repositoryProvider.getHistorikkinnslagRepository());
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.etterkontrollRepository = etterkontrollRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        LOG.info("Etterkontrollerer fagsak med fagsakId = {}", fagsakId);

        var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingIdAsLong());

        etterkontrollRepository.avflaggDersomEksisterer(fagsakId, KontrollType.MANGLENDE_FØDSEL);

        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            return;
        }

        List<FødtBarnInfo> barnFødtIPeriode = new ArrayList<>();
        var familieHendelseGrunnlag = familieHendelseTjeneste.finnAggregat(behandling.getId()).orElse(null);
        if (familieHendelseGrunnlag != null) {
            var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
            barnFødtIPeriode.addAll(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(behandling.getFagsakYtelseType(), behandling.getAktørId(), intervaller));
            if (!barnFødtIPeriode.isEmpty()) {
                revurderingHistorikk.opprettHistorikkinnslagForFødsler(behandling, barnFødtIPeriode);
            }
        }

        var automatiskEtterkontrollTjeneste = FagsakYtelseTypeRef.Lookup
                .find(EtterkontrollTjeneste.class, behandling.getFagsak().getYtelseType()).orElseThrow();
        var revurderingsÅrsak = automatiskEtterkontrollTjeneste.utledRevurderingÅrsak(behandling, familieHendelseGrunnlag,
                barnFødtIPeriode);

        revurderingsÅrsak.ifPresent(årsak -> {
            var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());

            automatiskEtterkontrollTjeneste.opprettRevurdering(behandling, årsak, enhet);
        });
    }
}
