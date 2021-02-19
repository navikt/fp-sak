package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderteArbeidsforholdDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD")
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
    public void lagHistorikk(Long behandlingId,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {

        VurderTidsbegrensetArbeidsforholdDto tidsbegrensetDto = dto.getVurderTidsbegrensetArbeidsforhold();
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold = tidsbegrensetDto.getFastsatteArbeidsforhold();
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        for (VurderteArbeidsforholdDto arbeidsforhold : fastsatteArbeidsforhold) {
            BeregningsgrunnlagPrStatusOgAndel korrektAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> a.getAndelsnr().equals(arbeidsforhold.getAndelsnr()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Finner ikke andel med andelsnr " + arbeidsforhold.getAndelsnr()));
            lagHistorikkInnslag(arbeidsforhold, korrektAndel, tekstBuilder, arbeidsforholdOverstyringer);
        }
    }

    private void lagHistorikkInnslag(VurderteArbeidsforholdDto arbeidsforhold, BeregningsgrunnlagPrStatusOgAndel andel,
                                     HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        oppdaterVedEndretVerdi(HistorikkEndretFeltType.ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD, arbeidsforhold, andel, tekstBuilder, arbeidsforholdOverstyringer);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? HistorikkEndretFeltVerdiType.TIDSBEGRENSET_ARBEIDSFORHOLD : HistorikkEndretFeltVerdiType.IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, VurderteArbeidsforholdDto arbeidsforhold, BeregningsgrunnlagPrStatusOgAndel andel,
                                        HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(), andel.getArbeidsgiver(), andel.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
        HistorikkEndretFeltVerdiType opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isOpprinneligVerdi());
        HistorikkEndretFeltVerdiType nyVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isTidsbegrensetArbeidsforhold());
        if (opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(historikkEndretFeltType, arbeidsforholdInfo, opprinneligVerdi, nyVerdi);
        }
    }


}
