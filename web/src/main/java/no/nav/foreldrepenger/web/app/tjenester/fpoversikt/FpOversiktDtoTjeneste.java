package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FpOversiktDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private FagsakRepository fagsakRepository;
    private FpDtoTjeneste fpDtoTjeneste;
    private SvpDtoTjeneste svpDtoTjeneste;
    private EsDtoTjeneste esDtoTjeneste;

    @Inject
    public FpOversiktDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 FpDtoTjeneste fpDtoTjeneste,
                                 SvpDtoTjeneste svpDtoTjeneste,
                                 EsDtoTjeneste esDtoTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fpDtoTjeneste = fpDtoTjeneste;
        this.svpDtoTjeneste = svpDtoTjeneste;
        this.esDtoTjeneste = esDtoTjeneste;
    }

    FpOversiktDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        LOG.info("Henter sak {}", fagsak.getSaksnummer());
        return switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> esDtoTjeneste.hentSak(fagsak);
            case FORELDREPENGER -> fpDtoTjeneste.hentSak(fagsak);
            case SVANGERSKAPSPENGER -> svpDtoTjeneste.hentSak(fagsak);
            case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + fagsak.getYtelseType());
        };
    }
}
