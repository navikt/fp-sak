package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringEøsUttakDto.class, adapter = Overstyringshåndterer.class)
public class EøsUttakOverstyringshåndterer implements Overstyringshåndterer<OverstyringEøsUttakDto> {

    private final EøsUttakFellesTjeneste fellesTjeneste;

    @Inject
    public EøsUttakOverstyringshåndterer(EøsUttakFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringEøsUttakDto dto, BehandlingReferanse ref) {
        return fellesTjeneste.oppdater(ref, dto.getPerioder(), dto.getBegrunnelse(), true);
    }
}
