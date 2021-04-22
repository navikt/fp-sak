package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.KontrollerBesteberegningDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerBesteberegningDto.class, adapter = AksjonspunktOppdaterer.class)
public class KontrollerBesteberegningOppdaterer implements AksjonspunktOppdaterer<KontrollerBesteberegningDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    protected KontrollerBesteberegningOppdaterer() {
        // CDI
    }

    @Inject
    public KontrollerBesteberegningOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerBesteberegningDto dto, AksjonspunktOppdaterParameter param) {
        var builder = OppdateringResultat.utenTransisjon();
        if (!dto.getBesteberegningErKorrekt()) {
            var frist = LocalDateTime.now().plusDays(14);
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KORRIGERT_BESTEBERERGNING, Venteårsak.VENT_PÅ_KORRIGERT_BESTEBEREGNING, frist);
            builder.medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET);
        }
        lagHistorikk(dto);
        return builder.build();
    }

    private void lagHistorikk(KontrollerBesteberegningDto dto) {
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        historikkInnslagTekstBuilder
            .medEndretFelt(HistorikkEndretFeltType.KONTROLL_AV_BESTEBEREGNING, null, dto.getBesteberegningErKorrekt())
            .medSkjermlenke(SkjermlenkeType.BESTEBEREGNING)
            .medBegrunnelse(dto.getBegrunnelse());
    }
}
