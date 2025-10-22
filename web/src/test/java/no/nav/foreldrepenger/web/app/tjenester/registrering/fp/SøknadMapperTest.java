package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.ARBEID_OG_UTDANNING;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.IKKE_OPPGITT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.INNLAGT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.UFØRE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettAnnenForelderDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettEgenVirksomhetDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettGraderingDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettPermisjonPeriodeDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettTidsromPermisjonDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettUtsettelseDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.AnnenPartOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadWrapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class SøknadMapperTest {

    private static final Saksnummer SAKSNUMMER = new Saksnummer("123456789");
    private static final AktørId STD_KVINNE_AKTØR_ID = AktørId.dummy();
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;

    private SøknadMapper ytelseSøknadMapper;
    private SøknadDataFraTidligereVedtakTjeneste oppgittPeriodeMottattDato;
    private PersoninfoKjønn kvinne;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;

    @BeforeEach
    void before(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        behandlingRevurderingTjeneste = new BehandlingRevurderingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        oppgittPeriodeMottattDato = new SøknadDataFraTidligereVedtakTjeneste(
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()), new FpUttakRepository(entityManager),
            repositoryProvider.getBehandlingRepository());

        kvinne = new PersoninfoKjønn(STD_KVINNE_AKTØR_ID, NavBrukerKjønn.KVINNE);
        ytelseSøknadMapper = new YtelseSøknadMapper(personinfoAdapter, virksomhetTjeneste);
    }

    @Test
    void test_mapForeldrepenger() {
        var dto = new ManuellRegistreringForeldrepengerDto();
        dto.setAnnenForelderInformert(false);
        oppdaterDtoForFødsel(dto, true, LocalDate.now().minusWeeks(3), 1);
        assertThatCode(() -> ytelseSøknadMapper.mapSøknad(dto, opprettBruker())).doesNotThrowAnyException();
    }

    @Test
    void testMedDekningsgrad() {
        var dto = new ManuellRegistreringForeldrepengerDto();
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);

        var dekningsgrad = YtelseSøknadMapper.mapDekningsgrad(dto);
        assertThat(dekningsgrad).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad()).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad().getKode()).isEqualTo(DekningsgradDto.HUNDRE.getValue());
    }

    @Test
    void testUtenDekningsgrad() {
        assertThat(YtelseSøknadMapper.mapDekningsgrad(new ManuellRegistreringForeldrepengerDto())).isNull();
    }

    @Test
    void test_mapOverfoeringsperiode() {
        var overføringsperiode = new OverføringsperiodeDto();
        overføringsperiode.setOverforingArsak(INSTITUSJONSOPPHOLD_ANNEN_FORELDER);

        var overfoeringsperiode = YtelseSøknadMapper.mapOverføringsperiode(overføringsperiode, ForeldreType.FAR);
        assertThat(overfoeringsperiode).isNotNull();
        assertThat(overfoeringsperiode.getAarsak().getKode()).isEqualTo(INSTITUSJONSOPPHOLD_ANNEN_FORELDER.getKode());
    }

    @Test
    void test_mapUtsettelsesperiode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var utsettelserDto = opprettUtsettelseDto(fraDato, tomDato, FELLESPERIODE);
        var utsettelsesperiode = YtelseSøknadMapper.mapUtsettelsesperiode(utsettelserDto);
        assertThat(utsettelsesperiode).isNotNull();
        assertThat(utsettelsesperiode.getAarsak().getKode()).isEqualTo(UtsettelseÅrsak.FERIE.getKode());
        assertThat(utsettelsesperiode.getFom()).isEqualTo(fraDato);
        assertThat(utsettelsesperiode.getTom()).isEqualTo(tomDato);
        assertThat(utsettelsesperiode.getUtsettelseAv().getKode()).isEqualTo(FELLESPERIODE.getKode());
    }

    @Test
    void test_mapFedrekvotePeriodeDto() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var fedrekvotePeriodeDto = opprettPermisjonPeriodeDto(fraDato, tomDato, FEDREKVOTE, null);
        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(fedrekvotePeriodeDto);
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(FEDREKVOTE.getKode());
    }

    @Test
    void test_mapMødrekvotePeriodeDto() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(opprettPermisjonPeriodeDto(fraDato, tomDato, MØDREKVOTE, null));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(MØDREKVOTE.getKode());
    }

    @Test
    void test_mapForeldrepengerFørFødselPeriode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(opprettPermisjonPeriodeDto(fraDato, tomDato, FORELDREPENGER_FØR_FØDSEL, null));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(FORELDREPENGER_FØR_FØDSEL.getKode());
    }

    @Test
    void test_mapGraderingsperiode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15);
        var orgNr = KUNSTIG_ORG + "1";
        var gradering = (Gradering) YtelseSøknadMapper.mapGraderingsperiode(
            opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, orgNr));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(FEDREKVOTE.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isTrue();
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    void test_mapGraderingsperiode_arbeidsprosent_desimal() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15.55);
        var orgNr = KUNSTIG_ORG + "1";
        var gradering = (Gradering) YtelseSøknadMapper.mapGraderingsperiode(
            opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, orgNr));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(FEDREKVOTE.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isTrue();
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    void test_mapFellesPeriodeDto() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);

        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(opprettPermisjonPeriodeDto(fraDato, tomDato, FELLESPERIODE, ARBEID));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(FELLESPERIODE.getKode());
        assertThat(uttaksperiode.getMorsAktivitetIPerioden().getKode()).isEqualTo(ARBEID.getKode());
    }

    @Test
    void test_mapUttaksperioder_gradering() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15);
        var uttaksperiodes = YtelseSøknadMapper.mapGraderingsperioder(
            List.of(opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, null)));
        assertThat(uttaksperiodes).isNotNull().hasSize(1);
    }

    @Test
    void test_mapUttaksperioder_gradering_med_samtidig_uttak() {
        var graderingDto = opprettGraderingDto(LocalDate.now(), LocalDate.now().plusDays(3), BigDecimal.valueOf(15), FEDREKVOTE, true, false, false,
            null);
        graderingDto.setHarSamtidigUttak(true);
        graderingDto.setSamtidigUttaksprosent(BigDecimal.TEN);

        var uttakperioder = YtelseSøknadMapper.mapGraderingsperioder(List.of(graderingDto));
        assertThat(uttakperioder).hasSize(1);
        assertThat(uttakperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(graderingDto.getHarSamtidigUttak());
        assertThat(uttakperioder.get(0).getSamtidigUttakProsent()).isEqualTo(graderingDto.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    void test_mapUttaksperioder_samtidig_uttak() {
        var periode = opprettPermisjonPeriodeDto(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 1), FELLESPERIODE, ARBEID_OG_UTDANNING);
        periode.setHarSamtidigUttak(true);
        periode.setSamtidigUttaksprosent(BigDecimal.TEN);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(List.of(periode));
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(periode.getHarSamtidigUttak());
        assertThat(uttaksperioder.get(0).getSamtidigUttakProsent()).isEqualTo(periode.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    void test_mapUttaksperioder_opphold() {
        var oppholdDto = new OppholdDto();
        oppholdDto.setPeriodeFom(LocalDate.now().minusDays(1));
        oppholdDto.setPeriodeTom(LocalDate.now());
        oppholdDto.setÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER);

        var oppholdsperiode = YtelseSøknadMapper.mapOppholdsperioder(List.of(oppholdDto));
        assertThat(oppholdsperiode).hasSize(1);
        assertThat(oppholdsperiode.get(0).getAarsak().getKode()).isEqualTo(oppholdDto.getÅrsak().getKode());
        assertThat(oppholdsperiode.get(0).getAarsak().getKodeverk()).isEqualTo(oppholdDto.getÅrsak().getKodeverk());
        assertThat(oppholdsperiode.get(0).getFom()).isEqualTo(oppholdDto.getPeriodeFom());
        assertThat(oppholdsperiode.get(0).getTom()).isEqualTo(oppholdDto.getPeriodeTom());
    }

    @Test
    void test_mapUttaksperioder_permisjonperioder() {
        // Test av permisjonsperiodene:
        // dato før fødsel -> fødselsdato -> mødrekvote
        var perminsjonstartFørFødsel = LocalDate.now().minusWeeks(3);
        var fødselsdato = LocalDate.now();
        var mødrekvoteSlutt = fødselsdato.plusDays(1).plusWeeks(3);

        var permisjonPeriodeFørFødselDto = opprettPermisjonPeriodeDto(perminsjonstartFørFødsel, fødselsdato, FORELDREPENGER_FØR_FØDSEL, null);
        var permisjonPeriodeMødrekvote = opprettPermisjonPeriodeDto(LocalDate.now().plusDays(1), mødrekvoteSlutt, MØDREKVOTE, null);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(List.of(permisjonPeriodeFørFødselDto, permisjonPeriodeMødrekvote));
        assertThat(uttaksperioder).isNotNull()
            .hasSize(2)
            .anyMatch(uttaksperiode -> FORELDREPENGER_FØR_FØDSEL.getKode().equals(uttaksperiode.getType().getKode()))
            .anyMatch(uttaksperiode -> MØDREKVOTE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    void test_mapUttaksperioder_fellesperioder() {
        var mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        var fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        var fellesPeriodeDto = opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, FELLESPERIODE, ARBEID_OG_UTDANNING);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(List.of(fellesPeriodeDto));
        assertThat(uttaksperioder).isNotNull()
            .hasSize(1)
            .first()
            .matches(uttaksperiode -> ARBEID_OG_UTDANNING.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()))
            .matches(uttaksperiode -> FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    void test_map_mors_aktivitet_uføretrygd() {
        var mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        var fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(
            List.of(opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, FELLESPERIODE, UFØRE)));
        assertThat(uttaksperioder).isNotNull()
            .hasSize(1)
            .first()
            .matches(uttaksperiode -> UFØRE.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()))
            .matches(uttaksperiode -> FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    void test_map_mors_aktivitet_ikke_oppgitt() {
        var mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        var fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(
            List.of(opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, FORELDREPENGER, IKKE_OPPGITT)));
        assertThat(uttaksperioder).isNotNull()
            .hasSize(1)
            .first()
            .matches(uttaksperiode -> IKKE_OPPGITT.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()))
            .matches(uttaksperiode -> FORELDREPENGER.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    void test_mapFordeling_morSøkerMenfarRettPåFedrekvote() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), FORELDREPENGER_FØR_FØDSEL, null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(List.of(permisjonPeriodeDto)));
        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);
        var fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
        // Forventer å ha mødrekvote periode basert på forventet permisjon før fødsel
        assertThat(fordeling.getPerioder()).hasSize(1);
    }

    @Test
    void test_skal_ta_inn_papirsøknadDTO_mappe_til_xml_så_mappe_til_domenemodell() {
        var navBruker = opprettBruker();
        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2).minusDays(1), MØDREKVOTE,
            null);
        var permisjonPeriodeDto2 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(2), LocalDate.now().minusWeeks(1).minusDays(1),
            FELLESPERIODE, ARBEID);
        var permisjonPeriodeDto3 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(1), LocalDate.now(), FORELDREPENGER, INNLAGT);
        var permisjonPerioder = List.of(permisjonPeriodeDto, permisjonPeriodeDto2, permisjonPeriodeDto3);

        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);

        // Annen forelder er informert
        dto.setAnnenForelderInformert(true);

        var annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(true);
        dto.setAnnenForelder(annenForelderDto);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(), any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(STD_KVINNE_AKTØR_ID));
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), BehandlingEventPubliserer.NULL_EVENT_PUB);
        var oversetter = new SøknadOversetter(personopplysningTjeneste, repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste,
            grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter, oppgittPeriodeMottattDato,
            new AnnenPartOversetter(personinfoAdapter));
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, SAKSNUMMER);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        oversetter.trekkUtDataOgPersister((SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad),
            new MottattDokument.Builder().medMottattDato(LocalDate.now())
                .medFagsakId(behandling.getFagsakId())
                .medElektroniskRegistrert(true)
                .build(), behandling, Optional.empty());

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getPerioder()).isNotEmpty();
    }

    @Test
    void test_verdikjeden_fra_papirsøknad_til_domenemodell_mor_søker_med_mødrekvote_periode_før_fødsel_utsettelse() {
        var navBruker = opprettBruker();
        var fødselsdato = LocalDate.now().minusDays(10);
        var mødrekvoteSlutt = fødselsdato.plusWeeks(10);
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, fødselsdato, 1);
        // Perioder: permisjon før fødsel og mødrekvote
        List<PermisjonPeriodeDto> permisjonsperioder = new ArrayList<>();
        permisjonsperioder.add(opprettPermisjonPeriodeDto(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), FORELDREPENGER_FØR_FØDSEL, null));
        permisjonsperioder.add(opprettPermisjonPeriodeDto(fødselsdato, mødrekvoteSlutt.minusDays(1), MØDREKVOTE, null));
        var tidsromPermisjonDto = opprettTidsromPermisjonDto(permisjonsperioder);

        var utsettelserDto = opprettUtsettelseDto(mødrekvoteSlutt, mødrekvoteSlutt.plusWeeks(1), MØDREKVOTE);
        tidsromPermisjonDto.setUtsettelsePeriode(List.of(utsettelserDto));

        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(tidsromPermisjonDto);

        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.AATI);

        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);

        var annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(false);
        annenForelderDto.setSokerHarAleneomsorg(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenForelderDto);
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(STD_KVINNE_AKTØR_ID));

        var soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), BehandlingEventPubliserer.NULL_EVENT_PUB);
        var oversetter = new SøknadOversetter(personopplysningTjeneste, repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste,
            grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter, oppgittPeriodeMottattDato,
            new AnnenPartOversetter(personinfoAdapter));
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, SAKSNUMMER);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(), any(AktørId.class))).thenReturn(Optional.of(kvinne));
        oversetter.trekkUtDataOgPersister((SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad),
            new MottattDokument.Builder().medMottattDato(LocalDate.now())
                .medFagsakId(behandling.getFagsakId())
                .medElektroniskRegistrert(true)
                .build(), behandling, Optional.empty());

        var ytelseFordeling = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(ytelseFordeling.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordeling.getOppgittRettighet().getHarAnnenForeldreRett()).isFalse();
        assertThat(ytelseFordeling.getOppgittRettighet().getHarAleneomsorgForBarnet()).isTrue();

        assertThat(ytelseFordeling.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordeling.getOppgittFordeling().getPerioder()).isNotEmpty();
        // Foreldrepenger før fødsel, mødrekvote og utsettelse
        assertThat(ytelseFordeling.getOppgittFordeling().getPerioder()).hasSize(3);
        assertThat(ytelseFordeling.getOppgittFordeling().getPerioder()).anySatisfy(
            periode -> assertThat(periode.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL));
        assertThat(ytelseFordeling.getOppgittFordeling().getPerioder()).anySatisfy(
            periode -> assertThat(periode.getPeriodeType()).isEqualTo(MØDREKVOTE));
        assertThat(ytelseFordeling.getOppgittFordeling()
            .getPerioder()
            .stream()
            .map(OppgittPeriodeEntitet::getÅrsak)
            .filter(u -> u instanceof UtsettelseÅrsak)
            .toList()).anySatisfy(årsak -> assertThat((UtsettelseÅrsak) årsak).isEqualTo(UtsettelseÅrsak.FERIE));
    }

    @Test
    void test_mapFordeling_morAleneomsorg() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now(), LocalDate.now().plusWeeks(3), MØDREKVOTE, null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(List.of(permisjonPeriodeDto)));
        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);

        var fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
    }

    @Test
    void test_mapRettigheter_farRettPåFedrekvote() {
        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        dto.setAnnenForelder(opprettAnnenForelderDto(true, true, true));
        var rettigheter = YtelseSøknadMapper.mapRettigheter(dto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isTrue();
        assertThat(rettigheter.isHarAleneomsorgForBarnet()).isTrue();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    void test_mapRettigheter_morAleneomsorg() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        manuellRegistreringForeldrepengerDto.setAnnenForelder(opprettAnnenForelderDto(true, true, false));
        var rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isFalse();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    void test_mapRettigheter_bareFarRett_morUfør() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        var annenforelder = opprettAnnenForelderDto(false, false, false);
        annenforelder.setMorMottarUføretrygd(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenforelder);
        var rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isFalse();
        assertThat(rettigheter.isHarMorUforetrygd()).isTrue();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    void test_mapRettigheter_bareFarRett_morEØS() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        var annenforelder = opprettAnnenForelderDto(false, false, false);
        annenforelder.setMorMottarUføretrygd(false);
        annenforelder.setAnnenForelderRettEØS(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenforelder);
        var rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isFalse();
        assertThat(rettigheter.isHarMorUforetrygd()).isFalse();
        assertThat(rettigheter.isHarAnnenForelderTilsvarendeRettEOS()).isTrue();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    void skal_mappe_og_lagre_oppgitt_opptjening_når_det_allerede_finnes_i_grunnlaget() {
        var iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(), any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(iayGrunnlag.getGjeldendeOppgittOpptjening()).thenReturn(Optional.of(mock(OppgittOpptjening.class)));
        when(iayGrunnlag.getOverstyrtOppgittOpptjening()).thenReturn(Optional.empty());
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var navBruker = opprettBruker();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, SAKSNUMMER);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPerioder = List.of(opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE, null));
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);
        dto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        dto.setAnnenForelderInformert(false);
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);

        var mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId())
            .medElektroniskRegistrert(true);
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), BehandlingEventPubliserer.NULL_EVENT_PUB);
        var oversetter = new SøknadOversetter(personopplysningTjeneste, repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste,
            grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter, oppgittPeriodeMottattDato,
            new AnnenPartOversetter(personinfoAdapter));

        oversetter.trekkUtDataOgPersister((SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad), mottattDokument.build(), behandling,
            Optional.empty());

        verify(iayTjeneste, times(1)).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }

    @Test
    void skal_mappe_og_lagre_oppgitt_opptjening_når_det_allerede_finnes_overstyrt_i_grunnlaget() {
        var iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(), any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(iayGrunnlag.getGjeldendeOppgittOpptjening()).thenReturn(Optional.of(mock(OppgittOpptjening.class)));
        when(iayGrunnlag.getOverstyrtOppgittOpptjening()).thenReturn(Optional.of(mock(OppgittOpptjening.class)));
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var navBruker = opprettBruker();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, SAKSNUMMER);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPerioder = List.of(opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE, null));
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);
        dto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        dto.setAnnenForelderInformert(false);
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);

        var mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId())
            .medElektroniskRegistrert(true);
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), BehandlingEventPubliserer.NULL_EVENT_PUB);
        var oversetter = new SøknadOversetter(personopplysningTjeneste, repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste,
            grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter, oppgittPeriodeMottattDato,
            new AnnenPartOversetter(personinfoAdapter));

        oversetter.trekkUtDataOgPersister((SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad), mottattDokument.build(), behandling,
            Optional.empty());

        verify(iayTjeneste, times(1)).lagreOppgittOpptjeningNullstillOverstyring(anyLong(), any(OppgittOpptjeningBuilder.class));
    }

    @Test
    void skal_mappe_og_lagre_oppgitt_opptjening_når_det_ikke_finnes_i_grunnlaget() {
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(), any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var navBruker = opprettBruker();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, SAKSNUMMER);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPerioder = List.of(opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE, null));
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);
        dto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        dto.setAnnenForelderInformert(true);
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);

        var mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId())
            .medElektroniskRegistrert(true);
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), BehandlingEventPubliserer.NULL_EVENT_PUB);
        var oversetter = new SøknadOversetter(personopplysningTjeneste, repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste,
            grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter, oppgittPeriodeMottattDato,
            new AnnenPartOversetter(personinfoAdapter));

        oversetter.trekkUtDataOgPersister((SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad), mottattDokument.build(), behandling,
            Optional.empty());
        verify(iayTjeneste).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }
}
