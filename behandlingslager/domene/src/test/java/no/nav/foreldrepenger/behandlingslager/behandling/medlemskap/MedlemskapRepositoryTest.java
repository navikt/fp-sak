package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MedlemskapRepositoryTest extends EntityManagerAwareTest {

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
    void skal_hente_eldste_versjon_av_aggregat() {
        var behandling = lagBehandling();
        var perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.FORELOPIG).build();
        var behandlingId = behandling.getId();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        var medlemskapAggregat = repository.hentMedlemskap(behandlingId);
        var førsteVersjonMedlemskapAggregat = repository.hentFørsteVersjonAvMedlemskap(behandlingId);

        var perioderEntitet = medlemskapAggregat.get().getRegistrertMedlemskapPerioder()
                .stream().findFirst().get();
        var førstePerioderEntitet = førsteVersjonMedlemskapAggregat.get()
                .getRegistrertMedlemskapPerioder().stream().findFirst().get();

        assertThat(medlemskapAggregat.get()).isNotEqualTo(førsteVersjonMedlemskapAggregat.get());
        assertThat(perioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.ENDELIG);
        assertThat(førstePerioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.FORELOPIG);
    }

    @Test
    void skal_lagre_vurdering_av_løpende_medlemskap() {
        var behandling = lagBehandling();
        var vurderingsdato = LocalDate.now();
        var builder = new VurdertMedlemskapPeriodeEntitet.Builder();
        var løpendeMedlemskapBuilder = builder.getBuilderFor(vurderingsdato);

        løpendeMedlemskapBuilder.medBosattVurdering(true);
        løpendeMedlemskapBuilder.medVurderingsdato(LocalDate.now());

        builder.leggTil(løpendeMedlemskapBuilder);

        var hvaSkalLagres = builder.build();
        repository.lagreLøpendeMedlemskapVurdering(behandling.getId(), hvaSkalLagres);

        var medlemskapAggregat = repository.hentMedlemskap(behandling.getId());
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.get().getVurderingLøpendeMedlemskap()).contains(hvaSkalLagres);
    }

    private Behandling lagBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
