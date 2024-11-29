package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

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
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        return fellesTjeneste.oppdater(dto.getBegrunnelse(), dto.getPerioder(), behandling.getId(), behandling.getFagsakId(), true);
    }
}
