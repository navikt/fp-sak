package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.opptjening.dto.MerkOpptjeningUtlandDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
public class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    private OpptjeningIUtlandDokStatusTjeneste tjeneste;

    @Inject
    public MerkOpptjeningUtlandOppdaterer(OpptjeningIUtlandDokStatusTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    MerkOpptjeningUtlandOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        tjeneste.lagreStatus(param.getRef().behandlingId(), dto.getDokStatus());
        return OppdateringResultat.utenOveropp();
    }
}
