package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
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
                                                Historikkinnslag2Repository historikkinnslag2Repository) {
        super(VilkårType.FØDSELSVILKÅRET_MOR, inngangsvilkårTjeneste, historikkinnslag2Repository);
    }

    @Override
    public void lagHistorikkInnslag(OverstyringFødselsvilkåretDto dto, Behandling behandling) {
        lagHistorikkInnslagForOverstyrtVilkår(behandling, dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_FOEDSEL);
    }
}
