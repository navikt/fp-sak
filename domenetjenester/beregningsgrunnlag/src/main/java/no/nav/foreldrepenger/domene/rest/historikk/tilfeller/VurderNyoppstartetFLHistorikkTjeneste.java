package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderNyoppstartetFLDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL)
public class VurderNyoppstartetFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var nyoppstartetDto = dto.getVurderNyoppstartetFL();
        var opprinneligErNyoppstartetFLVerdi = getOpprinneligErNyoppstartetFLVerdi(forrigeGrunnlag);
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        linjerBuilder.add(lagHistorikkInnslag(nyoppstartetDto, opprinneligErNyoppstartetFLVerdi));
        linjerBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);

        return linjerBuilder;
    }

    private Boolean getOpprinneligErNyoppstartetFLVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
            .orElse(null);
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkInnslag(VurderNyoppstartetFLDto dto, Boolean opprinneligErNyoppstartetFLVerdi) {
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligErNyoppstartetFLVerdi);
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erErNyoppstartetFL());
        if (opprinneligVerdi != nyVerdi) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Frilansvirksomhet", opprinneligVerdi, nyVerdi);
        }
        return null;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

}
