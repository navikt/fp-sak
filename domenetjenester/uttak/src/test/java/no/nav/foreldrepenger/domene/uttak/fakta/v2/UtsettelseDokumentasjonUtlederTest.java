package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

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
        var utsettelseInnlagt = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_SØKER);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseInnlagt);
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_SØKER);
    }

    @Test
    void sammenhengende_uttak_utsettelse_innleggelse_barn_trenger_vurdering() {
        var utsettelseInnlagt = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_BARN);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(utsettelseInnlagt);
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_BARN);
    }

    @Test
    void sammenhengende_uttak_utsettelse_ferie_skal_ikke_vurderes() {
        var ferie = periodeMedUtsettelse(UtsettelseÅrsak.FERIE);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(ferie);
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void sammenhengende_uttak_utsettelse_arbeid_skal_ikke_vurderes() {
        var arbeid = periodeMedUtsettelse(UtsettelseÅrsak.ARBEID);
        var vurderingBehov = utledVurderingBehovSammenhengendeUttak(arbeid);
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void utsettelse_sykdom_trenger_ikke_vurdering_etter_uke_6() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
        var vurderingBehov = utledVurderingBehov(utsettelseSykdom, utsettelseSykdom.getFom().minusMonths(2), List.of());
        assertThat(vurderingBehov).isEmpty();
    }

    @Test
    void utsettelse_sykdom_trenger_vurdering_før_uke_6() {
        var utsettelseSykdom = periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
        var vurderingBehov = utledVurderingBehov(utsettelseSykdom, utsettelseSykdom.getFom(), List.of());
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER);
    }

    @Test
    void utsettelse_innleggelse_barn_trenger_vurdering() {
        var utsettelseBarnInnlagt = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_BARN);
        var vurderingBehov = utledVurderingBehov(utsettelseBarnInnlagt, utsettelseBarnInnlagt.getFom(), List.of());
        assertThat(vurderingBehov).isPresent();
        assertThat(vurderingBehov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE);
        assertThat(vurderingBehov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_BARN);
    }

    @Test
    void utsettelse_innleggelse_barn_med_innleggelse_pleiepenger_trenger_vurdering() {
        var pleiepenger = periodeMedUtsettelse(UtsettelseÅrsak.INSTITUSJON_BARN);
        var vurderingBehov = utledVurderingBehov(pleiepenger, pleiepenger.getFom(), List.of(new PleiepengerInnleggelseEntitet.Builder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(pleiepenger.getFom(), pleiepenger.getTom()))
                .medPleiepengerSaksnummer(new Saksnummer("000"))
                .medPleietrengendeAktørId(AktørId.dummy())
            .build()));
        assertThat(vurderingBehov).isEmpty();
    }

    private Optional<DokumentasjonVurderingBehov.Behov> utledVurderingBehovSammenhengendeUttak(OppgittPeriodeEntitet oppgittPeriode) {
        return UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, oppgittPeriode.getFom().minusMonths(2), true, List.of());
    }

    private Optional<DokumentasjonVurderingBehov.Behov> utledVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, LocalDate familiehendelse,
                                                                            List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        return UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, familiehendelse, false, pleiepengerInnleggelser);
    }

    private OppgittPeriodeEntitet periodeMedUtsettelse(UtsettelseÅrsak utsettelseÅrsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(utsettelseÅrsak)
            .medPeriode(LocalDate.of(2018, 3, 15), LocalDate.of(2018, 3, 15)).build();
    }

}
