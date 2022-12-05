package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.LegacyLocalDateDeserializer;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

/**
 * Denne finnes utelukkende pga VedtakXML og DvhSML - deserialisere gamle ES-vilkår som har dato som objekt (se FødselsVilkårDocTest for exempel)
 */
@RuleDocumentationGrunnlag
public record FødselsvilkårGrunnlagLegacy(@JsonProperty("soekersKjonn") RegelKjønn søkersKjønn,
                                          @JsonProperty("soekerRolle") RegelSøkerRolle søkerRolle,
                                          @JsonDeserialize(using = LegacyLocalDateDeserializer.class) @JsonProperty("dagensdato") @JsonAlias("soeknadsdato") LocalDate behandlingsdato,
                                          @JsonDeserialize(using = LegacyLocalDateDeserializer.class) @JsonProperty("bekreftetFoedselsdato") LocalDate bekreftetFødselsdato,
                                          @JsonDeserialize(using = LegacyLocalDateDeserializer.class) @JsonProperty("bekreftetTermindato") LocalDate terminbekreftelseTermindato,
                                          int antallBarn,
                                          boolean erFødselRegistreringFristUtløpt,
                                          boolean erMorForSykVedFødsel,
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
