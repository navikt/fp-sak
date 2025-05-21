package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_IKKE_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class VurderUttakDokumentasjonOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    @Any
    private VurderUttakDokumentasjonOppdaterer oppdaterer;

    @Test
    void skal_oppdatere_vurderinger_i_yf() {
        var eksisterendeUtsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2).minusDays(1))
            .build();

        var eksisterendeUttak = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4).minusDays(1))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var behandling = behandlingMedAp(List.of(eksisterendeUtsettelse, eksisterendeUttak));


        var vurdering1 = new DokumentasjonVurderingBehovDto(eksisterendeUtsettelse.getFom(),
            eksisterendeUtsettelse.getFom().plusWeeks(1).minusDays(1), DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE,
            DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER, DokumentasjonVurderingBehovDto.Vurdering.GODKJENT, null, Set.of());
        var vurdering2 = new DokumentasjonVurderingBehovDto(vurdering1.tom().plusDays(1), eksisterendeUtsettelse.getTom(),
            DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE, DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER,
            DokumentasjonVurderingBehovDto.Vurdering.IKKE_GODKJENT, null, Set.of());
        var dto = new VurderUttakDokumentasjonDto("begrunnelse", List.of(vurdering1, vurdering2));

        var resultat = kjørOppdatering(behandling, dto);

        var lagretPerioder = hentLagretPerioder(behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(3);
        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(vurdering1.fom());
        assertThat(lagretPerioder.get(0).getTom()).isEqualTo(vurdering1.tom());
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isEqualTo(new DokumentasjonVurdering(SYKDOM_SØKER_GODKJENT));

        assertThat(lagretPerioder.get(1).getFom()).isEqualTo(vurdering2.fom());
        assertThat(lagretPerioder.get(1).getTom()).isEqualTo(vurdering2.tom());
        assertThat(lagretPerioder.get(1).getDokumentasjonVurdering()).isEqualTo(new DokumentasjonVurdering(SYKDOM_SØKER_IKKE_GODKJENT));

        assertThat(lagretPerioder.get(2)).isEqualTo(eksisterendeUttak);

        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON);
        assertThat(historikk.getFirst().getLinjer()).hasSize(3);
        assertThat(historikk.getFirst().getLinjer().get(0).getTekst()).contains("Avklart dokumentasjon for periode", SYKDOM_SØKER_GODKJENT.getNavn());
        assertThat(historikk.getFirst().getLinjer().get(1).getTekst()).contains("Avklart dokumentasjon for periode", SYKDOM_SØKER_IKKE_GODKJENT.getNavn());
        assertThat(historikk.getFirst().getLinjer().get(2).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_holde_ap_åpent_hvis_mangler_vurdering() {
        var eksisterendeØverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2).minusDays(1))
            .build();

        var behandling = behandlingMedAp(List.of(eksisterendeØverføring));

        var dto = new VurderUttakDokumentasjonDto();
        var vurdering = new DokumentasjonVurderingBehovDto(eksisterendeØverføring.getFom(), eksisterendeØverføring.getTom(),
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER, null, null, Set.of());
        dto.setVurderingBehov(List.of(vurdering));

        var resultat = kjørOppdatering(behandling, dto);

        var lagretPerioder = hentLagretPerioder(behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isFalse();
        assertThat(lagretPerioder).hasSize(1);
        assertThat(lagretPerioder.get(0)).isEqualTo(eksisterendeØverføring);
    }

    @Test
    void skal_kaste_exception_hvis_avklaringer_før_eksisterende_periode() {
        var eksisterendeØverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2).minusDays(1))
            .build();

        var behandling = behandlingMedAp(List.of(eksisterendeØverføring));

        var dto = new VurderUttakDokumentasjonDto();
        var vurdering = new DokumentasjonVurderingBehovDto(eksisterendeØverføring.getFom().minusWeeks(1), eksisterendeØverføring.getTom(),
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER, null, null, Set.of());
        dto.setVurderingBehov(List.of(vurdering));

        assertThatThrownBy(() -> kjørOppdatering(behandling, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skal_kaste_exception_hvis_avklaringer_etter_eksisterende_periode() {
        var eksisterendeØverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2).minusDays(1))
            .build();

        var behandling = behandlingMedAp(List.of(eksisterendeØverføring));

        var dto = new VurderUttakDokumentasjonDto();
        var vurdering = new DokumentasjonVurderingBehovDto(eksisterendeØverføring.getFom(), eksisterendeØverføring.getTom().plusWeeks(1),
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER, null, null, Set.of());
        dto.setVurderingBehov(List.of(vurdering));

        assertThatThrownBy(() -> kjørOppdatering(behandling, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skal_oppdatere_vurderinger_i_yf_med_mors_stillingsprosent() {
        var eksisterendeUttak = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();

        var vurdering1 = new DokumentasjonVurderingBehovDto(eksisterendeUttak.getFom(), eksisterendeUttak.getTom(),
            DokumentasjonVurderingBehov.Behov.Type.UTTAK, DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID,
            DokumentasjonVurderingBehovDto.Vurdering.GODKJENT, new MorsStillingsprosent(BigDecimal.valueOf(74.99)), Set.of());
        var dto = new VurderUttakDokumentasjonDto("begrunnelse", List.of(vurdering1));

        var behandling = behandlingMedAp(List.of(eksisterendeUttak));


        var resultat = kjørOppdatering(behandling, dto);

        var lagretPerioder = hentLagretPerioder(behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(1);
        assertThat(lagretPerioder.getFirst().getFom()).isEqualTo(vurdering1.fom());
        assertThat(lagretPerioder.getFirst().getTom()).isEqualTo(vurdering1.tom());
        assertThat(lagretPerioder.getFirst().getDokumentasjonVurdering()).isEqualTo(new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT, vurdering1.morsStillingsprosent()));

        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON);
        assertThat(historikk.getFirst().getLinjer()).hasSize(2);
        assertThat(historikk.getFirst().getLinjer().get(0).getTekst()).contains("Avklart dokumentasjon for periode", MORS_AKTIVITET_GODKJENT.getNavn(), vurdering1.morsStillingsprosent().toString());
        assertThat(historikk.getFirst().getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_kaste_exception_hvis_ingen_avklaringer() {
        var behandling = behandlingMedAp(List.of());
        var dto = new VurderUttakDokumentasjonDto();
        assertThatThrownBy(() -> kjørOppdatering(behandling, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<OppgittPeriodeEntitet> hentLagretPerioder(Behandling behandling) {
        return repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getGjeldendeFordeling().getPerioder();
    }

    private Behandling behandlingMedAp(List<OppgittPeriodeEntitet> fordeling) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON, BehandlingStegType.FAKTA_UTTAK)
            .medFordeling(new OppgittFordelingEntitet(fordeling, true));
        scenario.medBekreftetHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        return scenario.lagre(repositoryProvider);
    }

    private OppdateringResultat kjørOppdatering(Behandling behandling, VurderUttakDokumentasjonDto dto) {
        return oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON)));
    }
}
