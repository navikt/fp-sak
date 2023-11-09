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

    public static OpptjeningsperiodeGrunnlag grunnlag(FagsakÅrsak fagsakÅrsak, RegelSøkerRolle søkerRolle, LovVersjoner lovVersjon) {
        return new OpptjeningsperiodeGrunnlag(fagsakÅrsak, søkerRolle, null, null, null, null, lovVersjon);
    }

    public OpptjeningsperiodeGrunnlag medFørsteUttaksDato(LocalDate førsteUttaksDato) {
        return new OpptjeningsperiodeGrunnlag(fagsakÅrsak(), søkerRolle(), førsteUttaksDato, hendelsesDato(), terminDato(), morsMaksdato(), lovVersjon());
    }

    public OpptjeningsperiodeGrunnlag medHendelsesDato(LocalDate hendelsesDato) {
        return new OpptjeningsperiodeGrunnlag(fagsakÅrsak(), søkerRolle(), førsteUttaksDato(), hendelsesDato, terminDato(), morsMaksdato(), lovVersjon());
    }

    public OpptjeningsperiodeGrunnlag medTerminDato(LocalDate terminDato) {
        return new OpptjeningsperiodeGrunnlag(fagsakÅrsak(), søkerRolle(), førsteUttaksDato(), hendelsesDato(), terminDato, morsMaksdato(), lovVersjon());
    }

    public OpptjeningsperiodeGrunnlag medMorsMaksdato(LocalDate morsMaksdato) {
        return new OpptjeningsperiodeGrunnlag(fagsakÅrsak(), søkerRolle(), førsteUttaksDato(), hendelsesDato(), terminDato(), morsMaksdato, lovVersjon());
    }
}


