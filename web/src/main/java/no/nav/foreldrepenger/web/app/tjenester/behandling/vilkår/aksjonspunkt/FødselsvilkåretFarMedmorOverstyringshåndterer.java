package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselvilkåretFarMedmorDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFødselvilkåretFarMedmorDto.class, adapter = Overstyringshåndterer.class)
public class FødselsvilkåretFarMedmorOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringFødselvilkåretFarMedmorDto> {

    FødselsvilkåretFarMedmorOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FødselsvilkåretFarMedmorOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
            inngangsvilkårTjeneste);
    }


    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringFødselvilkåretFarMedmorDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_FOEDSEL);
    }
}
