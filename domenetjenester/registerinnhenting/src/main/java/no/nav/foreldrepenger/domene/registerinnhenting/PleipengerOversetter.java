package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PleipengerOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(PleipengerOversetter.class);

    public static PleiepengerOpplysninger oversettTilleggsopplysninger(String tilleggsOpplysninger) {
        try {
            // TODO finne ut hvor double-quote escapes og gir &#34;
            var midlertidigKonvertering = tilleggsOpplysninger.replace("&#34;", "\"");
            return StandardJsonConfig.fromJson(midlertidigKonvertering, PleiepengerOpplysninger.class);
        } catch (Exception e) {
            LOG.warn("Feil ved oversetting av pleiepenger / innleggelse for {}", tilleggsOpplysninger, e);
            return null;
        }
    }


    public record PleiepengerOpplysninger(AktørId pleietrengende, List<PleiepengerInnlagtPeriode> innleggelsesPerioder) {
    }

    public record PleiepengerInnlagtPeriode(LocalDate fom, LocalDate tom) {
    }

}
