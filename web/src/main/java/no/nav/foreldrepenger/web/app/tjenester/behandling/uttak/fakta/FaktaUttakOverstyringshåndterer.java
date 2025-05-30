package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaUttakDto.class, adapter = Overstyringshåndterer.class)
public class FaktaUttakOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaUttakDto> {

    private FaktaUttakFellesTjeneste fellesTjeneste;

    @Inject
    public FaktaUttakOverstyringshåndterer(FaktaUttakFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    FaktaUttakOverstyringshåndterer() {
        //CDI
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto dto, BehandlingReferanse ref) {
        return fellesTjeneste.oppdater(dto.getBegrunnelse(), dto.getPerioder(), ref.behandlingId(), ref.fagsakId(), true);
    }
}
