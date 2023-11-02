package no.nav.foreldrepenger.datavarehus.dto;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TilkjentPeriode {

    private LocalDate fom;
    private LocalDate tom;
    private Aktivitet aktivitet;

    // TODO: Må diskuteres - mulig det må mappes om, kanskje lovhjemmler?
    private PeriodeResultatÅrsak uttaksResultat;

    private BigDecimal uttaksgrad;

    // private List<TilkjentAndel> utbetalingsgrader // mottakere??

    private RettighetType rettighetType = RettighetUtleder.utledRettighet();

    private Stønadskonto stønadskonto;

    // brukt av rettighet
    // minsterett
    // morsaktivitet
    // søkt gradering
    // sammtidig uttak
    // aleneomsorg
    // annen foreldre har rett
    // flerbarnsdager
}
