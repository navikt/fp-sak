package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;

public abstract class InngangsvilkårOverstyringshåndterer<T extends OverstyringAksjonspunktDto> extends AbstractOverstyringshåndterer<T> {

    private VilkårType vilkårType;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    protected InngangsvilkårOverstyringshåndterer() {
        // for CDI proxy
    }

    public InngangsvilkårOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
            AksjonspunktDefinisjon aksjonspunktDefinisjon,
            VilkårType vilkårType,
            InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, aksjonspunktDefinisjon);
        this.vilkårType = vilkårType;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(T dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        VilkårUtfallType utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;

        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), vilkårType, utfall, dto.getAvslagskode(), kontekst);

        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }

        return OppdateringResultat.utenOveropp();
    }
}
