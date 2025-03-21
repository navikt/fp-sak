package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET)
public class VurderSNNyIArbeidslivetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var nyIArbeidslivetDto = dto.getVurderNyIArbeidslivet();
        var opprinneligNyIArbeidslivetVerdi = getOpprinneligNyIArbeidslivetVerdi(forrigeGrunnlag);
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        linjerBuilder.add(lagHistorikkInnslag(nyIArbeidslivetDto, opprinneligNyIArbeidslivetVerdi));

        return linjerBuilder;
    }

    private Boolean getOpprinneligNyIArbeidslivetVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().getFirst())
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst()
            .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
            .orElse(null);
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkInnslag(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto dto,
                                                             Boolean opprinneligNyIArbeidslivetVerdi) {
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligNyIArbeidslivetVerdi);
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erNyIArbeidslivet());
        if (opprinneligVerdi != nyVerdi) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Selvstendig næringsdrivende", opprinneligVerdi, nyVerdi);
        }
        return null;
    }

    private String konvertBooleanTilFaktaEndretVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? "ny i arbeidslivet" : "ikke ny i arbeidslivet";
    }

}
