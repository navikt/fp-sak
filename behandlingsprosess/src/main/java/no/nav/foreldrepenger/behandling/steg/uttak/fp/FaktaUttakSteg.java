package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakSteg implements UttakSteg {

    private static final Environment ENV = Environment.current();
    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private EøsUttakAnnenpartTjeneste eøsUttakAnnenpartTjeneste;

    @Inject
    public FaktaUttakSteg(FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder,
                          UttakInputTjeneste uttakInputTjeneste,
                          BehandlingRepository behandlingRepository,
                          YtelseFordelingTjeneste ytelseFordelingTjeneste,
                          EøsUttakAnnenpartTjeneste eøsUttakAnnenpartTjeneste) {
        this.faktaUttakAksjonspunktUtleder = faktaUttakAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.eøsUttakAnnenpartTjeneste = eøsUttakAnnenpartTjeneste;
    }

    FaktaUttakSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(kontekst.getBehandlingId());
        var behandlingReferanse = uttakInput.getBehandlingReferanse();
        if (!ytelseFordelingAggregat.avklartAnnenForelderHarRettEØS()) {
            eøsUttakAnnenpartTjeneste.fjernEøsUttak(behandlingReferanse);
        }
        var faktaUttakAP = utledFaktaUttakAp(uttakInput);

        if (ENV.isProd()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(faktaUttakAP);
        }

        var eøsUttakAP = eøsUttakAnnenpartTjeneste.utledUttakIEøsForAnnenpartAP(behandlingReferanse);
        return BehandleStegResultat.utførtMedAksjonspunkter(Stream.concat(faktaUttakAP.stream(), eøsUttakAP.stream()).toList());
    }

    private List<AksjonspunktDefinisjon> utledFaktaUttakAp(UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        if (harÅpentOverstyringAp(behandlingId)) {
            return List.of();
        }
        //Bruker justert her for å reutlede avbrutt AP ved tilbakehopp
        return faktaUttakAksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
    }

    private boolean harÅpentOverstyringAp(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId)
            .harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK);
    }
}
