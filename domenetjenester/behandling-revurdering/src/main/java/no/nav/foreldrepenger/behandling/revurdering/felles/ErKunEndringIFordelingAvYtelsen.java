package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

class ErKunEndringIFordelingAvYtelsen {

    private ErKunEndringIFordelingAvYtelsen() {
    }

    public static boolean vurder(boolean erEndringIBeregning,
                                 boolean erEndringIUttakFraEndringsdato,
                                 Optional<Beregningsgrunnlag> revurderingsGrunnlagOpt,
                                 Optional<Beregningsgrunnlag> originalGrunnlagOpt,
                                 boolean erEndringISkalHindreTilbaketrekk) {
        return !erEndringIBeregning && !erEndringIUttakFraEndringsdato && (
            kontrollerEndringIFordelingAvYtelsen(revurderingsGrunnlagOpt, originalGrunnlagOpt) || erEndringISkalHindreTilbaketrekk);
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat, boolean erVarselOmRevurderingSendt) {
        var vedtaksbrev = utledVedtaksbrev(erVarselOmRevurderingSendt);
        return RevurderingBehandlingsresultatutlederFelles.buildBehandlingsresultat(revurdering, behandlingsresultat,
            BehandlingResultatType.FORELDREPENGER_ENDRET, RettenTil.HAR_RETT_TIL_FP, vedtaksbrev,
            List.of(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));
    }

    private static boolean kontrollerEndringIFordelingAvYtelsen(Optional<Beregningsgrunnlag> revurderingsGrunnlagOpt,
                                                                Optional<Beregningsgrunnlag> originalGrunnlagOpt) {

        if (revurderingsGrunnlagOpt.isEmpty() && originalGrunnlagOpt.isEmpty()) {
            return false;
        }
        if (revurderingsGrunnlagOpt.isEmpty() || originalGrunnlagOpt.isEmpty()) {
            return true;
        }

        var revurderingsGrunnlag = revurderingsGrunnlagOpt.get();
        var originalgGrunnlag = originalGrunnlagOpt.get();

        var revurderingPerioder = revurderingsGrunnlag.getBeregningsgrunnlagPerioder();
        var originalePerioder = originalgGrunnlag.getBeregningsgrunnlagPerioder();

        for (var periode : revurderingPerioder) {
            if (erUlikKorresponderendePeriode(originalePerioder, periode)) {
                return true;
            }
        }

        for (var periode : originalePerioder) {
            if (erUlikKorresponderendePeriode(revurderingPerioder, periode)) {
                return true;
            }
        }

        return false;
    }

    private static boolean erUlikKorresponderendePeriode(List<BeregningsgrunnlagPeriode> sammenlignPerioder,
                                                         BeregningsgrunnlagPeriode periodeÅSammenligne) {
        var fom = periodeÅSammenligne.getBeregningsgrunnlagPeriodeFom();

        var førsteKronologiskePeriode = sammenlignPerioder.stream()
            .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.isPresent() && fom.isBefore(førsteKronologiskePeriode.get().getBeregningsgrunnlagPeriodeFom())) {
            return harPerioderUlikeAndeler(periodeÅSammenligne, førsteKronologiskePeriode.get());
        }

        var korresponderendePeriode = sammenlignPerioder.stream()
            .filter(originalPeriode -> periodeInneholderDato(originalPeriode, fom))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ingen overlapp for beregningsgrunnlagperiode"));

        return harPerioderUlikeAndeler(periodeÅSammenligne, korresponderendePeriode);
    }

    private static boolean periodeInneholderDato(BeregningsgrunnlagPeriode periode, LocalDate dato) {
        if (periode.getBeregningsgrunnlagPeriodeTom() == null) {
            return !dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom());
        }
        return !dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom()) && !dato.isAfter(periode.getBeregningsgrunnlagPeriodeTom());
    }

    private static boolean harPerioderUlikeAndeler(BeregningsgrunnlagPeriode revuderingPeriode, BeregningsgrunnlagPeriode originalPeriode) {
        var revuderingAndeler = revuderingPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        var originaleAndeler = originalPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        for (var andel : revuderingAndeler) {
            var matchetAndel = finnMatchendeAndel(andel, originaleAndeler);
            if (matchetAndel.isEmpty() || !erAndelerLike(andel, matchetAndel.get())) {
                return true;
            }
        }
        return false;
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnMatchendeAndel(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                  List<BeregningsgrunnlagPrStatusOgAndel> originaleAndeler) {
        return originaleAndeler.stream().filter(orginalAndel -> orginalAndel.equals(andel)).findFirst();
    }

    private static boolean erAndelerLike(BeregningsgrunnlagPrStatusOgAndel revurderingAndel, BeregningsgrunnlagPrStatusOgAndel originalAndel) {
        return revurderingAndel.getDagsatsArbeidsgiver().equals(originalAndel.getDagsatsArbeidsgiver()) && revurderingAndel.getDagsatsBruker()
            .equals(originalAndel.getDagsatsBruker());
    }

    // https://confluence.adeo.no/display/PK/PK-50504+-+02+-+Kravbeskrivelser
    private static Vedtaksbrev utledVedtaksbrev(boolean erVarselOmRevurderingSendt) {
        if (erVarselOmRevurderingSendt) {
            // Krav 17
            return Vedtaksbrev.AUTOMATISK;
        }
        // Krav 12
        return Vedtaksbrev.INGEN;
    }
}
