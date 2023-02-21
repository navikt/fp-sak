package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_UTTAK)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaUttakSteg implements UttakSteg {

    private RyddFaktaUttakTjeneste ryddFaktaUttakTjeneste;

    @Inject
    public KontrollerFaktaUttakSteg(RyddFaktaUttakTjeneste ryddFaktaUttakTjeneste) {
        this.ryddFaktaUttakTjeneste = ryddFaktaUttakTjeneste;
    }

    KontrollerFaktaUttakSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        ryddFaktaUttakTjeneste.ryddVedHoppOverBakover(kontekst);
    }
}
