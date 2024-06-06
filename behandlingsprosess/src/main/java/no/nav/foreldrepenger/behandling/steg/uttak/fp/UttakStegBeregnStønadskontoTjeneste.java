package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;


@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste,
                                               FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                               BehandlingsresultatRepository behandlingsresultatRepository) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        // CDI
    }

    public Stønadskontoberegning fastsettStønadskontoerForBehandling(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var gjeldendeStønadskontoberegning = fagsakRelasjonTjeneste.finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        var kreverNyBeregning = gjeldendeStønadskontoberegning.isPresent() && behandlingHarAvklartDekningsgrad(ref);
        var tidligereStønadskontoberegning = gjeldendeStønadskontoberegning.filter(sb -> !kreverNyBeregning);
        var gjeldendeKontoutregning = tidligereStønadskontoberegning.map(Stønadskontoberegning::getStønadskontoutregning).orElse(Map.of());
        return beregnStønadskontoerTjeneste.beregnForBehandling(input, gjeldendeKontoutregning)
            .or(() -> tidligereStønadskontoberegning)
            .orElseThrow();
    }

    private boolean behandlingHarAvklartDekningsgrad(BehandlingReferanse ref) {
        //Sjekker behandlingsres for å støtte caser der behandling er avklart til søkers oppgitte dekningsgrad,
        //og annen part allerede har committet stønadskontoberegning til fagsakrel
        return dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref)
            || behandlingsresultatRepository.hent(ref.behandlingId()).isEndretDekningsgrad();
    }

}
