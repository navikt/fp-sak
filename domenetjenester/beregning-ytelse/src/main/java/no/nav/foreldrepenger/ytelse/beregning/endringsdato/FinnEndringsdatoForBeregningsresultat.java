package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatEndringModell;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class FinnEndringsdatoForBeregningsresultat {

    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister;

    FinnEndringsdatoForBeregningsresultat(){
        // for CDI proxy
    }

    @Inject
    public FinnEndringsdatoForBeregningsresultat(FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister){
        this.finnEndringsdatoMellomPeriodeLister = finnEndringsdatoMellomPeriodeLister;
    }

    /**
     * Utleder endringsdato fra originalt beregningsresultat og revurdert beregningsresultat.
     * Hvis det finnes en endringsdato i tilkjent ytelse (beregningsresultatperioder) skal denne brukes.
     * Om denne ikke finnes sjekker vi om det finnes en endringsdato for feriepengene og bruker isåfall denne
     * @param originalBeregningsresultatEntitet beregningsresultat fra forrige behandling
     * @param revurderingBeregningsresultatEntitet nytt beregningsresultat
     * @return endringsdato om den finnes
     */
    public Optional<LocalDate> utledEndringsdato(BeregningsresultatEntitet originalBeregningsresultatEntitet,
                                                 BeregningsresultatEntitet revurderingBeregningsresultatEntitet) {
        BeregningsresultatEndringModell originaltBeregningsresultat = new MapBeregningsresultatTilEndringsmodell(originalBeregningsresultatEntitet).map();
        BeregningsresultatEndringModell revurderingBeregningsresultat = new MapBeregningsresultatTilEndringsmodell(revurderingBeregningsresultatEntitet).map();
        Optional<LocalDate> endringsdatoTilkjentYtelse = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingBeregningsresultat.getBeregningsresultatperioder(),
            originaltBeregningsresultat.getBeregningsresultatperioder());
        if (endringsdatoTilkjentYtelse.isPresent()) {
            return endringsdatoTilkjentYtelse;
        }
        return FinnEndringsdatoForFeriepenger.finnEndringsdato(originalBeregningsresultatEntitet.getBeregningsresultatFeriepenger(),
            revurderingBeregningsresultatEntitet.getBeregningsresultatFeriepenger());
    }
}
