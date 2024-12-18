package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.saksmerkebarefarrett.single", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class BareFarRettSaksmerkingSingleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BareFarRettSaksmerkingSingleTask.class);

    static final String FAGSAK_ID = "fagsakId";
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final FpUttakRepository fpUttakRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public BareFarRettSaksmerkingSingleTask(FagsakRepository fagsakRepository,
                                            BehandlingRepository behandlingRepository,
                                            YtelsesFordelingRepository ytelsesFordelingRepository,
                                            FagsakRelasjonTjeneste fagsakRelasjonTjeneste, FpUttakRepository fpUttakRepository,
                                            FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.fpUttakRepository = fpUttakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsak = Optional.ofNullable(prosessTaskData.getPropertyValue(FAGSAK_ID))
            .map(fid -> fagsakRepository.finnEksaktFagsak(Long.parseLong(fid)))
            .orElseThrow();
        if (RelasjonsRolleType.MORA.equals(fagsak.getRelasjonsRolleType())) {
            LOG.info("BareFarRettMarkering: Sak {} bruker er mor", fagsak.getSaksnummer().getVerdi());
            return;
        }
        var relatertSak = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak));
        if (relatertSak.isPresent()) {
            LOG.info("BareFarRettMarkering: Sak {} har relatert sak {}", fagsak.getSaksnummer().getVerdi(), relatertSak.get().getSaksnummer().getVerdi());
            return;
        }
        var eksisterende = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        if (eksisterende.contains(FagsakMarkering.BARE_FAR_RETT)) {
            LOG.info("BareFarRettMarkering: Sak {} allerede merket BARE_FAR_RETT", fagsak.getSaksnummer().getVerdi());
            return;
        }
        var behandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingReadOnlyFor(fagsak.getId());
        var yfAggregat = behandling.flatMap(b -> ytelsesFordelingRepository.hentAggregatHvisEksisterer(b.getId()));
        if (yfAggregat.isEmpty() || !harKontoForeldrepenger(behandling.get())) {
            LOG.info("BareFarRettMarkering: Sak {} finner ikke søknadsopplysninger", fagsak.getSaksnummer().getVerdi());
            return;
        }
        var aleneomsorg = yfAggregat.filter(YtelseFordelingAggregat::harAleneomsorg).isPresent();
        var bareFarRett = yfAggregat.filter(a -> a.harAnnenForelderRett(false)).isEmpty();
        if (bareFarRett && !aleneomsorg) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(fagsak.getId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }

    private boolean harKontoForeldrepenger(Behandling behandling) {
        var konti =  fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())
            .map(UttakResultatEntitet::getStønadskontoberegning)
            .map(Stønadskontoberegning::getStønadskontoutregning)
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsakId())
                .flatMap(FagsakRelasjon::getStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
        return konti.get(StønadskontoType.FORELDREPENGER) != null && konti.get(StønadskontoType.FORELDREPENGER) > 0;
    }


}
