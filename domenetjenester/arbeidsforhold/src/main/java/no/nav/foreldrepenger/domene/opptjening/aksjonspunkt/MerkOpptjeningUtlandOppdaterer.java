package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
import no.nav.foreldrepenger.domene.opptjening.dto.MerkOpptjeningUtlandDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
public class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    private OpptjeningIUtlandDokStatusTjeneste tjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public MerkOpptjeningUtlandOppdaterer(OpptjeningIUtlandDokStatusTjeneste tjeneste,
                                          FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.tjeneste = tjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    MerkOpptjeningUtlandOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        tjeneste.lagreStatus(param.getRef().behandlingId(), dto.getDokStatus());
        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(param.getRef().fagsakId(), UtlandDokumentasjonStatus.valueOf(dto.getDokStatus().name()));
        return OppdateringResultat.utenOveropp();
    }
}
