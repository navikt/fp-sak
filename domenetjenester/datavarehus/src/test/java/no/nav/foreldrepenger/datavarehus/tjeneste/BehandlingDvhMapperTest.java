package no.nav.foreldrepenger.datavarehus.tjeneste;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingDvhMapperTest extends EntityManagerAwareTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private static final long VEDTAK_ID = 1L;
    private static final String BEHANDLENDE_ENHET = "behandlendeEnhet";
    private static final String ANSVARLIG_BESLUTTER = "ansvarligBeslutter";
    private static final String ANSVARLIG_SAKSBEHANDLER = "ansvarligSaksbehandler";
    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER  = new Saksnummer("12345");
    private static LocalDateTime OPPRETTET_TID = LocalDateTime.now();
    private static final String KLAGE_BEGRUNNELSE = "Begrunnelse for klagevurdering er bla.bla.bla.";
    private static final String BEHANDLENDE_ENHET_ID = "1234";
    private Arbeidsgiver arbeidsgiver;

    @BeforeEach
    public void setUp() {
        arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
    }

    @Test
    public void skal_mappe_familiehendelse_til_behandling_dvh_ikke_vedtatt() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.OPPHØR, false);
        LocalDate fødselsdato = LocalDate.of(2017, JANUARY, 1);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggHendelseGrunnlag(fødselsdato, fødselsdato);
        Optional<FamilieHendelseGrunnlagEntitet> fh = Optional.of(grunnlag);
        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), fh, Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isVedtatt()).isFalse();
        assertThat(dvh.getSoeknadFamilieHendelse()).isEqualTo("FODSL");
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }


    @Test
    public void skal_mappe_til_behandling_dvh_uten_vedtak() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.IKKE_FASTSATT, false);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(dvh.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.IKKE_FASTSATT.getKode());
        assertThat(dvh.getBehandlingStatus()).isEqualTo(BehandlingStatus.OPPRETTET.getKode());
        assertThat(dvh.getBehandlingType()).isEqualTo(BehandlingType.FØRSTEGANGSSØKNAD.getKode());
        assertThat(dvh.getEndretAv()).isEqualTo("OpprettetAv");
        assertThat(dvh.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(dvh.getFunksjonellTid()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(dvh.getOpprettetDato()).isEqualTo(OPPRETTET_TID.toLocalDate());
        assertThat(dvh.getUtlandstilsnitt()).isEqualTo("NASJONAL");
        assertThat(dvh.getVedtakId()).isNull();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
        assertThat(dvh.getFoersteStoenadsdag()).isNull();
    }

    @Test
    public void skal_mappe_til_behandling_dvh_foerste_stoenadsdag() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        final Repository repository = new Repository(getEntityManager());

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        Behandling behandling = scenario.lagre(repositoryProvider);
        repository.lagre(behandling.getBehandlingsresultat());

        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        final FpUttakRepository fpUttakRepository = new FpUttakRepository(getEntityManager());
        final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinnelig);
        var hentetUttakResultatOpt = uttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            hentetUttakResultatOpt, Optional.empty());
        assertThat(dvh.getFoersteStoenadsdag()).isEqualTo(LocalDate.now());

        UttakResultatPerioderEntitet uttakResultat = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultat);
        hentetUttakResultatOpt = uttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            hentetUttakResultatOpt, Optional.empty());
        assertThat(dvh.getFoersteStoenadsdag()).isEqualTo(LocalDate.now().plusDays(1));

    }


    @Test
    public void skal_mappe_til_behandling_dvh_ikke_vedtatt() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.OPPHØR, false);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isVedtatt()).isFalse();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_til_behandling_dvh_vedtatt() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.AVSLÅTT, true);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isVedtatt()).isTrue();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_til_behandling_dvh_ferdig() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.AVSLÅTT, true);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isFerdig()).isTrue();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_til_behandling_dvh_ikke_ferdig() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.AVSLÅTT, false);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isFerdig()).isFalse();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_til_behandling_dvh_abrutt() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET, true);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isAvbrutt()).isTrue();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_til_behandling_dvh_ikke_abrutt() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.IKKE_FASTSATT, false);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());
        assertThat(dvh).isNotNull();
        assertThat(dvh.isAvbrutt()).isFalse();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void mapping_klage_med_påklagd_behandling() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        ScenarioMorSøkerEngangsstønad scenarioMorSøkerEngangsstønad = opprettFørstegangssøknadScenario();
        Behandling behandling = byggBehandling(scenarioMorSøkerEngangsstønad, BehandlingResultatType.AVSLÅTT, true);

        ScenarioKlageEngangsstønad scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad, KlageMedholdÅrsak.NYE_OPPLYSNINGER, KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        Behandling klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        KlageRepository klageRepository = scenarioKlageEngangsstønad.getKlageRepository();
        klageRepository.settPåklagdBehandlingId(klageBehandling.getId(), behandling.getId());

        Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(klageBehandling);

        BehandlingDvh dvh = BehandlingDvhMapper.map(klageBehandling, mottattTidspunkt, Optional.empty(), Optional.empty(), klageVurderingResultat,Optional.empty(),Optional.empty());

        assertThat(dvh.getRelatertBehandling()).as("Forventer at relatert behandling på klagen er satt itl orginalbehandlingen vi klager på").isEqualTo(behandling.getId());
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void mapping_klage_uten_påklagd_behandling() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        ScenarioMorSøkerEngangsstønad scenarioMorSøkerEngangsstønad = opprettFørstegangssøknadScenario();

        ScenarioKlageEngangsstønad scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad, KlageMedholdÅrsak.NYE_OPPLYSNINGER, KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        Behandling klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        KlageRepository klageRepository = scenarioKlageEngangsstønad.getKlageRepository();

        Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(klageBehandling);

        BehandlingDvh dvh = BehandlingDvhMapper.map(klageBehandling, mottattTidspunkt, Optional.empty(), Optional.empty(), klageVurderingResultat,Optional.empty(),Optional.empty());

        assertThat(dvh.getRelatertBehandling()).as("Forventer at relatert behandling på klagen ikke blir satt når det ikke er påklagd ett vedtak.").isNull();
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    @Test
    public void skal_mappe_vedtak_id() {
        LocalDateTime mottattTidspunkt = LocalDateTime.now();
        Behandling behandling = byggBehandling(opprettFørstegangssøknadScenario(), BehandlingResultatType.INNVILGET, true);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakResultatType(VedtakResultatType.INNVILGET).medBehandlingsresultat(behandlingsresultat)
                .medVedtakstidspunkt(LocalDateTime.now()).medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER).build();
        Whitebox.setInternalState(behandlingVedtak, "id", VEDTAK_ID);

        BehandlingDvh dvh = BehandlingDvhMapper.map(behandling, mottattTidspunkt, Optional.of(behandlingVedtak), Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty());

        assertThat(dvh.getVedtakId()).isEqualTo(VEDTAK_ID);
        assertThat(dvh.getMottattTidspunkt()).isEqualTo(mottattTidspunkt);
    }

    private ScenarioKlageEngangsstønad opprettKlageScenario(AbstractTestScenario<?> abstractTestScenario, KlageMedholdÅrsak klageMedholdÅrsak, KlageVurderingOmgjør klageVurderingOmgjør) {
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNFP(abstractTestScenario);
        return scenario.medKlageMedholdÅrsak(klageMedholdÅrsak).medKlageVurderingOmgjør(klageVurderingOmgjør)
            .medBegrunnelse(KLAGE_BEGRUNNELSE).medBehandlendeEnhet(BEHANDLENDE_ENHET_ID);
    }

    private Behandling byggBehandling(ScenarioMorSøkerEngangsstønad morSøkerEngangsstønad, BehandlingResultatType behandlingResultatType, boolean avsluttetFagsak) {
        Behandling behandling = morSøkerEngangsstønad.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        opprettBehandlingsresultat(behandling, behandlingResultatType);
        setFaksak(behandling, avsluttetFagsak);

        Whitebox.setInternalState(behandling, "opprettetAv", "OpprettetAv");
        Whitebox.setInternalState(behandling, "opprettetTidspunkt", OPPRETTET_TID);
        return behandling;
    }

    private ScenarioMorSøkerEngangsstønad opprettFørstegangssøknadScenario() {
        return ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
    }

    private void opprettBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType).buildFor(behandling);
    }

    private void setFaksak(Behandling behandling, boolean avsluttet) {
        if (avsluttet) {
            behandling.getFagsak().setAvsluttet();
        }
    }
    private static FamilieHendelseGrunnlagEntitet byggHendelseGrunnlag(LocalDate fødselsdato, LocalDate oppgittFødselsdato) {
        final FamilieHendelseBuilder hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        if (oppgittFødselsdato != null) {
            hendelseBuilder.medFødselsDato(oppgittFødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(hendelseBuilder)
            .medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
                .medFødselsDato(fødselsdato))
            .build();
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, new BigDecimal("100.00"));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, graderingArbeidsprosent, new Utbetalingsgrad(100));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent,
                                                                     Utbetalingsgrad utbetalingsgrad) {

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();
        UttakResultatDokRegelEntitet dokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medArbeidsprosent(graderingArbeidsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttakResultatPeriode);

        return perioder;
    }
}
