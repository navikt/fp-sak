package no.nav.foreldrepenger.behandling;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class DekningsgradTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DekningsgradTjeneste.class);

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    DekningsgradTjeneste() {
        // CDI
    }

    @Inject
    public DekningsgradTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                BehandlingsresultatRepository behandlingsresultatRepository,
                                YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(Behandling behandling) {
        var gammel = finnGjeldendeDekningsgradHvisEksisterer(behandling.getFagsak().getSaksnummer()).orElse(null);
        var ny = gjeldendeFraBehandling(behandling.getId());
        logDiff(behandling.getId(), gammel, ny, "gjeldende1");
        return Optional.ofNullable(gammel);
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse ref) {
        var gammel = finnGjeldendeDekningsgradHvisEksisterer(ref.saksnummer()).orElse(null);
        var ny = gjeldendeFraBehandling(ref.behandlingId());
        logDiff(ref.behandlingId(), gammel, ny, "gjeldende2");
        return Optional.ofNullable(gammel);
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(BehandlingReferanse ref) {
        var saksnummer = ref.saksnummer();
        var gammel = finnGjeldendeDekningsgradHvisEksisterer(saksnummer).orElseThrow();
        var ny = gjeldendeFraBehandling(ref.behandlingId());
        logDiff(ref.behandlingId(), gammel, ny, "gjeldende3");
        return gammel;
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(Behandling behandling) {
        var gammel = finnGjeldendeDekningsgradHvisEksisterer(behandling).orElseThrow();
        var ny = gjeldendeFraBehandling(behandling.getId());
        logDiff(behandling.getId(), gammel, ny, "gjeldende4");
        return gammel;
    }

    private Dekningsgrad gjeldendeFraBehandling(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getGjeldendeDekningsgrad)
            .orElse(null);
    }

    public Optional<Dekningsgrad> finnOppgittDekningsgrad(Behandling behandling) {
        var gammel = fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(behandling.getFagsak().getSaksnummer())
            .map(FagsakRelasjon::getDekningsgrad)
            .orElse(null);
        var ny = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId()).map(YtelseFordelingAggregat::getOppgittDekningsgrad).orElse(null);

        logDiff(behandling.getId(), gammel, ny, "oppgitt");

        return Optional.ofNullable(gammel);
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(Saksnummer saksnummer) {
        //Skal hente gjeldende dekningsgrad i sakskomplekset. Fra kontogrunnlag på fagsakrel
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer).map(FagsakRelasjon::getGjeldendeDekningsgrad);
    }

    public boolean behandlingHarEndretDekningsgrad(BehandlingReferanse ref) {
        var gammel = behandingHarEndretDekningsgradGammel(ref);
        var ny = behandlingHarEndretDekningsgradNy(ref);
        if (!Objects.equals(gammel, ny)) {
            LOG.info("Dekningsgrad diff - endret {} {} {}", ref.behandlingId(), gammel, ny);
        }
        return gammel;
    }

    private boolean behandingHarEndretDekningsgradGammel(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.behandlingId());
        if (behandlingsresultat.isPresent() && behandlingsresultat.get().isEndretDekningsgrad()) {
            return dekningsgradEndretVerdi(ref);
        }
        return false;
    }

    public boolean behandlingHarEndretDekningsgradNy(BehandlingReferanse ref) {
        var yfaOpt = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId());
        if (yfaOpt.isEmpty()) {
            return false;
        }
        var ytelseFordelingAggregat = yfaOpt.orElseThrow();
        return ref.getOriginalBehandlingId().map(originalBehandling -> {
            var originalDekningsgrad = ytelsesFordelingRepository.hentAggregat(originalBehandling).getGjeldendeDekningsgrad();
            var behandlingDekningsgad = ytelseFordelingAggregat.getGjeldendeDekningsgrad();
            return !Objects.equals(originalDekningsgrad, behandlingDekningsgad);
        }).orElseGet(() -> !Objects.equals(ytelseFordelingAggregat.getGjeldendeDekningsgrad(), ytelseFordelingAggregat.getOppgittDekningsgrad()));

    }

    private boolean dekningsgradEndretVerdi(BehandlingReferanse ref) {
        var relasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        var overstyrtDekningsgrad = relasjon.getOverstyrtDekningsgrad();
        return overstyrtDekningsgrad.isPresent() && !overstyrtDekningsgrad.get().equals(relasjon.getDekningsgrad());
    }

    private static void logDiff(Long behandlingId, Dekningsgrad gammel, Dekningsgrad ny, String msg) {
        if (!Objects.equals(gammel, ny)) {
            LOG.info("Dekningsgrad diff - {} {} {} {}", msg, behandlingId, gammel, ny);
        }
    }
}
