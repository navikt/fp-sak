package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;


@ApplicationScoped
public class UtregnetStønadskontoTjeneste {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;


    @Inject
    public UtregnetStønadskontoTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                        ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }

    UtregnetStønadskontoTjeneste() {
        //For CDI
    }

    public Map<StønadskontoType, Integer> gjeldendeKontoutregning(BehandlingReferanse ref) {
        return uttakTjeneste.hentUttakHvisEksisterer(ref.behandlingId())
            .map(ForeldrepengerUttak::getStønadskontoBeregning)
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(ref.fagsakId())
                .flatMap(FagsakRelasjon::getStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
    }

    public Map<StønadskontoType, Integer> gjeldendeKontoutregning(Long behandlingId, FagsakRelasjon fagsakRelasjon) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandlingId)
            .map(ForeldrepengerUttak::getStønadskontoBeregning)
            .or(() -> Optional.ofNullable(fagsakRelasjon)
                .flatMap(FagsakRelasjon::getStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
    }

    public static boolean harSammeAntallStønadsdager(Map<StønadskontoType, Integer> forrigeUtregning, Map<StønadskontoType, Integer> nyUtregning) {
        var forrigeDager = forrigeUtregning.entrySet().stream()
            .filter(e -> e.getKey().erStønadsdager())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var nyeDager = nyUtregning.entrySet().stream()
            .filter(e -> e.getKey().erStønadsdager())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return forrigeDager.equals(nyeDager);
    }

    public static boolean harEndretStrukturEllerRedusertAntallStønadsdager(Map<StønadskontoType, Integer> forrigeUtregning,
                                                                           Map<StønadskontoType, Integer> nyUtregning) {
        var nyutregnetForeldrepenger = nyUtregning.getOrDefault(StønadskontoType.FORELDREPENGER, 0);
        var eksisterendeForeldrepenger = forrigeUtregning.getOrDefault(StønadskontoType.FORELDREPENGER, 0);
        if (nyutregnetForeldrepenger > 0 && eksisterendeForeldrepenger > 0) {
            return nyutregnetForeldrepenger < eksisterendeForeldrepenger;
        }
        var nyutregnetFellesperiode = nyUtregning.getOrDefault(StønadskontoType.FELLESPERIODE, 0);
        var eksisterendeFellesperiode = forrigeUtregning.getOrDefault(StønadskontoType.FELLESPERIODE, 0);
        if (nyutregnetFellesperiode > 0 && eksisterendeFellesperiode > 0) {
            return nyutregnetFellesperiode < eksisterendeFellesperiode;
        } else {
            // Endret fra Foreldrepenger til Kvoter eller omvendt
            return (nyutregnetForeldrepenger > 0 && eksisterendeFellesperiode > 0) ||  (nyutregnetFellesperiode > 0 && eksisterendeForeldrepenger > 0);
        }
    }
}
