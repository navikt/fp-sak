package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringAvklarStartdatoForPeriodenDto.class, adapter = Overstyringshåndterer.class)
public class OverstyringAvklarStartdatoForPeriodenHåndterer implements Overstyringshåndterer<OverstyringAvklarStartdatoForPeriodenDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    OverstyringAvklarStartdatoForPeriodenHåndterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringAvklarStartdatoForPeriodenHåndterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringAvklarStartdatoForPeriodenDto dto, BehandlingReferanse ref) {
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(ref.behandlingId(), dto.getStartdatoFraSoknad());
        return OppdateringResultat.utenTransisjon().build();
    }

    @Override
    public void lagHistorikkInnslag(OverstyringAvklarStartdatoForPeriodenDto dto, BehandlingReferanse ref) {
        var opprinneligDato = dto.getOpprinneligDato();
        var startdatoFraSoknad = dto.getStartdatoFraSoknad();
        if (!startdatoFraSoknad.isEqual(opprinneligDato)) {
            var historikkinnslag = new Historikkinnslag.Builder().medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(ref.behandlingId())
                .medFagsakId(ref.fagsakId())
                .addLinje(fraTilEquals("Startdato for foreldrepengeperioden", opprinneligDato, startdatoFraSoknad))
                .addLinje(dto.getBegrunnelse())
                .build();
            historikkinnslagRepository.lagre(historikkinnslag);
        }
    }
}
