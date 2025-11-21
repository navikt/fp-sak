package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class BeregningOversiktDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;
    private FagsakRepository fagsakRepository;


    @Inject
    public BeregningOversiktDtoTjeneste(BehandlingRepository behandlingRepository, BeregningTjeneste beregningTjeneste,
                                        FagsakRepository fagsakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public BeregningOversiktDtoTjeneste() {}

    public FpSakBeregningDto hentBeregningForSak(Saksnummer saksnummer) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(saksnummer).map(Fagsak::getId).orElseThrow(() -> new IllegalStateException("Fikk saksnummer som ikke finnes"));
        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
    }


}
