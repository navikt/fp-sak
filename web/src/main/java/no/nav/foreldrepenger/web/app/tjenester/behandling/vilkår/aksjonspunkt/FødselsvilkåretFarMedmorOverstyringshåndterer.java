package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselvilkåretFarMedmorDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFødselvilkåretFarMedmorDto.class, adapter = Overstyringshåndterer.class)
public class FødselsvilkåretFarMedmorOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringFødselvilkåretFarMedmorDto> {

    FødselsvilkåretFarMedmorOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FødselsvilkåretFarMedmorOverstyringshåndterer(InngangsvilkårTjeneste inngangsvilkårTjeneste, HistorikkinnslagRepository historikkinnslagRepository) {
        super(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, inngangsvilkårTjeneste, historikkinnslagRepository);
    }


    @Override
    public void lagHistorikkInnslag(OverstyringFødselvilkåretFarMedmorDto dto, BehandlingReferanse ref) {
        lagHistorikkInnslagForOverstyrtVilkår(ref, dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_FOEDSEL);
    }
}
