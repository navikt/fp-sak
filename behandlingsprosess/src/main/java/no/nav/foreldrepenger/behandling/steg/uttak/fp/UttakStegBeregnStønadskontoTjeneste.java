package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;


@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste,
                                               FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        // CDI
    }

    public Stønadskontoberegning fastsettStønadskontoerForBehandling(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var gjeldendeStønadskontoberegning = fagsakRelasjonTjeneste.finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        var kreverNyBeregning = gjeldendeStønadskontoberegning.isPresent() && dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref) &&
            Dekningsgrad._100.equals(dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref));
        var tidligereStønadskontoberegning = gjeldendeStønadskontoberegning.filter(sb -> !kreverNyBeregning);
        var gjeldendeKontoutregning = tidligereStønadskontoberegning.map(Stønadskontoberegning::getStønadskontoutregning).orElse(Map.of());
        return beregnStønadskontoerTjeneste.beregnForBehandling(input, gjeldendeKontoutregning)
            .or(() -> tidligereStønadskontoberegning)
            .orElseThrow();
    }

}
