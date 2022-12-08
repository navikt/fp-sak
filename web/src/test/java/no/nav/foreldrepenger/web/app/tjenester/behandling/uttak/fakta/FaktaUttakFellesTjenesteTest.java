package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.ANDRE_NAV_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.TIDLIGERE_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.ANNET;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.UDEFINERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjenesteImpl;

@CdiDbAwareTest
class FaktaUttakFellesTjenesteTest {

    @Inject
    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private BeregningUttakTjeneste beregningUttakTjeneste;

    @Test
    void skal_lagre_perioder() {
        var opprinneligFom = LocalDate.now();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(
                OppgittPeriodeBuilder.ny()
                    .medPeriodeType(MØDREKVOTE)
                    .medPeriodeKilde(TIDLIGERE_VEDTAK)
                    .medPeriode(opprinneligFom, opprinneligFom.plusWeeks(2))
                    .build(),
                OppgittPeriodeBuilder.ny()
                    .medPeriodeType(MØDREKVOTE)
                    .medPeriodeKilde(SØKNAD)
                    .medPeriode(opprinneligFom.plusWeeks(2).plusDays(1), opprinneligFom.plusWeeks(5))
                    .build()), true))
            .medBekreftetHendelse(familiehendelse(opprinneligFom))
            .medAvklarteUttakDatoer(avklarteUttakDatoer(opprinneligFom))
            .lagre(repositoryProvider);
        var mødrekvoteDto = mødrekvote(opprinneligFom, opprinneligFom.plusWeeks(2));
        var utsettelseDto = new FaktaUttakPeriodeDto(mødrekvoteDto.tom().plusDays(1), mødrekvoteDto.tom().plusWeeks(1), null, UtsettelseÅrsak.SYKDOM,
            null, null, null, null, null, false, MorsAktivitet.ARBEID, SØKNAD);
        var oppholdDto = new FaktaUttakPeriodeDto(utsettelseDto.tom().plusDays(1), utsettelseDto.tom().plusWeeks(1), null, null,
            null, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, null, null, null, false, null, SØKNAD);
        var overføringDto = new FaktaUttakPeriodeDto(oppholdDto.tom().plusDays(1), oppholdDto.tom().plusWeeks(1), FEDREKVOTE,
            null, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null, null, null, null, false, null, ANDRE_NAV_VEDTAK);
        var samtidigUttak = new FaktaUttakPeriodeDto(overføringDto.tom().plusDays(1), overføringDto.tom().plusWeeks(1), FELLESPERIODE,
            null, null, null, null, null, SamtidigUttaksprosent.TEN, true, null, SØKNAD);
        var resultat = kjørOppdaterer(behandling, List.of(mødrekvoteDto, utsettelseDto, oppholdDto, overføringDto, samtidigUttak));
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var lagretPerioder = yfa.getGjeldendeFordeling().getPerioder();

        //Endrer endringsdato pga første periode som lagres starter før opprinnelig endringsdato
        assertThat(yfa.getGjeldendeEndringsdato()).isEqualTo(mødrekvoteDto.fom());
        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(5);

        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(mødrekvoteDto.fom());
        assertThat(lagretPerioder.get(0).getTom()).isEqualTo(mødrekvoteDto.tom());
        assertThat(lagretPerioder.get(0).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(0).getArbeidsprosent()).isEqualTo(mødrekvoteDto.arbeidstidsprosent());
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(0).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(0).getPeriodeKilde()).isEqualTo(TIDLIGERE_VEDTAK);
        assertThat(lagretPerioder.get(0).getSamtidigUttaksprosent()).isEqualTo(mødrekvoteDto.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(0).getPeriodeType()).isEqualTo(mødrekvoteDto.uttakPeriodeType());
        assertThat(lagretPerioder.get(0).isFlerbarnsdager()).isEqualTo(mødrekvoteDto.flerbarnsdager());
        assertThat(lagretPerioder.get(0).getÅrsak()).isEqualTo(Årsak.UKJENT);

        assertThat(lagretPerioder.get(1).getFom()).isEqualTo(utsettelseDto.fom());
        assertThat(lagretPerioder.get(1).getTom()).isEqualTo(utsettelseDto.tom());
        assertThat(lagretPerioder.get(1).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(1).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(1).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(1).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(1).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SAKSBEHANDLER);
        assertThat(lagretPerioder.get(1).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(1).getPeriodeType()).isEqualTo(UDEFINERT);
        assertThat(lagretPerioder.get(1).isFlerbarnsdager()).isEqualTo(utsettelseDto.flerbarnsdager());
        assertThat(lagretPerioder.get(1).getÅrsak()).isEqualTo(utsettelseDto.utsettelseÅrsak());

        assertThat(lagretPerioder.get(2).getFom()).isEqualTo(oppholdDto.fom());
        assertThat(lagretPerioder.get(2).getTom()).isEqualTo(oppholdDto.tom());
        assertThat(lagretPerioder.get(2).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(2).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SAKSBEHANDLER);
        assertThat(lagretPerioder.get(2).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeType()).isEqualTo(ANNET);
        assertThat(lagretPerioder.get(2).isFlerbarnsdager()).isEqualTo(oppholdDto.flerbarnsdager());
        assertThat(lagretPerioder.get(2).getÅrsak()).isEqualTo(oppholdDto.oppholdÅrsak());

        assertThat(lagretPerioder.get(3).getFom()).isEqualTo(overføringDto.fom());
        assertThat(lagretPerioder.get(3).getTom()).isEqualTo(overføringDto.tom());
        assertThat(lagretPerioder.get(3).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(3).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(3).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SAKSBEHANDLER);
        assertThat(lagretPerioder.get(3).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(3).getPeriodeType()).isEqualTo(overføringDto.uttakPeriodeType());
        assertThat(lagretPerioder.get(3).isFlerbarnsdager()).isEqualTo(overføringDto.flerbarnsdager());
        assertThat(lagretPerioder.get(3).getÅrsak()).isEqualTo(overføringDto.overføringÅrsak());

        assertThat(lagretPerioder.get(4).getFom()).isEqualTo(samtidigUttak.fom());
        assertThat(lagretPerioder.get(4).getTom()).isEqualTo(samtidigUttak.tom());
        assertThat(lagretPerioder.get(4).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(4).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(4).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(4).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(4).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SAKSBEHANDLER);
        assertThat(lagretPerioder.get(4).getSamtidigUttaksprosent()).isEqualTo(samtidigUttak.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(4).getPeriodeType()).isEqualTo(samtidigUttak.uttakPeriodeType());
        assertThat(lagretPerioder.get(4).isFlerbarnsdager()).isEqualTo(samtidigUttak.flerbarnsdager());
        assertThat(lagretPerioder.get(4).getÅrsak()).isEqualTo(Årsak.UKJENT);
    }

    @Test
    void gir_exception_hvis_det_lagres_uttak_som_starter_før_gyldig_første_uttaksdag() {
        var opprinneligFom = LocalDate.now();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeType(MØDREKVOTE)
                .medPeriodeKilde(SØKNAD)
                .medPeriode(opprinneligFom, opprinneligFom.plusWeeks(5))
                .build()), true))
            .medBekreftetHendelse(familiehendelse(opprinneligFom))
            .lagre(repositoryProvider);
        var mødrekvoteDto = mødrekvote(opprinneligFom.minusWeeks(1), opprinneligFom.plusWeeks(2));
        assertThatThrownBy(() -> kjørOppdaterer(behandling, List.of(mødrekvoteDto))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gir_ikke_exception_hvis_det_lagres_utsettelse_som_starter_før_gyldig_første_uttaksdag() {
        var opprinneligFom = LocalDate.now();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeType(MØDREKVOTE)
                .medPeriodeKilde(SØKNAD)
                .medPeriode(opprinneligFom, opprinneligFom.plusWeeks(5))
                .build()), true))
            .medBekreftetHendelse(familiehendelse(opprinneligFom))
            .lagre(repositoryProvider);
        var mødrekvoteDto = mødrekvote(opprinneligFom, opprinneligFom.plusWeeks(2));
        var utsettelseDto = new FaktaUttakPeriodeDto(opprinneligFom.minusWeeks(2), mødrekvoteDto.fom().minusDays(1),
            null, UtsettelseÅrsak.SYKDOM, null, null, null, null, null, false, null, SØKNAD);
        kjørOppdaterer(behandling, List.of(mødrekvoteDto, utsettelseDto));

        var lagretPerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeFordeling().getPerioder();
        assertThat(lagretPerioder).hasSize(2);
    }

    private static FaktaUttakPeriodeDto mødrekvote(LocalDate fom, LocalDate tom) {
        return new FaktaUttakPeriodeDto(fom, tom, MØDREKVOTE, null, null, null, null,
            null, null, false, null, SØKNAD);
    }

    private static AvklarteUttakDatoerEntitet avklarteUttakDatoer(LocalDate endringsdato) {
        return new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(endringsdato.plusWeeks(2)).build();
    }

    private static FamilieHendelseBuilder familiehendelse(LocalDate fødselsdato) {
        return FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET).medFødselsDato(fødselsdato);
    }

    private OppdateringResultat kjørOppdaterer(Behandling behandling, List<FaktaUttakPeriodeDto> perioder) {
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider,
            new HentOgLagreBeregningsgrunnlagTjeneste(repositoryProvider.getEntityManager()), new AbakusInMemoryInntektArbeidYtelseTjeneste(),
            skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()), true);

        var tjeneste = new FaktaUttakFellesTjeneste(uttakInputTjeneste, faktaUttakAksjonspunktUtleder, ytelseFordelingTjeneste,
            ytelsesFordelingRepository, repositoryProvider.getFpUttakRepository(), new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository())),
            new FaktaUttakHistorikkinnslagTjeneste(new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(), Mockito.mock(DokumentArkivTjeneste.class), repositoryProvider.getBehandlingRepository())),
            repositoryProvider.getBehandlingRepository());
        return tjeneste.oppdater("begrunnelse", perioder, behandling.getId());
    }
}
