package no.nav.foreldrepenger.behandlingslager.behandling;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class BehandlingsgrunnlagEntitetTest extends EntityManagerAwareTest {

    private static final String OSLO = "0103";

    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private SøknadRepository søknadRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private FagsakRepository fagsakRepository;
    private MedlemskapRepository medlemskapRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        familieGrunnlagRepository = new FamilieHendelseRepository(entityManager);
        søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        medlemskapRepository = new MedlemskapRepository(entityManager);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_søknad() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now();
        LocalDate fødselsdato = LocalDate.now().plusDays(1);
        int antallBarnFraSøknad = 1;

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medAntallBarn(antallBarnFraSøknad)
            .medFødselsDato(fødselsdato);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder().medAdoptererAlene(true));

        familieGrunnlagRepository.lagre(behandling, hendelseBuilder);

        SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE)
            .medSøknadsdato(søknadsdato);
        søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());

        // Assert
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandling.getId());
        assertThat(søknad).isNotNull();

        assertThat(søknad.getSøknadsdato()).isEqualTo(søknadsdato);
        assertThat(søknad.getFarSøkerType()).isEqualTo(FarSøkerType.ADOPTERER_ALENE);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandling.getId());
        final FamilieHendelseEntitet søknadVersjon = familieHendelseGrunnlag.getSøknadVersjon();
        assertThat(søknadVersjon.getAdopsjon()).isPresent();
        assertThat(søknadVersjon.getAdopsjon().get().getAdoptererAlene()).isTrue();
        assertThat(søknadVersjon.getBarna()).hasSize(1);
        assertThat(søknadVersjon.getBarna().get(0).getFødselsdato()).isEqualTo(fødselsdato);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_adopsjon() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now();
        LocalDate fødselAdopsjonsdato = LocalDate.now();
        LocalDate omsorgsovertakelseDato = LocalDate.now().plusDays(1);
        int antallBarnFraSøknad = 1;

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medAntallBarn(antallBarnFraSøknad)
            .leggTilBarn(fødselAdopsjonsdato);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder().medAdoptererAlene(true));
        familieGrunnlagRepository.lagre(behandling, hendelseBuilder);
        final FamilieHendelseBuilder oppdatere = familieGrunnlagRepository.opprettBuilderFor(behandling);
        oppdatere.medAdopsjon(oppdatere.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsovertakelseDato)
            .medAdoptererAlene(true)
            .medErEktefellesBarn(false));
        familieGrunnlagRepository.lagre(behandling, oppdatere);

        SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE)
            .medSøknadsdato(søknadsdato);
        søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());

        // Assert
        final FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getGjeldendeBekreftetVersjon()).isPresent();
        assertThat(grunnlag.getGjeldendeBekreftetVersjon().get().getBarna()).isNotEmpty();

        Optional<AdopsjonEntitet> optionalAdopsjon = grunnlag.getGjeldendeVersjon().getAdopsjon();
        assertThat(optionalAdopsjon).isPresent();
        final AdopsjonEntitet adopsjon = optionalAdopsjon.get();
        assertThat(adopsjon.getOmsorgsovertakelseDato()).isEqualTo(omsorgsovertakelseDato);
        assertThat(adopsjon.getErEktefellesBarn()).isFalse();
        assertThat(adopsjon.getAdoptererAlene()).isTrue();
        assertThat(grunnlag.getGjeldendeVersjon().getBarna().iterator().next().getFødselsdato()).isEqualTo(fødselAdopsjonsdato);
    }

    @Test
    public void skal_oppdatere_eksisterende_søknad_med_endringer_i_adopsjon() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now();
        LocalDate fødselAdopsjonsdato = LocalDate.now();
        Map<Long, LocalDate> map = new HashMap<>();
        map.put(1L, fødselAdopsjonsdato);
        LocalDate omsorgsovertakelseDato = LocalDate.now().plusDays(1);
        int antallBarnFraSøknad = 1;

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medAntallBarn(antallBarnFraSøknad)
            .leggTilBarn(fødselAdopsjonsdato);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder().medAdoptererAlene(true));
        familieGrunnlagRepository.lagre(behandling, hendelseBuilder);

        SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE)
            .medSøknadsdato(søknadsdato)
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        Behandling hentet = behandlingRepository.hentBehandling(behandling.getId());

        // Act
        final FamilieHendelseBuilder oppdatere = familieGrunnlagRepository.opprettBuilderFor(hentet);
        oppdatere.medAdopsjon(oppdatere.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsovertakelseDato))
            .tilbakestillBarn()
            .leggTilBarn(new UidentifisertBarnEntitet(fødselAdopsjonsdato, 1));
        familieGrunnlagRepository.lagre(hentet, oppdatere);

        lagreBehandling(hentet);

        // Arrange
        final FamilieHendelseEntitet hendelse = familieHendelseRepository.hentAggregat(behandling.getId()).getGjeldendeVersjon();
        final Optional<AdopsjonEntitet> adopsjon1 = hendelse.getAdopsjon();

        assertThat(adopsjon1).isPresent();
        assertThat(hendelse.getBarna()).hasSize(1);
        assertThat(hendelse.getBarna().iterator().next().getFødselsdato()).isEqualTo(fødselAdopsjonsdato);
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_søknad_adopsjon_barn() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now();
        LocalDate fødselAdopsjonsdato = LocalDate.now();
        int antallBarnFraSøknad = 1;

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medAntallBarn(antallBarnFraSøknad)
            .leggTilBarn(fødselAdopsjonsdato);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder().medAdoptererAlene(true));
        familieGrunnlagRepository.lagre(behandling, hendelseBuilder);

        SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE)
            .medSøknadsdato(søknadsdato)
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Assert
        søknad = søknadRepository.hentSøknad(behandling.getId());
        assertThat(søknad).isNotNull();
        UidentifisertBarn søknadAdopsjonBarn = familieGrunnlagRepository.hentAggregat(behandling.getId()).getSøknadVersjon().getBarna().iterator().next();
        assertThat(søknadAdopsjonBarn.getFødselsdato()).isEqualTo(fødselAdopsjonsdato);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_fødsel() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato);
        familieGrunnlagRepository.lagre(behandling, hendelseBuilder);
        final FamilieHendelseBuilder hendelseBuilder1 = familieGrunnlagRepository.opprettBuilderFor(behandling).tilbakestillBarn().medAntallBarn(1).leggTilBarn(fødselsdato);
        familieGrunnlagRepository.lagre(behandling, hendelseBuilder1);

        SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now());
        søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());

        // Assert
        FamilieHendelseGrunnlagEntitet grunnlag = familieGrunnlagRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();

        assertThat(grunnlag.getGjeldendeVersjon()).isNotNull();
        assertThat(grunnlag.getGjeldendeVersjon().getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst().get()).isEqualTo(fødselsdato);
        assertThat(grunnlag.getGjeldendeVersjon().getAntallBarn()).isEqualTo(1);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_terminbekreftelse() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        LocalDate utstedtDato = LocalDate.now().minusMonths(2);

        Behandling behandling = lagBehandling();
        final FamilieHendelseBuilder søknadVersjon = familieGrunnlagRepository.opprettBuilderFor(behandling);
        søknadVersjon.medTerminbekreftelse(søknadVersjon.getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now())
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));
        familieGrunnlagRepository.lagre(behandling, søknadVersjon);
        final FamilieHendelseBuilder oppdatere = familieGrunnlagRepository.opprettBuilderFor(behandling);
        oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(utstedtDato))
            .medAntallBarn(1);
        familieGrunnlagRepository.lagre(behandling, oppdatere);
        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medSøknadsdato(LocalDate.now())
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Assert
        final FamilieHendelseGrunnlagEntitet grunnlag = familieGrunnlagRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();

        final Optional<TerminbekreftelseEntitet> terminbekreftelse1 = grunnlag.getGjeldendeVersjon().getTerminbekreftelse();
        assertThat(terminbekreftelse1).isPresent();
        final TerminbekreftelseEntitet terminbekreftelse2 = terminbekreftelse1.get();
        assertThat(terminbekreftelse2.getTermindato()).isEqualTo(termindato);
        assertThat(terminbekreftelse2.getUtstedtdato()).isEqualTo(utstedtDato);
        assertThat(grunnlag.getGjeldendeVersjon().getAntallBarn()).isEqualTo(1);
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_omsorgsovertakelse() {
        // Arrange
        LocalDate omsorgsovertakelsesdato = LocalDate.now();

        Behandling behandling = lagBehandling();

        final FamilieHendelseBuilder søknadVersjon = familieGrunnlagRepository.opprettBuilderFor(behandling);
        søknadVersjon.medAdopsjon(søknadVersjon.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now()));
        familieGrunnlagRepository.lagre(behandling, søknadVersjon);
        final FamilieHendelseBuilder oppdatere = familieGrunnlagRepository.opprettBuilderFor(behandling);
        oppdatere.medAdopsjon(oppdatere.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsovertakelsesdato)
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET));
        familieGrunnlagRepository.lagre(behandling, oppdatere);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medSøknadsdato(LocalDate.now())
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Assert
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandling.getId());
        assertThat(familieHendelseGrunnlag).isNotNull();
        final Optional<FamilieHendelseEntitet> bekreftetVersjon = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        assertThat(bekreftetVersjon).isPresent();
        assertThat(bekreftetVersjon.get().getAdopsjon()).isPresent();
        assertThat(bekreftetVersjon.get().getAdopsjon().get().getOmsorgsovertakelseDato()).isEqualTo(omsorgsovertakelsesdato);
        assertThat(bekreftetVersjon.get().getAdopsjon().get().getOmsorgovertakelseVilkår()).isEqualTo(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET);
    }

    @Test
    public void skal_innsette_bekrefet_barn_og_oppdatere_ved_endring() {
        LocalDate fødselsdato = LocalDate.now();
        LocalDate oppdatertFødselsdato = fødselsdato.plusDays(1);
        AktørId forelderAktørId = AktørId.dummy();
        AktørId barnAktørId = AktørId.dummy();

        Behandling behandling = lagBehandling();

        Long behandlingId = behandling.getId();
        final PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medNavn("Barn 1")
                    .medFødselsdato(fødselsdato)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(forelderAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medFødselsdato(fødselsdato.minusYears(25))
                    .medRegion(Region.NORDEN)
                    .medNavn("Forelder"))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(forelderAktørId, barnAktørId, RelasjonsRolleType.BARN)
                    .harSammeBosted(true))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(barnAktørId, forelderAktørId, RelasjonsRolleType.FARA)
                    .harSammeBosted(true))
            .leggTil(
                informasjonBuilder.
                    getAdresseBuilder(forelderAktørId, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(12)), AdresseType.BOSTEDSADRESSE)
                    .medAdresselinje1("Lyckliga gatan 1")
                    .medPostnummer("1150")
                    .medPoststed("Hundremeterskogen"))
            .leggTil(
                informasjonBuilder.
                    getPersonstatusBuilder(forelderAktørId, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(12))).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(
                informasjonBuilder
                    .getStatsborgerskapBuilder(forelderAktørId, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(12)), Landkoder.NOR, Region.NORDEN));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        final FamilieHendelseBuilder søknadVersjon = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medFødselsDato(LocalDate.now().minusDays(10));
        familieGrunnlagRepository.lagre(behandling, søknadVersjon);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medSøknadsdato(LocalDate.now())
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Assert 1: Barn 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet1 = behandlingRepository.hentBehandling(behandlingId);

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = hentSøkerPersonopplysninger(behandlingId);
        PersonInformasjonEntitet personInformasjon = personopplysningGrunnlag.getGjeldendeVersjon();

        assertThat(personInformasjon.getPersonopplysninger()).hasSize(2);
        assertThat(personInformasjon.getAdresser()).hasSize(1);
        assertThat(personInformasjon.getRelasjoner()).hasSize(2);
        assertThat(personInformasjon.getPersonstatus()).hasSize(1);
        assertThat(personInformasjon.getStatsborgerskap()).hasSize(1);

        List<PersonRelasjonEntitet> barna = personInformasjon.getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(forelderAktørId))
            .collect(toList());

        assertThat(barna).hasSize(1);
        assertThat(barna.get(0).getRelasjonsrolle()).isEqualTo(RelasjonsRolleType.BARN);
        assertThat(barna.get(0).getTilAktørId()).isEqualTo(barnAktørId);

        // Arrange 2. Oppdater barn 1

        PersonInformasjonBuilder overstyringBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        overstyringBuilder.leggTil(overstyringBuilder
            .getPersonopplysningBuilder(barnAktørId)
            .medFødselsdato(oppdatertFødselsdato));

        personopplysningRepository.lagre(behandlingId, overstyringBuilder);

        // Assert 2: Barn 1 er oppdatert
        Behandling opphentet2 = behandlingRepository.hentBehandling(behandlingId);

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = hentSøkerPersonopplysninger(behandlingId);
        Optional<PersonInformasjonEntitet> personInformasjon2 = personopplysningGrunnlag2.getRegisterVersjon();

        assertThat(personInformasjon2).isPresent();
        barna = personInformasjon2.get().getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(forelderAktørId))
            .collect(toList());

        assertThat(barna).hasSize(1);
        assertThat(
            personInformasjon2.get().getPersonopplysninger().stream()
                .filter(e -> e.getAktørId().equals(barnAktørId))
                .findFirst().get().getFødselsdato()).isEqualTo(oppdatertFødselsdato);

        // Arrange 3: Anvend grunnlagsbuilder uten å gjøre endringer på bekreftet barn
        // -> skal ikke føre til utilsiktede oppdateringer av BekreftetBarn
        final FamilieHendelseBuilder builder = familieGrunnlagRepository.opprettBuilderFor(opphentet2)
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1);

        familieGrunnlagRepository.lagre(opphentet2, builder);

        lagreBehandling(opphentet2);

        // Assert 3: Fortsatt bare barn 1 lagret
        @SuppressWarnings("unused")
        Behandling opphentet3 = behandlingRepository.hentBehandling(behandlingId);

        barna = hentSøkerPersonopplysninger(behandlingId)
            .getGjeldendeVersjon()
            .getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(forelderAktørId)).collect(toList());

        assertThat(barna).hasSize(1);
        assertThat(barna.get(0).getTilAktørId()).isEqualTo(barnAktørId);
    }

    @Test
    public void skal_innsette_nytt_bekreftet_barn_dersom_barnet_ikke_finnes_fra_før() {

        AktørId forelderAktørId = AktørId.dummy();
        LocalDate fødselsdatoBarn1 = LocalDate.now();
        LocalDate fødselsdatoBarn2 = fødselsdatoBarn1.plusDays(1);
        LocalDate fødselsdatoForelder = fødselsdatoBarn1.minusYears(25);
        AktørId barnNummer1 = AktørId.dummy();
        AktørId barnNummer2 = AktørId.dummy();

        Behandling behandling = lagBehandling();

        // Arrange 1. Legge til forelder og barn 1
        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(forelderAktørId)
                    .medNavn("Forelder")
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medFødselsdato(fødselsdatoForelder)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnNummer1)
                    .medNavn("Barn 1")
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medFødselsdato(fødselsdatoBarn1)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(
                informasjonBuilder
                    .getPersonstatusBuilder(forelderAktørId, DatoIntervallEntitet.fraOgMed(fødselsdatoForelder)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getPersonstatusBuilder(barnNummer1, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn1)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(forelderAktørId, DatoIntervallEntitet.fraOgMed(fødselsdatoForelder), Landkoder.NOR, Region.NORDEN))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(barnNummer1, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn1), Landkoder.NOR, Region.NORDEN))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(barnNummer1, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn1), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("Sverige").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getRelasjonBuilder(forelderAktørId, barnNummer1, RelasjonsRolleType.BARN))
            .leggTil(informasjonBuilder
               .getRelasjonBuilder(barnNummer1, forelderAktørId, RelasjonsRolleType.MORA)
            );

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Assert 1: Barn 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet1 = behandlingRepository.hentBehandling(behandlingId);

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = hentSøkerPersonopplysninger(behandlingId);
        PersonInformasjonEntitet personInformasjon = personopplysningGrunnlag.getGjeldendeVersjon();
        assertThat(personInformasjon.getPersonopplysninger()).hasSize(2);
        assertThat(personInformasjon.getAdresser()).hasSize(1);
        assertThat(personInformasjon.getRelasjoner()).hasSize(2);
        assertThat(personInformasjon.getPersonstatus()).hasSize(2);
        assertThat(personInformasjon.getStatsborgerskap()).hasSize(2);

        List<PersonRelasjonEntitet> barna = personopplysningGrunnlag.getGjeldendeVersjon().getRelasjoner()
            .stream().filter(e -> e.getAktørId().equals(forelderAktørId))
            .collect(toList());
        assertThat(barna).hasSize(1);
        assertThat(barna.get(0).getRelasjonsrolle()).isEqualTo(RelasjonsRolleType.BARN);
        assertThat(barna.get(0).getTilAktørId()).isEqualTo(barnNummer1);

        // Arrange 2. Legg til barn 2
        informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnNummer2)
                    .medNavn("Barn 2")
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medFødselsdato(fødselsdatoBarn2)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(informasjonBuilder
                .getPersonstatusBuilder(barnNummer2, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn2)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(barnNummer2, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn2), Landkoder.NOR, Region.NORDEN))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(barnNummer2, DatoIntervallEntitet.fraOgMed(fødselsdatoBarn2), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("Sverige").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getRelasjonBuilder(forelderAktørId, barnNummer2, RelasjonsRolleType.BARN))
            .leggTil(informasjonBuilder
               .getRelasjonBuilder(barnNummer2, forelderAktørId, RelasjonsRolleType.MORA)
            );

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Assert 2: Barn 1 og barn 2 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet2 = behandlingRepository.hentBehandling(behandlingId);
        personopplysningGrunnlag = hentSøkerPersonopplysninger(behandlingId);
        personInformasjon = personopplysningGrunnlag.getGjeldendeVersjon();
        assertThat(personInformasjon.getPersonopplysninger()).hasSize(3);
        assertThat(personInformasjon.getAdresser()).hasSize(2);
        assertThat(personInformasjon.getRelasjoner()).hasSize(4);
        assertThat(personInformasjon.getPersonstatus()).hasSize(3);
        assertThat(personInformasjon.getStatsborgerskap()).hasSize(3);


        barna = personInformasjon.getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(forelderAktørId))
            .collect(toList());
        assertThat(barna).hasSize(2);

        assertThat(barna.stream().map(PersonRelasjonEntitet::getRelasjonsrolle).collect(Collectors.toSet())).containsExactly(RelasjonsRolleType.BARN);
        assertThat(barna.stream().map(PersonRelasjonEntitet::getAktørId).collect(Collectors.toSet())).containsExactly(forelderAktørId);
        assertThat(barna.stream().map(PersonRelasjonEntitet::getTilAktørId).collect(Collectors.toSet())).containsExactlyInAnyOrder(barnNummer1, barnNummer2);
    }

    private PersonopplysningGrunnlagEntitet hentSøkerPersonopplysninger(Long behandlingId) {
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }

    @Test
    public void skal_innsette_bekrefet_forelder_og_oppdatere_ved_endring() {
        LocalDate dødsdato = LocalDate.now();
        LocalDate fødselsdato = dødsdato.minusYears(50);
        LocalDate oppdatertDødsdato = dødsdato.plusDays(1);
        AktørId forelder = AktørId.dummy();

        Behandling behandling = lagBehandling();

        // Arrange 1. Legge til forelder 1
        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(forelder)
                    .medNavn("Navn")
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medFødselsdato(fødselsdato)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(
                informasjonBuilder
                    .getPersonstatusBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato), Landkoder.NOR, Region.NORDEN))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("NOR").medPostnummer(OSLO));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        final FamilieHendelseBuilder søknadVersjon = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .medFødselsDato(LocalDate.now().minusDays(10));
        familieGrunnlagRepository.lagre(behandling, søknadVersjon);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medSøknadsdato(LocalDate.now())
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Assert 1: Forelder 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet1 = behandlingRepository.hentBehandling(behandlingId);

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = hentSøkerPersonopplysninger(behandling.getId());
        PersonInformasjonEntitet personInformasjon = personopplysningGrunnlag.getGjeldendeVersjon();
        assertThat(personopplysningGrunnlag).isNotNull();
        assertThat(personInformasjon).isNotNull();

        // Arrange 2. Oppdater forelder 1
        informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder.leggTil(
            informasjonBuilder.getPersonopplysningBuilder(forelder)
                .medDødsdato(oppdatertDødsdato));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Assert 2: Forelder 1 er oppdatert
        Behandling opphentet2 = behandlingRepository.hentBehandling(behandlingId);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = hentSøkerPersonopplysninger(behandling.getId());
        PersonInformasjonEntitet personInformasjon1 = personopplysningGrunnlag1.getGjeldendeVersjon();
        assertThat(personopplysningGrunnlag1).isNotNull();
        assertThat(personInformasjon1).isNotNull();

        LocalDate dødsdatoFraBasen = personInformasjon1.getPersonopplysninger().stream()
            .filter(e -> e.getAktørId().equals(forelder))
            .findFirst().get().getDødsdato();

        assertThat(dødsdatoFraBasen).isEqualTo(oppdatertDødsdato);

        // Arrange 3: Anvend grunnlagsbuilder uten å gjøre endringer på bekreftet forelder
        // -> skal ikke føre til utilsiktede oppdateringer av BekreftetForeldre
        final FamilieHendelseBuilder builder = familieGrunnlagRepository.opprettBuilderFor(opphentet2)
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1);
        familieGrunnlagRepository.lagre(opphentet2, builder);

        lagreBehandling(opphentet2);

        // Assert 3: Fortsatt bare barn 1 lagret
        @SuppressWarnings("unused")
        Behandling opphentet3 = behandlingRepository.hentBehandling(behandlingId);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = hentSøkerPersonopplysninger(behandlingId);
        assertThat(personopplysningGrunnlag2).isNotNull();
        assertThat(personopplysningGrunnlag2.getGjeldendeVersjon()).isNotNull();
    }

    @Test
    public void skal_innsette_bekrefet_forelder() {
        LocalDate dødsdato = LocalDate.now();
        LocalDate fødselsdato = dødsdato.minusYears(50);
        AktørId forelder = AktørId.dummy();

        Behandling behandling = lagBehandling();

        // Arrange 1. Legge til forelder 1
        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(forelder)
                    .medNavn("Forelder 1")
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medFødselsdato(fødselsdato)
                    .medDødsdato(dødsdato)
                    .medSivilstand(SivilstandType.UGIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(
                informasjonBuilder
                    .getPersonstatusBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato), Landkoder.NOR, Region.NORDEN))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND)
                .medAdresselinje1("Utlandsadresse")
                .medLand("Sverige"))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(forelder, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("NOR").medPostnummer(OSLO));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Assert 1: Forelder 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet1 = behandlingRepository.hentBehandling(behandlingId);

        PersonopplysningGrunnlagEntitet personopplysninger = hentSøkerPersonopplysninger(behandlingId);
        PersonInformasjonEntitet personInformasjon = personopplysninger.getGjeldendeVersjon();
        assertThat(personopplysninger).isNotNull();
        assertThat(personopplysninger).isNotNull();

        assertThat(personInformasjon.getPersonopplysninger()).hasSize(1);
        assertThat(personInformasjon.getAdresser()).hasSize(2);
        assertThat(personInformasjon.getRelasjoner()).isEmpty();
        assertThat(personInformasjon.getPersonstatus()).hasSize(1);
        assertThat(personInformasjon.getStatsborgerskap()).hasSize(1);

        assertThat(personInformasjon.getPersonopplysninger().stream()
            .filter(e -> e.getAktørId().equals(forelder)).findFirst().get().getAktørId()).isEqualTo(forelder);
    }

    @Test
    public void skal_kunne_lagre_statsborgerskap_til_en_bekrefet_forelder() {
        LocalDate dødsdatoForelder1 = LocalDate.now();

        AktørId aktørId = AktørId.dummy();

        Behandling behandling = lagBehandling();

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        LocalDate fødselsdato = dødsdatoForelder1.minusYears(40);
        informasjonBuilder.leggTil(
            informasjonBuilder.getPersonopplysningBuilder(aktørId)
                .medNavn("Navn")
                .medKjønn(NavBrukerKjønn.KVINNE)
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødsdatoForelder1)
                .medSivilstand(SivilstandType.GIFT)
                .medRegion(Region.NORDEN)
        ).leggTil(informasjonBuilder
            .getPersonstatusBuilder(aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato, dødsdatoForelder1)).medPersonstatus(PersonstatusType.BOSA)
        ).leggTil(informasjonBuilder
            .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato, dødsdatoForelder1), AdresseType.BOSTEDSADRESSE)
            .medAdresselinje1("Testadresse")
            .medLand("NOR").medPostnummer("1234").medPoststed(OSLO)
        ).leggTil(informasjonBuilder
            .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato, dødsdatoForelder1), AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND)
            .medAdresselinje1("Testadresse")
            .medLand("Sverige").medPostnummer("1234")
        ).leggTil(informasjonBuilder
            .getStatsborgerskapBuilder(aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato, dødsdatoForelder1), Landkoder.NOR, Region.NORDEN)
        );

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Assert 1: Forelder 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet = behandlingRepository.hentBehandling(behandlingId);
        PersonopplysningGrunnlagEntitet personopplysninger = hentSøkerPersonopplysninger(behandlingId);
        assertThat(personopplysninger).isNotNull();

        PersonInformasjonEntitet personInformasjon = personopplysninger.getGjeldendeVersjon();
        assertThat(personInformasjon.getPersonopplysninger()).hasSize(1);
        assertThat(personInformasjon.getAdresser()).hasSize(2);
        assertThat(personInformasjon.getRelasjoner()).isEmpty();
        assertThat(personInformasjon.getPersonstatus()).hasSize(1);
        assertThat(personInformasjon.getStatsborgerskap()).hasSize(1);

        assertThat(personInformasjon.getPersonstatus().get(0).getPersonstatus()).isEqualTo(PersonstatusType.BOSA);

        StatsborgerskapEntitet statsborgerskap = personInformasjon.getStatsborgerskap().get(0);
        assertThat(statsborgerskap.getStatsborgerskap()).isEqualTo(Landkoder.NOR);

        // Assert på de øvrige attributter
        PersonopplysningEntitet personopplysning = personInformasjon.getPersonopplysninger().get(0);
        assertThat(personopplysning.getKjønn()).isEqualTo(NavBrukerKjønn.KVINNE);
        assertThat(personopplysning.getDødsdato()).isEqualTo(dødsdatoForelder1);
        assertThat(personopplysning.getNavn()).isEqualTo("Navn");
    }

    @Test
    public void skal_kunne_lagre_medlemskap_perioder() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(100);
        LocalDate beslutningsdato = LocalDate.now().minusDays(10);

        MedlemskapPerioderEntitet medlemskapPerioder1 = new MedlemskapPerioderBuilder()
            .medPeriode(fom, tom)
            .medMedlemskapType(MedlemskapType.FORELOPIG)
            .medDekningType(MedlemskapDekningType.FTL_2_7_a)
            .medKildeType(MedlemskapKildeType.FS22)
            .medBeslutningsdato(beslutningsdato)
            .build();

        MedlemskapPerioderEntitet medlemskapPerioder2 = new MedlemskapPerioderBuilder()
            .medPeriode(fom, tom)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medDekningType(MedlemskapDekningType.FTL_2_7_b)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(beslutningsdato)
            .build();

        Behandling behandling = lagBehandling();

        Long behandlingId = behandling.getId();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(medlemskapPerioder1, medlemskapPerioder2));

        // Assert
        Set<MedlemskapPerioderEntitet> medlemskapPerioders = medlemskapRepository.hentMedlemskap(behandlingId).get().getRegistrertMedlemskapPerioder();
        assertThat(medlemskapPerioders).hasSize(2);
        assertThat(medlemskapPerioders).containsExactlyInAnyOrder(medlemskapPerioder1, medlemskapPerioder2);
    }

    @Test
    public void skal_kunne_lagre_medlemskap() {
        Behandling behandling = lagBehandling();

        VurdertMedlemskap vurdertMedlemskap = new VurdertMedlemskapBuilder()
            .medOppholdsrettVurdering(true)
            .medLovligOppholdVurdering(true)
            .medBosattVurdering(true)
            .build();

        MedlemskapRepository medlemskapRepository = new MedlemskapRepository(getEntityManager());

        // Act
        Long behandlingId = behandling.getId();
        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, vurdertMedlemskap);

        // Assert
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        assertThat(medlemskap).isNotNull().isPresent();
        assertThat(medlemskap.get().getVurdertMedlemskap().get()).isEqualTo(vurdertMedlemskap);
    }

    private Behandling lagBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        fagsakRepository.opprettNy(fagsak);
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);
        return behandling;
    }

}
