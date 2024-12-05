package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderteArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD)
public class VurderTidsbegrensetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    VurderTidsbegrensetHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderTidsbegrensetHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {

        var tidsbegrensetDto = dto.getVurderTidsbegrensetArbeidsforhold();
        var periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var fastsatteArbeidsforhold = tidsbegrensetDto.getFastsatteArbeidsforhold();
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        for (var arbeidsforhold : fastsatteArbeidsforhold) {
            var korrektAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(a -> a.getAndelsnr().equals(arbeidsforhold.getAndelsnr()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Finner ikke andel med andelsnr " + arbeidsforhold.getAndelsnr()));
            linjeBuilder.addAll(lagHistorikkInnslag(arbeidsforhold, korrektAndel, arbeidsforholdOverstyringer));
        }
        return linjeBuilder;
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkInnslag(VurderteArbeidsforholdDto arbeidsforhold,
                                                                   BeregningsgrunnlagPrStatusOgAndel andel,
                                                                   List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(),
            andel.getArbeidsgiver(), andel.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isOpprinneligVerdi());
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isTidsbegrensetArbeidsforhold());
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        if (opprinneligVerdi != nyVerdi) {
            linjerBuilder.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Arbeidsforhold hos " + arbeidsforholdInfo, opprinneligVerdi, nyVerdi));
            linjerBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjerBuilder;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? HistorikkEndretFeltVerdiType.TIDSBEGRENSET_ARBEIDSFORHOLD : HistorikkEndretFeltVerdiType.IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD;
    }


}
