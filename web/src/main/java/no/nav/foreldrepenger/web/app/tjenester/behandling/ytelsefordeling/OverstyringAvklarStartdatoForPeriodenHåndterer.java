package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAvklarStartdatoForPeriodenDto.class, adapter = Overstyringshåndterer.class)
public class OverstyringAvklarStartdatoForPeriodenHåndterer extends AbstractOverstyringshåndterer<OverstyringAvklarStartdatoForPeriodenDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    OverstyringAvklarStartdatoForPeriodenHåndterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringAvklarStartdatoForPeriodenHåndterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                          Historikkinnslag2Repository historikkinnslag2Repository) {
        super(AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringAvklarStartdatoForPeriodenDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(behandling.getId(), dto.getStartdatoFraSoknad());
        return OppdateringResultat.utenTransisjon().build();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringAvklarStartdatoForPeriodenDto dto) {
        var opprinneligDato = dto.getOpprinneligDato();
        var startdatoFraSoknad = dto.getStartdatoFraSoknad();
        if (!startdatoFraSoknad.isEqual(opprinneligDato)) {
            var historikkinnslag = new Historikkinnslag2.Builder().medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(behandling.getId())
                .medFagsakId(behandling.getFagsakId())
                .addTekstlinje(fraTilEquals("Startdato fra søknad", opprinneligDato, startdatoFraSoknad))
                .addTekstlinje(dto.getBegrunnelse())
                .build();
            historikkinnslag2Repository.lagre(historikkinnslag);
        }
    }
}
