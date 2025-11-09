package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class DokumentForhandsvisningTest {

    @Test
    void builder_test() {
        var expectedFritekst = "fritekst";
        var expectedDokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;
        var expectedBehandlignUuid = UUID.randomUUID();
        var expectedÅrsak = RevurderingVarslingÅrsak.BRUKER_REGISTRERT_UTVANDRET;
        var expectedSaksnummer = "123";
        var expectedTittel = "tittel";
        var expectedDokumentType = DokumentForhandsvisning.DokumentType.AUTOMATISK;

        var dokumentForhandsvisning = DokumentForhandsvisning.builder()
                .medBehandlingUuid(expectedBehandlignUuid)
                .medDokumentMal(expectedDokumentMal)
                .medFritekst(expectedFritekst)
                .medRevurderingÅrsak(expectedÅrsak)
                .medSaksnummer(new Saksnummer(expectedSaksnummer))
                .medTittel(expectedTittel)
                .medDokumentType(expectedDokumentType)
                .build();

        assertThat(dokumentForhandsvisning).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dokumentForhandsvisning.behandlingUuid()).isEqualTo(expectedBehandlignUuid);
            softly.assertThat(dokumentForhandsvisning.dokumentMal()).isEqualTo(expectedDokumentMal);
            softly.assertThat(dokumentForhandsvisning.fritekst()).isEqualTo(expectedFritekst);
            softly.assertThat(dokumentForhandsvisning.revurderingÅrsak()).isEqualTo(expectedÅrsak);
            softly.assertThat(dokumentForhandsvisning.saksnummer().getVerdi()).isEqualTo(expectedSaksnummer);
            softly.assertThat(dokumentForhandsvisning.tittel()).isEqualTo(expectedTittel);
            softly.assertThat(dokumentForhandsvisning.dokumentType()).isEqualTo(expectedDokumentType);
        });
    }

    @Test
    void should_fail_when_revurderingAarsak_is_not_set_and_dokumentMal_is_VARSEL_OM_REVURDERING() {
        var expectedFritekst = "fritekst";
        var expectedDokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;
        var expectedBehandlignUuid = UUID.randomUUID();
        var expectedSaksnummer = "123";
        var expectedTittel = "tittel";
        var expectedDokumentType = DokumentForhandsvisning.DokumentType.AUTOMATISK;

        assertThatThrownBy(() -> DokumentForhandsvisning.builder()
            .medBehandlingUuid(expectedBehandlignUuid)
            .medDokumentMal(expectedDokumentMal)
            .medFritekst(expectedFritekst)
            .medSaksnummer(new Saksnummer(expectedSaksnummer))
            .medTittel(expectedTittel)
            .medDokumentType(expectedDokumentType)
            .build()).isInstanceOf(NullPointerException.class).hasMessageContaining("Revurdering årsak må være satt.");
    }

    @Test
    void skal_hive_exception_hvis_fritekstbrev_mangler_fritekst() {
        assertThatThrownBy(() -> DokumentForhandsvisning.builder()
            .medBehandlingUuid(UUID.randomUUID())
            .medDokumentMal(DokumentMalType.VEDTAKSBREV_FRITEKST_HTML)
            //.medFritekst()
            .medSaksnummer(new Saksnummer("123"))
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build()).isInstanceOf(NullPointerException.class).hasMessageContaining("Fritekst må være satt for fritekstbre");
    }

    @Test
    void skal_ikke_hive_exception_hvis_fritekstbrev_har_innhold_i_fritekst() {
        var dokumentForhandsvisning = DokumentForhandsvisning.builder()
            .medBehandlingUuid(UUID.randomUUID())
            .medDokumentMal(DokumentMalType.VEDTAKSBREV_FRITEKST_HTML)
            .medFritekst("Fritekst")
            .medSaksnummer(new Saksnummer("123"))
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build();
        assertThat(dokumentForhandsvisning.fritekst()).isEqualTo("Fritekst");
    }
}
