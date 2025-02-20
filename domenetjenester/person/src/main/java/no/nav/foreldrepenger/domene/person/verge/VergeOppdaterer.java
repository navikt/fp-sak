package no.nav.foreldrepenger.domene.person.verge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.person.verge.dto.OpprettVergeDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class VergeOppdaterer implements AksjonspunktOppdaterer<AvklarVergeDto> {

    private OpprettVergeTjeneste opprettVergeTjeneste;

    protected VergeOppdaterer() {
        // CDI
    }

    @Inject
    public VergeOppdaterer(OpprettVergeTjeneste opprettVergeTjeneste) {
        this.opprettVergeTjeneste = opprettVergeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {

        opprettVergeTjeneste.opprettVerge(param.getBehandlingId(), param.getFagsakId(), map(dto));

        return OppdateringResultat.utenOverhopp();
    }

    private OpprettVergeDto map(AvklarVergeDto dto) {
        return new OpprettVergeDto(
                dto.getNavn(),
                dto.getFnr(),
                dto.getGyldigFom(),
                dto.getGyldigTom(),
                dto.getVergeType(),
                dto.getOrganisasjonsnummer(),
                dto.getBegrunnelse()
        );
    }
}
