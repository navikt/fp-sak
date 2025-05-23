package no.nav.foreldrepenger.behandling.steg.anke;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;

@BehandlingStegRef(BehandlingStegType.ANKE_MERKNADER)
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

        if (klageAnkeVedtakTjeneste.harKjennelseTrygdretten(behandling)) {
            if (klageAnkeVedtakTjeneste.erOversendtTrygdretten(behandling)) {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            } else {
                throw new IllegalStateException("AnkeMerknaderSteg: KabalAnke Har lagret TR-kjennelse, mangler oversendelsesdato");
            }
        }
        if (klageAnkeVedtakTjeneste.erOversendtTrygdretten(behandling)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåTrygderetten());
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private AksjonspunktResultat ventPåTrygderetten() {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN, Venteårsak.VENT_KABAL, null);
    }



}
