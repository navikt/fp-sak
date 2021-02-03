package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.ARBEID_OG_UTDANNING;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.OppgittPeriodeMottattDatoTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentOversetterSøknad;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Rettigheter;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class SøknadMapperTest {

    private static final AktørId STD_KVINNE_AKTØR_ID = AktørId.dummy();
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private DatavarehusTjeneste datavarehusTjeneste;

    private SøknadMapper ytelseSøknadMapper;
    private OppgittPeriodeMottattDatoTjeneste oppgittPeriodeMottattDato;
    private PersoninfoKjønn kvinne;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void before(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        oppgittPeriodeMottattDato = new OppgittPeriodeMottattDatoTjeneste(
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));

        kvinne = new PersoninfoKjønn.Builder().medAktørId(STD_KVINNE_AKTØR_ID)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .build();
        ytelseSøknadMapper = new YtelseSøknadMapper(personinfoAdapter, virksomhetTjeneste);
    }

    @Test
    public void test_mapForeldrepenger() {
        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now().minusWeeks(3), 1);
        ytelseSøknadMapper.mapSøknad(dto, opprettBruker());
    }

    @Test
    public void test_mapEndringssøknad_utenFordeling() {
        var dto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        var fordeling = YtelseSøknadMapper.mapFordelingEndringssøknad(dto);
        assertThat(fordeling).isNotNull();
        assertThat(fordeling.getPerioder()).isEmpty();
    }

    @Test
    public void test_mapEndringssøknad_medFordeling() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15);
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now(), LocalDate.now().plusWeeks(3), MØDREKVOTE,
            null);
        var tidsromPermisjonDto = opprettTidsromPermisjonDto(List.of(permisjonPeriodeDto));
        var dto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);

        // Oppretter utsettelseperiode, mapOverfoeringsperiode, mapGraderingsperioder
        var graderingDto = opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, false, true, false, null);
        tidsromPermisjonDto.setGraderingPeriode(Collections.singletonList(graderingDto));

        var utsettelserDto = opprettUtsettelseDto(fraDato, tomDato, FEDREKVOTE, false);
        tidsromPermisjonDto.setUtsettelsePeriode(Collections.singletonList(utsettelserDto));

        var overføringsperiodeDto = new OverføringsperiodeDto();
        overføringsperiodeDto.setOverforingArsak(INSTITUSJONSOPPHOLD_ANNEN_FORELDER);
        overføringsperiodeDto.setPeriodeFom(fraDato);
        overføringsperiodeDto.setPeriodeTom(tomDato);
        tidsromPermisjonDto.setOverforingsperioder(List.of(overføringsperiodeDto));

        dto.setTidsromPermisjon(tidsromPermisjonDto);
        Fordeling fordeling = YtelseSøknadMapper.mapFordelingEndringssøknad(dto);
        assertThat(fordeling).isNotNull();
        // Forventer å ha en periode for hver av: permisjonPeriode, utsettelseperiode,
        // Overfoeringsperiode og Graderingsperiode.
        assertThat(fordeling.getPerioder()).hasSize(4);
    }

    @Test
    public void testMedDekningsgrad() {
        var dto = new ManuellRegistreringForeldrepengerDto();
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);

        var dekningsgrad = YtelseSøknadMapper.mapDekningsgrad(dto);
        assertThat(dekningsgrad).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad()).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad().getKode()).isEqualTo(DekningsgradDto.HUNDRE.getValue());
    }

    @Test
    public void testUtenDekningsgrad() {
        assertThat(YtelseSøknadMapper.mapDekningsgrad(new ManuellRegistreringForeldrepengerDto())).isNull();
    }

    @Test
    public void test_mapOverfoeringsperiode() {
        var overføringsperiode = new OverføringsperiodeDto();
        overføringsperiode.setOverforingArsak(INSTITUSJONSOPPHOLD_ANNEN_FORELDER);

        var overfoeringsperiode = YtelseSøknadMapper.mapOverføringsperiode(overføringsperiode, ForeldreType.FAR);
        assertThat(overfoeringsperiode).isNotNull();
        assertThat(overfoeringsperiode.getAarsak().getKode()).isEqualTo(INSTITUSJONSOPPHOLD_ANNEN_FORELDER.getKode());
    }

    @Test
    public void test_mapUtsettelsesperiode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var utsettelserDto = opprettUtsettelseDto(fraDato, tomDato, FELLESPERIODE, true);
        var utsettelsesperiode = YtelseSøknadMapper.mapUtsettelsesperiode(utsettelserDto);
        assertThat(utsettelsesperiode).isNotNull();
        assertThat(utsettelsesperiode.getAarsak().getKode()).isEqualTo(UtsettelseÅrsak.FERIE.getKode());
        assertThat(utsettelsesperiode.getFom()).isEqualTo(fraDato);
        assertThat(utsettelsesperiode.getTom()).isEqualTo(tomDato);
        assertThat(utsettelsesperiode.getUtsettelseAv().getKode()).isEqualTo(FELLESPERIODE.getKode());
        assertThat(utsettelsesperiode.isErArbeidstaker()).isTrue();
    }

    @Test
    public void test_mapFedrekvotePeriodeDto() {
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
    public void test_mapMødrekvotePeriodeDto() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(
            opprettPermisjonPeriodeDto(fraDato, tomDato, MØDREKVOTE, null));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(MØDREKVOTE.getKode());
    }

    @Test
    public void test_mapForeldrepengerFørFødselPeriode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(
            opprettPermisjonPeriodeDto(fraDato, tomDato, FORELDREPENGER_FØR_FØDSEL, null));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(FORELDREPENGER_FØR_FØDSEL.getKode());
    }

    @Test
    public void test_mapGraderingsperiode() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15);
        String orgNr = KUNSTIG_ORG + "1";
        var gradering = ((Gradering) YtelseSøknadMapper.mapGraderingsperiode(
            opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, orgNr)));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(FEDREKVOTE.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isTrue();
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    public void test_mapGraderingsperiode_arbeidsprosent_desimal() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15.55);
        String orgNr = KUNSTIG_ORG + "1";
        Gradering gradering = ((Gradering) YtelseSøknadMapper.mapGraderingsperiode(
            opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, orgNr)));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(FEDREKVOTE.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isTrue();
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    public void test_mapFellesPeriodeDto() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);

        var uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(
            opprettPermisjonPeriodeDto(fraDato, tomDato, FELLESPERIODE, ARBEID));
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(FELLESPERIODE.getKode());
        assertThat(uttaksperiode.getMorsAktivitetIPerioden().getKode()).isEqualTo(ARBEID.getKode());
    }

    @Test
    public void test_mapUttaksperioder_gradering() {
        var fraDato = LocalDate.now();
        var tomDato = LocalDate.now().plusDays(3);
        var prosentAndel = BigDecimal.valueOf(15);
        var uttaksperiodes = YtelseSøknadMapper.mapGraderingsperioder(
            List.of(opprettGraderingDto(fraDato, tomDato, prosentAndel, FEDREKVOTE, true, false, false, null)));
        assertThat(uttaksperiodes).isNotNull();
        assertThat(uttaksperiodes).hasSize(1);
    }

    @Test
    public void test_mapUttaksperioder_gradering_med_samtidig_uttak() {
        var graderingDto = opprettGraderingDto(LocalDate.now(), LocalDate.now().plusDays(3), BigDecimal.valueOf(15),
            FEDREKVOTE, true, false, false, null);
        graderingDto.setHarSamtidigUttak(true);
        graderingDto.setSamtidigUttaksprosent(BigDecimal.TEN);

        var uttakperioder = YtelseSøknadMapper.mapGraderingsperioder(List.of(graderingDto));
        assertThat(uttakperioder).hasSize(1);
        assertThat(uttakperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(graderingDto.getHarSamtidigUttak());
        assertThat(uttakperioder.get(0).getSamtidigUttakProsent()).isEqualTo(
            graderingDto.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    public void test_mapUttaksperioder_samtidig_uttak() {
        var periode = opprettPermisjonPeriodeDto(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 1), FELLESPERIODE,
            ARBEID_OG_UTDANNING);
        periode.setHarSamtidigUttak(true);
        periode.setSamtidigUttaksprosent(BigDecimal.TEN);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(List.of(periode));
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(periode.getHarSamtidigUttak());
        assertThat(uttaksperioder.get(0).getSamtidigUttakProsent()).isEqualTo(
            periode.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    public void test_mapUttaksperioder_opphold() {
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
    public void test_mapUttaksperioder_permisjonperioder() {
        // Test av permisjonsperiodene:
        // dato før fødsel -> fødselsdato -> mødrekvote
        var perminsjonstartFørFødsel = LocalDate.now().minusWeeks(3);
        var fødselsdato = LocalDate.now();
        var mødrekvoteSlutt = fødselsdato.plusDays(1).plusWeeks(3);

        var permisjonPeriodeFørFødselDto = opprettPermisjonPeriodeDto(perminsjonstartFørFødsel, fødselsdato,
            FORELDREPENGER_FØR_FØDSEL, null);
        var permisjonPeriodeMødrekvote = opprettPermisjonPeriodeDto(LocalDate.now().plusDays(1), mødrekvoteSlutt,
            MØDREKVOTE, null);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(
            List.of(permisjonPeriodeFørFødselDto, permisjonPeriodeMødrekvote));
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(2);

        assertThat(uttaksperioder).anyMatch(
            uttaksperiode -> FORELDREPENGER_FØR_FØDSEL.getKode().equals(uttaksperiode.getType().getKode()));
        assertThat(uttaksperioder).anyMatch(
            uttaksperiode -> MØDREKVOTE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_mapUttaksperioder_fellesperioder() {
        var mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        var fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        var fellesPeriodeDto = opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, FELLESPERIODE,
            ARBEID_OG_UTDANNING);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(List.of(fellesPeriodeDto));
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder).first()
            .matches(uttaksperiode -> ARBEID_OG_UTDANNING.getKode()
                .equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()));
        assertThat(uttaksperioder).first()
            .matches(uttaksperiode -> FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_map_mors_aktivitet_uføretrygd() {
        var mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        var fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        var uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(
            List.of(opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, FELLESPERIODE, UFØRE)));
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder).first()
            .matches(uttaksperiode -> UFØRE.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()));
        assertThat(uttaksperioder).first()
            .matches(uttaksperiode -> FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_mapFordeling_morSøkerMenfarRettPåFedrekvote() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(),
            FORELDREPENGER_FØR_FØDSEL, null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(
            opprettTidsromPermisjonDto(Collections.singletonList(permisjonPeriodeDto)));
        Fordeling fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
        // Forventer å ha mødrekvote periode basert på forventet permisjon før fødsel
        assertThat(fordeling.getPerioder()).hasSize(1);
    }

    @Test
    public void test_skal_ta_inn_papirsøknadDTO_mappe_til_xml_så_mappe_til_domenemodell() {
        var navBruker = opprettBruker();
        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE,
            null);
        var permisjonPeriodeDto2 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(),
            FELLESPERIODE, ARBEID);
        var permisjonPeriodeDto3 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(),
            FORELDREPENGER, INNLAGT);
        var permisjonPerioder = List.of(permisjonPeriodeDto, permisjonPeriodeDto2, permisjonPeriodeDto3);

        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);

        // Annen forelder er informert
        dto.setAnnenForelderInformert(true);

        AnnenForelderDto annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(true);
        dto.setAnnenForelder(annenForelderDto);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(STD_KVINNE_AKTØR_ID));
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);
        var oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, virksomhetTjeneste, iayTjeneste,
            personinfoAdapter, datavarehusTjeneste, oppgittPeriodeMottattDato);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        oversetter.trekkUtDataOgPersister(
            (MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad),
            new MottattDokument.Builder().medMottattDato(LocalDate.now())
                .medFagsakId(behandling.getFagsakId())
                .medElektroniskRegistrert(true)
                .build(), behandling, Optional.empty());

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
    }

    @Test
    public void test_verdikjeden_fra_papirsøknad_til_domenemodell_mor_søker_med_mødrekvote_periode_før_fødsel_utsettelse() {
        var navBruker = opprettBruker();
        var fødselsdato = LocalDate.now().minusDays(10);
        var mødrekvoteSlutt = fødselsdato.plusWeeks(10);
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, fødselsdato, 1);
        // Perioder: permisjon før fødsel og mødrekvote
        List<PermisjonPeriodeDto> permisjonsperioder = new ArrayList<>();
        permisjonsperioder.add(
            opprettPermisjonPeriodeDto(fødselsdato.minusWeeks(3), fødselsdato, FORELDREPENGER_FØR_FØDSEL, null));
        permisjonsperioder.add(opprettPermisjonPeriodeDto(fødselsdato, mødrekvoteSlutt, MØDREKVOTE, null));
        var tidsromPermisjonDto = opprettTidsromPermisjonDto(permisjonsperioder);

        var utsettelserDto = opprettUtsettelseDto(mødrekvoteSlutt, mødrekvoteSlutt.plusWeeks(1), MØDREKVOTE, true);
        tidsromPermisjonDto.setUtsettelsePeriode(Collections.singletonList(utsettelserDto));

        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(tidsromPermisjonDto);

        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.AATI);

        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);

        var annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(false);
        annenForelderDto.setSokerHarAleneomsorg(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenForelderDto);
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(STD_KVINNE_AKTØR_ID));

        var soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);
        var oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, virksomhetTjeneste, iayTjeneste,
            personinfoAdapter, datavarehusTjeneste, oppgittPeriodeMottattDato);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(AktørId.class))).thenReturn(Optional.of(kvinne));
        oversetter.trekkUtDataOgPersister(
            (MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad),
            new MottattDokument.Builder().medMottattDato(LocalDate.now())
                .medFagsakId(behandling.getFagsakId())
                .medElektroniskRegistrert(true)
                .build(), behandling, Optional.empty());

        var ytelseFordeling = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(ytelseFordeling.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordeling.getOppgittRettighet().getHarAnnenForeldreRett()).isFalse();
        assertThat(ytelseFordeling.getOppgittRettighet().getHarAleneomsorgForBarnet()).isTrue();
        assertThat(ytelseFordeling.getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden()).isTrue();

        assertThat(ytelseFordeling.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordeling.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
        // Foreldrepenger før fødsel, mødrekvote og utsettelse
        assertThat(ytelseFordeling.getOppgittFordeling().getOppgittePerioder()).hasSize(3);
        assertThat(ytelseFordeling.getOppgittFordeling().getOppgittePerioder()).anySatisfy(
            periode -> assertThat(periode.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL));
        assertThat(ytelseFordeling.getOppgittFordeling().getOppgittePerioder()).anySatisfy(
            periode -> assertThat(periode.getPeriodeType()).isEqualTo(MØDREKVOTE));
        assertThat(ytelseFordeling.getOppgittFordeling().getOppgittePerioder()).anySatisfy(
            periode -> assertThat(periode.isArbeidstaker()).isEqualTo(Boolean.TRUE));
    }

    @Test
    public void test_mapFordeling_morAleneomsorg() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        var permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now(), LocalDate.now().plusWeeks(3), MØDREKVOTE,
            null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(
            opprettTidsromPermisjonDto(Collections.singletonList(permisjonPeriodeDto)));

        var fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
    }

    @Test
    public void test_mapForeldrepengerMedEgenNæring() {
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(registreringForeldrepengerDto, true, LocalDate.now().minusWeeks(3), 1);
        registreringForeldrepengerDto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        ytelseSøknadMapper.mapSøknad(registreringForeldrepengerDto, opprettBruker());
    }

    @Test
    public void test_mapRettigheter_farRettPåFedrekvote() {
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
    public void test_mapRettigheter_morAleneomsorg() {
        var manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        manuellRegistreringForeldrepengerDto.setAnnenForelder(opprettAnnenForelderDto(true, true, false));
        Rettigheter rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isFalse();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    public void skal_ikke_mappe_og_lagre_oppgitt_opptjening_når_det_allerede_finnes_i_grunnlaget() {
        var iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(iayGrunnlag.getOppgittOpptjening()).thenReturn(Optional.of(mock(OppgittOpptjening.class)));
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var navBruker = opprettBruker();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPerioder = List.of(
            opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE, null));
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);
        dto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);

        var mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId())
            .medElektroniskRegistrert(true);
        var oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, virksomhetTjeneste, iayTjeneste,
            personinfoAdapter, datavarehusTjeneste, oppgittPeriodeMottattDato);

        oversetter.trekkUtDataOgPersister(
            (MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), mottattDokument.build(),
            behandling, Optional.empty());

        verify(iayTjeneste, times(0)).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }

    @Test
    public void skal_mappe_og_lagre_oppgitt_opptjening_når_det_ikke_finnes_i_grunnlaget() {
        var iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(personinfoAdapter.hentBrukerKjønnForAktør(any(AktørId.class))).thenReturn(Optional.of(kvinne));
        when(iayGrunnlag.getOppgittOpptjening()).thenReturn(Optional.empty());
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusYears(1))
            .medRegistrert(LocalDate.now().minusYears(1))
            .build());

        var navBruker = opprettBruker();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var dto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(dto, true, LocalDate.now(), 1);
        var permisjonPerioder = List.of(
            opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), MØDREKVOTE, null));
        dto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        dto.setDekningsgrad(DekningsgradDto.HUNDRE);
        dto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        var soeknad = ytelseSøknadMapper.mapSøknad(dto, navBruker);

        var mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId())
            .medElektroniskRegistrert(true);
        var oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, virksomhetTjeneste, iayTjeneste,
            personinfoAdapter, datavarehusTjeneste, oppgittPeriodeMottattDato);

        oversetter.trekkUtDataOgPersister(
            (MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), mottattDokument.build(),
            behandling, Optional.empty());
        verify(iayTjeneste).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }
}
