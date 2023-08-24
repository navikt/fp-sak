package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RuleDocumentationGrunnlag
public record AdopsjonsvilkårGrunnlag (List<BekreftetAdopsjonBarn> bekreftetAdopsjonBarn,
                                       boolean ektefellesBarn,
                                       @JsonProperty("soekersKjonn") RegelKjønn søkersKjønn,
                                       boolean mannAdoptererAlene,
                                       LocalDate omsorgsovertakelsesdato,
                                       boolean erStønadsperiodeBruktOpp) implements VilkårGrunnlag {
    public AdopsjonsvilkårGrunnlag {
        Objects.requireNonNull(bekreftetAdopsjonBarn);
    }

    /*
     * Jepp det er ikke norske bokstaver - lot det ligge i denne omgangen pga vedtakxml-mapping (den bør også oppdateres)
     * For å fornorske: Lag en legacy-variant så LagretVedtakXML kan hente fra gammel/ny json
     * Men helst finn på noe smartere ifm vedtakslagring - deretter full historisk regenerering av alle saker
     */

}
