package no.nav.foreldrepenger.domene.risikoklassifisering.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikogruppeDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.vedtak.kontroll.kodeverk.KontrollResultatkode;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;

public class KontrollresultatMapperTest {

    private final KontrollresultatMapper kontrollresultatMapper = new KontrollresultatMapper();

    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_høy_risiko() {
        // Arrange
        var uuid = UUID.randomUUID();

        // Act
        var wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.HØY));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_ikke_høy_risiko() {
        // Arrange
        var uuid = UUID.randomUUID();

        // Act
        var wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.IKKE_HØY));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.IKKE_HØY);
    }


    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_ikke_klassifisert() {
        // Arrange
        var uuid = UUID.randomUUID();

        // Act
        var wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.IKKE_KLASSIFISERT));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.IKKE_KLASSIFISERT);
    }

    @Test
    public void skal_gjøre_faresignal_respons_om_til_wrapper() {
        // Arrange
        var faresignaler = Arrays.asList("Dette er en test", "Dette er også en test", "123 321 987");
        var risikogruppe = new RisikogruppeDto(faresignaler);
        var respons = new RisikovurderingResultatDto(RisikoklasseType.HØY, risikogruppe, risikogruppe, FaresignalVurdering.AVSLAG_FARESIGNAL);

        // Act
        var wrapper = kontrollresultatMapper.fraFaresignalRespons(respons);

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


    private KontrollResultatV1 lagKontrollresultat(UUID uuid, KontrollResultatkode resultatkode) {
        var builder = new KontrollResultatV1.Builder();
        return builder.medBehandlingUuid(uuid).medResultatkode(resultatkode).build();
    }
}
