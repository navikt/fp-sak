package no.nav.foreldrepenger.domene.risikoklassifisering.mapper;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikogruppeDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class KontrollresultatMapperTest {

    @Test
    void skal_gjøre_faresignal_respons_om_til_wrapper_ved_ikke_høy_risiko() {
        // Arrange
        var respons = new RisikovurderingResultatDto(RisikoklasseType.IKKE_HØY, null, null, null);

        // Act
        var wrapper = KontrollresultatMapper.fraFaresignalRespons(respons);

        // Assert
        assertThat(wrapper.kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(wrapper.iayFaresignaler()).isNull();
        assertThat(wrapper.medlemskapFaresignaler()).isNull();
        assertThat(wrapper.faresignalVurdering()).isNull();
    }

    @Test
    void skal_gjøre_faresignal_respons_om_til_wrapper_ved_høy_risiko() {
        // Arrange
        var faresignaler = Arrays.asList("Dette er en test", "Dette er også en test", "123 321 987");
        var risikogruppe = new RisikogruppeDto(faresignaler);
        var respons = new RisikovurderingResultatDto(RisikoklasseType.HØY, risikogruppe, risikogruppe, FaresignalVurdering.AVSLAG_FARESIGNAL);

        // Act
        var wrapper = KontrollresultatMapper.fraFaresignalRespons(respons);

        // Assert
        assertThat(wrapper.kontrollresultat()).isEqualTo(Kontrollresultat.HØY);
        assertThat(wrapper.iayFaresignaler()).isNotNull();
        assertThat(wrapper.iayFaresignaler().faresignaler()).hasSize(faresignaler.size());
        assertThat(wrapper.iayFaresignaler().faresignaler()).containsAll(faresignaler);

        assertThat(wrapper.medlemskapFaresignaler()).isNotNull();
        assertThat(wrapper.medlemskapFaresignaler().faresignaler()).hasSize(faresignaler.size());
        assertThat(wrapper.medlemskapFaresignaler().faresignaler()).containsAll(faresignaler);

        assertThat(wrapper.faresignalVurdering()).isNotNull();
        assertThat(wrapper.faresignalVurdering()).isEqualTo(no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering.AVSLAG_FARESIGNAL);
    }
}
