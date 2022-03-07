package no.nav.foreldrepenger.behandling.steg.anke;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;

@BehandlingStegRef(kode = "ANKE_MERKNADER")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeMerknaderSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;

    public AnkeMerknaderSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeMerknaderSteg(BehandlingRepository behandlingRepository,
            KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste) {
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (klageAnkeVedtakTjeneste.erBehandletAvKabal(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        if (klageAnkeVedtakTjeneste.skalOversendesTrygdretten(behandling)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
