package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public record FødselsvilkårGrunnlag(@JsonProperty("soekersKjonn") RegelKjønn søkersKjønn,
                                    @JsonProperty("soekerRolle") RegelSøkerRolle søkerRolle,
                                    @JsonProperty("dagensdato") LocalDate behandlingsdato,
                                    @JsonProperty("bekreftetFoedselsdato") LocalDate bekreftetFødselsdato,
                                    @JsonProperty("bekreftetTermindato") LocalDate terminbekreftelseTermindato,
                                    int antallBarn,
                                    boolean erFødselRegistreringFristUtløpt,
                                    boolean erMorForSykVedFødsel, // Legacy - tilfelle før WLB
                                    boolean erSøktOmTermin,
                                    boolean erBehandlingsdatoEtterTidligsteDato,
                                    @JsonProperty("erTerminBekreftelseUtstedtEtterXUker") boolean erTerminbekreftelseUtstedtEtterTidligsteDato,
                                    boolean farMedmorUttakRundtFødsel) implements VilkårGrunnlag {

    /*
     * Jepp det er ikke norske bokstaver - lot det ligge i denne omgangen pga vedtakxml-mapping (den bør også oppdateres)
     * For å fornorske: Lag en legacy-variant så LagretVedtakXML kan hente fra gammel/ny json
     * Men helst finn på noe smartere ifm vedtakslagring - deretter full historisk regenerering av alle saker
     */
}
