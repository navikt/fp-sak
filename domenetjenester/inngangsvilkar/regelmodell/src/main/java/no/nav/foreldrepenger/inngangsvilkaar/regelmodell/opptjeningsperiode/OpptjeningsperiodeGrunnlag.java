package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public record OpptjeningsperiodeGrunnlag(FagsakÅrsak fagsakÅrsak,
                                         RegelSøkerRolle søkerRolle,
                                         LocalDate førsteUttaksDato,
                                         LocalDate hendelsesDato,
                                         LocalDate terminDato,
                                         LocalDate morsMaksdato,
                                         LovVersjoner lovVersjon) implements VilkårGrunnlag {

    public Optional<LocalDate> morsMaksdatoOpt() {
        return Optional.ofNullable(morsMaksdato);
    }

    public LovVersjoner lovVersjonDefaultKlassisk() {
        return Optional.ofNullable(lovVersjon).orElse(LovVersjoner.KLASSISK);
    }
}


