package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatAndelEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatFeriepengerEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatFeriepengerPrÅrEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;

import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MapBeregningsresultatTilEndringsmodell {
    private final BeregningsresultatEntitet entitet;

    public MapBeregningsresultatTilEndringsmodell(BeregningsresultatEntitet entitet) {
        Objects.requireNonNull(entitet, "beregningsresultatentitet");
        this.entitet = entitet;
    }

    public BeregningsresultatEndringModell map() {
        Optional<BeregningsresultatFeriepengerEndringModell> feriepenger = entitet.getBeregningsresultatFeriepenger()
            .map(this::mapFeriepenger);
        List<BeregningsresultatPeriodeEndringModell> perioder = entitet.getBeregningsresultatPerioder().stream()
            .map(this::mapPeriode)
            .collect(Collectors.toList());
        return new BeregningsresultatEndringModell(feriepenger.orElse(null), perioder);
    }

    private BeregningsresultatFeriepengerEndringModell mapFeriepenger(BeregningsresultatFeriepenger fp) {
        List<BeregningsresultatFeriepengerPrÅrEndringModell> prÅrListe = fp.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .map(this::mapPrÅr)
            .collect(Collectors.toList());
        return new BeregningsresultatFeriepengerEndringModell(fp.getFeriepengerPeriodeFom(),
            fp.getFeriepengerPeriodeTom(),
            prÅrListe);
    }

    private BeregningsresultatFeriepengerPrÅrEndringModell mapPrÅr(BeregningsresultatFeriepengerPrÅr prÅr) {
        return new BeregningsresultatFeriepengerPrÅrEndringModell(prÅr.getÅrsbeløp(), Year.of(prÅr.getOpptjeningsåret()));
    }

    private BeregningsresultatPeriodeEndringModell mapPeriode(BeregningsresultatPeriode periodeEntitet) {
        List<BeregningsresultatAndelEndringModell> andeler = periodeEntitet.getBeregningsresultatAndelList().stream()
            .map(this::mapAndel)
            .collect(Collectors.toList());
        return new BeregningsresultatPeriodeEndringModell(periodeEntitet.getBeregningsresultatPeriodeFom(),
            periodeEntitet.getBeregningsresultatPeriodeTom(),
            andeler);
    }

    private BeregningsresultatAndelEndringModell mapAndel(BeregningsresultatAndel andel) {
        if (andel.getArbeidsgiver().isEmpty()) {
            return new BeregningsresultatAndelEndringModell(andel.getAktivitetStatus(),
                andel.getInntektskategori(),
                null,
                InternArbeidsforholdRef.nullRef(),
                andel.erBrukerMottaker(),
                andel.getDagsats());
        }
        return new BeregningsresultatAndelEndringModell(andel.getAktivitetStatus(),
            andel.getInntektskategori(),
            andel.getArbeidsgiver().get(),
            andel.getArbeidsforholdRef(),
            andel.erBrukerMottaker(),
            andel.getDagsats());
    }
}
