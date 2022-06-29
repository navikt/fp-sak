package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET)
public class VurderSNNyIArbeidslivetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var nyIArbeidslivetDto = dto.getVurderNyIArbeidslivet();
        var opprinneligNyIArbeidslivetVerdi = getOpprinneligNyIArbeidslivetVerdi(forrigeGrunnlag);
        lagHistorikkInnslag(nyIArbeidslivetDto, opprinneligNyIArbeidslivetVerdi, tekstBuilder);
    }

    private Boolean getOpprinneligNyIArbeidslivetVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst()
            .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
            .orElse(null);
    }

    private void lagHistorikkInnslag(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto dto, Boolean opprinneligNyIArbeidslivetVerdi,
                                     HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, dto, opprinneligNyIArbeidslivetVerdi, tekstBuilder);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? HistorikkEndretFeltVerdiType.NY_I_ARBEIDSLIVET : HistorikkEndretFeltVerdiType.IKKE_NY_I_ARBEIDSLIVET;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto dto,
                                        Boolean opprinneligNyIArbeidslivetVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligNyIArbeidslivetVerdi);
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erNyIArbeidslivet());
        if(opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(historikkEndretFeltType, opprinneligVerdi, nyVerdi);
        }
    }

}
