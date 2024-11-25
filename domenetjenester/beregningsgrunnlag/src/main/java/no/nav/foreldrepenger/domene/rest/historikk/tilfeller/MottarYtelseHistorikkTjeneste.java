package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.MottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

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
    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                                BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var mottarYtelseDto = dto.getMottarYtelse();
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        var forrigeBG = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (erFrilanser(nyttBeregningsgrunnlag) && mottarYtelseDto.getFrilansMottarYtelse() != null) {
            tekstlinjerBuilder.add(lagHistorikkinnslagForFrilans(forrigeBG, mottarYtelseDto));
        }
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse().forEach(a -> {
            var matchetAndel = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
                .get(0)
                .getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(andel -> a.getAndelsnr() == andel.getAndelsnr())
                .findFirst();
            matchetAndel.ifPresent(andel -> {
                var mottarYtelseVerdi = mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse()
                    .stream()
                    .filter(mottarYtelseAndel -> mottarYtelseAndel.getAndelsnr() == andel.getAndelsnr())
                    .findFirst()
                    .map(ArbeidstakerandelUtenIMMottarYtelseDto::getMottarYtelse);
                tekstlinjerBuilder.addAll(lagHistorikkinnslagForArbeidstakerUtenIM(forrigeBG, andel, mottarYtelseVerdi, arbeidsforholdOverstyringer));
            });
        });
        return tekstlinjerBuilder;
    }

    private boolean erFrilanser(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .anyMatch(a -> a.getAktivitetStatus().erFrilanser());
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkinnslagForArbeidstakerUtenIM(Optional<BeregningsgrunnlagEntitet> forrigeBG,
                                                                                             BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                             Optional<Boolean> mottarYtelseVerdi,
                                                                                             List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        mottarYtelseVerdi.ifPresent(mottarYtelse -> {
            var mottarYtelseForrige = finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(andel, forrigeBG);
            if (mottarYtelseForrige.isEmpty() || !mottarYtelseForrige.get().equals(mottarYtelse)) {
                var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(),
                    andel.getArbeidsgiver(), andel.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
                tekstlinjerBuilder.add(
                    new HistorikkinnslagTekstlinjeBuilder().fraTil("Mottar søker ytelse for arbeid i " + andelsInfo, mottarYtelseForrige.orElse(null),
                        mottarYtelse));
                tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());
            }
        });
        return tekstlinjerBuilder;
    }

    private HistorikkinnslagTekstlinjeBuilder lagHistorikkinnslagForFrilans(Optional<BeregningsgrunnlagEntitet> forrigeBG,
                                                                            MottarYtelseDto mottarYtelseDto) {
        var mottarYtelseForrige = finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(forrigeBG);
        if (mottarYtelseForrige.isEmpty() || !mottarYtelseForrige.get().equals(mottarYtelseDto.getFrilansMottarYtelse())) {
            return new HistorikkinnslagTekstlinjeBuilder().fraTil("Mottar søker ytelse for frilansaktiviteten", mottarYtelseForrige.orElse(null),
                mottarYtelseDto.getFrilansMottarYtelse());
        }
        return null;
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().erFrilanser())
            .findFirst()
            .stream()
            .flatMap(andel -> andel.mottarYtelse().stream())
            .findFirst();
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(BeregningsgrunnlagPrStatusOgAndel andelINyttBg,
                                                                               Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.gjelderSammeArbeidsforhold(andelINyttBg))
            .findFirst()
            .stream()
            .flatMap(andel -> andel.mottarYtelse().stream())
            .findFirst();
    }

}
