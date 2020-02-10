package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
        Behandling behandling = param.getBehandling();
        final AvklarFortsattMedlemskapAksjonspunktDto adapter = new AvklarFortsattMedlemskapAksjonspunktDto(mapTilAdapterFra(dto));

        medlemTjeneste.aksjonspunktAvklarFortsattMedlemskap(behandling.getId(), adapter);
        return OppdateringResultat.utenOveropp();
    }

    private List<BekreftedePerioderAdapter> mapTilAdapterFra(AvklarFortsattMedlemskapDto dto) {
        List<BekreftedePerioderAdapter> resultat = new ArrayList<>();
        dto.getBekreftedePerioder().forEach(periode -> {
            BekreftedePerioderAdapter adapter = new BekreftedePerioderAdapter();
            if (periode.getMedlemskapManuellVurderingType() != null) {
            adapter.setMedlemskapManuellVurderingType(periode.getMedlemskapManuellVurderingType());
            }
            adapter.setAksjonspunkter(periode.getAksjonspunkter());
            adapter.setBosattVurdering(periode.getBosattVurdering());
            adapter.setErEosBorger(periode.getErEosBorger());
            adapter.setFodselsdato(periode.getFodselsdato());
            adapter.setLovligOppholdVurdering(periode.getLovligOppholdVurdering());
            adapter.setVurderingsdato(periode.getVurderingsdato());
            adapter.setOmsorgsovertakelseDato(periode.getOmsorgsovertakelseDato());
            adapter.setOppholdsrettVurdering(periode.getOppholdsrettVurdering());
            adapter.setBegrunnelse(periode.getBegrunnelse());
            resultat.add(adapter);
        });
        return resultat;
    }
}
