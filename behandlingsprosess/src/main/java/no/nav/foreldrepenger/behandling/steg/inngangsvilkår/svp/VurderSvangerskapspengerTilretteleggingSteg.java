package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_TILRETTELEGGING)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderSvangerskapspengerTilretteleggingSteg implements BehandlingSteg {
    private NyeTilretteleggingerTjeneste tilretteleggingerTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;


    @Inject
    public VurderSvangerskapspengerTilretteleggingSteg(NyeTilretteleggingerTjeneste tilretteleggingerTjeneste,
                                                       BehandlingRepository behandlingRepository,
                                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.tilretteleggingerTjeneste = tilretteleggingerTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        tilretteleggingerTjeneste.utledNyeTilretteleggingerLagreJustert(behandling, skjæringstidspunkter);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(
            List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING)));
    }
}
