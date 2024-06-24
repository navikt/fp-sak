package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class OppdatereFagsakRelasjonVedVedtak {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FpUttakRepository uttakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;


    OppdatereFagsakRelasjonVedVedtak() {
        // CDI
    }

    @Inject
    public OppdatereFagsakRelasjonVedVedtak(FpUttakRepository uttakRepository,
                                            FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                            YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.uttakRepository = uttakRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }



    public void oppdaterRelasjonVedVedtattBehandling(Behandling behandling) {
        var uttak = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId()).orElse(null);
        if (uttak == null || uttak.getStønadskontoberegning().getStønadskontoutregning().isEmpty()) {
            // Ikke Foreldrepenger eller avslag inngangsvilkår
            return;
        }
        var fagsakrelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(behandling.getFagsak());
        var gjeldendeKontoutregning = fagsakrelasjon.getStønadskontoberegning()
            .map(Stønadskontoberegning::getStønadskontoutregning)
            .orElseGet(Map::of);
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        if (!UtregnetStønadskontoTjeneste.harSammeAntallStønadsdager(gjeldendeKontoutregning, uttak.getStønadskontoberegning().getStønadskontoutregning())) {
            fagsakRelasjonTjeneste.lagre(behandling.getFagsakId(), uttak.getStønadskontoberegning());
        }
        if (!Objects.equals(ytelseFordelingAggregat.getGjeldendeDekningsgrad(), fagsakrelasjon.getGjeldendeDekningsgrad())) {
            var dekningsgrad = ytelseFordelingAggregat.getGjeldendeDekningsgrad();
            fagsakRelasjonTjeneste.oppdaterDekningsgrad(behandling.getFagsakId(), dekningsgrad);
        }
    }
}
