package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;

public final class MapBeregningsgrunnlagFraVLTilRegel {

    private MapBeregningsgrunnlagFraVLTilRegel() {
    }

    public static Beregningsgrunnlag map(no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag vlBeregningsgrunnlag) {
        var aktivitetStatuser = vlBeregningsgrunnlag.getAktivitetStatuser().stream()
            .map(vlBGAktivitetStatus -> AktivitetStatusMapper.fraVLTilRegel(vlBGAktivitetStatus.getAktivitetStatus()))
            .toList();

        var perioder = mapBeregningsgrunnlagPerioder(vlBeregningsgrunnlag);

        Objects.requireNonNull(vlBeregningsgrunnlag.getSkjæringstidspunkt(), "skjæringstidspunkt");
        if (perioder.isEmpty()) {
            throw new IllegalStateException("Beregningsgrunnlaget må inneholde minst 1 periode");
        }
        if (aktivitetStatuser.isEmpty()) {
            throw new IllegalStateException("Beregningsgrunnlaget må inneholde minst 1 status");
        }

        return new Beregningsgrunnlag(perioder);
    }

    public static boolean arbeidstakerVedSkjæringstidspunkt(no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .min(Comparator.comparing(no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
            .map(no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList).orElse(List.of()).stream()
            .anyMatch(a -> AndelKilde.PROSESS_START.equals(a.getKilde()) && Inntektskategori.girFeriepenger().contains(a.getGjeldendeInntektskategori()));
    }

    private static List<BeregningsgrunnlagPeriode> mapBeregningsgrunnlagPerioder(no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapBeregningsgrunnlagFraVLTilRegel::mapBeregningsgrunnlagPeriode)
            .toList();
    }

    private static BeregningsgrunnlagPeriode mapBeregningsgrunnlagPeriode(no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        return new BeregningsgrunnlagPeriode(vlBGPeriode.getBeregningsgrunnlagPeriodeFom(), vlBGPeriode.getBeregningsgrunnlagPeriodeTom(), mapVLBGPrStatus(vlBGPeriode));
    }

    private static List<BeregningsgrunnlagPrStatus> mapVLBGPrStatus(no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        List<BeregningsgrunnlagPrStatus> liste = new ArrayList<>();
        var harATFL = vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(a -> AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(a.getAktivitetStatus())));

        if (harATFL) {
            liste.add(mapVLBGPStatusForATFL(vlBGPeriode));
        }
        vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> !AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(a.getAktivitetStatus())))
            .map(MapBeregningsgrunnlagFraVLTilRegel::mapVLBGPStatusForAlleAktivietetStatuser)
            .forEach(liste::add);
        return liste;
    }

    // Ikke ATFL og TY, de har separat mapping
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForAlleAktivietetStatuser(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        var regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
        return new BeregningsgrunnlagPrStatus(regelAktivitetStatus, vlBGPStatus.getRedusertBrukersAndelPrÅr(),
            InntektskategoriMapper.fraVLTilRegel(vlBGPStatus.getGjeldendeInntektskategori()));
    }

    // Felles mapping av alle statuser som mapper til ATFL
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForATFL(no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        var arbeidsforhold = vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(a.getAktivitetStatus())))
            .map(a -> BeregningsgrunnlagPrArbeidsforhold.opprett(ArbeidsforholdMapper.mapArbeidsforholdFraBeregningsgrunnlag(a),
                    InntektskategoriMapper.fraVLTilRegel(a.getGjeldendeInntektskategori()))
                    .medRedusertRefusjonPrÅr(a.getRedusertRefusjonPrÅr())
                    .medRedusertBrukersAndelPrÅr(a.getRedusertBrukersAndelPrÅr()))
            .toList();

        return new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, arbeidsforhold);
    }
}
