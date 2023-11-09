package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.api.BekreftedePerioderAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFortsattMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarFortsattMedlemskapOppdaterer implements AksjonspunktOppdaterer<AvklarFortsattMedlemskapDto> {

    private MedlemskapAksjonspunktTjeneste medlemTjeneste;

    AvklarFortsattMedlemskapOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarFortsattMedlemskapOppdaterer(MedlemskapAksjonspunktTjeneste medlemTjeneste) {
        this.medlemTjeneste = medlemTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarFortsattMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        var adapter = new AvklarFortsattMedlemskapAksjonspunktDto(mapTilAdapterFra(dto));

        medlemTjeneste.aksjonspunktAvklarFortsattMedlemskap(param.getBehandlingId(), adapter);
        return OppdateringResultat.utenOveropp();
    }

    private List<BekreftedePerioderAdapter> mapTilAdapterFra(AvklarFortsattMedlemskapDto dto) {
        return dto.getBekreftedePerioder().stream().map(AvklarFortsattMedlemskapOppdaterer::map).toList();
    }

    private static BekreftedePerioderAdapter map(BekreftedePerioderDto periode) {
        var adapter = new BekreftedePerioderAdapter();
        if (periode.getMedlemskapManuellVurderingType() != null) {
            adapter.setMedlemskapManuellVurderingType(periode.getMedlemskapManuellVurderingType());
        }
        adapter.setAksjonspunkter(periode.getAksjonspunkter());
        adapter.setBosattVurdering(periode.getBosattVurdering());
        adapter.setErEosBorger(periode.getErEosBorger());
        adapter.setLovligOppholdVurdering(periode.getLovligOppholdVurdering());
        adapter.setVurderingsdato(periode.getVurderingsdato());
        adapter.setOmsorgsovertakelseDato(periode.getOmsorgsovertakelseDato());
        adapter.setOppholdsrettVurdering(periode.getOppholdsrettVurdering());
        adapter.setBegrunnelse(periode.getBegrunnelse());
        return adapter;
    }
}
