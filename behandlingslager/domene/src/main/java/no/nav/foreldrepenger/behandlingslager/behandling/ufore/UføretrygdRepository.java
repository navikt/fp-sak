package no.nav.foreldrepenger.behandlingslager.behandling.ufore;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class UføretrygdRepository {

    private EntityManager entityManager;

    @Inject
    public UføretrygdRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    UføretrygdRepository() {
        // CDI proxy
    }

    public void lagreUføreGrunnlagRegisterVersjon(Long behandlingId, AktørId aktørId, boolean erUføretrygdet, LocalDate uføredato, LocalDate virkningsdato) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medBehandlingId(behandlingId)
            .medAktørIdUføretrygdet(aktørId)
            .medRegisterUføretrygd(erUføretrygdet, uføredato, virkningsdato);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag);
    }

    public void lagreUføreGrunnlagOverstyrtVersjon(Long behandlingId, boolean erUføretrygdet) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        if (aktivtGrunnlag.isEmpty()) throw new IllegalStateException("Utviklerfeil - finnes ikke UFO-grunnlag");
        var nyttGrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medBehandlingId(behandlingId)
            .medOverstyrtUføretrygd(erUføretrygdet);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag);
    }

    public void lagreUføreGrunnlagAvkreftetAleneomsorgVersjon(Long behandlingId, AktørId annenpartAktørId, boolean mottarUføretrygd) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medBehandlingId(behandlingId)
            .medAktørIdUføretrygdet(annenpartAktørId)
            .medManueltAvklartUføretrygd(mottarUføretrygd);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag);
    }

    private void lagreGrunnlag(Optional<UføretrygdGrunnlagEntitet> aktivtGrunnlag, UføretrygdGrunnlagEntitet.Builder builder) {
        var nyttGrunnlag = builder.build();

        if (!Objects.equals(aktivtGrunnlag.orElse(null), nyttGrunnlag)) {
            aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
                eksisterendeGrunnlag.deaktiver();
                entityManager.persist(eksisterendeGrunnlag);
            });
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        }
    }

    public Optional<UføretrygdGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "FROM UforetrygdGrunnlag u WHERE u.behandlingId = :behandlingId AND u.aktiv = true",
                    UføretrygdGrunnlagEntitet.class)
            .setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentGrunnlag(orginalBehandlingId);
        eksisterendeGrunnlag.ifPresent(g -> {
            var nyttgrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(eksisterendeGrunnlag)
                .medBehandlingId(nyBehandlingId);
            lagreGrunnlag(Optional.empty(), nyttgrunnlag);
        });
    }

}
