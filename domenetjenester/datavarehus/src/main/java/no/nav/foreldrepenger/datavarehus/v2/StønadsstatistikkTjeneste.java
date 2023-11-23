package no.nav.foreldrepenger.datavarehus.v2;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class StønadsstatistikkTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakTjeneste fagsakTjeneste;

    @Inject
    public StønadsstatistikkTjeneste(BehandlingRepository behandlingRepository,
                                     FagsakRelasjonRepository fagsakRelasjonRepository,
                                     YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                     FagsakTjeneste fagsakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    StønadsstatistikkTjeneste() {
        //CDI
    }

    public StønadsstatistikkVedtak genererVedtak(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var fagsak = behandling.getFagsak();
        return new StønadsstatistikkVedtak(map(fagsak.getSaksnummer()), map(fagsak.getYtelseType()), behandling.getUuid(), map(fagsak.getAktørId()),
            utledRettigheter(behandling));
    }

    private StønadsstatistikkVedtak.ForeldrepengerRettigheter utledRettigheter(Behandling behandling) {
        var fagsak = fagsakTjeneste.finnEksaktFagsak(behandling.getFagsakId());
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(fagsak);

        var gjeldendeStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        var konti = gjeldendeStønadskontoberegning.stream()
            .flatMap(b -> b.getStønadskontoer().stream())
            .filter(sk -> sk.getStønadskontoType() != StønadskontoType.FLERBARNSDAGER)
            .map(StønadsstatistikkTjeneste::map)
            .collect(Collectors.toSet());


        var rettighetType = StønadsstatistikkVedtak.RettighetType.BEGGE_RETT; // TODO hente fra vedtaksperiode eller finne på sak?;
        var flerbarnsdager = gjeldendeStønadskontoberegning.stream()
            .flatMap(b -> b.getStønadskontoer().stream())
            .filter(sk -> sk.getStønadskontoType() == StønadskontoType.FLERBARNSDAGER)
            .findFirst()
            .map(sk -> new StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager(sk.getMaxDager()))
            .orElse(null);
        var prematurdager = StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager.ZERO; //TODO;
        var minsterett = StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager.ZERO; //TODO
        var dekningsgrad = map(fagsakRelasjon.getDekningsgrad());
        return new StønadsstatistikkVedtak.ForeldrepengerRettigheter(dekningsgrad, rettighetType, konti, flerbarnsdager, prematurdager, minsterett);
    }

    private StønadsstatistikkVedtak.Dekningsgrad map(Dekningsgrad dekningsgrad) {
        return switch (dekningsgrad.getVerdi()) {
            case 80 -> StønadsstatistikkVedtak.Dekningsgrad.ÅTTI;
            case 100 -> StønadsstatistikkVedtak.Dekningsgrad.HUNDRE;
            default -> throw new IllegalStateException("Unexpected value: " + dekningsgrad.getVerdi());
        };
    }

    private static StønadsstatistikkVedtak.ForeldrepengerRettigheter.Stønadskonto map(Stønadskonto stønadskonto) {
        StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager minsterett = null; //TODO
        var stønadskontoType = map(stønadskonto.getStønadskontoType());
        var maksdager = map(stønadskonto.getMaxDager());
        return new StønadsstatistikkVedtak.ForeldrepengerRettigheter.Stønadskonto(stønadskontoType, maksdager, minsterett);
    }

    private static StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager map(int maxDager) {
        return new StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager(maxDager);
    }

    private static StønadsstatistikkVedtak.StønadskontoType map(StønadskontoType stønadskontoType) {
        return switch (stønadskontoType) {
            case FELLESPERIODE -> StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE;
            case MØDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE;
            case FEDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE;
            case FORELDREPENGER -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FLERBARNSDAGER, UDEFINERT -> throw new IllegalStateException("Unexpected value: " + stønadskontoType);
        };
    }

    private static StønadsstatistikkVedtak.AktørId map(AktørId aktørId) {
        return new StønadsstatistikkVedtak.AktørId(aktørId.getId());
    }

    private static StønadsstatistikkVedtak.YtelseType map(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> StønadsstatistikkVedtak.YtelseType.ENGANGSSTØNAD;
            case FORELDREPENGER -> StønadsstatistikkVedtak.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> StønadsstatistikkVedtak.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + ytelseType);
        };
    }

    private static StønadsstatistikkVedtak.Saksnummer map(Saksnummer saksnummer) {
        return new StønadsstatistikkVedtak.Saksnummer(saksnummer.getVerdi());
    }
}
