package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

public class MedlemskapRepositoryTest extends EntityManagerAwareTest {

    private MedlemskapRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repository = new MedlemskapRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void skal_hente_eldste_versjon_av_aggregat() {
        Behandling behandling = lagBehandling();
        MedlemskapPerioderEntitet perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.FORELOPIG).build();
        Long behandlingId = behandling.getId();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        Optional<MedlemskapAggregat> medlemskapAggregat = repository.hentMedlemskap(behandlingId);
        Optional<MedlemskapAggregat> førsteVersjonMedlemskapAggregat = repository.hentFørsteVersjonAvMedlemskap(behandlingId);

        MedlemskapPerioderEntitet perioderEntitet = medlemskapAggregat.get().getRegistrertMedlemskapPerioder()
                .stream().findFirst().get();
        MedlemskapPerioderEntitet førstePerioderEntitet = førsteVersjonMedlemskapAggregat.get()
                .getRegistrertMedlemskapPerioder().stream().findFirst().get();

        assertThat(medlemskapAggregat.get()).isNotEqualTo(førsteVersjonMedlemskapAggregat.get());
        assertThat(perioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.ENDELIG);
        assertThat(førstePerioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.FORELOPIG);
    }

    @Test
    public void skal_lagre_vurdering_av_løpende_medlemskap() {
        Behandling behandling = lagBehandling();
        LocalDate vurderingsdato = LocalDate.now();
        VurdertMedlemskapPeriodeEntitet.Builder builder = new VurdertMedlemskapPeriodeEntitet.Builder();
        VurdertLøpendeMedlemskapBuilder løpendeMedlemskapBuilder = builder.getBuilderFor(vurderingsdato);

        løpendeMedlemskapBuilder.medBosattVurdering(true);
        løpendeMedlemskapBuilder.medVurderingsdato(LocalDate.now());

        builder.leggTil(løpendeMedlemskapBuilder);

        VurdertMedlemskapPeriodeEntitet hvaSkalLagres = builder.build();
        repository.lagreLøpendeMedlemskapVurdering(behandling.getId(), hvaSkalLagres);

        Optional<MedlemskapAggregat> medlemskapAggregat = repository.hentMedlemskap(behandling.getId());
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.get().getVurderingLøpendeMedlemskap()).contains(hvaSkalLagres);
    }

    private Behandling lagBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
