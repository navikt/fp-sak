package no.nav.foreldrepenger.behandling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class DekningsgradTjeneste {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    DekningsgradTjeneste() {
        //CDI
    }

    @Inject
    public DekningsgradTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                BehandlingsresultatRepository behandlingsresultatRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public boolean behandlingHarEndretDekningsgrad(BehandlingReferanse ref) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (behandlingsresultat.isPresent() && behandlingsresultat.get().isEndretDekningsgrad()) {
            return dekningsgradEndretVerdi(ref);
        }
        return false;
    }

    private boolean dekningsgradEndretVerdi(BehandlingReferanse ref) {
        FagsakRelasjon relasjon = relasjon(ref);
        return relasjon.getOverstyrtDekningsgrad().isPresent() && !relasjon.getOverstyrtDekningsgrad().get().equals(relasjon.getDekningsgrad());
    }

    private FagsakRelasjon relasjon(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonFor(ref.getSaksnummer());
    }

    public Optional<Dekningsgrad> finnDekningsgrad(Saksnummer saksnummer) {
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer);
        if (fagsakRelasjon.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(fagsakRelasjon.get().getGjeldendeDekningsgrad());
    }
}
