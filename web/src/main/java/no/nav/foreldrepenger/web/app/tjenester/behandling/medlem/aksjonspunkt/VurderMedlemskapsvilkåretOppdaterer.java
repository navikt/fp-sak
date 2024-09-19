package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderMedlemskapsvilkåretOppdaterer implements AksjonspunktOppdaterer<VurderMedlemskapDto> {

    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;

    @Inject
    public VurderMedlemskapsvilkåretOppdaterer(
        MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste) {
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
    }

    VurderMedlemskapsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        var avslagskode = dto.getAvslagskode();
        var behandlingId = param.getBehandlingId();
        var opphørFom = dto.getOpphørFom();
        var begrunnelse = dto.getBegrunnelse();
        return medlemskapAksjonspunktFellesTjeneste.oppdater(behandlingId, avslagskode, opphørFom, begrunnelse, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);
    }
}
