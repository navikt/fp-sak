package no.nav.foreldrepenger.mottak;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;

class MottattDokumentSortererTest {

    @Test
    void skal_sortere_etter_mottatt_dag_og_kanalref_når_dag_er_lik() {
        // Arrange
        var builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR307765531");

        var første = builder.build();


        var builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR317765531");
        var andre = builder2.build();

        var dokumenter = List.of(andre, første);

        // Act
        var sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }

    @Test
    void skal_sortere_etter_mottatt_dag_når_den_er_ulik() {
        // Arrange
        var builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L);

        var første = builder.build();

        var builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now().plusDays(1))
            .medFagsakId(41337L);
        var andre = builder2.build();

        var dokumenter = List.of(andre, første);

        // Act
        var sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }

    @Test
    void skal_ikke_feile_når_kanalref_er_null() {
        // Arrange
        var builder = new MottattDokument.Builder();
        builder
            .medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse(null);

        var første = builder.build();


        var builder2 = new MottattDokument.Builder();
        builder2.medMottattDato(LocalDate.now())
            .medFagsakId(41337L)
            .medKanalreferanse("AR317765531");
        var andre = builder2.build();

        var dokumenter = List.of(andre, første);

        // Act
        var sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }
}
