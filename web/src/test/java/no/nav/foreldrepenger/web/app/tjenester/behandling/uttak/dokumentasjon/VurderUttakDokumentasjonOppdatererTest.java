package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class VurderUttakDokumentasjonOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private VurderUttakDokumentasjonAksjonspunktUtleder uttakDokumentasjonAksjonspunktUtleder;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private MedlemTjeneste medlemTjeneste;

    @Inject
    private BeregningUttakTjeneste beregningUttakTjeneste;

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
            .build();

        var behandling = behandlingMedAp(List.of(eksisterendeUtsettelse, eksisterendeUttak));

        var dto = new VurderUttakDokumentasjonDto();
        var vurdering1 = new DokumentasjonVurderingBehovDto(eksisterendeUtsettelse.getFom(), eksisterendeUtsettelse.getFom().plusWeeks(1).minusDays(1),
            DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE, DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER,
            DokumentasjonVurderingBehovDto.Vurdering.GODKJENT);
        var vurdering2 = new DokumentasjonVurderingBehovDto(vurdering1.tom().plusDays(1), eksisterendeUtsettelse.getTom(), DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE,
            DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER, DokumentasjonVurderingBehovDto.Vurdering.IKKE_GODKJENT);
        dto.setVurderingBehov(List.of(vurdering1, vurdering2));

        var resultat = kjørOppdatering(behandling, dto);

        var lagretPerioder = hentLagretPerioder(behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(3);
        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(vurdering1.fom());
        assertThat(lagretPerioder.get(0).getTom()).isEqualTo(vurdering1.tom());
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.SYKDOM_SØKER_GODKJENT);

        assertThat(lagretPerioder.get(1).getFom()).isEqualTo(vurdering2.fom());
        assertThat(lagretPerioder.get(1).getTom()).isEqualTo(vurdering2.tom());
        assertThat(lagretPerioder.get(1).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.SYKDOM_SØKER_IKKE_GODKJENT);

        assertThat(lagretPerioder.get(2)).isEqualTo(eksisterendeUttak);
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
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null);
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
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null);
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
            DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, DokumentasjonVurderingBehov.Behov.OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null);
        dto.setVurderingBehov(List.of(vurdering));

        assertThatThrownBy(() -> kjørOppdatering(behandling, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skal_kaste_exception_hvis_ingen_avklaringer() {
        var behandling = behandlingMedAp(List.of());
        var dto = new VurderUttakDokumentasjonDto();
        assertThatThrownBy(() -> kjørOppdatering(behandling, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<OppgittPeriodeEntitet> hentLagretPerioder(Behandling behandling) {
        return repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getGjeldendeSøknadsperioder()
            .getOppgittePerioder();
    }

    private Behandling behandlingMedAp(List<OppgittPeriodeEntitet> fordeling) {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON, BehandlingStegType.FAKTA_UTTAK)
            .medBekreftetHendelse(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET).medFødselsDato(LocalDate.now()))
            .medFordeling(new OppgittFordelingEntitet(fordeling, true))
            .lagre(repositoryProvider);
    }

    private OppdateringResultat kjørOppdatering(Behandling behandling, VurderUttakDokumentasjonDto dto) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var oppdaterer = new VurderUttakDokumentasjonOppdaterer(new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()),
            uttakDokumentasjonAksjonspunktUtleder,
            new UttakInputTjeneste(repositoryProvider, new HentOgLagreBeregningsgrunnlagTjeneste(repositoryProvider.getEntityManager()),
                new AbakusInMemoryInntektArbeidYtelseTjeneste(), skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste,
                new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()), true));
        return oppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON), stp,
                "begrunnelse"));
    }
}
