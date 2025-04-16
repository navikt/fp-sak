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
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFødselsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class FødselsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringFødselsvilkåretDto> {

    FødselsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FødselsvilkåretOverstyringshåndterer(InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                HistorikkinnslagRepository historikkinnslagRepository) {
        super(VilkårType.FØDSELSVILKÅRET_MOR, inngangsvilkårTjeneste, historikkinnslagRepository);
    }

    @Override
    public void lagHistorikkInnslag(OverstyringFødselsvilkåretDto dto, BehandlingReferanse ref) {
        lagHistorikkInnslagForOverstyrtVilkår(ref, dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_FOEDSEL);
    }
}
