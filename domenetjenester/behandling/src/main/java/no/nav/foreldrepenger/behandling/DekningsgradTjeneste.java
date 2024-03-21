package no.nav.foreldrepenger.behandling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class DekningsgradTjeneste {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    DekningsgradTjeneste() {
        // CDI
    }

    @Inject
    public DekningsgradTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(Behandling behandling) {
        return finnGjeldendeDekningsgradHvisEksisterer(behandling.getFagsak().getSaksnummer());
    }
    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse ref) {
        return finnGjeldendeDekningsgradHvisEksisterer(ref.saksnummer());
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(BehandlingReferanse ref) {
        var saksnummer = ref.saksnummer();
        return finnGjeldendeDekningsgrad(saksnummer);
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(Behandling behandling) {
        return finnGjeldendeDekningsgradHvisEksisterer(behandling).orElseThrow();
    }

    public Optional<Dekningsgrad> finnOppgittDekningsgrad(Behandling behandling) {
        //Gjøre ikke optional?
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(behandling.getFagsak().getSaksnummer()).map(FagsakRelasjon::getDekningsgrad);
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(Saksnummer saksnummer) {
        return finnGjeldendeDekningsgradHvisEksisterer(saksnummer).orElseThrow();
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(Saksnummer saksnummer) {
        //Skal hente gjeldende dekningsgrad i sakskomplekset. Fra kontogrunnlag på fagsakrel
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer).map(FagsakRelasjon::getGjeldendeDekningsgrad);
    }

    public boolean behandlingHarEndretDekningsgrad(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.behandlingId());
        if (behandlingsresultat.isPresent() && behandlingsresultat.get().isEndretDekningsgrad()) {
            return dekningsgradEndretVerdi(ref);
        }
        return false;
    }

    private boolean dekningsgradEndretVerdi(BehandlingReferanse ref) {
        var relasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        var overstyrtDekningsgrad = relasjon.getOverstyrtDekningsgrad();
        return overstyrtDekningsgrad.isPresent() && !overstyrtDekningsgrad.get().equals(relasjon.getDekningsgrad());
    }
}
