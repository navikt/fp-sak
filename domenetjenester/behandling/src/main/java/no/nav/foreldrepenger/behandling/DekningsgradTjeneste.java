package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.util.Optional;

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

    public boolean behandlingHarEndretDekningsgrad(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.behandlingId());
        if (behandlingsresultat.isPresent() && behandlingsresultat.get().isEndretDekningsgrad()) {
            return dekningsgradEndretVerdi(ref);
        }
        return false;
    }

    private boolean dekningsgradEndretVerdi(BehandlingReferanse ref) {
        var relasjon = relasjon(ref);
        var overstyrtDekningsgrad = relasjon.getOverstyrtDekningsgrad();
        return overstyrtDekningsgrad.isPresent() && !overstyrtDekningsgrad.get().equals(relasjon.getDekningsgrad());
    }

    private FagsakRelasjon relasjon(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
    }

    public Optional<Dekningsgrad> finnDekningsgrad(Saksnummer saksnummer) {
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer).map(FagsakRelasjon::getGjeldendeDekningsgrad);
    }
}
