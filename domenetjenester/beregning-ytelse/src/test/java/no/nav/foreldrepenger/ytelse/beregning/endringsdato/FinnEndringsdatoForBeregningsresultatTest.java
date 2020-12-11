package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import static org.junit.jupiter.api.Assertions.*;

class FinnEndringsdatoForBeregningsresultatTest {
    private SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler = new SjekkOmPerioderHarEndringIAndeler();
    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats = new SjekkForIngenAndelerOgAndelerUtenDagsats();
    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder = new SjekkForEndringMellomPerioder(sjekkForIngenAndelerOgAndelerUtenDagsats, sjekkOmPerioderHarEndringIAndeler);
    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister = new FinnEndringsdatoMellomPeriodeLister(sjekkForEndringMellomPerioder);
    private FinnEndringsdatoForBeregningsresultat tjeneste = new FinnEndringsdatoForBeregningsresultat(finnEndringsdatoMellomPeriodeLister);

}
