package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.opptjening.dto.MerkOpptjeningUtlandDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
