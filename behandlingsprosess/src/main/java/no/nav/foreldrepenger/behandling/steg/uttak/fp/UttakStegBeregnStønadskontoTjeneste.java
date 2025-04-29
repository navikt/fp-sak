package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;


@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste,
                                               FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                               BehandlingRepository behandlingRepository) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        // CDI
    }

    public Stønadskontoberegning fastsettStønadskontoerForBehandling(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var gjeldendeStønadskontoberegning = fagsakRelasjonTjeneste.finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        var kreverNyBeregning = gjeldendeStønadskontoberegning.isPresent() && (behandlingHarAvklartDekningsgrad(ref) || behandlingHarOverstyrtRettighetstype(ref));
        var tidligereStønadskontoberegning = gjeldendeStønadskontoberegning.filter(sb -> !kreverNyBeregning);
        var gjeldendeKontoutregning = tidligereStønadskontoberegning.map(Stønadskontoberegning::getStønadskontoutregning).orElse(Map.of());
        return beregnStønadskontoerTjeneste.beregnForBehandling(input, gjeldendeKontoutregning)
            .or(() -> tidligereStønadskontoberegning)
            .orElseThrow();
    }

    private boolean behandlingHarOverstyrtRettighetstype(BehandlingReferanse ref) {
        return behandlingRepository.hentBehandling(ref.behandlingId()).harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG);
    }

    private boolean behandlingHarAvklartDekningsgrad(BehandlingReferanse ref) {
        //Sjekker aksjonspunkt for å støtte førstegangsbehandlinger der behandling er avklart til søkers oppgitte dekningsgrad,
        //og annen part allerede har committet stønadskontoberegning til fagsakrel
        return dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref) || harLøstAksjonspunktOmDekningsgrad(ref);
    }

    private boolean harLøstAksjonspunktOmDekningsgrad(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        return behandling.harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.OVERSTYRING_AV_DEKNINGSGRAD)
            || behandling.harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_DEKNINGSGRAD);
    }

}
