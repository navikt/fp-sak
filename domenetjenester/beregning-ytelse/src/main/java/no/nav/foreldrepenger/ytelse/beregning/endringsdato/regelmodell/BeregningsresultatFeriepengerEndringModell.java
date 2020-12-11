package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeregningsresultatFeriepengerEndringModell {
    private LocalDate feriepengeperiodeFom;
    private LocalDate feriepengerperiodeTom;
    private List<BeregningsresultatFeriepengerPrÅrEndringModell> feriepengerPrÅrListe = new ArrayList<>();

    public BeregningsresultatFeriepengerEndringModell() {
        // For JSON-deserialisering i feilsøking
    }

    public BeregningsresultatFeriepengerEndringModell(LocalDate feriepengeperiodeFom,
                                                      LocalDate feriepengerperiodeTom,
                                                      List<BeregningsresultatFeriepengerPrÅrEndringModell> feriepengerPrÅrListe) {
        this.feriepengeperiodeFom = feriepengeperiodeFom;
        this.feriepengerperiodeTom = feriepengerperiodeTom;
        this.feriepengerPrÅrListe = feriepengerPrÅrListe;
    }

    public LocalDate getFeriepengeperiodeFom() {
        return feriepengeperiodeFom;
    }

    public LocalDate getFeriepengerperiodeTom() {
        return feriepengerperiodeTom;
    }

    public List<BeregningsresultatFeriepengerPrÅrEndringModell> getFeriepengerPrÅrListe() {
        return feriepengerPrÅrListe;
    }
}
