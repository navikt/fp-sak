package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.fp;

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
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.InngangsvilkårOverstyringshåndterer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAdopsjonsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class AdopsjonsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringAdopsjonsvilkåretDto> {

    AdopsjonsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public AdopsjonsvilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET_FP,
            VilkårType.ADOPSJONSVILKARET_FORELDREPENGER,
            inngangsvilkårTjeneste);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringAdopsjonsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_ADOPSJON);
    }
}
