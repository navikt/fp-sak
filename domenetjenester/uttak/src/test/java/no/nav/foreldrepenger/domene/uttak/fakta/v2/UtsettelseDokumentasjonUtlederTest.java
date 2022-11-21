package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;

class UtsettelseDokumentasjonUtlederTest {

    @Test
    void sammenhengende_uttak_utsettelse_sykdom_trenger_vurdering() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseSykdom);
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER);
    }

    @Test
    void sammenhengende_uttak_utsettelse_innleggelse_trenger_vurdering() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_SØKER);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseSykdom);
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_SØKER);
    }

    @Test
    void sammenhengende_uttak_utsettelse_innleggelse_barn_trenger_vurdering() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_BARN);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseSykdom);
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_BARN);
    }

    @Test
    void sammenhengende_uttak_utsettelse_ferie_skal_ikke_vurderes() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.FERIE);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseSykdom);
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void sammenhengende_uttak_utsettelse_arbeid_skal_ikke_vurderes() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.ARBEID);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseSykdom);
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void utsettelse_sykdom_trenger_ikke_vurdering_etter_uke_6() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
        var vurderingBehov = utledVurderingBehov(utsettelseSykdom, utsettelseSykdom.getFom().minusMonths(2));
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void utsettelse_sykdom_trenger_vurdering_før_uke_6() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
        var vurderingBehov = utledVurderingBehov(utsettelseSykdom, utsettelseSykdom.getFom());
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER);
    }

    private Optional<DokumentasjonVurderingBehov.Behov> utledVurderingBehovSammenhengendeUttak(OppgittPeriodeEntitet oppgittPeriode) {
        return UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, oppgittPeriode.getFom().minusMonths(2), true);
    }

    private Optional<DokumentasjonVurderingBehov.Behov> utledVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, LocalDate familiehendelse) {
        return UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, familiehendelse, false);
    }

    private OppgittPeriodeEntitet periodeMedUtsettelse(UtsettelseÅrsak utsettelseÅrsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(utsettelseÅrsak)
            .medPeriode(LocalDate.of(2018, 3, 15), LocalDate.of(2018, 3, 15)).build();
    }

}
