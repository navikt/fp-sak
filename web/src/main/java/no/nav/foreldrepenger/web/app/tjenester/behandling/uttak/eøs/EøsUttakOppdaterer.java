package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;

@ApplicationScoped
@DtoTilServiceAdapter(dto = EøsUttakDto.class, adapter = AksjonspunktOppdaterer.class)
public class EøsUttakOppdaterer implements AksjonspunktOppdaterer<EøsUttakDto> {

    private final EøsUttakFellesTjeneste fellesTjeneste;

    @Inject
    public EøsUttakOppdaterer(EøsUttakFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(EøsUttakDto dto, AksjonspunktOppdaterParameter param) {
        return fellesTjeneste.oppdater(param.getRef(), dto.getPerioder(), dto.getBegrunnelse(), false);
    }
}
