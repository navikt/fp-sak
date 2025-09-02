package no.nav.foreldrepenger.familiehendelse.overstyring;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.familiehendelse.FaktaFødselTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OverstyringFaktaOmFødselDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaOmFødselDto.class, adapter = Overstyringshåndterer.class)
public class FaktaOmFødselOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaOmFødselDto> {
    private FaktaFødselTjeneste faktaFødselTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;


    FaktaOmFødselOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaOmFødselOverstyringshåndterer(FaktaFødselTjeneste faktaFødselTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.faktaFødselTjeneste = faktaFødselTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaOmFødselDto dto, BehandlingReferanse ref) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        return faktaFødselTjeneste.overstyrFaktaOmFødsel(ref, familieHendelse, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            true);
    }
}
