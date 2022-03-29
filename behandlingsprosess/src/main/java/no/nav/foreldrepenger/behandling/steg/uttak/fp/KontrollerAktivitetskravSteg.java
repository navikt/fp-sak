package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder;

@BehandlingStegRef(kode = BehandlingStegKoder.KONTROLLER_AKTIVITETSKRAV_KODE)
@FagsakYtelseTypeRef("FP")
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerAktivitetskravSteg implements UttakSteg {

    private KontrollerAktivitetskravAksjonspunktUtleder aksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;


    @Inject
    public KontrollerAktivitetskravSteg(KontrollerAktivitetskravAksjonspunktUtleder aksjonspunktUtleder,
                                        UttakInputTjeneste uttakInputTjeneste,
                                        YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.aksjonspunktUtleder = aksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    KontrollerAktivitetskravSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktUtleder.utledFor(uttakInput));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        var behandlingId = kontekst.getBehandlingId();
        var yfa = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medSaksbehandledeAktivitetskravPerioder(null)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, yfa);
    }
}
