package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;

public class MapTilLønnsendring {

    private static final int MND_I_1_ÅR = 12;

    private MapTilLønnsendring() {
        // Skjul public constructor
    }


    public static List<Lønnsendring> mapTilLønnsendringFraBesteberegning(FaktaBeregningLagreDto dto,
                                                                         OppdaterBeregningsgrunnlagResultat oppdaterResultat) {

        var endringer = dto.getBesteberegningAndeler()
            .getBesteberegningAndelListe()
            .stream()
            .filter(a -> !a.getNyAndel())
            .flatMap(a -> finnAndelEndringForAndelsnr(oppdaterResultat, a).stream())
            .collect(Collectors.toCollection(ArrayList::new));
        var nyDagpengeAndel = dto.getBesteberegningAndeler().getNyDagpengeAndel();
        if (nyDagpengeAndel != null) {
            var dagpengerEndring = finnDagpengerEndring(oppdaterResultat);
            dagpengerEndring.ifPresent(endringer::add);
        }

        return endringer.stream().map(MapTilLønnsendring::mapAndelEndringTilLønnsendring).toList();
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndelEndring> finnDagpengerEndring(OppdaterBeregningsgrunnlagResultat oppdaterResultat) {
        return oppdaterResultat.getBeregningsgrunnlagEndring()
            .stream()
            .flatMap(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0).getBeregningsgrunnlagPrStatusOgAndelEndringer().stream())
            .filter(andelEndring -> andelEndring.getAktivitetStatus()
                .equals(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.DAGPENGER))
            .findFirst();
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndelEndring> finnAndelEndringForAndelsnr(OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                                                                                                  BesteberegningFødendeKvinneAndelDto a) {
        return oppdaterResultat.getBeregningsgrunnlagEndring()
            .stream()
            .flatMap(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0).getBeregningsgrunnlagPrStatusOgAndelEndringer().stream())
            .filter(andelEndring -> andelEndring.getAndelsnr().equals(a.getAndelsnr()))
            .findFirst();
    }

    public static List<Lønnsendring> mapLønnsendringFraATogFLSammeOrg(FaktaBeregningLagreDto dto,
                                                                      OppdaterBeregningsgrunnlagResultat oppdaterResultat) {
        return dto.getVurderATogFLiSammeOrganisasjon()
            .getVurderATogFLiSammeOrganisasjonAndelListe()
            .stream()
            .map(a -> mapTilLønnsendring(a.getAndelsnr(), oppdaterResultat))
            .collect(Collectors.toList());
    }

    public static List<Lønnsendring> mapLønnsendringFraATUtenInntektsmelding(OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                                                                             FaktaBeregningLagreDto dto) {
        return dto.getFastsattUtenInntektsmelding()
            .getAndelListe()
            .stream()
            .map(dtoAndel -> mapTilLønnsendring(dtoAndel.getAndelsnr(), oppdaterResultat))
            .collect(Collectors.toList());
    }

    private static Lønnsendring mapTilLønnsendring(Long andelsnr, OppdaterBeregningsgrunnlagResultat oppdaterResultat) {

        var endring = oppdaterResultat.getBeregningsgrunnlagEndring()
            .map(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelEndringer().stream())
            .filter(a -> a.getAndelsnr().equals(andelsnr))
            .findFirst()
            .orElseThrow();
        return mapAndelEndringTilLønnsendring(endring);
    }

    public static Lønnsendring mapAndelEndringTilLønnsendring(BeregningsgrunnlagPrStatusOgAndelEndring endring) {
        return Lønnsendring.Builder.ny()
            .medAktivitetStatus(endring.getAktivitetStatus())
            .medArbeidsgiver(endring.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(endring.getArbeidsforholdRef())
            .medNyArbeidsinntekt(endring.getInntektEndring().map(BeløpEndring::getTilMånedsbeløp).map(BigDecimal::intValue).orElse(null))
            .medGammelArbeidsinntekt(endring.getInntektEndring().map(BeløpEndring::getFraMånedsbeløp).map(BigDecimal::intValue).orElse(null))
            .medNyInntektskategori(endring.getInntektskategoriEndring().map(InntektskategoriEndring::getTilVerdi).orElse(null))
            .medGammelInntektskategori(endring.getInntektskategoriEndring().map(InntektskategoriEndring::getFraVerdi).orElse(null))
            .build();
    }


}
