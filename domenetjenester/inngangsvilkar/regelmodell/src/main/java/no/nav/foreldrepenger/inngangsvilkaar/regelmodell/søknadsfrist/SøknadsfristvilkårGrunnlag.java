package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist;

import java.time.LocalDate;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public record SøknadsfristvilkårGrunnlag(boolean elektroniskSoeknad,
                                         LocalDate skjaeringstidspunkt,
                                         LocalDate soeknadMottatDato) implements VilkårGrunnlag {
    /*
     * Jepp det er ikke norske bokstaver - lot det ligge i denne omgangen pga vedtakxml-mapping
     * For å fornorske: Lag en legacy-variant så LagretVedtakXML kan hente fra gammel/ny json
     * Men helst finn på noe smartere ifm vedtakslagring - deretter full historisk regenerering av alle saker
     */

}
