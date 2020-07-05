package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;

public class VurderDekningsgradVedDødsfallAksjonspunktUtlederTest {

    private LocalDate fødselsdato;

    @Before
    public void setup() {
        fødselsdato = LocalDate.now();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_pluss_en_dag() {
        // Arrange
        LocalDate dødsdato = fødselsdato.plusWeeks(6).plusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        int dekningsgrad = 80;

        // Act
        List<AksjonspunktResultat> apResultat = new ArrayList<>();
        VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(apResultat, dekningsgrad, List.of(barnList));

        // Assert
        var apDefs = apResultat.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).doesNotContain(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
    }

    @Test
    public void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_minus_en_dag() {
        // Arrange
        LocalDate dødsdato = fødselsdato.plusWeeks(6).minusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        int dekningsgrad = 80;

        // Act
        List<AksjonspunktResultat> apResultat = new ArrayList<>();
        VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(apResultat, dekningsgrad, List.of(barnList));

        // Assert
        var apDefs = apResultat.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
    }

    @Test
    public void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker() {
        // Arrange
        LocalDate dødsdato = fødselsdato.plusWeeks(6);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        int dekningsgrad = 80;

        // Act
        List<AksjonspunktResultat> apResultat = new ArrayList<>();
        VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(apResultat, dekningsgrad, List.of(barnList));
        // Assert
        var apDefs = apResultat.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
    }

    @Test
    public void skal_få_aksjonspunkt5087_når_dekningsgrad_er_annerledes_en_80() {
        // Arrange
        LocalDate dødsdato = fødselsdato.plusWeeks(6);
        var barn = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        int dekningsgrad = 100;

        // Act
        List<AksjonspunktResultat> apResultat = new ArrayList<>();
        VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(apResultat, dekningsgrad, List.of(barn));
        // Assert
        var apDefs = apResultat.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).doesNotContain(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
    }

    @Test
    public void skal_få_aksjonspunkt5087_når_barn_ikke_finnes() {
        // Arrange
        int dekningsgrad = 80;

        // Act
        List<AksjonspunktResultat> apResultat = new ArrayList<>();
        VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(apResultat, dekningsgrad, List.of());
        // Assert
        var apDefs = apResultat.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).doesNotContain(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
    }
}
