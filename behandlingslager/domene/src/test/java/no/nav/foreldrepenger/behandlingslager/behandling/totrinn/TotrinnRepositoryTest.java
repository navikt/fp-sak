package no.nav.foreldrepenger.behandlingslager.behandling.totrinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class TotrinnRepositoryTest extends EntityManagerAwareTest {

    private TotrinnRepository totrinnRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;

    @BeforeEach
    public void setup() {
        entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        totrinnRepository = new TotrinnRepository(entityManager);
    }

    @Test
    void skal_finne_ett_inaktivt_totrinnsgrunnlag_og_ett_aktivt_totrinnsgrunnlag() {

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var gammeltTotrinnresultatgrunnlag = new Totrinnresultatgrunnlag(behandling, null, null,
            null, null);

        var nyttTotrinnresultatgrunnlag = new Totrinnresultatgrunnlag(behandling, null, null, null,
            null);

        totrinnRepository.lagreOgFlush(gammeltTotrinnresultatgrunnlag);
        totrinnRepository.lagreOgFlush(nyttTotrinnresultatgrunnlag);

        // Hent ut aktiv totrinnsgrunnlag
        var optionalNyttTotrinnresultatgrunnlag = totrinnRepository.hentTotrinngrunnlag(behandling.getId());

        // Hent ut inaktive totrinnsgrunnlag
        var query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = false ",

            Totrinnresultatgrunnlag.class);
        query.setParameter("behandling_id", behandling.getId());
        var inaktive = query.getResultList();

        assertThat(inaktive).hasSize(1);
        assertThat(inaktive.get(0)).isEqualToComparingFieldByField(gammeltTotrinnresultatgrunnlag);
        assertThat(optionalNyttTotrinnresultatgrunnlag).isPresent();
        assertThat(optionalNyttTotrinnresultatgrunnlag.get().getId()).isNotEqualTo(
            gammeltTotrinnresultatgrunnlag.getId());
        assertThat(optionalNyttTotrinnresultatgrunnlag.get().isAktiv()).isTrue();

    }

    @Test
    void skal_finne_flere_inaktive_totrinnsvurderinger_og_flere_aktive_totrinnsvurdering() {

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Opprett vurderinger som skal være inaktive
        var inaktivTotrinnsvurdering1 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, true, "", VurderÅrsak.FEIL_FAKTA);
        var inaktivTotrinnsvurdering2 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, true, "", VurderÅrsak.FEIL_FAKTA);
        var inaktivTotrinnsvurdering3 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, true, "", VurderÅrsak.FEIL_FAKTA);

        List<Totrinnsvurdering> inaktivTotrinnsvurderingList = new ArrayList<>();
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering1);
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering2);
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering3);
        totrinnRepository.lagreOgFlush(inaktivTotrinnsvurderingList);

        // Opprett vurderinger som skal være aktive
        var aktivTotrinnsvurdering1 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, false, "", VurderÅrsak.FEIL_FAKTA);
        var aktivTotrinnsvurdering2 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, false, "", VurderÅrsak.FEIL_FAKTA);
        var aktivTotrinnsvurdering3 = lagTotrinnsvurdering(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, false, "", VurderÅrsak.FEIL_FAKTA);

        List<Totrinnsvurdering> aktivTotrinnsvurderingList = new ArrayList<>();
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering1);
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering2);
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering3);
        totrinnRepository.lagreOgFlush(aktivTotrinnsvurderingList);

        // Hent aktive vurderinger etter flush
        var repoAktiveTotrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling.getId());

        // Hent inaktive vurderinger etter flush
        var query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = false",

            Totrinnsvurdering.class);
        query.setParameter("behandling_id", behandling.getId());
        var repoInaktiveTotrinnsvurderinger = query.getResultList();

        // Sjekk lagrede aktive vurderinger
        assertThat(repoAktiveTotrinnsvurderinger).hasSize(3);
        repoAktiveTotrinnsvurderinger.forEach(totrinnsvurdering -> assertThat(totrinnsvurdering.isAktiv()).isTrue());

        // Sjekk lagrede inaktive vurderinger
        assertThat(repoInaktiveTotrinnsvurderinger).hasSize(3);
        repoInaktiveTotrinnsvurderinger.forEach(totrinnsvurdering -> assertThat(totrinnsvurdering.isAktiv()).isFalse());

    }

    private Totrinnsvurdering lagTotrinnsvurdering(Behandling behandling,
                                                   AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                   boolean godkjent,
                                                   String begrunnelse,
                                                   VurderÅrsak vurderÅrsak) {
        return new Totrinnsvurdering.Builder(behandling, aksjonspunktDefinisjon).medGodkjent(godkjent)
            .medBegrunnelse(begrunnelse)
            .medVurderÅrsak(vurderÅrsak)
            .build();
    }

}
