package no.nav.foreldrepenger.domene.fp;

import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BesteberegningYtelsegrunnlagMapper {
    private static final List<RelatertYtelseType> FPSAK_YTELSER = Arrays.asList(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    private BesteberegningYtelsegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static List<Saksnummer> saksnummerSomMåHentesFraFpsak(LocalDate førsteDatoDerYtelseKanBliMedIBB, YtelseFilter ytelseFilter) {
        Collection<Ytelse> ytelserFraFpsak = ytelseFilter.filter(y -> FPSAK_YTELSER.contains(y.getRelatertYtelseType()))
            .filter(y -> y.getKilde().equals(Fagsystem.FPSAK))
            .filter(y -> !y.getPeriode().getTomDato().isBefore(førsteDatoDerYtelseKanBliMedIBB))
            .getFiltrertYtelser();
        return ytelserFraFpsak.stream().map(Ytelse::getSaksnummer).collect(Collectors.toList());
    }

    public static Optional<Ytelsegrunnlag> mapSykepengerTilYtelegrunnlag(LocalDate førsteDatoDerYtelseKanBliMedIBB, YtelseFilter ytelseFilter) {
        Collection<Ytelse> sykepengegrunnlag = ytelseFilter.filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(y -> !y.getPeriode().getTomDato().isBefore(førsteDatoDerYtelseKanBliMedIBB))
            .getFiltrertYtelser();
        List<Ytelseperiode> sykepengeperioder = sykepengegrunnlag.stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapTilYtelsegrunnlag)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        return sykepengegrunnlag.isEmpty()
            ? Optional.empty()
            : Optional.of(new Ytelsegrunnlag(FagsakYtelseType.SYKEPENGER, sykepengeperioder));
    }

    public static Optional<Ytelsegrunnlag> mapFpsakYtelseTilYtelsegrunnlag(BeregningsresultatEntitet resultat,
                                                                           no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType ytelseType) {
        List<Ytelseperiode> ytelseperioder = resultat.getBeregningsresultatPerioder().stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapPeriode)
            .collect(Collectors.toList());
        return ytelseperioder.isEmpty()
            ? Optional.empty()
            : Optional.of(new Ytelsegrunnlag(FagsakYtelseType.fraKode(ytelseType.getKode()), ytelseperioder));
    }

    private static Ytelseperiode mapPeriode(BeregningsresultatPeriode periode) {
        List<Ytelseandel> andeler = periode.getBeregningsresultatAndelList().stream()
            .map(BesteberegningYtelsegrunnlagMapper::mapAndel)
            .collect(Collectors.toList());
        return new Ytelseperiode(Intervall.fraOgMedTilOgMed(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()), andeler);
    }

    private static Ytelseandel mapAndel(BeregningsresultatAndel a) {
        return new Ytelseandel(AktivitetStatus.fraKode(a.getAktivitetStatus().getKode()), a.getId());
    }


    private static Optional<Ytelseperiode> mapTilYtelsegrunnlag(Ytelse sp) {
        Optional<Arbeidskategori> arbeidskategori = sp.getYtelseGrunnlag()
            .flatMap(YtelseGrunnlag::getArbeidskategori);
        return arbeidskategori.map(ak -> {
            Ytelseandel andel = new Ytelseandel(no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori.fraKode(ak.getKode()), null);
            return new Ytelseperiode(Intervall.fraOgMedTilOgMed(sp.getPeriode().getFomDato(), sp.getPeriode().getTomDato()),
                Collections.singletonList(andel));
        });
    }

}
