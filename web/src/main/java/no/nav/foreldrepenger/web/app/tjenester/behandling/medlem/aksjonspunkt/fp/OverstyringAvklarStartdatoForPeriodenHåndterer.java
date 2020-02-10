package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftStartdatoForPerioden;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAvklarStartdatoForPeriodenDto.class, adapter = Overstyringshåndterer.class)
public class OverstyringAvklarStartdatoForPeriodenHåndterer extends AbstractOverstyringshåndterer<OverstyringAvklarStartdatoForPeriodenDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    OverstyringAvklarStartdatoForPeriodenHåndterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringAvklarStartdatoForPeriodenHåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                          YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringAvklarStartdatoForPeriodenDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(behandling.getId(), new BekreftStartdatoForPerioden(dto.getStartdatoFraSoknad()));
        avbrytOverflødignormaltAksjonpunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> avbrytOverflødignormaltAksjonpunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringAvklarStartdatoForPeriodenDto dto) {
        LocalDate opprinneligDato = dto.getOpprinneligDato();
        LocalDate startdatoFraSoknad = dto.getStartdatoFraSoknad();
        if (!startdatoFraSoknad.equals(opprinneligDato)) {
            getHistorikkAdapter().tekstBuilder()
                .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
                .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
                .medBegrunnelse(dto.getBegrunnelse())
                .medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, opprinneligDato, startdatoFraSoknad);
        }
    }
}
