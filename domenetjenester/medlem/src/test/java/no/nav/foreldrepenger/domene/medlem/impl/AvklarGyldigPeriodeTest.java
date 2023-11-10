
package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;

@CdiDbAwareTest
class AvklarGyldigPeriodeTest {

    @Inject
    private BehandlingRepositoryProvider provider;

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    private AvklarGyldigPeriode avklarGyldigPeriode;

    @BeforeEach
    public void setUp() {
        this.avklarGyldigPeriode = new AvklarGyldigPeriode(provider, medlemskapPerioderTjeneste);
    }

    @Test
    void skal_ikke_opprette_Aksjonspunkt_ved_gyldig_periode() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_A) // hjemlet i bokstav a
                .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
                .medPeriode(fødselsdato, fødselsdato)
                .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(gyldigPeriodeUnderFødsel);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skalIkkeOppretteAksjonspunktVedIngenTreffMedl() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skalIkkeOppretteAksjonspunktVedIngenUavklartPeriode() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var lukketPeriodeFørFødselsdato = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_B) // ikke hjemlet i bokstav a eller c
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(fødselsdato, fødselsdato)
                .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(lukketPeriodeFørFødselsdato);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skalOppretteAksjonspunktVedUavklartPeriode() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var medlemskapPeriodeUnderAvklaring = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_A) // hjemlet i bokstav a
                .medMedlemskapType(MedlemskapType.UNDER_AVKLARING)
                .medPeriode(fødselsdato, fødselsdato)
                .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(medlemskapPeriodeUnderAvklaring);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).contains(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }

    @Test
    void skalOppretteAksjonspunktVedÅpenPeriode() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var åpenPeriode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_A) // hjemlet i bokstav a
                .medMedlemskapType(MedlemskapType.FORELOPIG)
                .medPeriode(fødselsdato, null) // åpen periode
                .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(åpenPeriode);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).contains(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }
}
