package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FaktaUttakSaksbehandlerOverstyringshåndtererTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider behandlingRepositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    private void lagreIAYGrunnlag(Long behandlingId) {
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_slettet_søknadsperiode() {

        // Behandling
        Behandling behandling = opprettRevurderingBehandling(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreIAYGrunnlag(behandling.getId());

        // dto
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = opprettOverstyringUttaksperioderDto();

        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        // Verifiserer HistorikkinnslagDto
        Historikkinnslag historikkinnslag = behandlingRepositoryProvider.getHistorikkRepository().hentHistorikk(behandling.getId()).get(0);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.UTTAK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getResultat()).isEmpty();
        assertThat(del.getAvklartSoeknadsperiode()).as("soeknadsperiode").hasValueSatisfying(soeknadsperiode -> assertThat(soeknadsperiode.getNavn()).as("navn")
            .isEqualTo(HistorikkAvklartSoeknadsperiodeType.SLETTET_SOEKNASPERIODE.getKode()));
    }

    @Test
    public void skal_sette_totrinns_ved_endring_manuell_fakta_uttak() {
        // Behandling
        Behandling behandling = opprettRevurderingBehandling(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreIAYGrunnlag(behandling.getId());

        // dto
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = opprettOverstyringUttaksperioderDto();
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        // assert
        assertThat(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING)).isTrue();
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();

    }

    @Test
    public void skal_aktivere_manuell_avklar_fakta_uttak_hvis_finnes_fakta_uttak_med_avbrutt_status() {

        Behandling behandling = opprettRevurderingBehandlingMedAksjonspunktFaktaUttak();
        lagreIAYGrunnlag(behandling.getId());

        // dto
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = opprettOverstyringUttaksperioderDto();
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        // assert
        assertThat(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING)).isTrue();
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void skal_aktivere_manuell_avklar_fakta_uttak_hvis_det_er_gjenåpnet() {

        Behandling behandling = opprettRevurderingBehandling(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        lagreIAYGrunnlag(behandling.getId());
        aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);

        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = opprettOverstyringUttaksperioderDto();

        assertThatCode(() -> aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId())).doesNotThrowAnyException();

    }

    private OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto opprettOverstyringUttaksperioderDto() {
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = new OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto();
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11));
        SlettetUttakPeriodeDto slettetPeriodeDto = new SlettetUttakPeriodeDto();
        slettetPeriodeDto.setBegrunnelse("ugyldig søknadsperiode");
        slettetPeriodeDto.setUttakPeriodeType(UttakPeriodeType.FORELDREPENGER);
        slettetPeriodeDto.setFom(LocalDate.now().minusDays(10));
        slettetPeriodeDto.setTom(LocalDate.now());
        dto.setSlettedePerioder(List.of(slettetPeriodeDto));
        dto.setBekreftedePerioder(List.of(bekreftetOppgittPeriodeDto));
        return dto;
    }

    private Behandling opprettRevurderingBehandlingMedAksjonspunktFaktaUttak() {
        Behandling revurdering = opprettRevurderingBehandling(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(revurdering.getBehandlingsresultat())
            .medMottattDato(LocalDate.of(2010, 1, 1))
            .medFørsteLovligeUttaksdag(LocalDate.of(2010, 1, 1))
            .build();
        behandlingRepositoryProvider.getUttaksperiodegrenseRepository().lagre(revurdering.getId(), uttaksperiodegrense);

        Aksjonspunkt aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(revurdering, AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        // Der var aksjonspunkt 5070 pga avvik inntektsmelding men fikk nye inntektsmelding med riktig info, løsningen er tilbake hopp
        // og 5070 er avbrutt men ikke opprettet siden fått riktig inntektsmelding
        aksjonspunktRepository.setTilAvbrutt(aksjonspunkt);
        return revurdering;
    }

    private Behandling opprettRevurderingBehandling(BehandlingÅrsakType behandlingÅrsakType) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, false, true);
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER, BehandlingStegType.KONTROLLER_FAKTA_UTTAK);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        final OppgittPeriodeEntitet periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        final OppgittPeriodeEntitet periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(periode_1, periode_2), true));
        Behandling førstegangsbehandling = scenario.lagre(behandlingRepositoryProvider);

        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(førstegangsbehandling.getBehandlingsresultat())
            .medMottattDato(LocalDate.of(2019, 1, 1))
            .medFørsteLovligeUttaksdag(LocalDate.of(2010, 1, 1))
            .build();
        uttaksperiodegrenseRepository.lagre(førstegangsbehandling.getId(), uttaksperiodegrense);

        ScenarioMorSøkerForeldrepenger revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, behandlingÅrsakType)
            .medBehandlingType(BehandlingType.REVURDERING);

        revurderingsscenario.medSøknad().medMottattDato(LocalDate.now());
        var revurdering = revurderingsscenario.lagre(behandlingRepositoryProvider);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(førstegangsbehandling.getId(), revurdering.getId());
        behandlingRepositoryProvider.getYtelsesFordelingRepository().kopierGrunnlagFraEksisterendeBehandling(førstegangsbehandling.getId(), revurdering.getId());
        return revurdering;
    }

    private BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom) {
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        OppgittPeriodeEntitet bekreftetperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(fom)
            .build();
        KontrollerFaktaPeriodeLagreDto bekreftetPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.ubekreftet(bekreftetperiode),
            null)
                .build();
        bekreftetOppgittPeriodeDto.setBekreftetPeriode(bekreftetPeriodeDto);
        bekreftetOppgittPeriodeDto.setOrginalFom(fom);
        bekreftetOppgittPeriodeDto.setOrginalTom(tom);
        return bekreftetOppgittPeriodeDto;
    }

}
