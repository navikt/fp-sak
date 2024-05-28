package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;

@ApplicationScoped
public class OppdatereFagsakRelasjonVedVedtak {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FpUttakRepository uttakRepository;


    OppdatereFagsakRelasjonVedVedtak() {
        // CDI
    }

    @Inject
    public OppdatereFagsakRelasjonVedVedtak(FpUttakRepository uttakRepository,
                                            FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.uttakRepository = uttakRepository;
    }



    public void oppdaterRelasjonVedVedtattBehandling(Behandling behandling) {
        var uttak = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId()).orElse(null);
        if (uttak == null || uttak.getStønadskontoberegning().getStønadskontoutregning().isEmpty()) {
            // Ikke Foreldrepenger eller avslag inngangsvilkår
            return;
        }
        var fagsakrelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(behandling.getFagsak());
        var gjeldendeKontoutregning = fagsakrelasjon.getGjeldendeStønadskontoberegning()
            .map(Stønadskontoberegning::getStønadskontoutregning)
            .orElseGet(Map::of);
        if (!UtregnetStønadskontoTjeneste.harSammeAntallStønadsdager(gjeldendeKontoutregning, uttak.getStønadskontoberegning().getStønadskontoutregning())) {
            if (fagsakrelasjon.getOverstyrtStønadskontoberegning().isPresent()) {
                fagsakRelasjonTjeneste.nullstillOverstyrtStønadskontoberegning(behandling.getFagsak());
                fagsakrelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(behandling.getFagsak());
            }
            fagsakRelasjonTjeneste.lagre(behandling.getFagsakId(), fagsakrelasjon, behandling.getId(), uttak.getStønadskontoberegning());
        }
    }
}
