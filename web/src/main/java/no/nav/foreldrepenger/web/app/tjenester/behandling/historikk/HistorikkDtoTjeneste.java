package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HistorikkDtoTjeneste {

    private HistorikkRepository historikkRepository;

    @Inject
    public HistorikkDtoTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    HistorikkDtoTjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDtoV2> hentForSak(Saksnummer saksnummer) {
        return historikkRepository.hentHistorikkForSaksnummer(saksnummer)
            .stream().map(HistorikkV2Adapter::map)
            .toList();
    }

}
