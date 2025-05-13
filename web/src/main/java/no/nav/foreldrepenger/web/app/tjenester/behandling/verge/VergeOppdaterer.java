package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class VergeOppdaterer implements AksjonspunktOppdaterer<AvklarVergeDto> {

    private BehandlingRepository behandlingRepository;
    private VergeTjeneste vergeTjeneste;

    protected VergeOppdaterer() {
        // CDI
    }

    @Inject
    public VergeOppdaterer(BehandlingRepository behandlingRepository, VergeTjeneste vergeTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vergeTjeneste = vergeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {

        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        vergeTjeneste.opprettVerge(behandling, map(dto), dto.getBegrunnelse());

        return OppdateringResultat.utenOverhopp();
    }

    private VergeDto map(AvklarVergeDto dto) {
        return new VergeDto(dto.getVergeType(), dto.getGyldigFom(), dto.getGyldigTom(), dto.getNavn(), dto.getFnr(), dto.getOrganisasjonsnummer());
    }


}
