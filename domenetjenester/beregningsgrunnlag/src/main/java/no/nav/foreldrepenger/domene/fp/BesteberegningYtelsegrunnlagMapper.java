package no.nav.foreldrepenger.domene.fp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BesteberegningYtelsegrunnlagMapper {
    private static final List<RelatertYtelseType> FPSAK_YTELSER = Arrays.asList(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    private BesteberegningYtelsegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static List<Saksnummer> saksnummerSomMåHentesFraFpsak(DatoIntervallEntitet periodeYtelserKanVæreRelevantForBB, YtelseFilter ytelseFilter) {
        var ytelserFraFpsak = ytelseFilter.filter(y -> FPSAK_YTELSER.contains(y.getRelatertYtelseType()))
            .filter(y -> y.getKilde().equals(Fagsystem.FPSAK))
            .filter(y -> y.getPeriode().overlapper(periodeYtelserKanVæreRelevantForBB))
            .getFiltrertYtelser();
        return ytelserFraFpsak.stream().map(Ytelse::getSaksnummer).toList();
    }

    public static Optional<Ytelsegrunnlag> mapSykepengerTilYtelegrunnlag(DatoIntervallEntitet periodeYtelserKanVæreRelevantForBB,
                                                                         YtelseFilter ytelseFilter) {
        var sykepengegrunnlag = ytelseFilter.filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(y -> y.getPeriode().overlapper(periodeYtelserKanVæreRelevantForBB))
            .getFiltrertYtelser();
        var sykepengeperioder = sykepengegrunnlag.stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapTilYtelsegrunnlag)
            .flatMap(Optional::stream)
            .toList();
        return sykepengeperioder.isEmpty() ? Optional.empty() : Optional.of(new Ytelsegrunnlag(YtelseType.SYKEPENGER, sykepengeperioder));
    }

    public static Optional<Ytelsegrunnlag> mapFpsakYtelseTilYtelsegrunnlag(BeregningsresultatEntitet resultat, FagsakYtelseType ytelseType) {
        var ytelseperioder = resultat.getBeregningsresultatPerioder()
            .stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(BesteberegningYtelsegrunnlagMapper::mapPeriode)
            .toList();
        return ytelseperioder.isEmpty() ? Optional.empty() : Optional.of(new Ytelsegrunnlag(mapTilGenerellYtelse(ytelseType), ytelseperioder));
    }

    private static YtelseType mapTilGenerellYtelse(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalArgumentException("Skal ikke besteberegne basert på " + ytelseType.getKode());
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
        };
    }

    private static Ytelseperiode mapPeriode(BeregningsresultatPeriode periode) {
        var andeler = periode.getBeregningsresultatAndelList().stream().map(BesteberegningYtelsegrunnlagMapper::mapAndel).toList();
        return new Ytelseperiode(Intervall.fraOgMedTilOgMed(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()),
            andeler);
    }

    private static Ytelseandel mapAndel(BeregningsresultatAndel a) {
        return new Ytelseandel(AktivitetStatus.fraKode(a.getAktivitetStatus().getKode()), Inntektskategori.fraKode(a.getInntektskategori().getKode()),
            (long) a.getDagsats());
    }


    private static Optional<Ytelseperiode> mapTilYtelsegrunnlag(Ytelse sp) {
        var arbeidskategori = sp.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori);
        if (arbeidskategori.isEmpty() || harUgyldigTilstandForBesteberegning(arbeidskategori.get(), sp.getStatus())) {
            return Optional.empty();
        }
        var andel = new Ytelseandel(no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori.fraKode(arbeidskategori.get().getKode()), null);
        return Optional.of(new Ytelseperiode(Intervall.fraOgMedTilOgMed(sp.getPeriode().getFomDato(), sp.getPeriode().getTomDato()),
            Collections.singletonList(andel)));
    }

    private static boolean harUgyldigTilstandForBesteberegning(Arbeidskategori kategori, RelatertYtelseTilstand sp) {
        return Arbeidskategori.UGYLDIG.equals(kategori) && RelatertYtelseTilstand.ÅPEN.equals(sp);
    }
}
