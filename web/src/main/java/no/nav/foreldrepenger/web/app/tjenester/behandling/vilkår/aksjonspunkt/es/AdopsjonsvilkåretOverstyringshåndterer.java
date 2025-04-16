package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.InngangsvilkårOverstyringshåndterer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAdopsjonsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class AdopsjonsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringAdopsjonsvilkåretDto> {

    AdopsjonsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public AdopsjonsvilkåretOverstyringshåndterer(InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                  HistorikkinnslagRepository historikkinnslagRepository) {
        super(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, inngangsvilkårTjeneste, historikkinnslagRepository);
    }

    @Override
    public void lagHistorikkInnslag(OverstyringAdopsjonsvilkåretDto dto, BehandlingReferanse ref) {
        lagHistorikkInnslagForOverstyrtVilkår(ref, dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_ADOPSJON);
    }
}
