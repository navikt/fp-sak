package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

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
     * erStønadsperiodeBruktOpp er legacy - har slått til i to tilfelle
     *
     * Jepp det er ikke norske bokstaver - lot det ligge i denne omgangen pga vedtakxml-mapping (den bør også oppdateres)
     * For å fornorske: Lag en legacy-variant så LagretVedtakXML kan hente fra gammel/ny json
     * Men helst finn på noe smartere ifm vedtakslagring - deretter full historisk regenerering av alle saker
     *
     * Se eksempel fra Stønadskonto for lesing av eldre regelinput og evt ny lagring etter remapping til nye navn.
     * Krever GrunnlagV0, V1, V2 ,etc så jackson kan lese alle historisk versjoner
     */

}
