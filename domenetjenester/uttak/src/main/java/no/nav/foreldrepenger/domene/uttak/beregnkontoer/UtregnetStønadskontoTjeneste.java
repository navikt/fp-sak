package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.util.Map;
import java.util.Optional;

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
                .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
    }

    public Map<StønadskontoType, Integer> gjeldendeKontoutregning(Long behandlingId, FagsakRelasjon fagsakRelasjon) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandlingId)
            .map(ForeldrepengerUttak::getStønadskontoBeregning)
            .or(() -> Optional.ofNullable(fagsakRelasjon)
                .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
    }
}
