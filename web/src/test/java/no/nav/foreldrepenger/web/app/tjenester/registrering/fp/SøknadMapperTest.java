package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentOversetterSøknad;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Rettigheter;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Dekningsgrad;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

public class SøknadMapperTest {
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final AktørId STD_KVINNE_AKTØR_ID = AktørId.dummy();
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = Mockito.mock(InntektArbeidYtelseTjeneste.class);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private TpsTjeneste tpsTjeneste;
    private DatavarehusTjeneste datavarehusTjeneste = mock(DatavarehusTjeneste.class);
    private SvangerskapspengerRepository svangerskapspengerRepository = new SvangerskapspengerRepository(repositoryRule.getEntityManager());

    private SøknadMapper ytelseSøknadMapper;

    @Before
    public void setUp() throws Exception {
        tpsTjeneste = mock(TpsTjeneste.class);
        reset(tpsTjeneste);
        final Optional<AktørId> stdKvinneAktørId = Optional.of(STD_KVINNE_AKTØR_ID);
        when(tpsTjeneste.hentAktørForFnr(any())).thenReturn(stdKvinneAktørId);
        final Personinfo.Builder builder = new Personinfo.Builder()
            .medAktørId(STD_KVINNE_AKTØR_ID)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medNavn("Espen Utvikler")
            .medPersonIdent(PersonIdent.fra("12345678901"))
            .medFødselsdato(LocalDate.now().minusYears(20));
        final Optional<Personinfo> build = Optional.ofNullable(builder.build());
        when(tpsTjeneste.hentBrukerForAktør(any(AktørId.class))).thenReturn(build);
        when(iayTjeneste.hentGrunnlag(any(Long.class))).thenReturn(Mockito.mock(InntektArbeidYtelseGrunnlag.class));
        when(virksomhetTjeneste.finnOrganisasjon(any()))
            .thenReturn(Optional.of(Virksomhet.getBuilder().medOrgnr(KUNSTIG_ORG).medNavn("Ukjent Firma").medRegistrert(LocalDate.now().minusYears(1)).medRegistrert(LocalDate.now().minusYears(1)).build()));
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(any()))
            .thenReturn(Virksomhet.getBuilder().medOrgnr(KUNSTIG_ORG).medNavn("Ukjent Firma").medRegistrert(LocalDate.now().minusYears(1)).medRegistrert(LocalDate.now().minusYears(1)).build());
        ytelseSøknadMapper = new YtelseSøknadMapper(tpsTjeneste, virksomhetTjeneste);
    }

    @Test
    public void test_mapForeldrepenger() {
        ManuellRegistreringForeldrepengerDto registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(registreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now().minusWeeks(3), 1);
        ytelseSøknadMapper.mapSøknad(registreringForeldrepengerDto, opprettBruker());
    }

    @Test
    public void test_mapEndringssøknad_utenFordeling() {
        TidsromPermisjonDto tidsromPermisjonDto = opprettTidsromPermisjonDto(null);
        ManuellRegistreringEndringsøknadDto manuellRegistreringEndringsøknadDto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(manuellRegistreringEndringsøknadDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);

        manuellRegistreringEndringsøknadDto.setTidsromPermisjon(tidsromPermisjonDto);
        Fordeling fordeling = YtelseSøknadMapper.mapFordelingEndringssøknad(manuellRegistreringEndringsøknadDto);
        assertThat(fordeling).isNotNull();
        assertThat(fordeling.getPerioder()).isEmpty();
    }

    @Test
    public void test_mapEndringssøknad_medFordeling() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        BigDecimal prosentAndel = BigDecimal.valueOf(15);
        UttakPeriodeType uttakPeriodeType = UttakPeriodeType.FEDREKVOTE;

        PermisjonPeriodeDto permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now(), LocalDate.now().plusWeeks(3), UttakPeriodeType.MØDREKVOTE, null);
        TidsromPermisjonDto tidsromPermisjonDto = opprettTidsromPermisjonDto(Collections.singletonList(permisjonPeriodeDto));
        ManuellRegistreringEndringsøknadDto manuellRegistreringEndringsøknadDto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(manuellRegistreringEndringsøknadDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);

        //Oppretter utsettelseperiode, mapOverfoeringsperiode, mapGraderingsperioder
        GraderingDto graderingDto = opprettGraderingDto(fraDato, tomDato, prosentAndel, uttakPeriodeType, false, true, false, null);
        tidsromPermisjonDto.setGraderingPeriode(Collections.singletonList(graderingDto));

        UtsettelseDto utsettelserDto = opprettUtsettelseDto(fraDato, tomDato, uttakPeriodeType, false);
        tidsromPermisjonDto.setUtsettelsePeriode(Collections.singletonList(utsettelserDto));

        OverføringÅrsak årsak = OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
        OverføringsperiodeDto overføringsperiodeDto = new OverføringsperiodeDto();
        overføringsperiodeDto.setOverforingArsak(årsak);
        overføringsperiodeDto.setPeriodeFom(fraDato);
        overføringsperiodeDto.setPeriodeTom(tomDato);
        tidsromPermisjonDto.setOverforingsperioder(List.of(overføringsperiodeDto));

        manuellRegistreringEndringsøknadDto.setTidsromPermisjon(tidsromPermisjonDto);
        Fordeling fordeling = YtelseSøknadMapper.mapFordelingEndringssøknad(manuellRegistreringEndringsøknadDto);
        assertThat(fordeling).isNotNull();
        assertThat(fordeling.getPerioder()).hasSize(4); //Forventer å ha en periode for hver av: permisjonPeriode, utsettelseperiode, Overfoeringsperiode og Graderingsperiode.
    }



    @Test
    public void testMedDekningsgrad() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.HUNDRE);

        Dekningsgrad dekningsgrad = YtelseSøknadMapper.mapDekningsgrad(manuellRegistreringForeldrepengerDto);
        assertThat(dekningsgrad).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad()).isNotNull();
        assertThat(dekningsgrad.getDekningsgrad().getKode()).isEqualTo(DekningsgradDto.HUNDRE.getValue());
    }

    @Test
    public void testUtenDekningsgrad() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();

        Dekningsgrad dekningsgrad = YtelseSøknadMapper.mapDekningsgrad(manuellRegistreringForeldrepengerDto);
        assertThat(dekningsgrad).isNull();
    }

    @Test
    public void test_mapOverfoeringsperiode() {
        OverføringÅrsak årsak = OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
        OverføringsperiodeDto overføringsperiode = new OverføringsperiodeDto();
        overføringsperiode.setOverforingArsak(årsak);

        Overfoeringsperiode overfoeringsperiode = YtelseSøknadMapper.mapOverføringsperiode(overføringsperiode, ForeldreType.FAR);
        assertThat(overfoeringsperiode).isNotNull();
        assertThat(overfoeringsperiode.getAarsak().getKode()).isEqualTo(årsak.getKode());
    }

    @Test
    public void test_mapUtsettelsesperiode() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        UttakPeriodeType uttakPeriodeType = UttakPeriodeType.FELLESPERIODE;
        UtsettelseDto utsettelserDto = opprettUtsettelseDto(fraDato, tomDato, uttakPeriodeType, true);
        Utsettelsesperiode utsettelsesperiode = YtelseSøknadMapper.mapUtsettelsesperiode(utsettelserDto);
        assertThat(utsettelsesperiode).isNotNull();
        assertThat(utsettelsesperiode.getAarsak().getKode()).isEqualTo(UtsettelseÅrsak.FERIE.getKode());
        assertThat(utsettelsesperiode.getFom()).isEqualTo(fraDato);
        assertThat(utsettelsesperiode.getTom()).isEqualTo(tomDato);
        assertThat(utsettelsesperiode.getUtsettelseAv().getKode()).isEqualTo(uttakPeriodeType.getKode());
        assertThat(utsettelsesperiode.isErArbeidstaker()).isEqualTo(true);
    }


    @Test
    public void test_mapFedrekvotePeriodeDto() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        PermisjonPeriodeDto fedrekvotePeriodeDto = opprettPermisjonPeriodeDto(fraDato, tomDato, UttakPeriodeType.FEDREKVOTE, null);
        Uttaksperiode uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(fedrekvotePeriodeDto);
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(UttakPeriodeType.FEDREKVOTE.getKode());
    }

    @Test
    public void test_mapMødrekvotePeriodeDto() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        PermisjonPeriodeDto fedrekvotePeriodeDto = opprettPermisjonPeriodeDto(fraDato, tomDato, UttakPeriodeType.MØDREKVOTE, null);
        Uttaksperiode uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(fedrekvotePeriodeDto);
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(UttakPeriodeType.MØDREKVOTE.getKode());
    }

    @Test
    public void test_mapForeldrepengerFørFødselPeriode() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        PermisjonPeriodeDto fedrekvotePeriodeDto = opprettPermisjonPeriodeDto(fraDato, tomDato, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, null);
        Uttaksperiode uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(fedrekvotePeriodeDto);
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL.getKode());
    }

    @Test
    public void test_mapGraderingsperiode() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        BigDecimal prosentAndel = BigDecimal.valueOf(15);
        UttakPeriodeType uttakPeriodeType = UttakPeriodeType.FEDREKVOTE;
        String orgNr = KUNSTIG_ORG + "1";
        GraderingDto graderingDto = opprettGraderingDto(fraDato, tomDato, prosentAndel, uttakPeriodeType, true, false, false, orgNr);
        Gradering gradering = ((Gradering) YtelseSøknadMapper.mapGraderingsperiode(graderingDto));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(uttakPeriodeType.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isEqualTo(true);
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    public void test_mapGraderingsperiode_arbeidsprosent_desimal() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        BigDecimal prosentAndel = BigDecimal.valueOf(15.55);
        UttakPeriodeType uttakPeriodeType = UttakPeriodeType.FEDREKVOTE;
        String orgNr = KUNSTIG_ORG + "1";
        GraderingDto graderingDto = opprettGraderingDto(fraDato, tomDato, prosentAndel, uttakPeriodeType, true, false, false, orgNr);
        Gradering gradering = ((Gradering) YtelseSøknadMapper.mapGraderingsperiode(graderingDto));
        assertThat(gradering).isNotNull();
        assertThat(gradering.getFom()).isEqualTo(fraDato);
        assertThat(gradering.getTom()).isEqualTo(tomDato);
        assertThat(gradering.getType().getKode()).isEqualTo(uttakPeriodeType.getKode());
        assertThat(gradering.getArbeidtidProsent()).isEqualTo(prosentAndel.doubleValue());
        assertThat(gradering.isErArbeidstaker()).isEqualTo(true);
        assertThat(gradering.getArbeidsgiver().getIdentifikator()).isEqualTo(orgNr);
    }

    @Test
    public void test_mapFellesPeriodeDto() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);

        PermisjonPeriodeDto fellesPeriodeDto = opprettPermisjonPeriodeDto(fraDato, tomDato, UttakPeriodeType.FELLESPERIODE, MorsAktivitet.ARBEID);
        Uttaksperiode uttaksperiode = YtelseSøknadMapper.mapPermisjonPeriodeDto(fellesPeriodeDto);
        assertThat(uttaksperiode).isNotNull();
        assertThat(uttaksperiode.getFom()).isEqualTo(fraDato);
        assertThat(uttaksperiode.getTom()).isEqualTo(tomDato);
        assertThat(uttaksperiode.getType().getKode()).isEqualTo(UttakPeriodeType.FELLESPERIODE.getKode());
        assertThat(uttaksperiode.getMorsAktivitetIPerioden().getKode()).isEqualTo(MorsAktivitet.ARBEID.getKode());
    }

    @Test
    public void test_mapUttaksperioder_gradering() {
        LocalDate fraDato = LocalDate.now();
        LocalDate tomDato = LocalDate.now().plusDays(3);
        BigDecimal prosentAndel = BigDecimal.valueOf(15);
        UttakPeriodeType uttakPeriodeType = UttakPeriodeType.FEDREKVOTE;
        GraderingDto graderingDto = opprettGraderingDto(fraDato, tomDato, prosentAndel, uttakPeriodeType, true, false, false, null);

        List<Uttaksperiode> uttaksperiodes = YtelseSøknadMapper.mapGraderingsperioder(Collections.singletonList(graderingDto));
        assertThat(uttaksperiodes).isNotNull();
        assertThat(uttaksperiodes).hasSize(1);
    }

    @Test
    public void test_mapUttaksperioder_gradering_med_samtidig_uttak() {
        GraderingDto graderingDto = opprettGraderingDto(LocalDate.now(), LocalDate.now().plusDays(3), BigDecimal.valueOf(15),
            UttakPeriodeType.FEDREKVOTE, true, false, false, null);
        graderingDto.setHarSamtidigUttak(true);
        graderingDto.setSamtidigUttaksprosent(BigDecimal.TEN);

        List<Uttaksperiode> uttakperioder = YtelseSøknadMapper.mapGraderingsperioder(Collections.singletonList(graderingDto));
        assertThat(uttakperioder).hasSize(1);
        assertThat(uttakperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(graderingDto.getHarSamtidigUttak());
        assertThat(uttakperioder.get(0).getSamtidigUttakProsent()).isEqualTo(graderingDto.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    public void test_mapUttaksperioder_samtidig_uttak() {
        PermisjonPeriodeDto periode = opprettPermisjonPeriodeDto(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 1),
            UttakPeriodeType.FELLESPERIODE, MorsAktivitet.ARBEID_OG_UTDANNING);
        periode.setHarSamtidigUttak(true);
        periode.setSamtidigUttaksprosent(BigDecimal.TEN);

        List<Uttaksperiode> uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(Collections.singletonList(periode));
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder.get(0).isOenskerSamtidigUttak()).isEqualTo(periode.getHarSamtidigUttak());
        assertThat(uttaksperioder.get(0).getSamtidigUttakProsent()).isEqualTo(periode.getSamtidigUttaksprosent().doubleValue());
    }

    @Test
    public void test_mapUttaksperioder_opphold() {
        OppholdDto oppholdDto = new OppholdDto();
        oppholdDto.setPeriodeFom(LocalDate.now().minusDays(1));
        oppholdDto.setPeriodeTom(LocalDate.now());
        oppholdDto.setÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER);

        List<Oppholdsperiode> oppholdsperiode = YtelseSøknadMapper.mapOppholdsperioder(Collections.singletonList(oppholdDto));
        assertThat(oppholdsperiode).hasSize(1);
        assertThat(oppholdsperiode.get(0).getAarsak().getKode()).isEqualTo(oppholdDto.getÅrsak().getKode());
        assertThat(oppholdsperiode.get(0).getAarsak().getKodeverk()).isEqualTo(oppholdDto.getÅrsak().getKodeverk());
        assertThat(oppholdsperiode.get(0).getFom()).isEqualTo(oppholdDto.getPeriodeFom());
        assertThat(oppholdsperiode.get(0).getTom()).isEqualTo(oppholdDto.getPeriodeTom());
    }

    @Test
    public void test_mapUttaksperioder_permisjonperioder() {
        //Test av permisjonsperiodene:
        //dato før fødsel -> fødselsdato -> mødrekvote
        LocalDate perminsjonstartFørFødsel = LocalDate.now().minusWeeks(3);
        LocalDate fødselsdato = LocalDate.now();
        LocalDate mødrekvoteSlutt = fødselsdato.plusDays(1).plusWeeks(3);

        PermisjonPeriodeDto permisjonPeriodeFørFødselDto = opprettPermisjonPeriodeDto(perminsjonstartFørFødsel, fødselsdato, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, null);
        PermisjonPeriodeDto permisjonPeriodeMødrekvote = opprettPermisjonPeriodeDto(LocalDate.now().plusDays(1), mødrekvoteSlutt, UttakPeriodeType.MØDREKVOTE, null);

        List<PermisjonPeriodeDto> permisjonsperioder = new ArrayList<>();
        permisjonsperioder.add(permisjonPeriodeFørFødselDto);
        permisjonsperioder.add(permisjonPeriodeMødrekvote);

        List<Uttaksperiode> uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(permisjonsperioder);
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(2);
        assertThat(uttaksperioder).anySatisfy(uttaksperiode -> UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL.getKode().equals(uttaksperiode.getType().getKode()));
        assertThat(uttaksperioder).anySatisfy(uttaksperiode -> UttakPeriodeType.MØDREKVOTE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_mapUttaksperioder_Fellesperioder() {
        LocalDate mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        LocalDate fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        PermisjonPeriodeDto fellesPeriodeDto = opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, UttakPeriodeType.FELLESPERIODE, MorsAktivitet.ARBEID_OG_UTDANNING);

        List<Uttaksperiode> uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(Collections.singletonList(fellesPeriodeDto));
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder).first().satisfies(uttaksperiode -> MorsAktivitet.ARBEID_OG_UTDANNING.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()));
        assertThat(uttaksperioder).first().satisfies(uttaksperiode -> UttakPeriodeType.FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_map_mors_aktivitet_uføretrygd() {
        LocalDate mødrekvoteSlutt = LocalDate.now().plusWeeks(3);
        LocalDate fellesperiodeSlutt = mødrekvoteSlutt.plusWeeks(4);

        PermisjonPeriodeDto fellesPeriodeDto = opprettPermisjonPeriodeDto(mødrekvoteSlutt, fellesperiodeSlutt, UttakPeriodeType.FELLESPERIODE, MorsAktivitet.UFØRE);

        List<Uttaksperiode> uttaksperioder = YtelseSøknadMapper.mapUttaksperioder(Collections.singletonList(fellesPeriodeDto));
        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder).hasSize(1);
        assertThat(uttaksperioder).first().satisfies(uttaksperiode -> MorsAktivitet.UFØRE.getKode().equals(uttaksperiode.getMorsAktivitetIPerioden().getKode()));
        assertThat(uttaksperioder).first().satisfies(uttaksperiode -> UttakPeriodeType.FELLESPERIODE.getKode().equals(uttaksperiode.getType().getKode()));
    }

    @Test
    public void test_mapFordeling_morSøkerMenfarRettPåFedrekvote() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        PermisjonPeriodeDto permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(Collections.singletonList(permisjonPeriodeDto)));
        Fordeling fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
        assertThat(fordeling.getPerioder()).hasSize(1); //Forventer å ha mødrekvote periode basert på forventet permisjon før fødsel
    }

    @Test
    public void test_skal_ta_inn_papirsøknadDTO_mappe_til_xml_så_mappe_til_domenemodell() {
        NavBruker navBruker = opprettBruker();
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        List<PermisjonPeriodeDto> permisjonPerioder = new ArrayList<>();
        PermisjonPeriodeDto permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.MØDREKVOTE, null);
        PermisjonPeriodeDto permisjonPeriodeDto2 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.FELLESPERIODE, MorsAktivitet.ARBEID);
        PermisjonPeriodeDto permisjonPeriodeDto3 = opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.FORELDREPENGER, MorsAktivitet.INNLAGT);
        permisjonPerioder.add(permisjonPeriodeDto);
        permisjonPerioder.add(permisjonPeriodeDto2);
        permisjonPerioder.add(permisjonPeriodeDto3);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.HUNDRE);

        // Annen forelder er informert
        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);

        AnnenForelderDto annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenForelderDto);

        final Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);
        final MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, Mockito.mock(KodeverkRepository.class), virksomhetTjeneste, iayTjeneste, tpsTjeneste, datavarehusTjeneste, svangerskapspengerRepository);
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();

        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        final MottattDokument.Builder builder1 = new MottattDokument.Builder().medMottattDato(LocalDate.now()).medFagsakId(behandling.getFagsakId()).medElektroniskRegistrert(true);
        oversetter.trekkUtDataOgPersister((MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), builder1.build(), behandling, Optional.empty());

        final YtelseFordelingAggregat ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
    }

    @Test
    public void test_verdikjeden_fra_papirsøknad_til_domenemodell_mor_søker_med_mødrekvote_periode_før_fødsel_utsettelse() {
        NavBruker navBruker = opprettBruker();
        final LocalDate fødselsdato = LocalDate.now().minusDays(10);
        final LocalDate mødrekvoteSlutt = fødselsdato.plusWeeks(10);
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, fødselsdato, 1);
        //Perioder: permisjon før fødsel og mødrekvote
        List<PermisjonPeriodeDto> permisjonsperioder = new ArrayList<>();
        permisjonsperioder.add(opprettPermisjonPeriodeDto(fødselsdato.minusWeeks(3), fødselsdato, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, null));
        permisjonsperioder.add(opprettPermisjonPeriodeDto(fødselsdato, mødrekvoteSlutt, UttakPeriodeType.MØDREKVOTE, null));
        TidsromPermisjonDto tidsromPermisjonDto = opprettTidsromPermisjonDto(permisjonsperioder);

        //Utsettelse
        UtsettelseDto utsettelserDto = opprettUtsettelseDto(mødrekvoteSlutt, mødrekvoteSlutt.plusWeeks(1), UttakPeriodeType.MØDREKVOTE, true);
        tidsromPermisjonDto.setUtsettelsePeriode(Collections.singletonList(utsettelserDto));

        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(tidsromPermisjonDto);

        //Dekningsgrad
        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.AATI);

        // Annen forelder er informert
        manuellRegistreringForeldrepengerDto.setAnnenForelderInformert(true);

        AnnenForelderDto annenForelderDto = new AnnenForelderDto();
        annenForelderDto.setDenAndreForelderenHarRettPaForeldrepenger(false);
        annenForelderDto.setSokerHarAleneomsorg(true);
        manuellRegistreringForeldrepengerDto.setAnnenForelder(annenForelderDto);

        final Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);
        final MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, Mockito.mock(KodeverkRepository.class), virksomhetTjeneste, iayTjeneste, tpsTjeneste, datavarehusTjeneste, svangerskapspengerRepository);
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();

        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        final MottattDokument.Builder builder1 = new MottattDokument.Builder().medMottattDato(LocalDate.now())
            .medFagsakId(behandling.getFagsakId()).medElektroniskRegistrert(true);
        oversetter.trekkUtDataOgPersister((MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), builder1.build(), behandling, Optional.empty());

        final YtelseFordelingAggregat ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat.getOppgittRettighet()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett()).isFalse();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet()).isTrue();
        assertThat(ytelseFordelingAggregat.getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden()).isTrue();

        assertThat(ytelseFordelingAggregat.getOppgittFordeling()).isNotNull();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).hasSize(3); //Foreldrepenger før fødsel, mødrekvote og utsettelse
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).anySatisfy(periode -> assertThat(periode.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).anySatisfy(periode -> assertThat(periode.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE));
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).anySatisfy(periode -> assertThat(periode.getErArbeidstaker()).isEqualTo(Boolean.TRUE));
    }

    @Test
    public void test_mapFordeling_morAleneomsorg() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        PermisjonPeriodeDto permisjonPeriodeDto = opprettPermisjonPeriodeDto(LocalDate.now(), LocalDate.now().plusWeeks(3), UttakPeriodeType.MØDREKVOTE, null);
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(Collections.singletonList(permisjonPeriodeDto)));

        Fordeling fordeling = YtelseSøknadMapper.mapFordeling(manuellRegistreringForeldrepengerDto);
        assertThat(fordeling).isNotNull();
    }

    @Test
    public void test_mapForeldrepengerMedEgenNæring() {
        ManuellRegistreringForeldrepengerDto registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(registreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now().minusWeeks(3), 1);
        registreringForeldrepengerDto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        when(tpsTjeneste.hentAktørForFnr(any())).thenReturn(Optional.ofNullable(STD_KVINNE_AKTØR_ID));
        ytelseSøknadMapper.mapSøknad(registreringForeldrepengerDto, opprettBruker());
    }

    @Test
    public void test_mapRettigheter_farRettPåFedrekvote() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        manuellRegistreringForeldrepengerDto.setAnnenForelder(opprettAnnenForelderDto(true, true, true));
        Rettigheter rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isTrue();
        assertThat(rettigheter.isHarAleneomsorgForBarnet()).isTrue();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    public void test_mapRettigheter_morAleneomsorg() {
        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(null));
        manuellRegistreringForeldrepengerDto.setAnnenForelder(opprettAnnenForelderDto(true, true, false));
        Rettigheter rettigheter = YtelseSøknadMapper.mapRettigheter(manuellRegistreringForeldrepengerDto);
        assertThat(rettigheter).isNotNull();
        assertThat(rettigheter.isHarAnnenForelderRett()).isFalse();
        assertThat(rettigheter.isHarOmsorgForBarnetIPeriodene()).isTrue();
    }

    @Test
    public void skal_ikke_mappe_og_lagre_oppgitt_opptjening_når_det_allerede_finnes_i_grunnlaget() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlag.getOppgittOpptjening()).thenReturn(Optional.of(mock(OppgittOpptjening.class)));
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));

        NavBruker navBruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        List<PermisjonPeriodeDto> permisjonPerioder = List.of(opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.MØDREKVOTE, null));
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.HUNDRE);
        manuellRegistreringForeldrepengerDto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);

        MottattDokument.Builder mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now()).medFagsakId(behandling.getFagsakId()).medElektroniskRegistrert(true);
        MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, Mockito.mock(KodeverkRepository.class), virksomhetTjeneste, iayTjeneste, tpsTjeneste, datavarehusTjeneste, svangerskapspengerRepository);

        // Act
        oversetter.trekkUtDataOgPersister((MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), mottattDokument.build(), behandling, Optional.empty());

        // Assert
        verify(iayTjeneste, times(0)).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }

    @Test
    public void skal_mappe_og_lagre_oppgitt_opptjening_når_det_ikke_finnes_i_grunnlaget() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlag.getOppgittOpptjening()).thenReturn(Optional.empty());
        when(iayTjeneste.finnGrunnlag(any(Long.class))).thenReturn(Optional.of(iayGrunnlag));

        NavBruker navBruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        ManuellRegistreringForeldrepengerDto manuellRegistreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        oppdaterDtoForFødsel(manuellRegistreringForeldrepengerDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        List<PermisjonPeriodeDto> permisjonPerioder = List.of(opprettPermisjonPeriodeDto(LocalDate.now().minusWeeks(3), LocalDate.now(), UttakPeriodeType.MØDREKVOTE, null));
        manuellRegistreringForeldrepengerDto.setTidsromPermisjon(opprettTidsromPermisjonDto(permisjonPerioder));
        manuellRegistreringForeldrepengerDto.setDekningsgrad(DekningsgradDto.HUNDRE);
        manuellRegistreringForeldrepengerDto.setEgenVirksomhet(opprettEgenVirksomhetDto());
        Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringForeldrepengerDto, navBruker);

        MottattDokument.Builder mottattDokument = new MottattDokument.Builder().medMottattDato(LocalDate.now()).medFagsakId(behandling.getFagsakId()).medElektroniskRegistrert(true);
        MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, Mockito.mock(KodeverkRepository.class), virksomhetTjeneste, iayTjeneste, tpsTjeneste, datavarehusTjeneste, svangerskapspengerRepository);

        // Act
        oversetter.trekkUtDataOgPersister((MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), mottattDokument.build(), behandling, Optional.empty());

        // Assert
        verify(iayTjeneste, times(1)).lagreOppgittOpptjening(anyLong(), any(OppgittOpptjeningBuilder.class));
    }
}
