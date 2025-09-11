package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.familiehendelse.FaktaFødselTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFødselAksjonspunktDto;


@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFødselAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFødselAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private FaktaFødselTjeneste faktaFødselTjeneste;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste, FaktaFødselTjeneste faktaFødselTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.faktaFødselTjeneste = faktaFødselTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFødselAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());
        return faktaFødselTjeneste.overstyrFaktaOmFødsel(param.getRef(), familieHendelse, dto.getTermindato(), dto.getBarn(), dto.getBegrunnelse(),
            false);
    }
}
