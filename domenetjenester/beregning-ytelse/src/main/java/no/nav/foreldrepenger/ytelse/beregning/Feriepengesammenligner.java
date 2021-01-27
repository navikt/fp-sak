package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsforholdNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Feriepengesammenligner {
    private static final Logger logger = LoggerFactory.getLogger(Feriepengesammenligner.class);
    private final long behandlingId;
    private final Saksnummer saksnummer;
    private final BeregningsresultatEntitet nyttResultat;
    private final BeregningsresultatEntitet gjeldendeResultat;
    private final String AVVIK_KODE = "FP-110711: ";
    private final String INGEN_AVVIK_KODE = "FP-110710: ";

    private boolean finnesAvvik = false;

    public Feriepengesammenligner(long behandlingId,
                                  Saksnummer saksnummer,
                                  BeregningsresultatEntitet nyttResultat,
                                  BeregningsresultatEntitet gjeldendeResultat) {
        this.behandlingId = behandlingId;
        this.saksnummer = saksnummer;
        this.nyttResultat = nyttResultat;
        this.gjeldendeResultat = gjeldendeResultat;
    }

    protected boolean finnesAvvik() {
        Optional<BeregningsresultatFeriepenger> nyttFeriepengegrunnlag = nyttResultat.getBeregningsresultatFeriepenger();
        Optional<BeregningsresultatFeriepenger> gjeldendeFeriepengegrunnlag = gjeldendeResultat.getBeregningsresultatFeriepenger();
        if (nyttFeriepengegrunnlag.isPresent() && gjeldendeFeriepengegrunnlag.isPresent()) {
            sammenlignFeriepengeperiode(nyttFeriepengegrunnlag.get(), gjeldendeFeriepengegrunnlag.get());
            sammenlignFeriepengeandeler(nyttFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe(),
                gjeldendeFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe());
        }
        else if (nyttFeriepengegrunnlag.isPresent() || gjeldendeFeriepengegrunnlag.isPresent()) {
            // Vet at kun en av de er present
            loggOgSettAvvik("beregningsresultatFeriepenger", nyttFeriepengegrunnlag, gjeldendeFeriepengegrunnlag);
        }
        if (!finnesAvvik) {
            loggIngenAvvik();
        }
        return finnesAvvik;
    }

    private void sammenlignFeriepengeandeler(List<BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> nyÅrTilBeløpMap = årTilAndelerMap(nyeAndeler);
        Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> gjeldendeÅrTilBeløpMap = årTilAndelerMap(gjeldendeAndeler);
        nyÅrTilBeløpMap.forEach((år, andeler) -> {
            List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndelerForÅr = gjeldendeÅrTilBeløpMap.get(år);
            if (gjeldendeAndelerForÅr == null) {
                loggOgSettAvvik("Feriepengeandeler i år " + år, andeler, null);
            } else {
                Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> nyNøkkelTilAndelerMap = nøkkelTilAndelerMap(andeler);
                Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> gjeldendeNøkkelTilAndelerMap = nøkkelTilAndelerMap(gjeldendeAndelerForÅr);
                sammenlignNøkler(år, nyNøkkelTilAndelerMap, gjeldendeNøkkelTilAndelerMap);
            }
        });

    }

    private void sammenlignNøkler(LocalDate år, Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> nyNøkkelTilAndelerMap,
                                  Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> gjeldendeNøkkelTilAndelerMap) {
        nyNøkkelTilAndelerMap.forEach((nøkkel, andeler) -> {
            List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndelerForNøkkel = gjeldendeNøkkelTilAndelerMap.get(nøkkel);
            if (gjeldendeAndelerForNøkkel == null) {
                loggOgSettAvvik("AktivitetOgArbeidsforholdNøkkel", nøkkel, null);
            } else {
                Beløp gjeldendeÅrsbeløp = finnÅrsbeløp(gjeldendeAndelerForNøkkel);
                Beløp nyttÅrsbeløp = finnÅrsbeløp(andeler);
                if (gjeldendeÅrsbeløp.compareTo(nyttÅrsbeløp) != 0) {
                    loggOgSettAvvik("Årsbeløp for andel " + nøkkel + " i år " + år, nyttÅrsbeløp, gjeldendeÅrsbeløp);
                }
            }

        });
    }

    private Beløp finnÅrsbeløp(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream().map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp).reduce(Beløp::adder).orElse(Beløp.ZERO);
    }

    private Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> nøkkelTilAndelerMap(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream()
            .collect(Collectors.groupingBy(an -> an.getBeregningsresultatAndel().getAktivitetOgArbeidsforholdNøkkel()));
    }

    private Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> årTilAndelerMap(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream().collect(Collectors.groupingBy(BeregningsresultatFeriepengerPrÅr::getOpptjeningsår));
    }

    private void sammenlignFeriepengeperiode(BeregningsresultatFeriepenger nytt, BeregningsresultatFeriepenger gjeldende) {
        boolean ulikFOM = !Objects.equals(nytt.getFeriepengerPeriodeFom(), gjeldende.getFeriepengerPeriodeFom());
        boolean ulikTOM = !Objects.equals(nytt.getFeriepengerPeriodeTom(), gjeldende.getFeriepengerPeriodeTom());
        if (ulikFOM) {
            loggOgSettAvvik("feriepengeperiodeFOM", nytt.getFeriepengerPeriodeFom(), gjeldende.getFeriepengerPeriodeFom());
        }
        if (ulikTOM) {
            loggOgSettAvvik("feriepengeperiodeTOM", nytt.getFeriepengerPeriodeTom(), gjeldende.getFeriepengerPeriodeTom());
        }
    }

    private void loggOgSettAvvik(String beskrivelse, Object nytt, Object gammelt) {
        this.finnesAvvik = true;
        String gammelBeskrivelse = gammelt == null ? "null" : gammelt.toString();
        String nyBeskrivelse = nytt == null ? "null" : nytt.toString();
        logger.info(AVVIK_KODE + "Avvik mellom ny og gammel feriepengeberegning på saksnummer " + saksnummer.getVerdi()  + " på behandling med id: " + behandlingId +
            ". Avvik på " + beskrivelse + ". Gammelt grunnlag hadde verdi " + gammelBeskrivelse + ". Nytt grunnlag hadde verdi: " + nyBeskrivelse);
    }

    private void loggIngenAvvik() {
        logger.info(INGEN_AVVIK_KODE + "Finner ingen avvik mellom ny og gammel feriepengeberegning på saksnummer " + saksnummer.getVerdi() + " på behandling med id: " + behandlingId);
    }


}
