package no.nav.foreldrepenger.behandling.steg.klage;

import static java.util.Collections.singletonList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;


@BehandlingStegRef(kode = "VURDER_FK_OI")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderFormkrafNkSteg implements BehandlingSteg {

    private KlageRepository klageRepository;

    public VurderFormkrafNkSteg(){
        // For CDI proxy
    }

    @Inject
    public VurderFormkrafNkSteg(KlageRepository klageRepository){
        this.klageRepository = klageRepository;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        KlageVurdering klageVurderingResultat = klageRepository.hentKlageVurderingResultat(kontekst.getBehandlingId(), KlageVurdertAv.NFP)
            .map(KlageVurderingResultat::getKlageVurdering)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal alltid ha klagevurdering fra NFP "));
        if (klageVurderingResultat.equals(KlageVurdering.STADFESTE_YTELSESVEDTAK)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(
                AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}


