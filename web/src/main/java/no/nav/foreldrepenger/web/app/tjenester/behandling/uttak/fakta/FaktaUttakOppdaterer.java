package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FaktaUttakDto.class, adapter = AksjonspunktOppdaterer.class)
public class FaktaUttakOppdaterer implements AksjonspunktOppdaterer<FaktaUttakDto> {

    private FaktaUttakFellesTjeneste fellesTjeneste;

    @Inject
    public FaktaUttakOppdaterer(FaktaUttakFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    FaktaUttakOppdaterer() {
        //CDI
    }

    @Override
    public OppdateringResultat oppdater(FaktaUttakDto dto, AksjonspunktOppdaterParameter param) {
        return fellesTjeneste.oppdater(dto.getBegrunnelse(), dto.getPerioder(), param.getBehandlingId(), param.getRef().fagsakId(), false);
    }
}
