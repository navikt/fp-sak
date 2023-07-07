package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderNyoppstartetFLDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL)
public class VurderNyoppstartetFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var nyoppstartetDto = dto.getVurderNyoppstartetFL();
        var opprinneligErNyoppstartetFLVerdi = getOpprinneligErNyoppstartetFLVerdi(forrigeGrunnlag);
        lagHistorikkInnslag(nyoppstartetDto, opprinneligErNyoppstartetFLVerdi, tekstBuilder);
    }

    private Boolean getOpprinneligErNyoppstartetFLVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
            .orElse(null);
    }

    private void lagHistorikkInnslag(VurderNyoppstartetFLDto dto, Boolean opprinneligErNyoppstartetFLVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(dto, opprinneligErNyoppstartetFLVerdi, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(VurderNyoppstartetFLDto dto,
                                        Boolean opprinneligNyoppstartetFLVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligNyoppstartetFLVerdi);
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erErNyoppstartetFL());
        if(opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FRILANSVIRKSOMHET, opprinneligVerdi, nyVerdi);
        }
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

}
