package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@CdiDbAwareTest
class FaktaUttakOppdatererTest {

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
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO, BehandlingStegType.FAKTA_UTTAK)
            .medBekreftetHendelse(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET).medFødselsDato(opprinneligFom))
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(opprinneligFom).build())
            .lagre(repositoryProvider);
        var dto = new FaktaUttakDto.ManueltSattStartdatoDto();
        var mødrekvoteDto = new FaktaUttakPeriodeDto(opprinneligFom.minusWeeks(1), opprinneligFom.plusWeeks(2).minusDays(1), UttakPeriodeType.MØDREKVOTE, null, null, null,
            BigDecimal.TEN, new ArbeidsforholdDto("000000000", UttakArbeidType.ORDINÆRT_ARBEID), null, false, null, FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
        var utsettelseDto = new FaktaUttakPeriodeDto(mødrekvoteDto.tom().plusDays(1), mødrekvoteDto.tom().plusWeeks(1), null, UtsettelseÅrsak.SYKDOM,
            null, null, null, null, null, false, MorsAktivitet.ARBEID, FordelingPeriodeKilde.SØKNAD);
        var oppholdDto = new FaktaUttakPeriodeDto(utsettelseDto.tom().plusDays(1), utsettelseDto.tom().plusWeeks(1), null, null,
            null, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, null, null, null, false, null, FordelingPeriodeKilde.SØKNAD);
        var overføringDto = new FaktaUttakPeriodeDto(oppholdDto.tom().plusDays(1), oppholdDto.tom().plusWeeks(1), UttakPeriodeType.FEDREKVOTE,
            null, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null, null, null, null, false, null, FordelingPeriodeKilde.ANDRE_NAV_VEDTAK);
        var samtidigUttak = new FaktaUttakPeriodeDto(overføringDto.tom().plusDays(1), overføringDto.tom().plusWeeks(1), UttakPeriodeType.FELLESPERIODE,
            null, null, null, null, null, SamtidigUttaksprosent.TEN, true, null, FordelingPeriodeKilde.SØKNAD);
        dto.setPerioder(List.of(mødrekvoteDto, utsettelseDto, oppholdDto, overføringDto, samtidigUttak));
        var resultat = kjørOppdaterer(behandling, dto);
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var lagretPerioder = yfa.getGjeldendeFordeling().getPerioder();

        //Endrer endringsdato pga første periode som lagres starter før opprinnelig endringsdato
        assertThat(yfa.getGjeldendeEndringsdato()).isEqualTo(mødrekvoteDto.fom());
        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(5);

        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(mødrekvoteDto.fom());
        assertThat(lagretPerioder.get(0).getTom()).isEqualTo(mødrekvoteDto.tom());
        assertThat(lagretPerioder.get(0).getGraderingAktivitetType()).isEqualTo(GraderingAktivitetType.ARBEID);
        assertThat(lagretPerioder.get(0).getArbeidsprosent()).isEqualTo(mødrekvoteDto.arbeidstidsprosent());
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(mødrekvoteDto.arbeidsforhold().arbeidsgiverReferanse());
        assertThat(lagretPerioder.get(0).getPeriodeKilde()).isEqualTo(mødrekvoteDto.periodeKilde());
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
        assertThat(lagretPerioder.get(1).getPeriodeKilde()).isEqualTo(utsettelseDto.periodeKilde());
        assertThat(lagretPerioder.get(1).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.UDEFINERT);
        assertThat(lagretPerioder.get(1).isFlerbarnsdager()).isEqualTo(utsettelseDto.flerbarnsdager());
        assertThat(lagretPerioder.get(1).getÅrsak()).isEqualTo(utsettelseDto.utsettelseÅrsak());

        assertThat(lagretPerioder.get(2).getFom()).isEqualTo(oppholdDto.fom());
        assertThat(lagretPerioder.get(2).getTom()).isEqualTo(oppholdDto.tom());
        assertThat(lagretPerioder.get(2).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(2).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeKilde()).isEqualTo(oppholdDto.periodeKilde());
        assertThat(lagretPerioder.get(2).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeType()).isEqualTo(UttakPeriodeType.UDEFINERT);
        assertThat(lagretPerioder.get(2).isFlerbarnsdager()).isEqualTo(oppholdDto.flerbarnsdager());
        assertThat(lagretPerioder.get(2).getÅrsak()).isEqualTo(oppholdDto.oppholdÅrsak());

        assertThat(lagretPerioder.get(3).getFom()).isEqualTo(overføringDto.fom());
        assertThat(lagretPerioder.get(3).getTom()).isEqualTo(overføringDto.tom());
        assertThat(lagretPerioder.get(3).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(3).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(3).getPeriodeKilde()).isEqualTo(overføringDto.periodeKilde());
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
        assertThat(lagretPerioder.get(4).getPeriodeKilde()).isEqualTo(samtidigUttak.periodeKilde());
        assertThat(lagretPerioder.get(4).getSamtidigUttaksprosent()).isEqualTo(samtidigUttak.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(4).getPeriodeType()).isEqualTo(samtidigUttak.uttakPeriodeType());
        assertThat(lagretPerioder.get(4).isFlerbarnsdager()).isEqualTo(samtidigUttak.flerbarnsdager());
        assertThat(lagretPerioder.get(4).getÅrsak()).isEqualTo(Årsak.UKJENT);
    }

    private OppdateringResultat kjørOppdaterer(Behandling behandling, FaktaUttakDto dto) {
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider,
            new HentOgLagreBeregningsgrunnlagTjeneste(repositoryProvider.getEntityManager()), new AbakusInMemoryInntektArbeidYtelseTjeneste(),
            skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()), true);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var faktaUttakOppdaterer = new FaktaUttakOppdaterer(uttakInputTjeneste, faktaUttakAksjonspunktUtleder, ytelseFordelingTjeneste,
            ytelsesFordelingRepository);
        return faktaUttakOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO),
            stp, "begrunnelse"));
    }

}
