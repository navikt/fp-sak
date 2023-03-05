package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class FamilieHendelseRepositoryTest extends EntityManagerAwareTest {

    private FamilieHendelseRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repository = new FamilieHendelseRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_lage_søknad_versjon() {
        final var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final var builder = Behandling.forFørstegangssøknad(fagsak);
        final var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        final var hendelseBuilder = repository.opprettBuilderFor(behandling);
        hendelseBuilder.medFødselsDato(LocalDate.now()).medAntallBarn(1);
        repository.lagre(behandling, hendelseBuilder);

        var familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());

        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getBekreftetVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon()).isNotNull();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getAntallBarn()).isEqualTo(1);

        final var hendelseBuilder1 = repository.opprettBuilderFor(behandling);
        hendelseBuilder1.leggTilBarn(LocalDate.now()).medAntallBarn(2);
        repository.lagre(behandling, hendelseBuilder1);

        familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());

        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getBekreftetVersjon()).isPresent();
        assertThat(familieHendelseGrunnlag.getBekreftetVersjon().get().getBarna()).hasSize(2);
        assertThat(familieHendelseGrunnlag.getBekreftetVersjon().get().getAntallBarn()).isEqualTo(2);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon()).isNotNull();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getAntallBarn()).isEqualTo(1);
    }

    @Test
    void skal_lagre_med_endring_i_vilkår() {
        final var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final var builder = Behandling.forFørstegangssøknad(fagsak);
        final var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var hendelseBuilder = repository.opprettBuilderFor(behandling);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now()))
            .erOmsorgovertagelse()
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1);
        repository.lagre(behandling, hendelseBuilder);
        var familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());

        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getBekreftetVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon()).isNotNull();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getAntallBarn()).isEqualTo(1);

        hendelseBuilder = repository.opprettBuilderFor(behandling);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD));
        repository.lagreOverstyrtHendelse(behandling, hendelseBuilder);

        familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());

        assertThat(familieHendelseGrunnlag.getSøknadVersjon()).isNotNull();
        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon().get().getAdopsjon().get().getOmsorgovertakelseVilkår())
            .isEqualTo(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getAntallBarn()).isEqualTo(1);

        final var hendelseBuilder1 = repository.opprettBuilderFor(behandling);
        hendelseBuilder1.medAdopsjon(hendelseBuilder1.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD));
        repository.lagre(behandling, hendelseBuilder1);

        familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());

        assertThat(familieHendelseGrunnlag.getBekreftetVersjon()).isNotPresent();
        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon().get().getAdopsjon()).isPresent();
        assertThat(familieHendelseGrunnlag.getOverstyrtVersjon().get().getAdopsjon().get().getOmsorgovertakelseVilkår())
            .isEqualTo(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon()).isNotNull();
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(familieHendelseGrunnlag.getSøknadVersjon().getAntallBarn()).isEqualTo(1);
    }

    @Test
    void skal_hente_eldste_versjon_av_aggregat() {
        final var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final var builder = Behandling.forFørstegangssøknad(fagsak);
        final var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var hendelseBuilder = repository.opprettBuilderFor(behandling);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now()))
            .erOmsorgovertagelse()
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1);
        repository.lagre(behandling, hendelseBuilder);

        var grunnlagIdFørste = repository.hentIdPåAktivFamiliehendelse(behandling.getId());

        hendelseBuilder = repository.opprettBuilderFor(behandling);
        hendelseBuilder.medAdopsjon(hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET));
        repository.lagreOverstyrtHendelse(behandling, hendelseBuilder);

        final var hendelseBuilder1 = repository.opprettBuilderFor(behandling);
        hendelseBuilder1.medAdopsjon(hendelseBuilder1.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD));
        repository.lagre(behandling, hendelseBuilder1);

        final var hendelseBuilder2 = repository.opprettBuilderFor(behandling);
        hendelseBuilder2.medAdopsjon(hendelseBuilder2.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD));
        repository.lagre(behandling, hendelseBuilder2);

        var familieHendelseGrunnlag = repository.hentAggregat(behandling.getId());
        var førsteVersjonFamilieHendelseAggregat = repository.hentGrunnlagPåId(grunnlagIdFørste.get());

        assertThat(familieHendelseGrunnlag).isNotEqualTo(førsteVersjonFamilieHendelseAggregat);
        assertThat(familieHendelseGrunnlag.getErAktivt()).isTrue();
        assertThat(førsteVersjonFamilieHendelseAggregat.getErAktivt()).isFalse();
        assertThat((førsteVersjonFamilieHendelseAggregat).getSøknadVersjon().getAdopsjon().get().getOmsorgovertakelseVilkår()).isEqualTo(OmsorgsovertakelseVilkårType.UDEFINERT);
    }
}
