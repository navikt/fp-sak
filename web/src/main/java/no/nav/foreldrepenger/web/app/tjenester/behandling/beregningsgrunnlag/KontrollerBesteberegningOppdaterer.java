package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.KontrollerBesteberegningDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        if (dto.getBesteberegningErKorrekt() == null || !dto.getBesteberegningErKorrekt()) {
            throw new IllegalStateException("Feil: Besteberegningen er ikke godkjent, ugyldig tilstand");
        }
        lagHistorikk(dto);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagHistorikk(KontrollerBesteberegningDto dto) {
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        historikkInnslagTekstBuilder
            .medEndretFelt(HistorikkEndretFeltType.KONTROLL_AV_BESTEBEREGNING, null, dto.getBesteberegningErKorrekt())
            .medSkjermlenke(SkjermlenkeType.BESTEBEREGNING)
            .medBegrunnelse(dto.getBegrunnelse());
    }
}
