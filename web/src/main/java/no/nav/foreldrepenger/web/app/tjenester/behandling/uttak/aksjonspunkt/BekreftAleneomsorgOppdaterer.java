package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAleneomsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftAleneomsorgOppdaterer implements AksjonspunktOppdaterer<AvklarAleneomsorgVurderingDto> {


    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    BekreftAleneomsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftAleneomsorgOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste,
                                        PersonopplysningRepository personopplysningRepository) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAleneomsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {

        if (!dto.getAleneomsorg() && (dto.getAnnenforelderHarRett() == null || (!dto.getAnnenforelderHarRett() && dto.getAnnenforelderMottarUføretrygd() == null))) {
            throw new FunksjonellException("FP-093924", "Avkreftet aleneomsorg mangler verdi for annen forelder rett eller uføretrygd.",
                "Angi om annen forelder har rett eller om annen forelder mottar uføretrygd.");
        }
        var totrinn = faktaOmsorgRettTjeneste.totrinnForAleneomsorg(param, dto.getAleneomsorg());
        faktaOmsorgRettTjeneste.aleneomsorgHistorikkFelt(param, dto.getAleneomsorg());
        faktaOmsorgRettTjeneste.omsorgRettHistorikkInnslag(param, dto.getBegrunnelse());
        faktaOmsorgRettTjeneste.oppdaterAleneomsorg(param, dto.getAleneomsorg());
        if (!dto.getAleneomsorg() && dto.getAnnenforelderHarRett() != null) {
            var opprettUføre = !dto.getAnnenforelderHarRett() && dto.getAnnenforelderMottarUføretrygd() != null;
            var annenpartAktørId = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(param.getBehandlingId())
                .map(OppgittAnnenPartEntitet::getAktørId).orElse(null);
            // Inntil videre ...
            if (opprettUføre && dto.getAnnenforelderMottarUføretrygd() && annenpartAktørId == null) {
                throw new FunksjonellException("FP-093925", "Mangler oppgitt annenpart for saken kan ikke bekrefte uføretrygd.",
                    "Registrer annenpart eller kontakt support.");
            }
            totrinn = totrinn || faktaOmsorgRettTjeneste.totrinnForAnnenforelderRett(param, dto.getAnnenforelderHarRett(), dto.getAnnenforelderMottarUføretrygd(), opprettUføre);
            faktaOmsorgRettTjeneste.annenforelderRettHistorikkFelt(param, dto.getAnnenforelderHarRett(), dto.getAnnenforelderMottarUføretrygd(), opprettUføre);
            faktaOmsorgRettTjeneste.oppdaterAnnenforelderRett(param, dto.getAnnenforelderHarRett(), dto.getAnnenforelderMottarUføretrygd(), opprettUføre, annenpartAktørId);
        }
        faktaOmsorgRettTjeneste.omsorgRettHistorikkInnslag(param, dto.getBegrunnelse());
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

}
