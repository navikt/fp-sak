package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderteArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

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
    public void lagHistorikk(Long behandlingId,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {

        var tidsbegrensetDto = dto.getVurderTidsbegrensetArbeidsforhold();
        var periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var fastsatteArbeidsforhold = tidsbegrensetDto.getFastsatteArbeidsforhold();
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        for (var arbeidsforhold : fastsatteArbeidsforhold) {
            var korrektAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
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
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(), andel.getArbeidsgiver(), andel.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
        var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isOpprinneligVerdi());
        var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(arbeidsforhold.isTidsbegrensetArbeidsforhold());
        if (opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(historikkEndretFeltType, arbeidsforholdInfo, opprinneligVerdi, nyVerdi);
        }
    }


}
