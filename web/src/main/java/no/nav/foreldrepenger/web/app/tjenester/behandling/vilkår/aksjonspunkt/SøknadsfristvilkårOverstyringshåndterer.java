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
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringSøknadsfristvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringSøknadsfristvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class SøknadsfristvilkårOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringSøknadsfristvilkåretDto> {

    SøknadsfristvilkårOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public SøknadsfristvilkårOverstyringshåndterer(InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                   HistorikkinnslagRepository historikkinnslagRepository) {
        super(VilkårType.SØKNADSFRISTVILKÅRET, inngangsvilkårTjeneste, historikkinnslagRepository);
    }

    @Override
    public void lagHistorikkInnslag(OverstyringSøknadsfristvilkåretDto dto, BehandlingReferanse ref) {
        lagHistorikkInnslagForOverstyrtVilkår(ref, dto, SkjermlenkeType.SOEKNADSFRIST);
    }

}
