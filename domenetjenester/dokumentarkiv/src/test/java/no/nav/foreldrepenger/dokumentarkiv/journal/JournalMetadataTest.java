package no.nav.foreldrepenger.dokumentarkiv.journal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class JournalMetadataTest {

    @Test
    public void skal_lage_fullt_populert_metadata() {
        // Arrange

        JournalMetadata.Builder builder = JournalMetadata.builder();

        builder.medJournalpostId(new JournalpostId("jpId"));
        builder.medDokumentId("dokId");
        builder.medVariantFormat(VariantFormat.ARKIV);
        builder.medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON);
        builder.medDokumentKategori(DokumentKategori.ELEKTRONISK_SKJEMA);
        builder.medArkivFilType(ArkivFilType.PDFA);
        builder.medJournaltilstand(JournalMetadata.Journaltilstand.ENDELIG);
        builder.medErHoveddokument(true);
        final LocalDate naa = LocalDate.now();
        builder.medForsendelseMottatt(naa);
        final List<String> brukerIdentListe = List.of("brId1", "brId2");
        builder.medBrukerIdentListe(brukerIdentListe);

        // Act

        JournalMetadata jmd = builder.build();

        // Assert

        assertThat(jmd).isNotNull();
        assertThat(jmd.getJournalpostId().getVerdi()).isEqualTo("jpId");
        assertThat(jmd.getDokumentId()).isEqualTo("dokId");
        assertThat(jmd.getVariantFormat()).isEqualTo(VariantFormat.ARKIV);
        assertThat(jmd.getDokumentType()).isEqualTo(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON);
        assertThat(jmd.getDokumentKategori()).isEqualTo(DokumentKategori.ELEKTRONISK_SKJEMA);
        assertThat(jmd.getArkivFilType()).isEqualTo(ArkivFilType.PDFA);
        assertThat(jmd.getJournaltilstand()).isEqualTo(JournalMetadata.Journaltilstand.ENDELIG);
        assertThat(jmd.getErHoveddokument()).isTrue();
        assertThat(jmd.getForsendelseMottatt()).isEqualTo(naa);
        assertThat(jmd.getBrukerIdentListe()).isEqualTo(brukerIdentListe);
    }

}
