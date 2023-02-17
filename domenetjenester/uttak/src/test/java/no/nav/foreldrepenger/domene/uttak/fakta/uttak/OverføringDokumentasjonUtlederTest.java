package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;

class OverføringDokumentasjonUtlederTest {

    @Test
    void overføring_sykdom_skal_vurderes() {
        var overføring = overføring(OverføringÅrsak.SYKDOM_ANNEN_FORELDER);
        var behov = OverføringDokumentasjonUtleder.utledBehov(overføring);

        assertThat(behov).isPresent();
        assertThat(behov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
        assertThat(behov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER);
    }

    @Test
    void overføring_innleggelse_skal_vurderes() {
        var overføring = overføring(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER);
        var behov = OverføringDokumentasjonUtleder.utledBehov(overføring);

        assertThat(behov).isPresent();
        assertThat(behov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
        assertThat(behov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_ANNEN_FORELDER);
    }

    @Test
    void overføring_aleneomsorg_skal_vurderes() {
        var overføring = overføring(OverføringÅrsak.ALENEOMSORG);
        var behov = OverføringDokumentasjonUtleder.utledBehov(overføring);

        assertThat(behov).isPresent();
        assertThat(behov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
        assertThat(behov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.ALENEOMSORG);
    }

    @Test
    void overføring_ikke_rett_skal_vurderes() {
        var overføring = overføring(OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER);
        var behov = OverføringDokumentasjonUtleder.utledBehov(overføring);

        assertThat(behov).isPresent();
        assertThat(behov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
        assertThat(behov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.BARE_SØKER_RETT);
    }

    @Test
    void ukjent_årsak_gir_tomt_resultat() {
        var periode = overføring(Årsak.UKJENT);
        var behov = OverføringDokumentasjonUtleder.utledBehov(periode);

        assertThat(behov).isEmpty();
    }

    @Test
    void udefinert_årsak_kaster_exception() {
        var overføring = overføring(OverføringÅrsak.UDEFINERT);
        assertThatThrownBy(() -> OverføringDokumentasjonUtleder.utledBehov(overføring)).isInstanceOf(IllegalArgumentException.class);
    }

    private OppgittPeriodeEntitet overføring(Årsak overføringÅrsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2022, 10, 10), LocalDate.of(2023, 11, 11))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(overføringÅrsak)
            .build();
    }

}
