package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.MottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE)
public class MottarYtelseHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public MottarYtelseHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public MottarYtelseHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var mottarYtelseDto = dto.getMottarYtelse();
        var forrigeBG = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (erFrilanser(nyttBeregningsgrunnlag) && mottarYtelseDto.getFrilansMottarYtelse() != null) {
                lagHistorikkinnslagForFrilans(forrigeBG, mottarYtelseDto, tekstBuilder);
        }
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse()
            .forEach(a -> {
                var matchetAndel = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
                    .filter(andel -> a.getAndelsnr() == andel.getAndelsnr()).findFirst();
                matchetAndel.ifPresent(andel -> {
                    var mottarYtelseVerdi = mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse().stream()
                        .filter(mottarYtelseAndel -> mottarYtelseAndel.getAndelsnr() == andel.getAndelsnr())
                        .findFirst().map(ArbeidstakerandelUtenIMMottarYtelseDto::getMottarYtelse);
                    lagHistorikkinnslagForArbeidstakerUtenIM(forrigeBG, andel, mottarYtelseVerdi, tekstBuilder, arbeidsforholdOverstyringer);
                });
            });
    }

    private boolean erFrilanser(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream().anyMatch(a -> a.getAktivitetStatus().erFrilanser());
    }

    private void lagHistorikkinnslagForArbeidstakerUtenIM(Optional<BeregningsgrunnlagEntitet> forrigeBG, BeregningsgrunnlagPrStatusOgAndel andel,
                                                          Optional<Boolean> mottarYtelseVerdi, HistorikkInnslagTekstBuilder tekstBuilder,
                                                          List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        mottarYtelseVerdi.ifPresent(mottarYtelse -> {
            var mottarYtelseForrige = finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(andel, forrigeBG);
                if (!mottarYtelseForrige.isPresent() || !mottarYtelseForrige.get().equals(mottarYtelse)){
                    var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(),
                        andel.getArbeidsgiver(),
                        andel.getArbeidsforholdRef(),
                        arbeidsforholdOverstyringer);
                    tekstBuilder
                        .medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_ARBEID, andelsInfo,
                            mottarYtelseForrige.orElse(null), mottarYtelse);
                }
            }
        );
    }

    private void lagHistorikkinnslagForFrilans(Optional<BeregningsgrunnlagEntitet> forrigeBG, MottarYtelseDto mottarYtelseDto,
                                               HistorikkInnslagTekstBuilder tekstBuilder) {
        var mottarYtelseForrige = finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(forrigeBG);
        if (!mottarYtelseForrige.isPresent() || !mottarYtelseForrige.get().equals(mottarYtelseDto.getFrilansMottarYtelse())) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_FRILANS, mottarYtelseForrige.orElse(null), mottarYtelseDto.getFrilansMottarYtelse());
        }
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG
            .stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().erFrilanser())
            .findFirst().stream()
            .flatMap(andel -> andel.mottarYtelse().stream()).findFirst();
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(BeregningsgrunnlagPrStatusOgAndel andelINyttBg, Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG
            .stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.gjelderSammeArbeidsforhold(andelINyttBg))
            .findFirst().stream()
            .flatMap(andel -> andel.mottarYtelse().stream()).findFirst();
    }

}
