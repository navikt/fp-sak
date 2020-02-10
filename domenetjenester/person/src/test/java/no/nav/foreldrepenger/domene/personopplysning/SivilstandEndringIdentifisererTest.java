package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class SivilstandEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();


    @Test
    public void testSivilstandUendret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erSivilstandEndretForBruker();
        assertThat(erEndret).as("Forventer at sivilstand er uendret").isFalse();
    }

    @Test
    public void testSivilstandEndret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(SivilstandType.GIFT);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(SivilstandType.UGIFT);
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erSivilstandEndretForBruker();
        assertThat(erEndret).as("Forventer at endring i sivilstand blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(SivilstandType sivilstand) {
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID).medSivilstand(sivilstand));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
