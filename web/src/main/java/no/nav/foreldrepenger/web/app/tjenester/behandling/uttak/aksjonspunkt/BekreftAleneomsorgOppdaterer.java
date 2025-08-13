package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAleneomsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftAleneomsorgOppdaterer implements AksjonspunktOppdaterer<AvklarAleneomsorgVurderingDto> {

    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;
    private HistorikkinnslagRepository historikkRepository;

    BekreftAleneomsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftAleneomsorgOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste, HistorikkinnslagRepository historikkRepository) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAleneomsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var nyRettighetstype = utledRettighetstype(param.getRef().relasjonRolle(), dto);
        var totrinn = faktaOmsorgRettTjeneste.totrinnForRettighetsavklaring(param, nyRettighetstype);

        var historikkinnslagLinjer = faktaOmsorgRettTjeneste.aleneomsorgAvklaringHistorikkLinjer(nyRettighetstype, dto.getBegrunnelse());

        faktaOmsorgRettTjeneste.avklarRettighet(param, nyRettighetstype);

        historikkRepository.lagre(new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getRef().behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .medLinjer(historikkinnslagLinjer)
            .build());

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private Rettighetstype utledRettighetstype(RelasjonsRolleType relasjonsRolleType,
                                               AvklarAleneomsorgVurderingDto dto) {
        if (dto.getAleneomsorg()) {
            return Rettighetstype.ALENEOMSORG;
        } else if (Boolean.TRUE.equals(dto.getAnnenforelderHarRett())) {
            return Rettighetstype.BEGGE_RETT;
        } else if (Boolean.TRUE.equals(dto.getAnnenforelderMottarUføretrygd())) {
            return Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
        } else if (Boolean.TRUE.equals(dto.getAnnenForelderHarRettEØS())) {
            return Rettighetstype.BEGGE_RETT_EØS;
        } else {
            return relasjonsRolleType.erFarEllerMedMor() ? Rettighetstype.BARE_FAR_RETT : Rettighetstype.BARE_MOR_RETT;
        }
    }
}
