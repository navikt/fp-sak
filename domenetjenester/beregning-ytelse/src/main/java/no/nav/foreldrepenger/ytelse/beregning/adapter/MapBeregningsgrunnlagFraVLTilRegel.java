package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;

@ApplicationScoped
public class MapBeregningsgrunnlagFraVLTilRegel {

    private MapBeregningsgrunnlagFraVLTilRegel() {
    }

    public static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag map(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        List<AktivitetStatus> aktivitetStatuser = vlBeregningsgrunnlag.getAktivitetStatuser().stream()
            .map(vlBGAktivitetStatus -> AktivitetStatusMapper.fraVLTilRegel(vlBGAktivitetStatus.getAktivitetStatus()))
            .collect(Collectors.toList());

        List<BeregningsgrunnlagPeriode> perioder = mapBeregningsgrunnlagPerioder(vlBeregningsgrunnlag);

        return no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(vlBeregningsgrunnlag.getSkjæringstidspunkt())
            .medAktivitetStatuser(aktivitetStatuser)
            .medBeregningsgrunnlagPerioder(perioder)
            .build();
    }

    private static List<BeregningsgrunnlagPeriode> mapBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapBeregningsgrunnlagFraVLTilRegel::mapBeregningsgrunnlagPeriode)
            .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriode mapBeregningsgrunnlagPeriode(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode vlBGPeriode) {
        final BeregningsgrunnlagPeriode.Builder regelBGPeriode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(vlBGPeriode.getBeregningsgrunnlagPeriodeFom(), vlBGPeriode.getBeregningsgrunnlagPeriodeTom()));
        List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus = mapVLBGPrStatus(vlBGPeriode);
        beregningsgrunnlagPrStatus.forEach(regelBGPeriode::medBeregningsgrunnlagPrStatus);

        return regelBGPeriode.build();
    }

    private static List<BeregningsgrunnlagPrStatus> mapVLBGPrStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode vlBGPeriode) {
        List<BeregningsgrunnlagPrStatus> liste = new ArrayList<>();
        BeregningsgrunnlagPrStatus bgpsATFL = null;

        for (BeregningsgrunnlagPrStatusOgAndel vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            final AktivitetStatus regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
            if (AktivitetStatus.ATFL.equals(regelAktivitetStatus)) {
                if (bgpsATFL == null) {  // Alle ATFL håndteres samtidig her
                    bgpsATFL = mapVLBGPStatusForATFL(vlBGPeriode);
                    liste.add(bgpsATFL);
                }
            } else {
                BeregningsgrunnlagPrStatus bgps = mapVLBGPStatusForAlleAktivietetStatuser(vlBGPStatus);
                liste.add(bgps);
            }
        }
        return liste;
    }

    // Ikke ATFL og TY, de har separat mapping
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForAlleAktivietetStatuser(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        final AktivitetStatus regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medRedusertBrukersAndelPrÅr(vlBGPStatus.getRedusertBrukersAndelPrÅr())
            .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(vlBGPStatus.getInntektskategori()))
            .build();
    }

    // Felles mapping av alle statuser som mapper til ATFL
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForATFL(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode vlBGPeriode) {

        BeregningsgrunnlagPrStatus.Builder regelBGPStatusATFL = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL);

        for (BeregningsgrunnlagPrStatusOgAndel vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            if (AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus()))) {
                BeregningsgrunnlagPrArbeidsforhold regelArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder()
                    .medArbeidsforhold(ArbeidsforholdMapper.mapArbeidsforholdFraBeregningsgrunnlag(vlBGPStatus))
                    .medRedusertRefusjonPrÅr(vlBGPStatus.getRedusertRefusjonPrÅr())
                    .medRedusertBrukersAndelPrÅr(vlBGPStatus.getRedusertBrukersAndelPrÅr())
                    .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(vlBGPStatus.getInntektskategori()))
                    .build();
                regelBGPStatusATFL.medArbeidsforhold(regelArbeidsforhold);
            }
        }
        return regelBGPStatusATFL.build();
    }
}
