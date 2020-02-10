package no.nav.foreldrepenger.mottak;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.MottattDokumentSorterer;

public class MottattDokumentSortererTest {

    @Test
    public void skal_sortere_etter_mottatt_dag_og_kanalref_når_dag_er_lik() {
        // Arrange
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR307765531");

        MottattDokument første = builder.build();


        MottattDokument.Builder builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR317765531");
        MottattDokument andre = builder2.build();

        List<MottattDokument> dokumenter = List.of(andre, første);

        // Act
        List<MottattDokument> sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }

    @Test
    public void skal_sortere_etter_mottatt_dag_når_den_er_ulik() {
        // Arrange
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L);

        MottattDokument første = builder.build();

        MottattDokument.Builder builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now().plusDays(1))
            .medFagsakId(41337L);
        MottattDokument andre = builder2.build();

        List<MottattDokument> dokumenter = List.of(andre, første);

        // Act
        List<MottattDokument> sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }

    @Test
    public void skal_ikke_feile_når_kanalref_er_null() {
        // Arrange
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse(null);

        MottattDokument første = builder.build();


        MottattDokument.Builder builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR317765531");
        MottattDokument andre = builder2.build();

        List<MottattDokument> dokumenter = List.of(andre, første);

        // Act
        List<MottattDokument> sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }
}
