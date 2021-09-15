package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public record MedlemskapsvilkårGrunnlag(RegelPersonStatusType personStatusType,
                                     boolean brukerNorskNordisk,
                                     boolean brukerBorgerAvEUEOS,
                                     boolean brukerHarOppholdstillatelse,
                                     boolean harSøkerArbeidsforholdOgInntekt,
                                     boolean brukerErMedlem,
                                     boolean brukerAvklartPliktigEllerFrivillig,
                                     boolean brukerAvklartBosatt,
                                     boolean brukerAvklartLovligOppholdINorge,
                                     boolean brukerAvklartOppholdsrett) implements VilkårGrunnlag {

    /*
      * Hvis du tenker på annen navngivning - obs på deserialisering av tidligere/nye navn i VedtakXML+ DvhXML .
      * Anbefaler å gjennomgå all vilkårlagring og avklare med Stønadsstatistikk hva som brukes av DvhXML,
      * deretter evt regenerere alle tidliger vedtak. Prosess 2022.
     */

}
