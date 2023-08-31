package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class FpOversiktDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FpDtoTjeneste fpDtoTjeneste;
    private SvpDtoTjeneste svpDtoTjeneste;
    private EsDtoTjeneste esDtoTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    @Inject
    public FpOversiktDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 FpDtoTjeneste fpDtoTjeneste,
                                 SvpDtoTjeneste svpDtoTjeneste,
                                 EsDtoTjeneste esDtoTjeneste,
                                 KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fpDtoTjeneste = fpDtoTjeneste;
        this.svpDtoTjeneste = svpDtoTjeneste;
        this.esDtoTjeneste = esDtoTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
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

    public List<DokumentTypeId> hentmanglendeVedleggForSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        var behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        if (!behandlingOpt.isPresent()) {
            return List.of();
        }
        var behandling = behandlingOpt.get();
        var kompletthetsjekker = kompletthetsjekkerProvider.finnKompletthetsjekkerFor(fagsak.getYtelseType(), behandling.getType());
        return tilDto(kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(BehandlingReferanse.fra(behandling)));
    }

    private List<DokumentTypeId> tilDto(List<ManglendeVedlegg> manglendeVedleggs) {
        return manglendeVedleggs.stream()
                .map(ManglendeVedlegg::getDokumentType)
                .toList();
    }
}
