package no.nav.foreldrepenger.økonomi.ny.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomi.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomi.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Sats;
import no.nav.foreldrepenger.økonomi.ny.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomi.ny.domene.samlinger.GruppertYtelse;

public class TilkjentYtelseMapper {

    private FagsakYtelseType fagsakYtelseType;
    private FamilieYtelseType ytelseType;

    public TilkjentYtelseMapper(FagsakYtelseType fagsakYtelseType, FamilieYtelseType ytelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
        this.ytelseType = ytelseType;
    }

    public static TilkjentYtelseMapper lagFor(FagsakYtelseType fagsakYtelseType, FamilieYtelseType familieYtelseType) {
        return new TilkjentYtelseMapper(fagsakYtelseType, familieYtelseType);
    }

    public GruppertYtelse fordelPåNøkler(BeregningsresultatEntitet tilkjentYtelse) {
        return fordelPåNøkler(tilkjentYtelse.getBeregningsresultatPerioder());
    }

    public GruppertYtelse fordelPåNøkler(List<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        Map<KjedeNøkkel, Ytelse> resultat = new HashMap<>();
        resultat.putAll(fordelYtelsePåNøkler(tilkjentYtelsePerioder));
        resultat.putAll(fordelFeriepengerPåNøkler(tilkjentYtelsePerioder));

        return new GruppertYtelse(resultat);
    }

    public Map<KjedeNøkkel, Ytelse> fordelYtelsePåNøkler(List<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        Map<KjedeNøkkel, Ytelse.Builder> buildere = new HashMap<>();

        for (BeregningsresultatPeriode tyPeriode : tilkjentYtelsePerioder) {
            Map<KjedeNøkkel, List<YtelsePeriodeMedNøkkel>> andelPrNøkkel = tyPeriode.getBeregningsresultatAndelList().stream()
                .map(andel -> tilYtelsePeriodeMedNøkkel(tyPeriode, andel))
                .collect(Collectors.groupingBy(YtelsePeriodeMedNøkkel::getNøkkel));
            for (var entry : andelPrNøkkel.entrySet()) {
                KjedeNøkkel nøkkel = entry.getKey();
                YtelsePeriode ytelsePeriode = YtelsePeriode.summer(entry.getValue(), YtelsePeriodeMedNøkkel::getYtelsePeriode);
                if (!buildere.containsKey(nøkkel)) {
                    buildere.put(nøkkel, Ytelse.builder());
                }
                buildere.get(nøkkel).leggTilPeriode(ytelsePeriode);
            }
        }

        return build(buildere);
    }

    public Map<KjedeNøkkel, Ytelse> fordelFeriepengerPåNøkler(Collection<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        List<YtelsePeriodeMedNøkkel> alleFeriepenger = new ArrayList<>();
        for (BeregningsresultatPeriode periode : tilkjentYtelsePerioder) {
            for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                for (BeregningsresultatFeriepengerPrÅr feriepenger : andel.getBeregningsresultatFeriepengerPrÅrListe()) {
                    KjedeNøkkel nøkkel = tilNøkkelFeriepenger(andel, feriepenger.getOpptjeningsåret());
                    YtelsePeriode ytelsePeriode = lagYtelsePeriodeForFeriepenger(feriepenger);
                    alleFeriepenger.add(new YtelsePeriodeMedNøkkel(nøkkel, ytelsePeriode));
                }
            }
        }
        Map<KjedeNøkkel, List<YtelsePeriodeMedNøkkel>> feriepengerPrNøkkel = alleFeriepenger.stream()
            .collect(Collectors.groupingBy(YtelsePeriodeMedNøkkel::getNøkkel));

        Map<KjedeNøkkel, Ytelse> resultat = new HashMap<>();
        for (var entry : feriepengerPrNøkkel.entrySet()) {
            KjedeNøkkel nøkkel = entry.getKey();
            YtelsePeriode ytelsePeriode = YtelsePeriode.summer(entry.getValue(), YtelsePeriodeMedNøkkel::getYtelsePeriode);
            resultat.put(nøkkel, Ytelse.builder().leggTilPeriode(ytelsePeriode).build());
        }
        return resultat;
    }

    private YtelsePeriode lagYtelsePeriodeForFeriepenger(BeregningsresultatFeriepengerPrÅr feriepenger) {
        int utbetalingsår = feriepenger.getOpptjeningsåret() + 1;
        Periode utbetalingsperiode = Periode.of(LocalDate.of(utbetalingsår, 5, 1), LocalDate.of(utbetalingsår, 5, 31));
        return new YtelsePeriode(utbetalingsperiode, Sats.engang(feriepenger.getÅrsbeløp().getVerdi().intValueExact()));
    }

    private YtelsePeriodeMedNøkkel tilYtelsePeriodeMedNøkkel(BeregningsresultatPeriode periode, BeregningsresultatAndel andel) {
        return new YtelsePeriodeMedNøkkel(tilNøkkel(andel), tilYtelsePeriode(periode, andel));
    }

    private YtelsePeriode tilYtelsePeriode(BeregningsresultatPeriode periode, BeregningsresultatAndel andel) {
        return new YtelsePeriode(Periode.of(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()), Sats.dagsats(andel.getDagsats()), tilUtbetalingsgrad(andel));
    }

    private Utbetalingsgrad tilUtbetalingsgrad(BeregningsresultatAndel andel) {
        if (andel.getUtbetalingsgrad() == null) {
            return null;
        }
        return new Utbetalingsgrad(andel.getUtbetalingsgrad().intValue());
    }

    private KjedeNøkkel tilNøkkel(BeregningsresultatAndel andel) {
        boolean tilBruker = andel.skalTilBrukerEllerPrivatperson();
        return tilBruker
            ? new KjedeNøkkel(KlassekodeUtleder.utled(andel, ytelseType), Betalingsmottaker.BRUKER)
            : new KjedeNøkkel(KlassekodeUtleder.utled(andel, ytelseType), Betalingsmottaker.forArbeidsgiver(andel.getArbeidsgiver().get().getOrgnr()));
    }

    private KjedeNøkkel tilNøkkelFeriepenger(BeregningsresultatAndel andel, int opptjeningsår) {
        boolean tilBruker = andel.skalTilBrukerEllerPrivatperson();
        return tilBruker
            ? new KjedeNøkkel(KlassekodeUtleder.utledForFeriepenger(), Betalingsmottaker.BRUKER, opptjeningsår)
            : new KjedeNøkkel(KlassekodeUtleder.utledForFeriepengeRefusjon(ytelseType), Betalingsmottaker.forArbeidsgiver(andel.getArbeidsgiver().get().getOrgnr()), opptjeningsår);
    }

    private Map<KjedeNøkkel, Ytelse> build(Map<KjedeNøkkel, Ytelse.Builder> buildere) {
        Map<KjedeNøkkel, Ytelse> kjeder = new HashMap<>();
        for (var entry : buildere.entrySet()) {
            kjeder.put(entry.getKey(), entry.getValue().build());
        }
        return kjeder;
    }

}
