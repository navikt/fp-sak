package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.typer.AktørId;

class SivilstandEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    void testSivilstandUendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSivilstandEndretForBruker();
        assertThat(erEndret).as("Forventer at sivilstand er uendret").isFalse();
    }

    @Test
    void testSivilstandEndret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(SivilstandType.UGIFT);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSivilstandEndretForBruker();
        assertThat(erEndret).as("Forventer at endring i sivilstand blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(SivilstandType sivilstand) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID).medSivilstand(sivilstand));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
