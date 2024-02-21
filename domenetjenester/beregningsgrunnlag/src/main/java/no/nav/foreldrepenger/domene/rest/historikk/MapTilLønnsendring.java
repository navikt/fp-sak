package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteVerdierDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.RedigerbarAndelDto;

public class MapTilLønnsendring {

    private static final int MND_I_1_ÅR = 12;

    private MapTilLønnsendring() {
        // Skjul public constructor
    }


    // TODO: Refaktorer og gjer denne lettere å lese
    public static Lønnsendring mapTilLønnsendringForAndelIPeriode(RedigerbarAndelDto andel,
                                                                  FastsatteVerdierDto fastsatteVerdier,
                                                                  BeregningsgrunnlagPeriode periode,
                                                                  Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag) {
        if (andel.getNyAndel() || andel.getLagtTilAvSaksbehandler()) {
            if (andel.getNyAndel() && andel.getAndelsnr() == null && andel.getAktivitetStatus() != null) {
                return new Lønnsendring.Builder().medNyAndel(true)
                    .medAktivitetStatus(andel.getAktivitetStatus())
                    .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
                    .medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
                    .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue())
                    .build();
            }
            return lagLønnsendringForNyAndelFraAndelsreferanse(andel, periode, periodeForrigeGrunnlag,
                fastsatteVerdier);
        }
        var andelIAktivt = finnKorrektAndelIPeriode(periode, andel.getAndelsnr());
        var andelFraForrige = periodeForrigeGrunnlag.flatMap(
            p -> finnKorrektAndelIPeriodeHvisFinnes(p, andel.getAndelsnr()));
        return new Lønnsendring.Builder().medGammelInntektskategori(
            andelFraForrige.map(BeregningsgrunnlagPrStatusOgAndel::getInntektskategori).orElse(null))
            .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
            .medGammelArbeidsinntekt(andelFraForrige.map(MapTilLønnsendring::finnBeregnetPrMnd).orElse(null))
            .medGammelArbeidsinntektPrÅr(andelFraForrige.map(MapTilLønnsendring::finnBeregnetPrMnd).orElse(null))
            .medGammelRefusjonPrÅr(
                andelFraForrige.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
                    .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
                    .map(BigDecimal::intValue)
                    .orElse(0))
            .medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
            .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue())
            .medAktivitetStatus(andelIAktivt.getAktivitetStatus())
            .medArbeidsforholdRef(andelIAktivt.getArbeidsforholdRef().orElse(null))
            .medAktivitetStatus(andelIAktivt.getAktivitetStatus())
            .build();
    }

    private static Lønnsendring lagLønnsendringForNyAndelFraAndelsreferanse(RedigerbarAndelDto andel,
                                                                            BeregningsgrunnlagPeriode periode,
                                                                            Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag,
                                                                            FastsatteVerdierDto fastsatteVerdier) {
        var endringBuilder = new Lønnsendring.Builder();
        var forrigeAndelOpt = periodeForrigeGrunnlag.flatMap(
            p -> finnKorrektAndelIPeriodeHvisFinnes(p, andel.getAndelsnr()));
        if (!andel.getNyAndel() && forrigeAndelOpt.isPresent()) {
            var forrigeAndel = forrigeAndelOpt.get();
            endringBuilder.medNyAndel(false)
                .medAktivitetStatus(forrigeAndel.getAktivitetStatus())
                .medArbeidsforholdRef(forrigeAndel.getArbeidsforholdRef().orElse(null))
                .medArbeidsgiver(forrigeAndel.getArbeidsgiver().orElse(null))
                .medGammelArbeidsinntekt(finnBeregnetPrMnd(forrigeAndel))
                .medGammelArbeidsinntektPrÅr(
                    forrigeAndel.getBeregnetPrÅr() == null ? null : forrigeAndel.getBeregnetPrÅr().intValue())
                .medGammelInntektskategori(forrigeAndel.getInntektskategori())
                .medGammelRefusjonPrÅr(forrigeAndel.getBgAndelArbeidsforhold()
                    .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
                    .map(BigDecimal::intValue)
                    .orElse(0));
        } else {
            var andelSomErKopiert = finnKorrektAndelIPeriode(periode, andel.getAndelsnr());
            endringBuilder.medNyAndel(true)
                .medAktivitetStatus(andelSomErKopiert.getAktivitetStatus())
                .medArbeidsgiver(andelSomErKopiert.getArbeidsgiver().orElse(null))
                .medArbeidsforholdRef(andelSomErKopiert.getArbeidsforholdRef().orElse(null))
                .medGammelRefusjonPrÅr(0);
        }
        return endringBuilder.medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
            .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue())
            .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
            .medNyRefusjonPrÅr(fastsatteVerdier.getRefusjonPrÅr())
            .build();
    }


    private static Integer finnBeregnetPrMnd(BeregningsgrunnlagPrStatusOgAndel korrektAndel) {
        return korrektAndel.getBeregnetPrÅr() == null ? null : korrektAndel.getBeregnetPrÅr()
            .divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_EVEN)
            .intValue();
    }

    public static List<Lønnsendring> mapTilLønnsendringFraBesteberegning(FaktaBeregningLagreDto dto,
                                                                         BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                         Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var endringer = dto.getBesteberegningAndeler()
            .getBesteberegningAndelListe()
            .stream()
            .filter(a -> !a.getNyAndel())
            .map(dtoAndel -> mapTilLønnsendring(dtoAndel.getAndelsnr(),
                dtoAndel.getFastsatteVerdier().getFastsattBeløp(), nyttBeregningsgrunnlag, forrigeBg))
            .collect(Collectors.toList());
        var nyDagpengeAndel = dto.getBesteberegningAndeler().getNyDagpengeAndel();
        if (nyDagpengeAndel != null) {
            var lønnsendringForNyAndel = mapTilLønnsendring(
                AktivitetStatus.DAGPENGER, nyDagpengeAndel.getFastsatteVerdier().getFastsattBeløp(), nyttBeregningsgrunnlag, forrigeBg);
            endringer.add(lønnsendringForNyAndel);
        }
        return endringer.stream().toList();
    }

    public static List<Lønnsendring> mapLønnsendringFraATogFLSammeOrg(FaktaBeregningLagreDto dto,
                                                                      BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                      Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return dto.getVurderATogFLiSammeOrganisasjon()
            .getVurderATogFLiSammeOrganisasjonAndelListe()
            .stream()
            .map(a -> mapTilLønnsendring(a.getAndelsnr(), a.getArbeidsinntekt(), nyttBeregningsgrunnlag, forrigeBg))
            .toList();
    }

    public static List<Lønnsendring> mapLønnsendringFraATUtenInntektsmelding(FaktaBeregningLagreDto dto,
                                                                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                             Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return dto.getFastsattUtenInntektsmelding()
            .getAndelListe()
            .stream()
            .map(dtoAndel -> mapTilLønnsendring(dtoAndel.getAndelsnr(), mapTilFastsatteVerdier(dtoAndel),
                nyttBeregningsgrunnlag, forrigeBg))
            .toList();
    }

    private static FastsatteVerdierDto mapTilFastsatteVerdier(FastsettMånedsinntektUtenInntektsmeldingAndelDto dtoAndel) {
        return new FastsatteVerdierDto(dtoAndel.getFastsattBeløp());
    }

    private static Lønnsendring mapTilLønnsendring(Long andelsnr,
                                                   Integer nyArbeidsinntekt,
                                                   BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                   Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, andelsnr);
        var forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, andelsnr);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, nyArbeidsinntekt);
    }

    private static Lønnsendring mapTilLønnsendring(Long andelsnr,
                                                   FastsatteVerdierDto fastsatteVerdierDto,
                                                   BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                   Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, andelsnr);
        var forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, andelsnr);
        var forrigeInntektskategori = finnGammelInntektskategori(forrigeBg, andelsnr);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, forrigeInntektskategori, fastsatteVerdierDto);
    }

    public static Lønnsendring mapTilLønnsendring(AktivitetStatus aktivitetStatus,
                                                  Integer nyArbeidsinntekt,
                                                  BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                  Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        if (aktivitetStatus.equals(AktivitetStatus.ARBEIDSTAKER)) {
            throw new IllegalArgumentException(
                "Utviklerfeil: Kan ikke sette lønnsendring basert på status med flere andeler. Bruk andelsnr.");
        }
        var andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, aktivitetStatus).orElseThrow(
            () -> new IllegalStateException("Utviklerfeil: Fant ikke andel for aktivitetStatus " + aktivitetStatus));
        var forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, aktivitetStatus);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, nyArbeidsinntekt);
    }

    private static Integer finnGammelMånedsinntekt(Optional<BeregningsgrunnlagEntitet> forrigeBg, Long andelsnr) {
        return forrigeBg.flatMap(bg -> finnKorrektAndelIFørstePeriodeFraForrige(bg, andelsnr))
            .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
            .map(MapTilLønnsendring::mapTilMånedsbeløp)
            .orElse(null);
    }

    private static Integer finnGammelMånedsinntekt(Optional<BeregningsgrunnlagEntitet> forrigeBg,
                                                   AktivitetStatus aktivitetStatus) {
        return forrigeBg.flatMap(bg -> finnKorrektAndelIFørstePeriode(bg, aktivitetStatus))
            .stream()
            .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .findFirst()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
            .map(MapTilLønnsendring::mapTilMånedsbeløp)
            .orElse(null);
    }

    private static Inntektskategori finnGammelInntektskategori(Optional<BeregningsgrunnlagEntitet> forrigeBg,
                                                               Long andelsnr) {
        return forrigeBg.flatMap(bg -> finnKorrektAndelIFørstePeriodeFraForrige(bg, andelsnr))
            .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .map(BeregningsgrunnlagPrStatusOgAndel::getInntektskategori)
            .orElse(null);
    }

    private static Lønnsendring mapAndelTilLønnsendring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                        Integer forrigeInntekt,
                                                        Inntektskategori forrigeInntektskategori,
                                                        FastsatteVerdierDto fastsatteVerdierDto) {
        return Lønnsendring.Builder.ny()
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef().orElse(null))
            .medNyArbeidsinntekt(fastsatteVerdierDto.getFastsattBeløp())
            .medGammelArbeidsinntekt(forrigeInntekt)
            .medNyInntektskategori(fastsatteVerdierDto.getInntektskategori())
            .medGammelInntektskategori(forrigeInntektskategori)
            .build();
    }

    private static Lønnsendring mapAndelTilLønnsendring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                        Integer forrigeInntekt,
                                                        Integer nyArbeidsinntekt) {
        return Lønnsendring.Builder.ny()
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef().orElse(null))
            .medNyArbeidsinntekt(nyArbeidsinntekt)
            .medGammelArbeidsinntekt(forrigeInntekt)
            .build();
    }

    private static Integer mapTilMånedsbeløp(BigDecimal beregnet) {
        if (beregnet == null) {
            return null;
        }
        return beregnet.intValue() / MND_I_1_ÅR;
    }

    private static BeregningsgrunnlagPrStatusOgAndel finnKorrektAndelIPeriode(BeregningsgrunnlagPeriode periode,
                                                                              Long andelsnr) {
        return finnKorrektAndelIPeriodeHvisFinnes(periode, andelsnr).orElseThrow(
            () -> new IllegalStateException("Utviklerfeil: Fant ikke andel for andelsnr i aktivt grunnlag" + andelsnr));
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnKorrektAndelIPeriodeHvisFinnes(
        BeregningsgrunnlagPeriode periode,
        Long andelsnr) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bgAndel -> andelsnr.equals(bgAndel.getAndelsnr()))
            .findFirst();
    }

    private static BeregningsgrunnlagPrStatusOgAndel finnKorrektAndelIFørstePeriode(BeregningsgrunnlagEntitet bg,
                                                                                    Long andelsnr) {
        return finnKorrektAndelIPeriode(bg.getBeregningsgrunnlagPerioder().get(0), andelsnr);
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnKorrektAndelIFørstePeriodeFraForrige(
        BeregningsgrunnlagEntitet bg,
        Long andelsnr) {
        return finnKorrektAndelIPeriodeHvisFinnes(bg.getBeregningsgrunnlagPerioder().get(0), andelsnr);
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnKorrektAndelIFørstePeriode(BeregningsgrunnlagEntitet bg,
                                                                                              AktivitetStatus aktivitetStatus) {
        return bg.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bgAndel -> aktivitetStatus.equals(bgAndel.getAktivitetStatus()))
            .findFirst();
    }

}
