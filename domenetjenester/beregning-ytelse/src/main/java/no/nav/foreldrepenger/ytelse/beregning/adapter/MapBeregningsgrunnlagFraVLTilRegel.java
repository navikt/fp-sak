package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;

public final class MapBeregningsgrunnlagFraVLTilRegel {

    private MapBeregningsgrunnlagFraVLTilRegel() {
    }

    public static Beregningsgrunnlag map(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        var aktivitetStatuser = vlBeregningsgrunnlag.getAktivitetStatuser().stream()
            .map(vlBGAktivitetStatus -> AktivitetStatusMapper.fraVLTilRegel(vlBGAktivitetStatus.getAktivitetStatus()))
            .collect(Collectors.toList());

        var perioder = mapBeregningsgrunnlagPerioder(vlBeregningsgrunnlag);

        return Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(vlBeregningsgrunnlag.getSkjæringstidspunkt())
            .medAktivitetStatuser(aktivitetStatuser)
            .medBeregningsgrunnlagPerioder(perioder)
            .build();
    }

    public static boolean arbeidstakerVedSkjæringstidspunkt(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .min(Comparator.comparing(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
            .map(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList).orElse(List.of()).stream()
            .anyMatch(a -> AndelKilde.PROSESS_START.equals(a.getKilde()) && Inntektskategori.girFeriepenger().contains(a.getGjeldendeInntektskategori()));
    }

    private static List<BeregningsgrunnlagPeriode> mapBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapBeregningsgrunnlagFraVLTilRegel::mapBeregningsgrunnlagPeriode)
            .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriode mapBeregningsgrunnlagPeriode(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode vlBGPeriode) {
        final var regelBGPeriode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(vlBGPeriode.getBeregningsgrunnlagPeriodeFom(), vlBGPeriode.getBeregningsgrunnlagPeriodeTom()));
        var beregningsgrunnlagPrStatus = mapVLBGPrStatus(vlBGPeriode);
        beregningsgrunnlagPrStatus.forEach(regelBGPeriode::medBeregningsgrunnlagPrStatus);

        return regelBGPeriode.build();
    }

    private static List<BeregningsgrunnlagPrStatus> mapVLBGPrStatus(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode vlBGPeriode) {
        List<BeregningsgrunnlagPrStatus> liste = new ArrayList<>();
        BeregningsgrunnlagPrStatus bgpsATFL = null;

        for (var vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            final var regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
            if (AktivitetStatus.ATFL.equals(regelAktivitetStatus)) {
                if (bgpsATFL == null) {  // Alle ATFL håndteres samtidig her
                    bgpsATFL = mapVLBGPStatusForATFL(vlBGPeriode);
                    liste.add(bgpsATFL);
                }
            } else {
                var bgps = mapVLBGPStatusForAlleAktivietetStatuser(vlBGPStatus);
                liste.add(bgps);
            }
        }
        return liste;
    }

    // Ikke ATFL og TY, de har separat mapping
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForAlleAktivietetStatuser(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        final var regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medRedusertBrukersAndelPrÅr(vlBGPStatus.getRedusertBrukersAndelPrÅr())
            .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(vlBGPStatus.getGjeldendeInntektskategori()))
            .build();
    }

    // Felles mapping av alle statuser som mapper til ATFL
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForATFL(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode vlBGPeriode) {

        var regelBGPStatusATFL = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL);

        for (var vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            if (AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus()))) {
                var regelArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder()
                    .medArbeidsforhold(ArbeidsforholdMapper.mapArbeidsforholdFraBeregningsgrunnlag(vlBGPStatus))
                    .medRedusertRefusjonPrÅr(vlBGPStatus.getRedusertRefusjonPrÅr())
                    .medRedusertBrukersAndelPrÅr(vlBGPStatus.getRedusertBrukersAndelPrÅr())
                    .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(vlBGPStatus.getGjeldendeInntektskategori()))
                    .build();
                regelBGPStatusATFL.medArbeidsforhold(regelArbeidsforhold);
            }
        }
        return regelBGPStatusATFL.build();
    }
}
